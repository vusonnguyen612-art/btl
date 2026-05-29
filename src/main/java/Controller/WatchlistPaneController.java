package Controller;

import Controller.utils.ResponseUtils;
import Controller.utils.UIUtils;
import Model.AuctionSession;
import Network.Message;
import Network.NetworkService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller cho giao diện danh sách phiên đấu giá đang được theo dõi (Watchlist).
 * Quản lý việc hiển thị danh sách và xử lý các sự kiện tương tác trên card.
 */
public class WatchlistPaneController implements UserController.LinkedController {

    @FXML private ScrollPane watchlistScrollPane;
    @FXML private FlowPane watchlistFlowPane;

    private UserController userController;
    private final NetworkService networkService = NetworkService.getInstance();

    /**
     * Gán UserController để liên kết chuyển màn hình hoặc gọi các thông báo.
     *
     * @param userController Đối tượng UserController cha điều phối.
     */
    @Override
    public void setUserController(UserController userController) {
        this.userController = userController;
    }

    /**
     * Hàm initialize của JavaFX để cài đặt sự kiện chuột và màu nền của ScrollPane.
     */
    @FXML
    private void initialize() {
        UIUtils.setupScrollFocus(watchlistScrollPane);
        UIUtils.fixScrollPaneViewport(watchlistScrollPane);
    }

    /**
     * Tải và vẽ danh sách các phiên đấu giá đang được theo dõi từ server.
     */
    public void loadWatchlist() {
        if (watchlistFlowPane == null) return;
        watchlistFlowPane.getChildren().clear();

        try {
            Message response = networkService.getWatchlist();
            List<AuctionSession> list = ResponseUtils.extractList(response);
            if (list.isEmpty()) {
                Label emptyLabel = new Label("Bạn chưa theo dõi phiên đấu giá nào.");
                emptyLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px; -fx-alignment: center;");
                emptyLabel.prefWidthProperty().bind(watchlistFlowPane.widthProperty());
                watchlistFlowPane.getChildren().add(emptyLabel);
            } else {
                for (AuctionSession auction : list) {
                    VBox card = createAuctionCard(auction);
                    watchlistFlowPane.getChildren().add(card);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label("Lỗi hệ thống khi tải danh sách.");
            errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");
            watchlistFlowPane.getChildren().add(errorLabel);
        }
    }

    /**
     * Tạo card hiển thị thông tin phiên đấu giá được theo dõi.
     * Cài đặt các sự kiện tương tác như chọn phiên, bắt đầu phiên.
     *
     * @param auction Đối tượng phiên đấu giá cần hiển thị.
     * @return Node VBox chứa giao diện card phiên đấu giá.
     */
    private VBox createAuctionCard(AuctionSession auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_card.fxml"));
            VBox card = loader.load();
            AuctionCardController controller = loader.getController();

            boolean isRunning = auction.isRunning();
            boolean canStart = auction.isOpen() && auction.getSellerId().equals(networkService.getCurrentUser().getId());
            boolean isPaymentPending = auction.isPaymentPending();

            controller.setAuction(auction, isRunning, canStart, isPaymentPending);

            controller.setOnSelectAuction(() -> {
                if (userController != null) {
                    userController.openAuctionRoomForAuction(auction);
                }
            });

            controller.setOnStartAuction(() -> {
                Message res = networkService.startAuction(auction.getId());
                if (res.getType() == Message.Type.SUCCESS) {
                    if (userController != null) {
                        userController.showInfo("Thành công", "Phiên đấu giá đã được bắt đầu!");
                    }
                    loadWatchlist();
                } else {
                    if (userController != null) {
                        userController.showError("Lỗi", "Không thể bắt đầu phiên: " + res.getContent());
                    }
                }
            });

            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new VBox();
        }
    }
}
