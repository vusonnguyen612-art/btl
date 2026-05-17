package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import Model.User;
import Network.Message;
import Network.NetworkService;

public class UserController {

    @FXML
    private Label Name;

    @FXML
    private ToggleButton TrangchuButton;

    @FXML
    private ToggleButton KhoButton;

    @FXML
    private ToggleButton NaptienButton;

    @FXML
    private ToggleButton BidHistory;

    @FXML
    private ToggleButton CaidatButton;

    @FXML
    private AnchorPane TrangchuPane;

    @FXML
    private AnchorPane KhoPane;

    @FXML
    private AnchorPane NaptienPane;

    @FXML
    private AnchorPane LichsudaugiaPane;

    @FXML
    private AnchorPane CaidatPane;

    @FXML
    private TrangchuPaneController TrangchuPaneController;

    @FXML
    private KhoPaneController KhoPaneController;

    @FXML
    private LichsudaugiaPaneController LichsudaugiaPaneController;

    @FXML
    private NaptienPaneController NaptienPaneController;

    @FXML
    private CaidatPaneController CaidatPaneController;

    private ToggleGroup menuGroup = new ToggleGroup();
    private User currentUser;
    private NetworkService networkService = NetworkService.getInstance();
    private BigDecimal soDuTaiKhoan = new BigDecimal("300000");

    private static final String MENU_STYLE =
            "-fx-background-color: #1E1E1D;"
                    + "-fx-text-fill: #eacd8f;";

    private static final String MENU_SELECTED_STYLE =
            "-fx-background-color: #d9b15f;"
                    + "-fx-text-fill: #1E1E1D;"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-radius: 20;";

    private static final String CREATE_ITEM_FXML = "/CreateItems.fxml";
    private static final String LOGIN_FXML = "/login.fxml";

    @FXML
    private void initialize() {
        connectChildController(TrangchuPaneController);
        connectChildController(KhoPaneController);
        connectChildController(LichsudaugiaPaneController);
        connectChildController(NaptienPaneController);
        connectChildController(CaidatPaneController);

        setupMenuButtons();
        setupDefaultScreen();

        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
        if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
    }

    private void setupMenuButtons() {
        bindMenuButton(TrangchuButton, TrangchuPane);
        bindMenuButton(KhoButton, KhoPane);
        bindMenuButton(NaptienButton, NaptienPane);
        bindMenuButton(BidHistory, LichsudaugiaPane);
        bindMenuButton(CaidatButton, CaidatPane);
    }

    private void bindMenuButton(ToggleButton button, AnchorPane pane) {
        if (button == null || pane == null) {
            return;
        }

        button.setToggleGroup(menuGroup);
        button.setStyle(MENU_STYLE);

        button.setOnAction(event -> {
            showPane(pane);
            menuGroup.selectToggle(button);
            updateMenuStyle();
        });
    }

    private void setupDefaultScreen() {
        showPane(TrangchuPane);

        if (TrangchuButton != null) {
            menuGroup.selectToggle(TrangchuButton);
        }

        updateMenuStyle();
    }

    private void showPane(AnchorPane paneToShow) {
        AnchorPane[] panes = {
                TrangchuPane,
                KhoPane,
                NaptienPane,
                LichsudaugiaPane,
                CaidatPane
        };

        for (AnchorPane pane : panes) {
            if (pane != null) {
                pane.setVisible(false);
                pane.setManaged(false);
            }
        }

        if (paneToShow != null) {
            paneToShow.setVisible(true);
            paneToShow.setManaged(true);
            if (paneToShow == LichsudaugiaPane && LichsudaugiaPaneController != null) {
                LichsudaugiaPaneController.loadBidHistory();
            }
        }
    }

    private void updateMenuStyle() {
        ToggleButton[] buttons = {
                TrangchuButton,
                KhoButton,
                NaptienButton,
                BidHistory,
                CaidatButton
        };

        for (ToggleButton button : buttons) {
            if (button == null) {
                continue;
            }

            if (button.isSelected()) {
                button.setStyle(MENU_SELECTED_STYLE);
            } else {
                button.setStyle(MENU_STYLE);
            }
        }
    }

