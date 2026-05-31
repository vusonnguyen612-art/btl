package Controller;

import Controller.utils.AlertUtils;
import Controller.utils.CategoryMapper;
import Controller.utils.FormatUtils;
import Controller.utils.ResponseUtils;
import Controller.utils.UIUtils;
import Model.AuctionSession;
import Model.Bid;
import Model.Item;
import Model.SearchCriteria;
import Model.User;
import Model.ChatMessage;
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
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Controller cho phòng đấu giá chi tiết: đặt giá, auto-bid, timer, lịch sử, thanh toán. */
public class AuctionRoomController {

    @FXML
    private VBox auctionList;

    @FXML
    private VBox bidHistoryList;

    @FXML
    private VBox bidChartContainer;

    @FXML
    private ScrollPane auctionListScrollPane;

    @FXML
    private ScrollPane bidHistoryScrollPane;

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

    @FXML
    private Button watchlistToggleButton;

    @FXML
    private TabPane auctionTabs;

    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private VBox chatList;

    @FXML
    private TextField chatInputField;

    // Search fields
    @FXML
    private TextField searchKeywordField;
    @FXML
    private ComboBox<String> searchCategoryCombo;
    @FXML
    private ComboBox<String> searchStatusCombo;
    @FXML
    private TextField searchMinPriceField;
    @FXML
    private TextField searchMaxPriceField;
    @FXML
    private ComboBox<String> searchSortCombo;

    private boolean isSearchActive = false;
    private int lastChatCount = 0;
    private SearchCriteria currentSearchCriteria;

    private User currentUser;
    private AuctionSession selectedAuction;
    private NetworkService networkService = NetworkService.getInstance();
    private UserDAO userDAO = new UserDAO();
    private Timeline timerTimeline;
    private Timeline refreshTimeline;
    private Timeline selectedAuctionRefreshTimeline;
    private boolean autoBidActive = false;
    private final java.util.Map<String, Double> activeAutoBids = new java.util.HashMap<>();

    private LineChart<Number, Number> bidChart;
    private NumberAxis bidXAxis;
    private NumberAxis bidYAxis;
    private XYChart.Series<Number, Number> bidSeries;
    private boolean bidChartInitialized = false;

    /**
     * Đăng ký lắng nghe thông báo từ server.
     * Khi nhận được thông báo, hiển thị dialog thông báo cho người dùng.
     */
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
    /** Khởi tạo: đăng ký notification listener, bắt đầu auto refresh, setup search combos, focus cho scroll. */
    private void initialize() {
        setupNotificationListener();
        startAutoRefresh();
        initSearchCombos();
        setupScrollFocus();
    }

    /**
     * Thiết lập sự kiện focus cho cácScrollPane khi di chuột vào.
     * Cho phép cuộn bằng con lăn chuột mà không cần nhấp trước.
     */
    private void setupScrollFocus() {
        if (auctionListScrollPane != null) {
            auctionListScrollPane.setOnMouseEntered(e -> auctionListScrollPane.requestFocus());
        }
        if (bidHistoryScrollPane != null) {
            bidHistoryScrollPane.setOnMouseEntered(e -> bidHistoryScrollPane.requestFocus());
        }
    }

    /**
     * Khởi tạo các ComboBox tìm kiếm (danh mục, trạng thái, sắp xếp).
     */
    private void initSearchCombos() {
        UIUtils.initSearchCombos(searchCategoryCombo, searchStatusCombo, searchSortCombo);
    }

