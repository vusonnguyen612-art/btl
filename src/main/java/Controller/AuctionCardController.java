package Controller;

import Model.AuctionSession;
import DAO.AuctionDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AuctionCardController {

    @FXML private HBox cardRoot;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label statusLabel;
    @FXML private Label timeInfoLabel;
    @FXML private VBox actionBox;

    private AuctionSession auction;
    private AuctionDAO auctionDAO = new AuctionDAO();
    private Runnable onSelectAuction;
    private Runnable onStartAuction;

    private static final DecimalFormat moneyFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        moneyFormat = new DecimalFormat("#,###", symbols);
    }

    public void setAuction(AuctionSession auction, boolean isRunning, boolean canStart) {
        this.auction = auction;
        updateCard(isRunning, canStart);
    }

    public void setOnSelectAuction(Runnable callback) {
        this.onSelectAuction = callback;
    }

    public void setOnStartAuction(Runnable callback) {
        this.onStartAuction = callback;
    }

    private void updateCard(boolean isRunning, boolean canStart) {
        String itemName = auction.getItem() != null ? auction.getItem().getName() : "Unknown";
        nameLabel.setText(itemName);

        priceLabel.setText("GIÁ HIỆN TẠI: " + moneyFormat.format(auction.getCurrentPrice()) + " $");

        String statusText = isRunning ? "ĐANG DIỄN RA" : "CHƯA BẮT ĐẦU";
        String statusColor = isRunning ? "#4CAF50" : "#FF9800";
        statusLabel.setText(statusText);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        String durationText = formatDuration(auction.getDurationMinutes());
        timeInfoLabel.setText("THỜI GIAN: " + durationText);

        actionBox.getChildren().clear();

        if (isRunning) {
            Button joinBtn = new Button("VÀO PHÒNG");
            joinBtn.setStyle("-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;");
            joinBtn.setPrefSize(100, 35);
            joinBtn.setOnAction(e -> {
                if (onSelectAuction != null) onSelectAuction.run();
            });
            actionBox.getChildren().add(joinBtn);

            String timeRemaining = getTimeRemaining(auction);
            Label timeLabel = new Label(timeRemaining);
            timeLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px; -fx-font-weight: bold;");
            actionBox.getChildren().add(timeLabel);
        } else {
            Button startBtn = new Button("BẮT ĐẦU");
            startBtn.setStyle("-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;");
            startBtn.setPrefSize(100, 35);
            if (canStart) {
                startBtn.setOnAction(e -> {
                    if (onStartAuction != null) onStartAuction.run();
                });
            } else {
                startBtn.setDisable(true);
                startBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: #999999; -fx-background-radius: 30; -fx-font-size: 14px;");
            }
            actionBox.getChildren().add(startBtn);
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

    private String getTimeRemaining(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "ĐÃ KẾT THÚC";
        }

        long totalSeconds = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("CÒN %02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("CÒN %02d:%02d", minutes, seconds);
        } else {
            return String.format("CÒN %ds", seconds);
        }
    }

    public HBox getRoot() {
        return cardRoot;
    }
}
