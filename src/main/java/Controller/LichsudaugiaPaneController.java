package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Model.AuctionSession;
import Model.User;
import Network.Message;
import Network.NetworkService;

public class LichsudaugiaPaneController implements UserController.LinkedController {

    @FXML private ScrollPane bidHistoryScrollPane;
    @FXML private VBox bidHistoryList;

    private UserController userController;
    private NetworkService networkService = NetworkService.getInstance();

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    public void setUserData(User user) {
        loadBidHistory();
    }

    public void loadBidHistory() {
        if (bidHistoryList == null) return;
        bidHistoryList.getChildren().clear();

        if (userController == null || userController.getCurrentUser() == null) return;

        Message response = networkService.getUserAuctions(userController.getCurrentUser().getId());
        List<AuctionSession> auctions = (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List)
                ? (List<AuctionSession>) response.getData() : List.of();

        if (auctions.isEmpty()) {
            Label emptyLabel = new Label("Chưa có lịch sử đấu giá");
            emptyLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
            bidHistoryList.getChildren().add(emptyLabel);
            return;
        }

        for (AuctionSession auction : auctions) {
            HBox card = createFinishedAuctionCard(auction);
            bidHistoryList.getChildren().add(card);
        }
    }

    private HBox createFinishedAuctionCard(AuctionSession auction) {
        HBox card = new HBox(15);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #111111; -fx-border-color: #d4af5a; -fx-padding: 12; -fx-background-radius: 5; -fx-border-radius: 5;");
        card.setPrefWidth(Double.MAX_VALUE);
        card.setCursor(Cursor.HAND);

        String itemName = auction.getItem() != null ? auction.getItem().getName() : "Unknown";
        Label nameLabel = new Label(itemName);
        nameLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 14px; -fx-font-weight: bold;");
        nameLabel.setPrefWidth(180);

        String statusText = auction.getStatus() == AuctionSession.Status.FINISHED ? "Đã kết thúc" : "Đã thanh toán";
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill: " + (auction.getStatus() == AuctionSession.Status.FINISHED ? "#ff6b6b" : "#4CAF50") + "; -fx-font-size: 12px;");
        statusLabel.setPrefWidth(100);

        Label priceLabel = new Label(userController.formatMoney(new BigDecimal(String.valueOf(auction.getCurrentPrice()))) + " $");
        priceLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px; -fx-font-weight: bold;");
        priceLabel.setPrefWidth(120);

        String endTimeStr = auction.getEndTime() != null
                ? auction.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                : "--";
        Label timeLabel = new Label(endTimeStr);
        timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
        timeLabel.setPrefWidth(80);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label viewLabel = new Label("Xem >");
        viewLabel.setStyle("-fx-text-fill: #d9b15f; -fx-font-size: 13px; -fx-font-weight: bold;");

        card.getChildren().addAll(nameLabel, statusLabel, priceLabel, timeLabel, spacer, viewLabel);

        card.setOnMouseClicked(e -> openBidChartPopup(auction));

        return card;
    }

    private void openBidChartPopup(AuctionSession auction) {
        try {
            URL resourceUrl = getClass().getResource("/bid_chart_view.fxml");
            if (resourceUrl == null) {
                resourceUrl = Thread.currentThread().getContextClassLoader().getResource("bid_chart_view.fxml");
            }
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Object childController = loader.getController();
            if (childController instanceof BidChartViewController chartController) {
                chartController.setAuction(auction);
            }

            Stage stage = new Stage();
            stage.setTitle("Lịch sử đấu giá - " + auction.getItem().getName());
            stage.setScene(new Scene(root));
            stage.initOwner(userController.getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            userController.showError("Lỗi", "Không thể mở biểu đồ: " + e.getMessage());
        }
    }
}
