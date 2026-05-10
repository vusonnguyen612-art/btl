package Controller;

import Model.AuctionSession;
import Model.Bid;
import Model.Item;
import Model.User;
import DAO.UserDAO;
import Network.Message;
import Network.NetworkService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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

/** Controller cho phòng đấu giá chi tiết: đặt giá, auto-bid, timer, lịch sử, thanh toán. */
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

    @FXML
    private CheckBox autoBidCheckBox;

    @FXML
    private TextField autoBidMaxField;

    @FXML
    private Button autoBidButton;

    @FXML
    private TextField autoBidIncrementField;

    private User currentUser;
    private AuctionSession selectedAuction;
    private NetworkService networkService = NetworkService.getInstance();
    private UserDAO userDAO = new UserDAO();
    private Timeline timerTimeline;
    private Timeline refreshTimeline;
    private Timeline selectedAuctionRefreshTimeline;
    private boolean autoBidActive = false;
    private final java.util.Map<String, Double> activeAutoBids = new java.util.HashMap<>();

    private void setupNotificationListener() {
        networkService.setOnNotifications(notifs -> {
            Platform.runLater(() -> {
                for (Message notif : notifs) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText(null);
                    alert.setContentText(notif.getContent());
                    alert.show();
                }
            });
        });
    }

    private static final String BID_BUTTON_STYLE =
            "-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-font-size: 14px;";

    private static final String DETAIL_LABEL_STYLE =
            "-fx-text-fill: #eacd8f; -fx-font-size: 14px;";

    @FXML
    /** Khởi tạo: đăng ký notification listener, bắt đầu auto refresh. */
    private void initialize() {
        setupNotificationListener();
        startAutoRefresh();
    }

    /** Gán user hiện tại, load số dư và danh sách phiên. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            Message response = networkService.getUserBalance();
            if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
                BigDecimal balance = (BigDecimal) response.getData();
                if (userBalanceLabel != null) {
                    userBalanceLabel.setText(formatMoney(balance) + " $");
                }
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

        Message response = networkService.getAuctions();
        List<AuctionSession> allAuctions = (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List)
                ? (List<AuctionSession>) response.getData() : List.of();

        List<AuctionSession> runningAuctions = allAuctions.stream()
                .filter(AuctionSession::isRunning)
                .toList();
        List<AuctionSession> paymentPendingAuctions = allAuctions.stream()
                .filter(AuctionSession::isPaymentPending)
                .toList();
        List<AuctionSession> openAuctions = allAuctions.stream()
                .filter(AuctionSession::isOpen)
                .toList();

        if (runningAuctions.isEmpty() && openAuctions.isEmpty() && paymentPendingAuctions.isEmpty()) {
            Label emptyLabel = new Label("Không có phiên đấu giá nào.");
            emptyLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 16px;");
            auctionList.getChildren().add(emptyLabel);
            return;
        }

        if (!runningAuctions.isEmpty()) {
            Label runningHeader = new Label("Phiên đang diễn ra");
            runningHeader.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 18px; -fx-font-weight: bold;");
            runningHeader.setPadding(new Insets(5, 0, 10, 0));
            auctionList.getChildren().add(runningHeader);

            for (AuctionSession auction : runningAuctions) {
                HBox card = createAuctionCard(auction, "running");
                auctionList.getChildren().add(card);
            }
        }

        if (!paymentPendingAuctions.isEmpty()) {
            Label pendingHeader = new Label("Chờ thanh toán");
            pendingHeader.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 18px; -fx-font-weight: bold;");
            pendingHeader.setPadding(new Insets(15, 0, 10, 0));
            auctionList.getChildren().add(pendingHeader);

            for (AuctionSession auction : paymentPendingAuctions) {
                HBox card = createAuctionCard(auction, "payment_pending");
                auctionList.getChildren().add(card);
            }
        }

        if (!openAuctions.isEmpty()) {
            Label openHeader = new Label("Phiên sắp diễn ra");
            openHeader.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 18px; -fx-font-weight: bold;");
            openHeader.setPadding(new Insets(15, 0, 10, 0));
            auctionList.getChildren().add(openHeader);

            for (AuctionSession auction : openAuctions) {
                HBox card = createAuctionCard(auction, "open");
                auctionList.getChildren().add(card);
            }
        }
    }

    private HBox createAuctionCard(AuctionSession auction, String statusType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_card.fxml"));
            HBox card = loader.load();
            AuctionCardController controller = loader.getController();
            boolean canStart = currentUser != null && auction.getSellerId().equals(currentUser.getId());
            boolean isRunning = "running".equals(statusType);
            boolean isPaymentPending = "payment_pending".equals(statusType);
            controller.setAuction(auction, isRunning, canStart, isPaymentPending);
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

    private void selectAuction(AuctionSession auction) {
        this.selectedAuction = auction;

        removePaymentButton();
        removeStopButton();

        if (selectedAuctionName != null) {
            selectedAuctionName.setText(auction.getItem() != null ? auction.getItem().getName() : "Unknown");
        }

        if (selectedAuctionStatus != null) {
            String status = auction.getStatus().toString();
            selectedAuctionStatus.setText("Trạng thái: " + status);
        }

        if (durationDisplayLabel != null) {
            durationDisplayLabel.setText("Thời gian: " + formatDuration(auction.getDurationMinutes()));
        }

        if (startTimeLabel != null) {
            if (auction.getStartTime() != null) {
                startTimeLabel.setText(auction.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                startTimeLabel.setText("Chưa bắt đầu");
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
                String username = userDAO.getUsernameById(auction.getHighestBidderId());
                highestBidderLabel.setText(username);
            } else {
                highestBidderLabel.setText("Chưa có");
            }
        }

        if (bidAmountField != null) {
            boolean isSeller = currentUser != null && auction.getSellerId().equals(currentUser.getId());
            boolean isPaymentView = auction.isPaymentPending();
            boolean isWinner = currentUser != null && auction.getHighestBidderId() != null
                    && currentUser.getId().equals(auction.getHighestBidderId());

            bidAmountField.setDisable(isSeller || isPaymentView);
            if (autoBidCheckBox != null) autoBidCheckBox.setDisable(isSeller || isPaymentView);
            if (autoBidMaxField != null) autoBidMaxField.setDisable(isSeller || isPaymentView);
            if (autoBidIncrementField != null) autoBidIncrementField.setDisable(isSeller || isPaymentView);
            if (autoBidButton != null) autoBidButton.setDisable(isSeller || isPaymentView);

            if (isPaymentView && isWinner) {
                bidAmountField.clear();
                bidAmountField.setPromptText("Nhấn 'Thanh toán' để hoàn tất");
                showPaymentButton(auction);
            } else if (isPaymentView) {
                bidAmountField.clear();
                bidAmountField.setPromptText("Phiên chờ thanh toán");
            } else if (isSeller) {
                bidAmountField.clear();
                if (auction.isRunning()) {
                    showStopButton(auction);
                }
            } else {
                double minBid = auction.getCurrentPrice() + auction.getMinIncrement();
                bidAmountField.setText(String.valueOf(minBid));
            }
        }

        loadBidHistory(auction.getId());
        startTimer(auction);
        startSelectedAuctionRefresh(auction);

        if (auctionDetailPane != null) {
            auctionDetailPane.setVisible(true);
            auctionDetailPane.setManaged(true);
        }

        updateAutoBidUI(auction.getId());
    }

    private void updateAutoBidUI(String auctionId) {
        Double maxAmount = activeAutoBids.get(auctionId);
        if (maxAmount != null) {
            autoBidActive = true;
            if (autoBidCheckBox != null) autoBidCheckBox.setSelected(true);
            if (autoBidMaxField != null) autoBidMaxField.setText(String.valueOf(maxAmount));
            if (autoBidButton != null) autoBidButton.setText("Hủy");
        } else {
            autoBidActive = false;
            if (autoBidCheckBox != null) autoBidCheckBox.setSelected(false);
            if (autoBidMaxField != null) autoBidMaxField.clear();
            if (autoBidIncrementField != null) autoBidIncrementField.clear();
            if (autoBidButton != null) autoBidButton.setText("Xác nhận");
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

    private void startSelectedAuctionRefresh(AuctionSession auction) {
        if (selectedAuctionRefreshTimeline != null) {
            selectedAuctionRefreshTimeline.stop();
        }

        selectedAuctionRefreshTimeline = new Timeline();
        selectedAuctionRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        selectedAuctionRefreshTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1), e -> refreshSelectedAuction(auction))
        );
        selectedAuctionRefreshTimeline.play();
    }

    private void refreshSelectedAuction(AuctionSession auction) {
        Message auctionResponse = networkService.getAuction(auction.getId());
        if (auctionResponse.getType() == Message.Type.SUCCESS && auctionResponse.getData() != null) {
            AuctionSession updatedAuction = (AuctionSession) auctionResponse.getData();
            boolean statusChanged = updatedAuction.getStatus() != auction.getStatus();
            this.selectedAuction = updatedAuction;

            Platform.runLater(() -> {
                if (statusChanged) {
                    selectAuction(updatedAuction);
                    return;
                }

                if (currentPriceLabel != null) {
                    currentPriceLabel.setText(formatMoney(new BigDecimal(String.valueOf(updatedAuction.getCurrentPrice()))) + " $");
                }
                if (highestBidderLabel != null) {
                    if (updatedAuction.getHighestBidderId() != null) {
                        String username = userDAO.getUsernameById(updatedAuction.getHighestBidderId());
                        highestBidderLabel.setText(username);
                    } else {
                        highestBidderLabel.setText("Chua co");
                    }
                }
                loadBidHistory(updatedAuction.getId());
            });
        }
    }

    private void updateTimer(AuctionSession auction) {
        if (timerLabel == null) return;

        LocalDateTime now = LocalDateTime.now();

        if (auction.isPaymentPending()) {
            if (auction.getEndTime() == null) return;
            LocalDateTime deadline = auction.getEndTime().plusHours(1);
            if (now.isAfter(deadline)) {
                timerLabel.setText("HẾT HẠN");
                timerLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 20px; -fx-font-weight: bold;");
                timerTimeline.stop();
                loadAuctions();
                return;
            }
            long totalSeconds = java.time.Duration.between(now, deadline).getSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            String timeStr;
            if (hours > 0) {
                timeStr = String.format("Còn %02d:%02d:%02d để thanh toán", hours, minutes, seconds);
            } else {
                timeStr = String.format("Còn %02d:%02d để thanh toán", minutes, seconds);
            }
            timerLabel.setText(timeStr);
            if (totalSeconds < 300) {
                timerLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else {
                timerLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 18px; -fx-font-weight: bold;");
            }
            return;
        }

        if (auction.getEndTime() == null) return;

        if (now.isAfter(auction.getEndTime())) {
            timerLabel.setText("ĐÃ KẾT THÚC");
            timerLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 20px; -fx-font-weight: bold;");
            timerTimeline.stop();

            Message finishMsg = new Message(Message.Type.FINISH_AUCTION);
            finishMsg.setAuctionId(auction.getId());
            networkService.sendMessage(finishMsg);
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

        Message response = networkService.getBidHistory(auctionId);
        List<Bid> bids = (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List)
                ? (List<Bid>) response.getData() : List.of();

        if (bids.isEmpty()) {
            Label emptyLabel = new Label("Chưa có lịch sử đặt giá");
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
    /** Xử lý đặt giá thủ công: kiểm tra điều kiện, gọi NetworkService.placeBid(), cập nhật UI. */
    private void placeBid(ActionEvent event) {
        if (currentUser == null || selectedAuction == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng chọn phiên đấu giá.");
            return;
        }

        if (selectedAuction.getStatus() != AuctionSession.Status.RUNNING) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.");
            return;
        }

        if (selectedAuction.getSellerId().equals(currentUser.getId())) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Bạn không thể đặt giá cho sản phẩm của mình.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền không hợp lệ.");
            return;
        }

        if (bidAmount <= selectedAuction.getCurrentPrice()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi",
                    "Giá phải lớn hơn giá hiện tại: " + formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice()))) + " $");
            return;
        }

        if (bidAmount < selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi",
                    "Giá tối thiểu phải là: " + formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement()))) + " $");
            return;
        }

        Message response = networkService.placeBid(selectedAuction.getId(), bidAmount);

        if (response.getType() == Message.Type.SUCCESS) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đặt giá thành công: " + formatMoney(new BigDecimal(String.valueOf(bidAmount))) + " $");

            Message auctionResponse = networkService.getAuction(selectedAuction.getId());
            if (auctionResponse.getType() == Message.Type.SUCCESS && auctionResponse.getData() != null) {
                selectedAuction = (AuctionSession) auctionResponse.getData();
            }
            selectAuction(selectedAuction);

            if (userBalanceLabel != null) {
                Message balanceResponse = networkService.getUserBalance();
                if (balanceResponse.getType() == Message.Type.SUCCESS && balanceResponse.getData() != null) {
                    BigDecimal newBalance = (BigDecimal) balanceResponse.getData();
                    userBalanceLabel.setText(formatMoney(newBalance) + " $");
                }
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đặt giá. " + response.getContent());
        }
    }

    @FXML
    /** Bật/tắt AutoBid: gọi NetworkService.setAutoBid() hoặc removeAutoBid(). */
    private void toggleAutoBid(ActionEvent event) {
        if (currentUser == null || selectedAuction == null) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng chọn phiên đấu giá.");
            return;
        }

        if (autoBidActive) {
            Message response = networkService.removeAutoBid(selectedAuction.getId());
            if (response.getType() == Message.Type.SUCCESS) {
                activeAutoBids.remove(selectedAuction.getId());
                autoBidActive = false;
                if (autoBidCheckBox != null) autoBidCheckBox.setSelected(false);
                if (autoBidButton != null) autoBidButton.setText("Xác nhận");
                if (autoBidMaxField != null) autoBidMaxField.clear();
                if (autoBidIncrementField != null) autoBidIncrementField.clear();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tắt tự động trả giá.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tắt tự động trả giá.");
            }
        } else {
            double maxAmount;
            try {
                maxAmount = Double.parseDouble(autoBidMaxField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền tối đa không hợp lệ.");
                return;
            }
            if (maxAmount <= selectedAuction.getCurrentPrice()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền tối đa phải lớn hơn giá hiện tại.");
                return;
            }
            double increment = 1.0;
            String incText = autoBidIncrementField.getText();
            if (incText != null && !incText.trim().isEmpty()) {
                try {
                    increment = Double.parseDouble(incText.trim());
                    if (increment <= 0) {
                        showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền thêm phải lớn hơn 0.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền thêm không hợp lệ.");
                    return;
                }
            }
            Message response = networkService.setAutoBid(selectedAuction.getId(), maxAmount, increment);
            if (response.getType() == Message.Type.SUCCESS) {
                activeAutoBids.put(selectedAuction.getId(), maxAmount);
                autoBidActive = true;
                if (autoBidCheckBox != null) autoBidCheckBox.setSelected(true);
                if (autoBidButton != null) autoBidButton.setText("Hủy");
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã bật tự động trả giá (tối đa: " + formatMoney(new BigDecimal(String.valueOf(maxAmount))) + " $).");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể bật tự động trả giá.");
            }
        }
    }

    @FXML
    /** Đặt giá nhanh với mức tăng tối thiểu (currentPrice + minIncrement). */
    private void quickBid(ActionEvent event) {
        if (selectedAuction == null) return;

        double quickAmount = selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement();
        bidAmountField.setText(String.valueOf(quickAmount));
        placeBid(event);
    }

    private void startAuction(AuctionSession auction) {
        Message response = networkService.startAuction(auction.getId());
        if (response.getType() == Message.Type.SUCCESS) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã bắt đầu phiên đấu giá.");
            loadAuctions();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể bắt đầu phiên đấu giá.");
        }
    }

    private Button paymentButton;
    private Button stopButton;
    private HBox paymentBox;
    private HBox stopBox;

    private void removeStopButton() {
        if (stopBox != null && stopBox.getParent() != null) {
            ((javafx.scene.layout.Pane) stopBox.getParent()).getChildren().remove(stopBox);
        }
        stopBox = null;
        stopButton = null;
    }

    private void removePaymentButton() {
        if (paymentBox != null && paymentBox.getParent() != null) {
            ((javafx.scene.layout.Pane) paymentBox.getParent()).getChildren().remove(paymentBox);
        }
        paymentBox = null;
        paymentButton = null;
    }

    private void showPaymentButton(AuctionSession auction) {
        if (auctionDetailPane == null) return;
        removePaymentButton();
        paymentButton = new Button("Thanh toán " + formatMoney(new BigDecimal(String.valueOf(auction.getCurrentPrice()))) + " $");
        paymentButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-font-size: 16px;");
        paymentButton.setPrefHeight(45);
        paymentButton.setPrefWidth(300);
        paymentButton.setOnAction(e -> processPayment());

        paymentBox = new HBox(paymentButton);
        paymentBox.setAlignment(javafx.geometry.Pos.CENTER);
        paymentBox.setPadding(new Insets(10, 0, 20, 0));

        int insertIndex = Math.min(auctionDetailPane.getChildren().size(), 9);
        auctionDetailPane.getChildren().add(insertIndex, paymentBox);
    }

    private void showStopButton(AuctionSession auction) {
        if (auctionDetailPane == null) return;
        removeStopButton();
        stopButton = new Button("NGỪNG ĐẤU GIÁ");
        stopButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-font-size: 16px;");
        stopButton.setPrefHeight(45);
        stopButton.setPrefWidth(200);
        stopButton.setOnAction(e -> stopAuction());

        stopBox = new HBox(stopButton);
        stopBox.setAlignment(javafx.geometry.Pos.CENTER);
        stopBox.setPadding(new Insets(10, 0, 20, 0));

        int insertIndex = Math.min(auctionDetailPane.getChildren().size(), 9);
        auctionDetailPane.getChildren().add(insertIndex, stopBox);
    }

    private void stopAuction() {
        if (selectedAuction == null) return;
        boolean confirmed = showConfirm("Ngừng đấu giá",
                "Bạn có chắc muốn ngừng đấu giá? Người chơi sẽ có thêm 5 phút để đặt giá cuối.");
        if (!confirmed) return;

        Message response = networkService.stopAuction(selectedAuction.getId());
        if (response.getType() == Message.Type.SUCCESS) {
            Message auctionResponse = networkService.getAuction(selectedAuction.getId());
            if (auctionResponse.getType() == Message.Type.SUCCESS && auctionResponse.getData() != null) {
                selectedAuction = (AuctionSession) auctionResponse.getData();
            }
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã ngừng đấu giá. Người chơi còn 5 phút để đặt giá.");
            removeStopButton();
            selectAuction(selectedAuction);
            loadAuctions();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể ngừng đấu giá. " + response.getContent());
        }
    }

    private void processPayment() {
        if (selectedAuction == null) return;
        boolean confirmed = showConfirm("Xác nhận thanh toán",
                "Bạn xác nhận thanh toán " + formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice()))) + " $ cho sản phẩm này?");
        if (!confirmed) return;

        Message response = networkService.processPayment(selectedAuction.getId());
        if (response.getType() == Message.Type.SUCCESS) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thanh toán thành công!");

            Message auctionResponse = networkService.getAuction(selectedAuction.getId());
            if (auctionResponse.getType() == Message.Type.SUCCESS && auctionResponse.getData() != null) {
                selectedAuction = (AuctionSession) auctionResponse.getData();
            }

            if (userBalanceLabel != null) {
                Message balanceResponse = networkService.getUserBalance();
                if (balanceResponse.getType() == Message.Type.SUCCESS && balanceResponse.getData() != null) {
                    BigDecimal newBalance = (BigDecimal) balanceResponse.getData();
                    userBalanceLabel.setText(formatMoney(newBalance) + " $");
                }
            }

            removePaymentButton();
            selectAuction(selectedAuction);
            loadAuctions();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thanh toán thất bại. " + response.getContent());
        }
    }

    private boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(buttonType -> buttonType == ButtonType.OK).isPresent();
    }

    @FXML
    /** Quay lại danh sách: dừng timer, xóa AutoBid nếu có. */
    private void backToList(ActionEvent event) {
        if (selectedAuction != null && autoBidActive) {
            networkService.removeAutoBid(selectedAuction.getId());
            activeAutoBids.remove(selectedAuction.getId());
            autoBidActive = false;
        }

        if (auctionDetailPane != null) {
            auctionDetailPane.setVisible(false);
            auctionDetailPane.setManaged(false);
        }

        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        if (selectedAuctionRefreshTimeline != null) {
            selectedAuctionRefreshTimeline.stop();
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

    /** Dừng tất cả timeline refresh khi đóng phòng. */
    public void stopRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        if (selectedAuctionRefreshTimeline != null) {
            selectedAuctionRefreshTimeline.stop();
        }
    }
}
