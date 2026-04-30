package Controller;

import DAO.AuctionDAO;
import Model.AuctionSession;
import Model.Bid;
import Model.Item;
import Model.User;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AuctionRoomController {

    @FXML
    private VBox auctionList;

    @FXML
    private VBox bidHistoryList;

    @FXML
    private Label currentPriceLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Label highestBidderLabel;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Label selectedAuctionName;

    @FXML
    private Label selectedAuctionStatus;

    @FXML
    private VBox auctionDetailPane;

    @FXML
    private Label userBalanceLabel;

    @FXML
    private Label durationDisplayLabel;

    @FXML
    private Label startTimeLabel;

    @FXML
    private Label endTimeLabel;

    private User currentUser;
    private AuctionSession selectedAuction;
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private Timeline timerTimeline;
    private Timeline refreshTimeline;

    private static final String BID_BUTTON_STYLE =
            "-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;";

    private static final String DETAIL_LABEL_STYLE =
            "-fx-text-fill: #eacd8f; -fx-font-size: 14px;";

    @FXML
    private void initialize() {
        startAutoRefresh();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            BigDecimal balance = auctionDAO.getUserBalance(user.getId());
            if (balance != null) {
                userBalanceLabel.setText(formatMoney(balance) + " $");
            }
        }
        loadAuctions();
    }

    private void startAutoRefresh() {
        refreshTimeline = new Timeline();
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(3), e -> loadAuctions())
        );
        refreshTimeline.play();
    }

    private void loadAuctions() {
        if (auctionList == null) return;

        auctionList.getChildren().clear();

        List<AuctionSession> runningAuctions = auctionDAO.findRunningAuctions();
        List<AuctionSession> openAuctions = auctionDAO.findOpenAuctions();

        if (runningAuctions.isEmpty() && openAuctions.isEmpty()) {
            Label emptyLabel = new Label("Khong co phien dau gia nao.");
            emptyLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 16px;");
            auctionList.getChildren().add(emptyLabel);
            return;
        }

        if (!runningAuctions.isEmpty()) {
            Label runningHeader = new Label("Phien dang dien ra");
            runningHeader.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 18px; -fx-font-weight: bold;");
            runningHeader.setPadding(new Insets(5, 0, 10, 0));
            auctionList.getChildren().add(runningHeader);

            for (AuctionSession auction : runningAuctions) {
                HBox card = createAuctionCard(auction, true);
                auctionList.getChildren().add(card);
            }
        }

        if (!openAuctions.isEmpty()) {
            Label openHeader = new Label("Phien sap dien ra");
            openHeader.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 18px; -fx-font-weight: bold;");
            openHeader.setPadding(new Insets(15, 0, 10, 0));
            auctionList.getChildren().add(openHeader);

            for (AuctionSession auction : openAuctions) {
                HBox card = createAuctionCard(auction, false);
                auctionList.getChildren().add(card);
            }
        }
    }

    private HBox createAuctionCard(AuctionSession auction, boolean isRunning) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_card.fxml"));
            HBox card = loader.load();
            AuctionCardController controller = loader.getController();

            boolean canStart = currentUser != null && auction.getSellerId().equals(currentUser.getId());
            controller.setAuction(auction, isRunning, canStart);
            controller.setOnSelectAuction(() -> selectAuction(auction));
            controller.setOnStartAuction(() -> startAuction(auction));

            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new HBox();
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

    private String getTimeRemaining(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Da ket thuc";
        }

        long totalSeconds = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("Con %02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("Con %02d:%02d", minutes, seconds);
        } else {
            return String.format("Con %ds", seconds);
        }
    }

    private void selectAuction(AuctionSession auction) {
        this.selectedAuction = auction;

        if (selectedAuctionName != null) {
            selectedAuctionName.setText(auction.getItem() != null ? auction.getItem().getName() : "Unknown");
        }

        if (selectedAuctionStatus != null) {
            String status = auction.getStatus().toString();
            selectedAuctionStatus.setText("Trang thai: " + status);
        }

        if (durationDisplayLabel != null) {
            durationDisplayLabel.setText("Thoi gian: " + formatDuration(auction.getDurationMinutes()));
        }

        if (startTimeLabel != null) {
            if (auction.getStartTime() != null) {
                startTimeLabel.setText(auction.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                startTimeLabel.setText("Chua bat dau");
            }
        }

        if (endTimeLabel != null) {
            if (auction.getEndTime() != null) {
                endTimeLabel.setText(auction.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                endTimeLabel.setText("--:--");
            }
        }

        if (currentPriceLabel != null) {
            currentPriceLabel.setText(formatMoney(new BigDecimal(String.valueOf(auction.getCurrentPrice()))) + " $");
        }

        if (highestBidderLabel != null) {
            if (auction.getHighestBidderId() != null) {
                highestBidderLabel.setText(auction.getHighestBidderId());
            } else {
                highestBidderLabel.setText("Chua co");
            }
        }

        if (bidAmountField != null) {
            double minBid = auction.getCurrentPrice() + auction.getMinIncrement();
            bidAmountField.setText(String.valueOf(minBid));
        }

        loadBidHistory(auction.getId());
        startTimer(auction);

        if (auctionDetailPane != null) {
            auctionDetailPane.setVisible(true);
            auctionDetailPane.setManaged(true);
        }
    }

    private void startTimer(AuctionSession auction) {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        timerTimeline = new Timeline();
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), e -> updateTimer(auction))
        );
        timerTimeline.play();
    }

    private void updateTimer(AuctionSession auction) {
        if (auction.getEndTime() == null || timerLabel == null) return;

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            timerLabel.setText("DA KET THUC");
            timerLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 20px; -fx-font-weight: bold;");
            timerTimeline.stop();

            auctionDAO.finishAuction(auction.getId());
            loadAuctions();
            return;
        }

        long totalSeconds = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String timeStr;
        if (hours > 0) {
            timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeStr = String.format("%02d:%02d", minutes, seconds);
        }
        timerLabel.setText(timeStr);

        if (totalSeconds < 60) {
            timerLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else if (totalSeconds < 300) {
            timerLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 20px; -fx-font-weight: bold;");
        }
    }

    private void loadBidHistory(String auctionId) {
        if (bidHistoryList == null) return;

        bidHistoryList.getChildren().clear();

        List<Bid> bids = auctionDAO.getBidHistory(auctionId);

        if (bids.isEmpty()) {
            Label emptyLabel = new Label("Chua co lich su dat gia");
            emptyLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
            bidHistoryList.getChildren().add(emptyLabel);
            return;
        }

        for (Bid bid : bids) {
            HBox bidCard = createBidCard(bid);
            bidHistoryList.getChildren().add(bidCard);
        }
    }

    private HBox createBidCard(Bid bid) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/bid_card.fxml"));
            HBox card = loader.load();
            BidCardController controller = loader.getController();
            controller.setBid(bid);
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new HBox();
        }
    }

    @FXML
    private void placeBid(ActionEvent event) {
        if (currentUser == null || selectedAuction == null) {
            showAlert(Alert.AlertType.WARNING, "Loi", "Vui long chon phien dau gia.");
            return;
        }

        if (selectedAuction.getStatus() != AuctionSession.Status.RUNNING) {
            showAlert(Alert.AlertType.WARNING, "Loi", "Phien dau gia chua bat dau hoac da ket thuc.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Loi", "So tien khong hop le.");
            return;
        }

        if (bidAmount <= selectedAuction.getCurrentPrice()) {
            showAlert(Alert.AlertType.WARNING, "Loi",
                    "Gia phai lon hon gia hien tai: " + formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice()))) + " $");
            return;
        }

        if (bidAmount < selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement()) {
            showAlert(Alert.AlertType.WARNING, "Loi",
                    "Gia toi thieu phai la: " + formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement()))) + " $");
            return;
        }

        BigDecimal userBalance = auctionDAO.getUserBalance(currentUser.getId());
        if (userBalance.compareTo(new BigDecimal(String.valueOf(bidAmount))) < 0) {
            showAlert(Alert.AlertType.ERROR, "Loi", "So du khong du. So du hien tai: " + formatMoney(userBalance) + " $");
            return;
        }

        boolean success = auctionDAO.placeBid(selectedAuction.getId(), currentUser.getId(), bidAmount);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thanh cong",
                    "Dat gia thanh cong: " + formatMoney(new BigDecimal(String.valueOf(bidAmount))) + " $");

            selectedAuction = auctionDAO.findAuctionById(selectedAuction.getId()).orElse(selectedAuction);
            selectAuction(selectedAuction);

            if (userBalanceLabel != null) {
                BigDecimal newBalance = auctionDAO.getUserBalance(currentUser.getId());
                userBalanceLabel.setText(formatMoney(newBalance) + " $");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Loi", "Khong the dat gia. Vui long thu lai.");
        }
    }

    @FXML
    private void quickBid(ActionEvent event) {
        if (selectedAuction == null) return;

        double quickAmount = selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement();
        bidAmountField.setText(String.valueOf(quickAmount));
        placeBid(event);
    }

    private void startAuction(AuctionSession auction) {
        boolean success = auctionDAO.startAuction(auction.getId());

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thanh cong", "Da bat dau phien dau gia.");
            loadAuctions();
        } else {
            showAlert(Alert.AlertType.ERROR, "Loi", "Khong the bat dau phien dau gia.");
        }
    }

    @FXML
    private void backToList(ActionEvent event) {
        if (auctionDetailPane != null) {
            auctionDetailPane.setVisible(false);
            auctionDetailPane.setManaged(false);
        }

        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        selectedAuction = null;
    }

    private String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void stopRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
    }
}
