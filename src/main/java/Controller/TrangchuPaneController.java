package Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import Model.AuctionSession;
import Model.Item;
import Model.SearchCriteria;
import Model.User;
import Network.Message;
import Network.NetworkService;

/**
 * Controller cho pane Trang chủ (FXML: Indivisual.fxml -> TrangchuPane).
 * Hiển thị danh sách tất cả sản phẩm đấu giá dưới dạng các item card,
 * hỗ trợ tìm kiếm, lọc theo danh mục, trạng thái, khoảng giá, và sắp xếp.
 * Cho phép mở phòng đấu giá từ trang chủ.
 *
 * <p>Implement {@link UserController.LinkedController} để nhận tham chiếu
 * đến {@link UserController} cha.</p>
 */
public class TrangchuPaneController implements UserController.LinkedController {

    @FXML private AnchorPane TrangchuPane;
    @FXML private ScrollPane homeScrollPane;
    @FXML private FlowPane AllItems;
    @FXML private Label Sodutaikhoan1;
    @FXML private TextField homeSearchKeyword;
    @FXML private ComboBox<String> homeSearchCategory;
    @FXML private ComboBox<String> homeSearchStatus;
    @FXML private TextField homeSearchMinPrice;
    @FXML private TextField homeSearchMaxPrice;
    @FXML private ComboBox<String> homeSearchSort;

    private UserController userController;
    private NetworkService networkService = NetworkService.getInstance();

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    /**
     * Khởi tạo: thiết lập các ComboBox tìm kiếm, focus cho ScrollPane, và màu nền viewport.
     */
    @FXML
    private void initialize() {
        initHomeSearchCombos();
        setupScrollFocus();
        fixScrollPaneViewport();
    }