    /** Gán user hiện tại, load số dư và danh sách phiên. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            BigDecimal balance = ResponseUtils.extractBalance(networkService.getUserBalance());
            if (balance != null && userBalanceLabel != null) {
                userBalanceLabel.setText(FormatUtils.formatMoney(balance) + " $");
            }
        }
        loadAuctions();
    }

    /**
     * Bắt đầu tự động làm mới danh sách phiên đấu giá mỗi 3 giây.
     * Nếu đang tìm kiếm, thực hiện lại tìm kiếm; ngược lại tải lại toàn bộ danh sách.
     */
    private void startAutoRefresh() {
        refreshTimeline = new Timeline();
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(3), e -> {
                    if (isSearchActive && currentSearchCriteria != null) {
                        performSearch(currentSearchCriteria);
                    } else {
                        loadAuctions();
                    }
                })
        );
        refreshTimeline.play();
    }

    /**
     * Tải danh sách tất cả phiên đấu giá từ server và hiển thị theo từng nhóm:
     * Phiên đang diễn ra, chờ thanh toán, và sắp diễn ra.
     */
    private void loadAuctions() {
        if (auctionList == null) return;

        auctionList.getChildren().clear();

        Message response = networkService.getAuctions();
        List<AuctionSession> allAuctions = ResponseUtils.extractList(response);

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
                VBox card = createAuctionCard(auction, "running");
                auctionList.getChildren().add(card);
            }
        }

        if (!paymentPendingAuctions.isEmpty()) {
            Label pendingHeader = new Label("Chờ thanh toán");
            pendingHeader.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 18px; -fx-font-weight: bold;");
            pendingHeader.setPadding(new Insets(15, 0, 10, 0));
            auctionList.getChildren().add(pendingHeader);

            for (AuctionSession auction : paymentPendingAuctions) {
                VBox card = createAuctionCard(auction, "payment_pending");
                auctionList.getChildren().add(card);
            }
        }

        if (!openAuctions.isEmpty()) {
            Label openHeader = new Label("Phiên sắp diễn ra");
            openHeader.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 18px; -fx-font-weight: bold;");
            openHeader.setPadding(new Insets(15, 0, 10, 0));
            auctionList.getChildren().add(openHeader);

            for (AuctionSession auction : openAuctions) {
                VBox card = createAuctionCard(auction, "open");
                auctionList.getChildren().add(card);
            }
        }
    }

    /**
     * Xử lý sự kiện tìm kiếm khi người dùng nhấn nút tìm.
     * Thu thập các tiêu chí tìm kiếm từ giao diện và gọi performSearch().
     */
    @FXML
    private void searchAuctions() {
        isSearchActive = true;
        if (auctionList == null) return;

        SearchCriteria criteria = new SearchCriteria();

        String keyword = searchKeywordField.getText().trim();
        if (!keyword.isEmpty()) {
            criteria.setKeyword(keyword);
        }

        String category = searchCategoryCombo.getValue();
        if (category != null && !category.equals("Tất cả")) {
            criteria.setCategory(CategoryMapper.toEnglish(category));
        }

        String status = searchStatusCombo.getValue();
        if (status != null && !status.equals("Tất cả")) {
            List<AuctionSession.Status> statuses = CategoryMapper.mapStatus(status);
            if (!statuses.isEmpty()) {
                criteria.setStatuses(statuses);
            }
        }

        String minPriceText = searchMinPriceField.getText().trim();
        if (!minPriceText.isEmpty()) {
            try {
                criteria.setMinPrice(Double.parseDouble(minPriceText));
            } catch (NumberFormatException ignored) {}
        }

        String maxPriceText = searchMaxPriceField.getText().trim();
        if (!maxPriceText.isEmpty()) {
            try {
                criteria.setMaxPrice(Double.parseDouble(maxPriceText));
            } catch (NumberFormatException ignored) {}
        }

        String sort = searchSortCombo.getValue();
        if (sort != null) {
            String sortKey = switch (sort) {
                case "Cũ nhất" -> "oldest";
                case "Giá tăng dần" -> "price_asc";
                case "Giá giảm dần" -> "price_desc";
                case "Tên A-Z" -> "name";
                default -> "newest";
            };
            criteria.setSortBy(sortKey);
        }

        currentSearchCriteria = criteria;
        performSearch(criteria);
    }

    /**
     * Thực hiện tìm kiếm phiên đấu giá theo tiêu chí đã cho và hiển thị kết quả.
     *
     * @param criteria đối tượng chứa các tiêu chí tìm kiếm
     */
    private void performSearch(SearchCriteria criteria) {
        auctionList.getChildren().clear();
        Message response = networkService.searchAuctions(criteria);
        List<AuctionSession> results = ResponseUtils.extractList(response);

        if (results.isEmpty()) {
            Label emptyLabel = new Label("Không tìm thấy phiên đấu giá nào.");
            emptyLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 16px;");
            auctionList.getChildren().add(emptyLabel);
            return;
        }

        for (AuctionSession auction : results) {
            String type = auction.isRunning() ? "running"
                    : auction.isPaymentPending() ? "payment_pending"
                    : auction.isOpen() ? "open" : "other";
            VBox card = createAuctionCard(auction, type);
            auctionList.getChildren().add(card);
        }
    }

    /**
     * Đặt lại tất cả tiêu chí tìm kiếm và tải lại danh sách phiên đấu giá mặc định.
     */
    @FXML
    private void resetSearch() {
        isSearchActive = false;
        currentSearchCriteria = null;
        UIUtils.resetSearchFields(searchKeywordField, searchCategoryCombo, searchStatusCombo, searchMinPriceField, searchMaxPriceField, searchSortCombo);
        loadAuctions();
    }

    /**
     * Tạo thẻ phiên đấu giá (auction card) từ file FXML và cấu hình dữ liệu hiển thị.
     *
     * @param auction   phiên đấu giá cần hiển thị
     * @param statusType loại trạng thái: "running", "payment_pending", "open", hoặc "other"
     * @return VBox chứa giao diện thẻ phiên đấu giá
     */
    private VBox createAuctionCard(AuctionSession auction, String statusType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_card.fxml"));
            VBox card = loader.load();
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
            return new VBox();
        }
    }

    /**
     * Chọn và hiển thị chi tiết phiên đấu giá.
     * Cập nhật thông tin giá, người đặt cao nhất, thời gian, lịch sử đặt giá,
     * nút thanh toán/ngừng đấu giá, và trạng thái auto-bid.
     *
     * @param auction phiên đấu giá được chọn
     */
    public void selectAuction(AuctionSession auction) {
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
            durationDisplayLabel.setText("Thời gian: " + FormatUtils.formatDuration(auction.getDurationMinutes()));
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
            currentPriceLabel.setText(FormatUtils.formatMoney(new BigDecimal(String.valueOf(auction.getCurrentPrice()))) + " $");
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
        loadChatHistory(auction.getId());

        // Cập nhật trạng thái Watchlist của nút bấm
        boolean isWatched = false;
        Message watchlistRes = networkService.getWatchlist();
        if (watchlistRes.getType() == Message.Type.SUCCESS && watchlistRes.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<AuctionSession> list = (List<AuctionSession>) watchlistRes.getData();
            isWatched = list.stream().anyMatch(a -> a.getId().equals(auction.getId()));
        }
        if (watchlistToggleButton != null) {
            watchlistToggleButton.setText(isWatched ? "★" : "☆");
        }

        startTimer(auction);
        startSelectedAuctionRefresh(auction);

        if (auctionDetailPane != null) {
            auctionDetailPane.setVisible(true);
            auctionDetailPane.setManaged(true);
        }

        updateAutoBidUI(auction.getId());
    }

    /**
     * Cập nhật giao diện auto-bid dựa trên trạng thái hiện tại của phiên đấu giá.
     * Hiển thị số tiền tối đa và trạng thái bật/tắt auto-bid.
     *
     * @param auctionId ID của phiên đấu giá
     */
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

    /**
     * Bắt đầu bộ đếm ngược thời gian cho phiên đấu giá.
     * Mỗi giây gọi updateTimer() để cập nhật hiển thị thời gian còn lại.
     *
     * @param auction phiên đấu giá đang được theo dõi
     */
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

    /**
     * Bắt đầu tự động làm mới thông tin phiên đấu giá đang chọn mỗi giây.
     * Giúp cập nhật giá hiện tại và người đặt cao nhất theo thời gian thực.
     *
     * @param auction phiên đấu giá cần làm mới
     */
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

    /**
     * Làm mới thông tin phiên đấu giá từ server.
     * Nếu trạng thái thay đổi, gọi lại selectAuction(); nếu không chỉ cập nhật giá và lịch sử đặt giá.
     *
     * @param auction phiên đấu giá cần làm mới
     */
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
                    currentPriceLabel.setText(FormatUtils.formatMoney(new BigDecimal(String.valueOf(updatedAuction.getCurrentPrice()))) + " $");
                }
                if (highestBidderLabel != null) {
                    if (updatedAuction.getHighestBidderId() != null) {
                        String username = userDAO.getUsernameById(updatedAuction.getHighestBidderId());
                        highestBidderLabel.setText(username);
                    } else {
                        highestBidderLabel.setText("Chưa có");
                    }
                }
                loadBidHistory(updatedAuction.getId());
                refreshChatHistory(updatedAuction.getId());
            });
        }
    }

    /**
     * Cập nhật hiển thị bộ đếm thời gian mỗi giây.
     * Xử lý cả phiên đang diễn ra và phiên chờ thanh toán.
     * Khi hết thời gian, gửi thông báo kết thúc và tải lại danh sách.
     *
     * @param auction phiên đấu giá cần cập nhật thời gian
     */
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

    /**
     * Tải lịch sử đặt giá của phiên đấu giá từ server và hiển thị dưới dạng danh sách.
     * Đồng thời cập nhật biểu đồ giá đấu.
     *
     * @param auctionId ID của phiên đấu giá
     */
    private void loadBidHistory(String auctionId) {
        if (bidHistoryList == null) return;

        bidHistoryList.getChildren().clear();

        Message response = networkService.getBidHistory(auctionId);
        List<Bid> bids = ResponseUtils.extractList(response);

        updateBidChart(bids);

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

    /**
     * Tạo thẻ hiển thị một lần đặt giá từ file FXML.
     *
     * @param bid đối tượng đặt giá
     * @return HBox chứa giao diện thẻ đặt giá
     */
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

    /**
     * Khởi tạo biểu đồ đường hiển thị lịch sử giá đấu.
     * Thiết lập trục X (thời gian), trục Y (giá), và định dạng giao diện tối.
     */
    private void initBidChart() {
        bidXAxis = new NumberAxis();
        bidXAxis.setLabel("Thời gian (giây)");
        bidXAxis.setAnimated(false);
        bidXAxis.setForceZeroInRange(false);
        bidXAxis.setTickLabelFill(Color.valueOf("#eacd8f"));

        bidYAxis = new NumberAxis();
        bidYAxis.setLabel("Giá ($)");
        bidYAxis.setAnimated(false);
        bidYAxis.setForceZeroInRange(false);
        bidYAxis.setTickLabelFill(Color.valueOf("#eacd8f"));

        bidSeries = new XYChart.Series<>();
        bidSeries.setName("Giá đấu");

        bidChart = new LineChart<>(bidXAxis, bidYAxis);
        bidChart.setAnimated(false);
        bidChart.setLegendVisible(false);
        bidChart.setPrefHeight(200);
        bidChart.setTitle("Biểu đồ giá đấu");
        bidChart.setStyle("-fx-background-color: #1E1E1D;");

        bidChart.getData().add(bidSeries);

        bidChartContainer.getChildren().add(bidChart);

        Platform.runLater(() -> {
            bidChart.applyCss();
            Node node;
            node = bidChart.lookup(".chart-title");
            if (node != null) node.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 14px;");
            node = bidChart.lookup(".chart-plot-background");
            if (node != null) node.setStyle("-fx-background-color: #1E1E1D;");
            node = bidChart.lookup(".chart-series-line");
            if (node != null) node.setStyle("-fx-stroke: #4CAF50; -fx-stroke-width: 2px;");
            node = bidChart.lookup(".chart-alternative-column-fill");
            if (node != null) node.setStyle("-fx-fill: transparent;");
            node = bidChart.lookup(".chart-alternative-row-fill");
            if (node != null) node.setStyle("-fx-fill: transparent;");
            node = bidChart.lookup(".chart-vertical-zero-line");
            if (node != null) node.setStyle("-fx-stroke: #333333;");
            node = bidChart.lookup(".chart-horizontal-zero-line");
            if (node != null) node.setStyle("-fx-stroke: #333333;");
        });

        bidChartInitialized = true;
    }

    /**
     * Cập nhật dữ liệu biểu đồ giá đấu từ danh sách các lần đặt giá.
     * Trục X hiển thị thời gian tính bằng giây từ lúc bắt đầu phiên.
     *
     * @param bids danh sách các lần đặt giá
     */
    private void updateBidChart(List<Bid> bids) {
        if (!bidChartInitialized) {
            initBidChart();
        }

        bidSeries.getData().clear();

        if (bids.isEmpty() || selectedAuction == null) return;

        LocalDateTime baseTime = selectedAuction.getStartTime();
        if (baseTime == null) {
            baseTime = bids.get(0).getTimestamp();
        }
        if (baseTime == null) return;

        LocalDateTime finalBaseTime = baseTime;
        for (Bid bid : bids) {
            long seconds = ChronoUnit.SECONDS.between(finalBaseTime, bid.getTimestamp());
            bidSeries.getData().add(new XYChart.Data<>(seconds, bid.getAmount()));
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
                    "Giá phải lớn hơn giá hiện tại: " + FormatUtils.formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice()))) + " $");
            return;
        }

        if (bidAmount < selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi",
                    "Giá tối thiểu phải là: " + FormatUtils.formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice() + selectedAuction.getMinIncrement()))) + " $");
            return;
        }

        Message response = networkService.placeBid(selectedAuction.getId(), bidAmount);

        if (response.getType() == Message.Type.SUCCESS) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đặt giá thành công: " + FormatUtils.formatMoney(new BigDecimal(String.valueOf(bidAmount))) + " $");

            Message auctionResponse = networkService.getAuction(selectedAuction.getId());
            if (auctionResponse.getType() == Message.Type.SUCCESS && auctionResponse.getData() != null) {
                selectedAuction = (AuctionSession) auctionResponse.getData();
            }
            selectAuction(selectedAuction);

            if (userBalanceLabel != null) {
                BigDecimal newBalance = ResponseUtils.extractBalance(networkService.getUserBalance());
                if (newBalance != null) {
                    userBalanceLabel.setText(FormatUtils.formatMoney(newBalance) + " $");
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
                        "Đã bật tự động trả giá (tối đa: " + FormatUtils.formatMoney(new BigDecimal(String.valueOf(maxAmount))) + " $).");
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

    /**
     * Bắt đầu phiên đấu giá (chỉ dành cho người bán).
     * Gửi yêu cầu lên server và tải lại danh sách nếu thành công.
     *
     * @param auction phiên đấu giá cần bắt đầu
     */
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

    /**
     * Xóa nút "Ngừng đấu giá" khỏi giao diện chi tiết phiên.
     */
    private void removeStopButton() {
        if (stopBox != null && stopBox.getParent() != null) {
            ((javafx.scene.layout.Pane) stopBox.getParent()).getChildren().remove(stopBox);
        }
        stopBox = null;
        stopButton = null;
    }

    /**
     * Xóa nút "Thanh toán" khỏi giao diện chi tiết phiên.
     */
    private void removePaymentButton() {
        if (paymentBox != null && paymentBox.getParent() != null) {
            ((javafx.scene.layout.Pane) paymentBox.getParent()).getChildren().remove(paymentBox);
        }
        paymentBox = null;
        paymentButton = null;
    }

    /**
     * Hiển thị nút "Thanh toán" cho người thắng phiên đấu giá.
     *
     * @param auction phiên đấu giá chờ thanh toán
     */
    private void showPaymentButton(AuctionSession auction) {
        if (auctionDetailPane == null) return;
        removePaymentButton();
        paymentButton = new Button("Thanh toán " + FormatUtils.formatMoney(new BigDecimal(String.valueOf(auction.getCurrentPrice()))) + " $");
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

    /**
     * Hiển thị nút "Ngừng đấu giá" cho người bán.
     *
     * @param auction phiên đấu giá đang diễn ra
     */
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

    /**
     * Xử lý ngừng đấu giá theo yêu cầu của người bán.
     * Hiển thị hộp thoại xác nhận, gửi yêu cầu lên server, và cập nhật giao diện.
     */
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

    /**
     * Xử lý thanh toán cho phiên đấu giá đã thắng.
     * Hiển thị hộp thoại xác nhận, gọi API thanh toán, cập nhật số dư và giao diện.
     */
    private void processPayment() {
        if (selectedAuction == null) return;
        boolean confirmed = showConfirm("Xác nhận thanh toán",
                "Bạn xác nhận thanh toán " + FormatUtils.formatMoney(new BigDecimal(String.valueOf(selectedAuction.getCurrentPrice()))) + " $ cho sản phẩm này?");
        if (!confirmed) return;

        Message response = networkService.processPayment(selectedAuction.getId());
        if (response.getType() == Message.Type.SUCCESS) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thanh toán thành công!");

            Message auctionResponse = networkService.getAuction(selectedAuction.getId());
            if (auctionResponse.getType() == Message.Type.SUCCESS && auctionResponse.getData() != null) {
                selectedAuction = (AuctionSession) auctionResponse.getData();
            }

            if (userBalanceLabel != null) {
                BigDecimal newBalance = ResponseUtils.extractBalance(networkService.getUserBalance());
                if (newBalance != null) {
                    userBalanceLabel.setText(FormatUtils.formatMoney(newBalance) + " $");
                }
            }

            removePaymentButton();
            selectAuction(selectedAuction);
            loadAuctions();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thanh toán thất bại. " + response.getContent());
        }
    }

    /**
     * Hiển thị hộp thoại xác nhận (Yes/No) và trả về kết quả lựa chọn.
     *
     * @param title   tiêu đề hộp thoại
     * @param message nội dung thông báo
     * @return true nếu người dùng chọn "Có", false nếu chọn "Không"
     */
    private boolean showConfirm(String title, String message) {
        return AlertUtils.showConfirm(title, message);
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

    /**
     * Hiển thị thông báo trên giao diện JavaFX (chạy trên FX Thread).
     *
     * @param type    loại thông báo (INFORMATION, WARNING, ERROR)
     * @param title   tiêu đề thông báo
     * @param message nội dung thông báo
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        AlertUtils.showAlertOnFXThread(type, title, message);
    }

    /**
     * Bật/Tắt trạng thái theo dõi (Watchlist) của phiên đấu giá hiện tại.
     * Cập nhật ký tự trên nút bấm sau khi thao tác thành công.
     */
    @FXML
    private void toggleWatchlist() {
        if (selectedAuction == null || watchlistToggleButton == null) return;
        boolean currentlyWatched = "★".equals(watchlistToggleButton.getText());
        Message res;
        if (currentlyWatched) {
            res = networkService.removeWatchlist(selectedAuction.getId());
        } else {
            res = networkService.addWatchlist(selectedAuction.getId());
        }

        if (res.getType() == Message.Type.SUCCESS) {
            watchlistToggleButton.setText(currentlyWatched ? "☆" : "★");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật theo dõi: " + res.getContent());
        }
    }

    /**
     * Tải và hiển thị lịch sử tin nhắn chat trực tuyến trong phiên đấu giá.
     *
     * @param auctionId ID của phiên đấu giá
     */
    private void loadChatHistory(String auctionId) {
        if (chatList == null) return;

        Message response = networkService.getChatHistory(auctionId);
        if (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<ChatMessage> chats = (List<ChatMessage>) response.getData();
            lastChatCount = chats.size();
            chatList.getChildren().clear();
            for (ChatMessage chat : chats) {
                chatList.getChildren().add(createChatBubble(chat));
            }
            scrollChatToBottom();
        }
    }

    /**
     * Refresh tin nhắn chat trực tuyến nếu có tin nhắn mới gửi lên hệ thống.
     *
     * @param auctionId ID của phiên đấu giá
     */
    private void refreshChatHistory(String auctionId) {
        if (chatList == null) return;

        Message response = networkService.getChatHistory(auctionId);
        if (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<ChatMessage> chats = (List<ChatMessage>) response.getData();
            if (chats.size() > lastChatCount) {
                for (int i = lastChatCount; i < chats.size(); i++) {
                    chatList.getChildren().add(createChatBubble(chats.get(i)));
                }
                lastChatCount = chats.size();
                scrollChatToBottom();
            }
        }
    }

    /**
     * Tạo giao diện bong bóng tin nhắn hiển thị tên người gửi và nội dung tin nhắn.
     *
     * @param chat đối tượng tin nhắn
     * @return HBox chứa bong bóng chat được định dạng
     */
    private HBox createChatBubble(ChatMessage chat) {
        HBox bubbleContainer = new HBox();
        bubbleContainer.setPadding(new Insets(3, 5, 3, 5));

        VBox textContainer = new VBox();
        textContainer.setPadding(new Insets(5, 10, 5, 10));
        textContainer.setMaxWidth(300);

        Label senderLabel = new Label(chat.getSenderName() != null ? chat.getSenderName() : chat.getSenderId());
        senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        Label messageLabel = new Label(chat.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px;");

        textContainer.getChildren().addAll(senderLabel, messageLabel);

        boolean isMe = currentUser != null && chat.getSenderId().equals(currentUser.getId());
        if (isMe) {
            bubbleContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            textContainer.setStyle("-fx-background-color: #d9b15f; -fx-background-radius: 10 10 0 10;");
            senderLabel.setStyle(senderLabel.getStyle() + " -fx-text-fill: #1E1E1D;");
            messageLabel.setStyle(messageLabel.getStyle() + " -fx-text-fill: #1E1E1D;");
        } else {
            bubbleContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            textContainer.setStyle("-fx-background-color: #111111; -fx-border-color: #d4af5a; -fx-border-width: 1; -fx-background-radius: 10 10 10 0; -fx-border-radius: 10 10 10 0;");
            senderLabel.setStyle(senderLabel.getStyle() + " -fx-text-fill: #eacd8f;");
            messageLabel.setStyle(messageLabel.getStyle() + " -fx-text-fill: #ffffff;");
        }

        bubbleContainer.getChildren().add(textContainer);
        return bubbleContainer;
    }

    /**
     * Tự động cuộn khung chat xuống cuối cùng.
     */
    private void scrollChatToBottom() {
        if (chatScrollPane != null) {
            Platform.runLater(() -> {
                chatScrollPane.setVvalue(1.0);
            });
        }
    }

    /**
     * Gửi tin nhắn chat trong phòng đấu giá hiện tại lên server.
     */
    @FXML
    private void sendChatMessage() {
        if (selectedAuction == null || chatInputField == null) return;
        String text = chatInputField.getText().trim();
        if (text.isEmpty()) return;

        Message res = networkService.sendChatMessage(selectedAuction.getId(), text);
        if (res.getType() == Message.Type.SUCCESS) {
            chatInputField.clear();
            refreshChatHistory(selectedAuction.getId());
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể gửi tin nhắn: " + res.getContent());
        }
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
