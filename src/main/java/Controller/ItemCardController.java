package Controller;

import Model.Item;
import Model.AuctionSession;
import DAO.AuctionDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ItemCardController {

    @FXML private HBox cardRoot;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private VBox actionBox;

    private AuctionDAO auctionDAO = new AuctionDAO();

    private static final DecimalFormat moneyFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        moneyFormat = new DecimalFormat("#,###", symbols);
    }

    public void setItem(Item item) {
        this.setItem(item, null);
    }

    public void setItem(Item item, List<AuctionSession> auctions) {
        nameLabel.setText(item.getName());

        BigDecimal price = new BigDecimal(String.valueOf(item.getStartPrice()));
        priceLabel.setText(moneyFormat.format(price) + " $");

        actionBox.getChildren().clear();

        AuctionSession itemAuction = null;
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
            String statusText = itemAuction.isRunning() ? "DANG DIEN RA" : itemAuction.isOpen() ? "CHUA BAT DAU" : "DA KET THUC";
            Label statusLabel = new Label(statusText);
            statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            BigDecimal currentPrice = new BigDecimal(String.valueOf(itemAuction.getCurrentPrice()));
            Label currentPriceLabel = new Label("Gia hien tai: " + moneyFormat.format(currentPrice) + " $");
            currentPriceLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13px;");

            String timeText = itemAuction.isRunning() && itemAuction.getEndTime() != null
                    ? "Con: " + getRemainingTime(itemAuction)
                    : "Thoi gian: " + formatDuration(itemAuction.getDurationMinutes());
            Label timeLabel = new Label(timeText);
            timeLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");

            actionBox.getChildren().addAll(statusLabel, currentPriceLabel, timeLabel);
        } else {
            Label noAuctionLabel = new Label("Chua co phien dau gia");
            noAuctionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
            actionBox.getChildren().add(noAuctionLabel);
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
        return minutes + " phut";
    }

    private String getRemainingTime(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Da ket thuc";
        }

        long minutes = java.time.Duration.between(now, auction.getEndTime()).toMinutes();
        long seconds = java.time.Duration.between(now, auction.getEndTime()).getSeconds() % 60;

        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public HBox getRoot() {
        return cardRoot;
    }
}
