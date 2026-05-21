package Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
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

    @FXML
    private void initialize() {
        fixScrollPaneViewport();
    }

    private void fixScrollPaneViewport() {
        Platform.runLater(() -> {
            Node viewport = khoScrollPane.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: #1E1E1D;");
            }
        });
    }

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    public void setUserData(User user) {
        loadWarehouseItems();
    }

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
            Message itemsResponse = networkService.getItems();
            Message auctionsResponse = networkService.getAuctions();

            List<Item> userItems = (itemsResponse.getType() == Message.Type.SUCCESS && itemsResponse.getData() instanceof List)
                    ? (List<Item>) itemsResponse.getData() : List.of();
            List<AuctionSession> userAuctions = (auctionsResponse.getType() == Message.Type.SUCCESS && auctionsResponse.getData() instanceof List)
                    ? (List<AuctionSession>) auctionsResponse.getData() : List.of();

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

    @FXML
    private void onCreateItems(ActionEvent event) {
        if (userController != null) {
            userController.createItems(event);
        }
    }

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
