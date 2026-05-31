package Controller;

import Controller.utils.FormatUtils;
import Controller.utils.UIUtils;
import Model.Item;
import Model.AuctionSession;
import DAO.AuctionDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.List;

/** Controller cho card hiển thị thông tin vật phẩm kèm trạng thái phiên đấu giá và countdown. */
public class ItemCardController {

    @FXML private VBox cardRoot;
    @FXML private ImageView itemImageView;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private VBox actionBox;

    private AuctionDAO auctionDAO = new AuctionDAO();
    private AuctionSession itemAuction;
    private Label timeLabel;
    private Timeline timeUpdateTimeline;

    /** Gán item (không có thông tin auction). */
    public void setItem(Item item) {
        this.setItem(item, null);
    }

    /** Gán item và danh sách auction để hiển thị trạng thái + countdown nếu đang chạy. */
    public void setItem(Item item, List<AuctionSession> auctions) {
        stopTimeUpdate();

        updateItemImage(item);
        nameLabel.setText(item.getName());

        BigDecimal price = new BigDecimal(String.valueOf(item.getStartPrice()));
        priceLabel.setText(FormatUtils.formatMoney(price) + " $");

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
            Label currentPriceLabel = new Label("Giá hiện tại: " + FormatUtils.formatMoney(currentPrice) + " $");
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

    /** Tải và hiển thị hình ảnh vật phẩm lên ImageView trong card. */
    private void updateItemImage(Item item) {
        UIUtils.resolveAndSetImage(itemImageView, item.getImagePath());
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

    /** Cập nhật nội dung nhãn thời gian: hiển thị thời gian còn lại nếu phiên đang chạy, ngược lại hiển thị tổng thời lượng. */
    private void updateTimeLabel() {
        if (timeLabel == null || itemAuction == null) return;
        if (itemAuction.isRunning() && itemAuction.getEndTime() != null) {
            timeLabel.setText("Còn: " + FormatUtils.getRemainingTimeShort(itemAuction));
        } else {
            timeLabel.setText("Thời gian: " + FormatUtils.formatDuration(itemAuction.getDurationMinutes()));
        }
    }

    /** @return root VBox của card */
    public VBox getRoot() {
        return cardRoot;
    }
}
