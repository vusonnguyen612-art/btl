package Controller;

import Controller.utils.FormatUtils;
import Controller.utils.UIUtils;
import Model.AuctionSession;
import DAO.AuctionDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Controller cho card phiên đấu giá: hiển thị thông tin, nút Vào phòng / Bắt đầu, countdown. */
public class    AuctionCardController {

    @FXML private VBox cardRoot;
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
    private Runnable onEditAuction;
    private Label timeRemainingLabel;
    private Timeline timeUpdateTimeline;



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

    /** Dừng bộ đếm thời gian countdown nếu đang chạy. */
    private void stopTimeUpdate() {
        if (timeUpdateTimeline != null) {
            timeUpdateTimeline.stop();
            timeUpdateTimeline = null;
        }
    }

    /** Khởi chạy bộ đếm thời gian countdown, cập nhật nhãn thời gian mỗi giây. */
    private void startTimeUpdate() {
        stopTimeUpdate();
        timeUpdateTimeline = new Timeline();
        timeUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        timeUpdateTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), e -> updateTimeLabel())
        );
        timeUpdateTimeline.play();
    }

    /** Cập nhật nội dung nhãn hiển thị thời gian còn lại của phiên đấu giá. */
    private void updateTimeLabel() {
        if (timeRemainingLabel == null || auction == null) return;
        timeRemainingLabel.setText(FormatUtils.getTimeRemaining(auction));
    }

    /** Đăng ký callback khi nhấn "Vào phòng". */
    public void setOnSelectAuction(Runnable callback) {
        this.onSelectAuction = callback;
    }

    /** Đăng ký callback khi nhấn "Bắt đầu". */
    public void setOnStartAuction(Runnable callback) {
        this.onStartAuction = callback;
    }

    /** Đăng ký callback khi nhấn "Chỉnh sửa". */
    public void setOnEditAuction(Runnable callback) {
        this.onEditAuction = callback;
    }

    /** Cập nhật toàn bộ giao diện card: tên vật phẩm, giá, trạng thái, nút thao tác và countdown. */
    private void updateCard(boolean isRunning, boolean canStart, boolean isPaymentPending) {
        String itemName = auction.getItem() != null ? auction.getItem().getName() : "Unknown";
        nameLabel.setText(itemName);
        updateItemImage();

        priceLabel.setText("Giá hiện tại: " + FormatUtils.formatMoney(auction.getCurrentPrice()) + " $");

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

        String durationText = FormatUtils.formatDuration(auction.getDurationMinutes());
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
            startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;");
            startBtn.setPrefSize(100, 35);
            startBtn.setOnAction(e -> {
                if (onStartAuction != null) onStartAuction.run();
            });
            actionBox.getChildren().add(startBtn);

            Button editBtn = new Button("Sửa");
            editBtn.setStyle("-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;");
            editBtn.setPrefSize(80, 35);
            editBtn.setOnAction(e -> {
                if (onEditAuction != null) onEditAuction.run();
            });
            actionBox.getChildren().add(editBtn);
        } else if (auction.isOpen()) {
            Button startBtn = new Button("Bắt đầu");
            startBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: #999999; -fx-background-radius: 30; -fx-font-size: 14px;");
            startBtn.setPrefSize(100, 35);
            startBtn.setDisable(true);
            actionBox.getChildren().add(startBtn);
        }
    }

    /** Tải và hiển thị hình ảnh vật phẩm trong phiên đấu giá lên ImageView. */
    private void updateItemImage() {
        if (auction.getItem() == null) {
            UIUtils.resolveAndSetImage(itemImageView, null);
            return;
        }
        UIUtils.resolveAndSetImage(itemImageView, auction.getItem().getImagePath());
    }

    /** @return root VBox của card */
    public VBox getRoot() {
        return cardRoot;
    }
}