    public String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value);
    }

    @FXML
    public void openAuctionRoom(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionRoom.fxml"));
            Parent root = loader.load();

            AuctionRoomController auctionRoomController = loader.getController();
            auctionRoomController.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setTitle("Phong Dau Gia");
            stage.setScene(new Scene(root, 900, 600));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setOnHidden(e -> {
                auctionRoomController.stopRefresh();
                Message balanceResponse = networkService.getUserBalance();
                if (balanceResponse.getType() == Message.Type.SUCCESS && balanceResponse.getData() != null) {
                    soDuTaiKhoan = (BigDecimal) balanceResponse.getData();
                    updateAllBalances();
                }
                if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
                if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
            });
            stage.show();

        } catch (Exception e) {
            showError("Lỗi", "Không thể mở phòng đấu giá: " + e.getMessage());
        }
    }

    public void createItems(ActionEvent event) {
        openModalFXML(CREATE_ITEM_FXML, "Tạo sản phẩm");
        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
        if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
    }

    public void doitaikhoan(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Đổi tài khoản",
                "Bạn có chắc muốn đăng xuất và đổi tài khoản không?"
        );

        if (!confirmed) {
            return;
        }

        switchScene(LOGIN_FXML, "Đăng nhập");
    }

    public void exit(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Thoát",
                "Bạn có chắc chắn muốn thoát chương trình không?"
        );

        if (!confirmed) {
            return;
        }

        Stage stage = getCurrentStage();
        if (stage != null) {
            stage.close();
        }
    }

    public void setUserData(User user) {
        this.currentUser = user;

        if (user != null && user.getUsername() != null) {
            if (Name != null) {
                Name.setText(user.getUsername());
            }
        }

        Message response = networkService.getUserBalance();
        if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
            this.soDuTaiKhoan = (BigDecimal) response.getData();
        }

        updateAllBalances();
        if (TrangchuPaneController != null) TrangchuPaneController.setUserData(user);
        if (KhoPaneController != null) KhoPaneController.setUserData(user);
        if (LichsudaugiaPaneController != null) LichsudaugiaPaneController.setUserData(user);
    }

    public void updateAllBalances() {
        if (TrangchuPaneController != null) TrangchuPaneController.updateBalance(soDuTaiKhoan);
        if (NaptienPaneController != null) NaptienPaneController.updateBalance(soDuTaiKhoan);
    }

    public void refreshAllData() {
        updateAllBalances();
        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
        if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
    }

    public void refreshHomeItems() {
        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public BigDecimal getSoDuTaiKhoan() {
        return soDuTaiKhoan;
    }

    public void setSoDuTaiKhoan(BigDecimal soDuTaiKhoan) {
        this.soDuTaiKhoan = soDuTaiKhoan;
    }

    public NetworkService getNetworkService() {
        return networkService;
    }

    public Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px;");
        return label;
    }

    public Stage getCurrentStage() {
        if (TrangchuPane != null && TrangchuPane.getScene() != null) {
            return (Stage) TrangchuPane.getScene().getWindow();
        }

        if (KhoPane != null && KhoPane.getScene() != null) {
            return (Stage) KhoPane.getScene().getWindow();
        }

        if (NaptienPane != null && NaptienPane.getScene() != null) {
            return (Stage) NaptienPane.getScene().getWindow();
        }

        if (CaidatPane != null && CaidatPane.getScene() != null) {
            return (Stage) CaidatPane.getScene().getWindow();
        }

        return null;
    }

    private void openModalFXML(String fxmlPath, String title) {
        try {
            URL resourceUrl = getClass().getResource(fxmlPath);
            if (resourceUrl == null) {
                resourceUrl = Thread.currentThread().getContextClassLoader().getResource(
                        fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath
                );
            }
            System.out.println("Loading FXML from: " + resourceUrl);
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Object childController = loader.getController();
            connectChildController(childController);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError(
                    "Không thể mở giao diện",
                    "Không tìm thấy hoặc không LOAD được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object nextController = loader.getController();
            connectChildController(nextController);

            Stage stage = getCurrentStage();

            if (stage == null) {
                stage = new Stage();
            }

            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            showError(
                    "Không thể chuyển giao diện",
                    "Không tìm thấy hoặc không LOAD được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private void connectChildController(Object controller) {
        if (controller instanceof LinkedController linkedController) {
            linkedController.setUserController(this);
        }
    }

    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }

    public interface LinkedController {
        void setUserController(UserController userController);
    }
}
