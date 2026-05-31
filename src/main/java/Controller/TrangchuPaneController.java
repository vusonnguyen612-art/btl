package Controller;

import Controller.utils.CategoryMapper;
import Controller.utils.ResponseUtils;
import Controller.utils.UIUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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

    /**
     * Gán {@link UserController} để controller này có thể gọi lại
     * các phương thức của controller chính (formatMoney, createEmptyLabel, v.v.).
     *
     * @param uc UserController quản lý điều khiển người dùng
     */
    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    /**
     * Khởi tạo giao diện trang chủ khi FXML được load.
     * Thiết lập các ComboBox tìm kiếm, cuộn chuột trên ScrollPane,
     * và sửa lỗi hiển thị viewport của ScrollPane.
     */
    @FXML
    private void initialize() {
        initHomeSearchCombos();
        UIUtils.setupScrollFocus(homeScrollPane);
        UIUtils.fixScrollPaneViewport(homeScrollPane);
    }

    /**
     * Nhận dữ liệu người dùng từ controller chính và tải danh sách sản phẩm lên giao diện.
     *
     * @param user đối tượng người dùng hiện tại
     */
    public void setUserData(User user) {
        loadHomeItems();
    }

    /**
     * Cập nhật hiển thị số dư tài khoản trên giao diện trang chủ.
     *
     * @param balance số dư tài khoản mới cần hiển thị
     */
    public void updateBalance(BigDecimal balance) {
        if (Sodutaikhoan1 != null) {
            Sodutaikhoan1.setText(userController.formatMoney(balance));
        }
    }

    /**
     * Tải tất cả sản phẩm đấu giá từ server, phân loại thành sản phẩm đang đấu giá
     * và sản phẩm chưa đấu giá, rồi hiển thị lên giao diện dưới dạng card.
     * Sản phẩm đang đấu giá sẽ được ưu tiên hiển thị trước.
     */
    public void loadHomeItems() {
        if (AllItems == null) return;
        AllItems.getChildren().clear();

        try {
            Message itemsResponse = networkService.getItems();
            List<Item> allItems = ResponseUtils.extractList(itemsResponse);
            List<AuctionSession> allAuctions = ResponseUtils.extractList(networkService.getAuctions());

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
        } catch (Exception e) {
            Label errorLabel = userController.createEmptyLabel("Lỗi tại sản phẩm.");
            AllItems.getChildren().add(errorLabel);
        }
    }

    /**
     * Tìm kiếm phiên đấu giá theo các tiêu chí: từ khóa, danh mục,
     * trạng thái, khoảng giá, và thứ tự sắp xếp.
     * Hiển thị kết quả tìm kiếm trên giao diện.
     */
    @FXML
    private void searchHomeAuctions() {
        if (AllItems == null) return;

        SearchCriteria criteria = new SearchCriteria();
        String keyword = homeSearchKeyword.getText().trim();
        if (!keyword.isEmpty()) criteria.setKeyword(keyword);

        String category = homeSearchCategory.getValue();
        if (category != null && !category.equals("Tất cả")) {
            criteria.setCategory(CategoryMapper.toEnglish(category));
        }

        String status = homeSearchStatus.getValue();
        if (status != null && !status.equals("Tất cả")) {
            java.util.List<AuctionSession.Status> statuses = CategoryMapper.mapStatus(status);
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
        List<AuctionSession> results = ResponseUtils.extractList(networkService.searchAuctions(criteria));

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
     * Xóa tất cả các trường tìm kiếm về giá trị mặc định
     * và tải lại toàn bộ sản phẩm lên giao diện.
     */
    @FXML
    private void resetHomeSearch() {
        UIUtils.resetSearchFields(homeSearchKeyword, homeSearchCategory, homeSearchStatus, homeSearchMinPrice, homeSearchMaxPrice, homeSearchSort);
        loadHomeItems();
    }

    /**
     * Mở phòng đấu giá khi người dùng nhấp vào nút tương ứng.
     * Delegate sang {@link UserController#openAuctionRoom(ActionEvent)}.
     *
     * @param event sự kiện nhấp chuột
     */
    @FXML
    private void openAuctionRoom(ActionEvent event) {
        if (userController != null) {
            userController.openAuctionRoom(event);
        }
    }

    /**
     * Khởi tạo các ComboBox tìm kiếm (danh mục, trạng thái, sắp xếp)
     * với các giá trị mặc định thông qua {@link UIUtils}.
     */
    private void initHomeSearchCombos() {
        UIUtils.initSearchCombos(homeSearchCategory, homeSearchStatus, homeSearchSort);
    }

    /**
     * Tạo card hiển thị thông tin sản phẩm đấu giá từ file FXML item_card.fxml.
     * Gán dữ liệu sản phẩm và danh sách phiên đấu giá cho controller của card.
     *
     * @param item      sản phẩm cần hiển thị
     * @param auctions  danh sách tất cả phiên đấu giá để xác định trạng thái sản phẩm
     * @return VBox chứa giao diện card sản phẩm, hoặc VBox rỗng nếu có lỗi
     */
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
