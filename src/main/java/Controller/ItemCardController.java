package Controller;

import Model.Item;
import Model.AuctionSession;
import DAO.AuctionDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/** Controller cho card hiển thị thông tin vật phẩm kèm trạng thái phiên đấu giá và countdown. */
public class ItemCardController {

    @FXML private HBox cardRoot;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private VBox actionBox;

    private AuctionDAO auctionDAO = new AuctionDAO();
    private AuctionSession itemAuction;
    private Label timeLabel;
    private Timeline timeUpdateTimeline;

    private static final DecimalFormat moneyFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        moneyFormat = new DecimalFormat("#,###", symbols);
    }

    /** Gán item (không có thông tin auction). */
    public void setItem(Item item) {
        this.setItem(item, null);
    }

    /** Gán item và danh sách auction để hiển thị trạng thái + countdown nếu đang chạy. */
    public void setItem(Item item, List<AuctionSession> auctions) {
        stopTimeUpdate();

        nameLabel.setText(item.getName());

        BigDecimal price = new BigDecimal(String.valueOf(item.getStartPrice()));
        priceLabel.setText(moneyFormat.format(price) + " $");

        actionBox.getChildren().clear();

        itemAuction = null;
        if (auctions != null) {
            for (AuctionSession auction : auctions) {
                if (auction.getItem() != null && auction.getItem().getId().equals(item.getId())) {
                    itemAuction = auction;
                    break;
                }
            }
        }

        if (itemAuction != null) {
            String statusColor = itemAuction.isRunning() ? "#4CAF50" : "#FF9800";
            String statusText = itemAuction.isRunning() ? "ĐANG DIỄN RA" : itemAuction.isOpen() ? "CHƯA BẮT ĐẦU" : "ĐÃ KẾT THÚC";
            Label statusLabel = new Label(statusText);
            statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            BigDecimal currentPrice = new BigDecimal(String.valueOf(itemAuction.getCurrentPrice()));
            Label currentPriceLabel = new Label("Gia hien tai: " + moneyFormat.format(currentPrice) + " $");
            currentPriceLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13px;");

            timeLabel = new Label();
            timeLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");
            updateTimeLabel();

            actionBox.getChildren().addAll(statusLabel, currentPriceLabel, timeLabel);

            if (itemAuction.isRunning()) {
                startTimeUpdate();
            }
        } else {
            Label noAuctionLabel = new Label("Chưa có phiên đấu giá");
            noAuctionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
            actionBox.getChildren().add(noAuctionLabel);
        }
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
        if (timeLabel == null || itemAuction == null) return;
        if (itemAuction.isRunning() && itemAuction.getEndTime() != null) {
            timeLabel.setText("Còn: " + getRemainingTime(itemAuction));
        } else {
            timeLabel.setText("Thời gian: " + formatDuration(itemAuction.getDurationMinutes()));
        }
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

    private String getRemainingTime(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Đã kết thúc";
        }

        long minutes = java.time.Duration.between(now, auction.getEndTime()).toMinutes();
        long seconds = java.time.Duration.between(now, auction.getEndTime()).getSeconds() % 60;

        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /** @return root HBox của card */
    public HBox getRoot() {
        return cardRoot;
    }
}
