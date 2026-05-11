package Controller;

import Model.AuctionSession;
import DAO.AuctionDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Controller cho card phiên đấu giá: hiển thị thông tin, nút Vào phòng / Bắt đầu, countdown. */
public class AuctionCardController {

    @FXML private HBox cardRoot;
    @FXML private ImageView itemImageView;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label statusLabel;
    @FXML private Label timeInfoLabel;
    @FXML private VBox actionBox;

    private AuctionSession auction;
    private AuctionDAO auctionDAO = new AuctionDAO();
    private Runnable onSelectAuction;
    private Runnable onStartAuction;
    private Label timeRemainingLabel;
    private Timeline timeUpdateTimeline;

    private static final DecimalFormat moneyFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        moneyFormat = new DecimalFormat("#,###", symbols);
    }

    /** Gán dữ liệu phiên (3 tham số). */
    public void setAuction(AuctionSession auction, boolean isRunning, boolean canStart) {
        this.auction = auction;
        stopTimeUpdate();
        updateCard(isRunning, canStart, false);
    }

    /** Gán dữ liệu phiên (4 tham số, có isPaymentPending). */
    public void setAuction(AuctionSession auction, boolean isRunning, boolean canStart, boolean isPaymentPending) {
        this.auction = auction;
        stopTimeUpdate();
        updateCard(isRunning, canStart, isPaymentPending);
    }

    private void stopTimeUpdate() {
        if (timeUpdateTimeline != null) {
            timeUpdateTimeline.stop();
            timeUpdateTimeline = null;
        }
    }

    private void startTimeUpdate() {
        stopTimeUpdate();
        timeUpdateTimeline = new Timeline();
        timeUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        timeUpdateTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), e -> updateTimeLabel())
        );
        timeUpdateTimeline.play();
    }

    private void updateTimeLabel() {
        if (timeRemainingLabel == null || auction == null) return;
        timeRemainingLabel.setText(getTimeRemaining(auction));
    }

    /** Đăng ký callback khi nhấn "Vào phòng". */
    public void setOnSelectAuction(Runnable callback) {
        this.onSelectAuction = callback;
    }

    /** Đăng ký callback khi nhấn "Bắt đầu". */
    public void setOnStartAuction(Runnable callback) {
        this.onStartAuction = callback;
    }

    private void updateCard(boolean isRunning, boolean canStart, boolean isPaymentPending) {
        String itemName = auction.getItem() != null ? auction.getItem().getName() : "Unknown";
        nameLabel.setText(itemName);
        updateItemImage();

        priceLabel.setText("Giá hiện tại: " + moneyFormat.format(auction.getCurrentPrice()) + " $");

        String statusText;
        String statusColor;
        if (auction.isPaid()) {
            statusText = "ĐÃ BÁN";
            statusColor = "#9E9E9E";
        } else if (auction.isFinished()) {
            statusText = "ĐÃ KẾT THÚC";
            statusColor = "#888888";
        } else if (auction.isCanceled()) {
            statusText = "ĐÃ HỦY";
            statusColor = "#ff6b6b";
        } else if (isPaymentPending) {
            statusText = "CHỜ THANH TOÁN";
            statusColor = "#FF9800";
        } else if (isRunning) {
            statusText = "ĐANG DIỄN RA";
            statusColor = "#4CAF50";
        } else {
            statusText = "CHƯA BẮT ĐẦU";
            statusColor = "#FF9800";
        }
        statusLabel.setText(statusText);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        String durationText = formatDuration(auction.getDurationMinutes());
        timeInfoLabel.setText("Thời gian: " + durationText);

        actionBox.getChildren().clear();

        if (isRunning) {
            Button joinBtn = new Button("Vào phòng");
            joinBtn.setStyle("-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;");
            joinBtn.setPrefSize(100, 35);
            joinBtn.setOnAction(e -> {
                if (onSelectAuction != null) onSelectAuction.run();
            });
            actionBox.getChildren().add(joinBtn);

            timeRemainingLabel = new Label();
            timeRemainingLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px; -fx-font-weight: bold;");
            actionBox.getChildren().add(timeRemainingLabel);
            updateTimeLabel();
            startTimeUpdate();
        } else if (isPaymentPending) {
            Button viewBtn = new Button("Xem");
            viewBtn.setStyle("-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;");
            viewBtn.setPrefSize(100, 35);
            viewBtn.setOnAction(e -> {
                if (onSelectAuction != null) onSelectAuction.run();
            });
            actionBox.getChildren().add(viewBtn);
        } else if (auction.isOpen() && canStart) {
            Button startBtn = new Button("Bắt đầu");
            startBtn.setStyle("-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;");
            startBtn.setPrefSize(100, 35);
            startBtn.setOnAction(e -> {
                if (onStartAuction != null) onStartAuction.run();
            });
            actionBox.getChildren().add(startBtn);
        } else if (auction.isOpen()) {
            Button startBtn = new Button("Bắt đầu");
            startBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: #999999; -fx-background-radius: 30; -fx-font-size: 14px;");
            startBtn.setPrefSize(100, 35);
            startBtn.setDisable(true);
            actionBox.getChildren().add(startBtn);
        }
    }

    private void updateItemImage() {
        if (auction.getItem() == null || auction.getItem().getImagePath() == null || auction.getItem().getImagePath().isBlank()) {
            itemImageView.setImage(null);
            itemImageView.setVisible(false);
            itemImageView.setManaged(false);
            return;
        }

        String imagePath = auction.getItem().getImagePath();
        String imageUrl;
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            imageUrl = imageFile.toURI().toString();
        } else if (imagePath.startsWith("file:") || imagePath.startsWith("http:") || imagePath.startsWith("https:")) {
            imageUrl = imagePath;
        } else {
            itemImageView.setImage(null);
            itemImageView.setVisible(false);
            itemImageView.setManaged(false);
            return;
        }

        itemImageView.setImage(new Image(imageUrl, true));
        itemImageView.setVisible(true);
        itemImageView.setManaged(true);
    }

    private String formatDuration(long minutes) {
        if (minutes >= 60) {
            long hours = minutes / 60;
            long mins = minutes % 60;
            if (mins > 0) {
                return hours + "h " + mins + "p";
            }
            return hours + "h";
        }
        return minutes + " phút";
    }

    private String getTimeRemaining(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Đã kết thúc";
        }

        long totalSeconds = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("Còn %02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("Còn %02d:%02d", minutes, seconds);
        } else {
            return String.format("Còn %ds", seconds);
        }
    }

    /** @return root HBox của card */
    public HBox getRoot() {
        return cardRoot;
    }
}
