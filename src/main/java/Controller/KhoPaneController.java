package Controller;

import Controller.utils.ResponseUtils;
import Controller.utils.UIUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

import Model.AuctionSession;
import Model.Item;
import Model.User;
import Network.Message;
import Network.NetworkService;

public class KhoPaneController implements UserController.LinkedController {

    @FXML private FlowPane Items;
    @FXML private ScrollPane khoScrollPane;

    private UserController userController;
    private NetworkService networkService = NetworkService.getInstance();

    /**
     * Khởi tạo giao diện kho khi FXML được load.
     * Sửa lỗi hiển thị viewport của ScrollPane để cuộn hoạt động chính xác.
     */
    @FXML
    private void initialize() {
        UIUtils.fixScrollPaneViewport(khoScrollPane);
    }

    /**
     * Gán {@link UserController} để controller kho có thể gọi lại
     * các phương thức của controller chính (getCurrentUser, createEmptyLabel, v.v.).
     *
     * @param uc UserController quản lý điều khiển người dùng
     */
    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    /**
     * Nhận dữ liệu người dùng từ controller chính và tải danh sách sản phẩm trong kho.
     *
     * @param user đối tượng người dùng hiện tại
     */
    public void setUserData(User user) {
        loadWarehouseItems();
    }

    /**
     * Tải danh sách phiên đấu giá của người dùng hiện tại từ server.
     * Hiển thị các card phiên đấu giá mà người dùng là người bán.
     * Nếu chưa có phiên đấu giá nào, hiển thị thông báo hướng dẫn tạo sản phẩm.
     */
    public void loadWarehouseItems() {
        if (Items == null) return;
        Items.getChildren().clear();

        if (userController == null || userController.getCurrentUser() == null) {
            Label emptyLabel = userController != null
                    ? userController.createEmptyLabel("Vui lòng đăng nhập.")
                    : new Label("Vui lòng đăng nhập.");
            Items.getChildren().add(emptyLabel);
            return;
        }

        try {
            List<Item> userItems = ResponseUtils.extractList(networkService.getItems());
            List<AuctionSession> userAuctions = ResponseUtils.extractList(networkService.getAuctions());

            if (userItems.isEmpty() && userAuctions.isEmpty()) {
                Label emptyLabel = userController.createEmptyLabel("Kho của bạn hiện chưa có sản phẩm.");
                Items.getChildren().add(emptyLabel);
            } else {
                Label headerLabel = new Label("Phiên đấu giá của bạn");
                headerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px; -fx-font-weight: bold;");
                headerLabel.setPadding(new javafx.geometry.Insets(0, 0, 10, 0));
                headerLabel.prefWidthProperty().bind(Items.widthProperty());
                Items.getChildren().add(headerLabel);

                boolean hasAuctions = false;
                for (AuctionSession auction : userAuctions) {
                    if (auction.getSellerId() != null && auction.getSellerId().equals(userController.getCurrentUser().getId())) {
                        VBox auctionCard = createAuctionCard(auction);
                        Items.getChildren().add(auctionCard);
                        hasAuctions = true;
                    }
                }

                if (!hasAuctions && !userItems.isEmpty()) {
                    Label noAuctionLabel = new Label("Chưa tạo phiên đấu giá. Nhấn 'Tạo sản phẩm để bắt đầu.");
                    noAuctionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
                    noAuctionLabel.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));
                    noAuctionLabel.prefWidthProperty().bind(Items.widthProperty());
                    Items.getChildren().add(noAuctionLabel);
                }
            }
        } catch (Exception e) {
            Label errorLabel = new Label("Lỗi tại kho: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px;");
            Items.getChildren().add(errorLabel);
        }
    }

    /**
     * Xử lý sự kiện nhấp nút "Tạo sản phẩm".
     * Delegate sang {@link UserController#createItems(ActionEvent)} để mở form tạo sản phẩm mới.
     *
     * @param event sự kiện nhấp chuột
     */
    @FXML
    private void onCreateItems(ActionEvent event) {
        if (userController != null) {
            userController.createItems(event);
        }
    }

    /**
     * Tạo card hiển thị thông tin phiên đấu giá từ file FXML auction_card.fxml.
     * Gán dữ liệu phiên đấu giá và các callback: chọn phòng đấu giá,
     * bắt đầu đấu giá, chỉnh sửa sản phẩm.
     *
     * @param auction phiên đấu giá cần hiển thị
     * @return VBox chứa giao diện card phiên đấu giá, hoặc VBox rỗng nếu có lỗi
     */
    private VBox createAuctionCard(AuctionSession auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_card.fxml"));
            VBox card = loader.load();
            card.setPrefWidth(185);
            AuctionCardController controller = loader.getController();

            boolean isRunning = auction.isRunning();
            boolean canStart = auction.isOpen();

            controller.setAuction(auction, isRunning, canStart);
            controller.setOnSelectAuction(() -> {
                if (userController != null) {
                    userController.openAuctionRoomForAuction(auction);
                }
            });
            controller.setOnStartAuction(() -> {
                Message response = networkService.startAuction(auction.getId());
                if (response.getType() == Message.Type.SUCCESS) {
                    userController.showInfo("Thành công", "Đã bắt đầu phiên đấu giá.");
                    loadWarehouseItems();
                    if (userController != null) userController.refreshHomeItems();
                } else {
                    userController.showError("Lỗi", "Không thể bắt đầu phiên đấu giá.");
                }
            });
            controller.setOnEditAuction(() -> {
                if (userController != null && auction.getItem() != null) {
                    userController.editItem(auction.getItem());
                }
            });

            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new VBox();
        }
    }
}