    /**
     * Thiết lập màu nền cho viewport của ScrollPane để đồng bộ với theme tối.
     */
    private void fixScrollPaneViewport() {
        Platform.runLater(() -> {
            Node viewport = homeScrollPane.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: #1E1E1D;");
            }
        });
    }

    /**
     * Gán thông tin người dùng và tải lại danh sách sản phẩm trang chủ.
     *
     * @param user Đối tượng User hiện tại.
     */
    public void setUserData(User user) {
        loadHomeItems();
    }

    /**
     * Cập nhật số dư hiển thị trên Label Sodutaikhoan1.
     *
     * @param balance Số dư mới của người dùng.
     */
    public void updateBalance(BigDecimal balance) {
        if (Sodutaikhoan1 != null) {
            Sodutaikhoan1.setText(userController.formatMoney(balance));
        }
    }

    /**
     * Tải danh sách sản phẩm đấu giá từ server và hiển thị dưới dạng các item card
     * trong FlowPane AllItems. Các sản phẩm đang diễn ra được ưu tiên hiển thị trước.
     */
    public void loadHomeItems() {
        if (AllItems == null) return;
        AllItems.getChildren().clear();

        try {
            Message itemsResponse = networkService.getItems();
            Message auctionsResponse = networkService.getAuctions();
            if (itemsResponse.getType() == Message.Type.SUCCESS && itemsResponse.getData() instanceof List) {
                List<Item> allItems = (List<Item>) itemsResponse.getData();
                List<AuctionSession> allAuctions = (auctionsResponse.getType() == Message.Type.SUCCESS && auctionsResponse.getData() instanceof List)
                        ? (List<AuctionSession>) auctionsResponse.getData() : List.of();

                if (allItems.isEmpty()) {
                    Label emptyLabel = userController.createEmptyLabel("Chưa có sản phẩm đấu giá nào.");
                    AllItems.getChildren().add(emptyLabel);
                } else {
                    Label headerLabel = new Label("Sản phẩm đấu giá");
                    headerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 20px; -fx-font-weight: bold;");
                    headerLabel.setPadding(new Insets(0, 0, 10, 0));
                    headerLabel.prefWidthProperty().bind(AllItems.widthProperty());
                    AllItems.getChildren().add(headerLabel);

                    List<Item> runningItems = new ArrayList<>();
                    List<Item> otherItems = new ArrayList<>();
                    java.util.Set<String> runningItemIds = new java.util.HashSet<>();
                    for (AuctionSession auction : allAuctions) {
                        if (auction.isRunning() && auction.getItem() != null) {
                            runningItemIds.add(auction.getItem().getId());
                        }
                    }
                    for (Item item : allItems) {
                        if (runningItemIds.contains(item.getId())) {
                            runningItems.add(item);
                        } else {
                            otherItems.add(item);
                        }
                    }
                    runningItems.addAll(otherItems);

                    for (Item item : runningItems) {
                        VBox itemBox = createItemCard(item, allAuctions);
                        AllItems.getChildren().add(itemBox);
                    }
                }
            }
        } catch (Exception e) {
            Label errorLabel = userController.createEmptyLabel("Lỗi tại sản phẩm.");
            AllItems.getChildren().add(errorLabel);
        }
    }

    /**
     * Xử lý tìm kiếm nâng cao trên trang chủ: lọc theo từ khóa, danh mục, trạng thái,
     * khoảng giá, và sắp xếp. Kết quả hiển thị trong FlowPane AllItems.
     */
    @FXML
    private void searchHomeAuctions() {
        if (AllItems == null) return;

        SearchCriteria criteria = new SearchCriteria();
        String keyword = homeSearchKeyword.getText().trim();
        if (!keyword.isEmpty()) criteria.setKeyword(keyword);

        String category = homeSearchCategory.getValue();
        if (category != null && !category.equals("Tất cả")) {
            String catMap = switch (category) {
                case "Điện tử" -> "ELECTRONICS";
                case "Xe cộ" -> "VEHICLE";
                case "Nghệ thuật" -> "ART";
                case "Thời trang" -> "FASHION";
                case "Sách" -> "BOOKS";
                case "Thể thao" -> "SPORTS";
                case "Trang sức" -> "JEWELRY";
                case "Âm nhạc" -> "MUSIC";
                case "Nội thất" -> "FURNITURE";
                default -> null;
            };
            criteria.setCategory(catMap);
        }

        String status = homeSearchStatus.getValue();
        if (status != null && !status.equals("Tất cả")) {
            List<AuctionSession.Status> statuses = new ArrayList<>();
            switch (status) {
                case "Đang diễn ra" -> statuses.add(AuctionSession.Status.RUNNING);
                case "Sắp diễn ra" -> statuses.add(AuctionSession.Status.OPEN);
                case "Chờ thanh toán" -> statuses.add(AuctionSession.Status.PAYMENT_PENDING);
                case "Đã kết thúc" -> {
                    statuses.add(AuctionSession.Status.FINISHED);
                    statuses.add(AuctionSession.Status.PAID);
                }
                case "Đã hủy" -> statuses.add(AuctionSession.Status.CANCELED);
            }
            if (!statuses.isEmpty()) criteria.setStatuses(statuses);
        }

        String minPriceText = homeSearchMinPrice.getText().trim();
        if (!minPriceText.isEmpty()) {
            try { criteria.setMinPrice(Double.parseDouble(minPriceText)); } catch (NumberFormatException ignored) {}
        }

        String maxPriceText = homeSearchMaxPrice.getText().trim();
        if (!maxPriceText.isEmpty()) {
            try { criteria.setMaxPrice(Double.parseDouble(maxPriceText)); } catch (NumberFormatException ignored) {}
        }

        String sort = homeSearchSort.getValue();
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

        AllItems.getChildren().clear();
        Message response = networkService.searchAuctions(criteria);
        List<AuctionSession> results = (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List)
                ? (List<AuctionSession>) response.getData() : List.of();

        if (results.isEmpty()) {
            Label emptyLabel = userController.createEmptyLabel("Không tìm thấy phiên đấu giá nào.");
            AllItems.getChildren().add(emptyLabel);
            return;
        }

        Label headerLabel = new Label("Kết quả tìm kiếm (" + results.size() + ")");
        headerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 20px; -fx-font-weight: bold;");
        headerLabel.setPadding(new Insets(0, 0, 10, 0));
        headerLabel.prefWidthProperty().bind(AllItems.widthProperty());
        AllItems.getChildren().add(headerLabel);

        for (AuctionSession auction : results) {
            if (auction.getItem() != null) {
                VBox itemBox = createItemCard(auction.getItem(), results);
                AllItems.getChildren().add(itemBox);
            }
        }
    }

    /**
     * Đặt lại tất cả các trường tìm kiếm về giá trị mặc định và tải lại toàn bộ danh sách.
     */
    @FXML
    private void resetHomeSearch() {
        if (homeSearchKeyword != null) homeSearchKeyword.clear();
        if (homeSearchCategory != null) homeSearchCategory.getSelectionModel().select("Tất cả");
        if (homeSearchStatus != null) homeSearchStatus.getSelectionModel().select("Tất cả");
        if (homeSearchMinPrice != null) homeSearchMinPrice.clear();
        if (homeSearchMaxPrice != null) homeSearchMaxPrice.clear();
        if (homeSearchSort != null) homeSearchSort.getSelectionModel().select("Mới nhất");
        loadHomeItems();
    }

    /**
     * Mở phòng đấu giá thông qua UserController cha.
     *
     * @param event ActionEvent kích hoạt.
     */
    @FXML
    private void openAuctionRoom(ActionEvent event) {
        if (userController != null) {
            userController.openAuctionRoom(event);
        }
    }

    private void initHomeSearchCombos() {
        if (homeSearchCategory != null) {
            homeSearchCategory.getItems().add("Tất cả");
            homeSearchCategory.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật", "Thời trang", "Sách", "Thể thao", "Trang sức", "Âm nhạc", "Nội thất");
            homeSearchCategory.getSelectionModel().select("Tất cả");
        }
        if (homeSearchStatus != null) {
            homeSearchStatus.getItems().add("Tất cả");
            homeSearchStatus.getItems().addAll("Đang diễn ra", "Sắp diễn ra", "Chờ thanh toán", "Đã kết thúc", "Đã hủy");
            homeSearchStatus.getSelectionModel().select("Tất cả");
        }
        if (homeSearchSort != null) {
            homeSearchSort.getItems().addAll("Mới nhất", "Cũ nhất", "Giá tăng dần", "Giá giảm dần", "Tên A-Z");
            homeSearchSort.getSelectionModel().select("Mới nhất");
        }
    }

    private void setupScrollFocus() {
        if (homeScrollPane != null) {
            homeScrollPane.setOnMouseEntered(e -> homeScrollPane.requestFocus());
        }
    }

    private VBox createItemCard(Item item, List<AuctionSession> auctions) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/item_card.fxml"));
            VBox card = loader.load();
            ItemCardController controller = loader.getController();
            controller.setItem(item, auctions);
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new VBox();
        }
    }
}
