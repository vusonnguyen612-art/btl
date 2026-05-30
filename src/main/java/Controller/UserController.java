package Controller;

import Model.AuctionSession;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import Model.Item;
import Model.User;
import Network.Message;
import Network.NetworkService;

/**
 * Controller chính cho giao diện người dùng sau đăng nhập (FXML: Indivisual.fxml).
 * Quản lý menu điều hướng giữa các pane (Trang chủ, Kho, Nạp tiền, Lịch sử, Cài đặt,
 * Watchlist, Thống kê), mở phòng đấu giá, đổi mật khẩu, đổi ảnh đại diện, và phối hợp
 * dữ liệu giữa các child controller (LinkedController pattern).
 */
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

    @FXML
    private ToggleButton WatchlistButton;

    @FXML
    private ToggleButton ThongkeButton;

    @FXML
    private ToggleButton AdminButton;

    @FXML
    private AnchorPane WatchlistPane;

    @FXML
    private AnchorPane ThongkePane;

    @FXML
    private AnchorPane AdminPane;

    @FXML
    private WatchlistPaneController WatchlistPaneController;

    @FXML
    private ThongkePaneController ThongkePaneController;

    @FXML
    private AdminPaneController AdminPaneController;

    @FXML
    private ImageView avatarImageView;

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

    /**
     * Khởi tạo JavaFX: kết nối các child controller, thiết lập menu buttons,
     * hiển thị màn hình mặc định, tải dữ liệu trang chủ và kho.
     */
    @FXML
    private void initialize() {
        connectChildController(TrangchuPaneController);
        connectChildController(KhoPaneController);
        connectChildController(LichsudaugiaPaneController);
        connectChildController(NaptienPaneController);
        connectChildController(CaidatPaneController);
        connectChildController(WatchlistPaneController);
        connectChildController(ThongkePaneController);
        connectChildController(AdminPaneController);

        setupMenuButtons();
        setupDefaultScreen();

        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
        if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
    }

    /**
     * Thiết lập menu buttons: gán toggle group, style, và action để chuyển pane.
     */
    private void setupMenuButtons() {
        bindMenuButton(TrangchuButton, TrangchuPane);
        bindMenuButton(KhoButton, KhoPane);
        bindMenuButton(NaptienButton, NaptienPane);
        bindMenuButton(BidHistory, LichsudaugiaPane);
        bindMenuButton(WatchlistButton, WatchlistPane);
        bindMenuButton(ThongkeButton, ThongkePane);
        bindMenuButton(CaidatButton, CaidatPane);
        bindMenuButton(AdminButton, AdminPane);
    }

    /**
     * Gán một toggle button với pane tương ứng. Khi button được click, pane đó sẽ được hiển thị.
     *
     * @param button ToggleButton menu.
     * @param pane   AnchorPane tương ứng cần hiển thị.
     */
    private void bindMenuButton(ToggleButton button, AnchorPane pane) {
        if (button == null || pane == null) {
            return;
        }

        button.setToggleGroup(menuGroup);
        button.setStyle(MENU_STYLE);

        button.setOnAction(event -> {
            // Kiểm tra quyền admin khi click nút Admin
            if (button == AdminButton && (currentUser == null || !currentUser.isAdmin())) {
                showWarning("Từ chối truy cập", "Chỉ Admin mới có quyền truy cập.");
                menuGroup.selectToggle(null);
                button.setSelected(false);
                return;
            }
            showPane(pane);
            menuGroup.selectToggle(button);
            updateMenuStyle();
        });
    }

    /**
     * Thiết lập màn hình mặc định: hiển thị TrangchuPane.
     */
    private void setupDefaultScreen() {
        showPane(TrangchuPane);

        if (TrangchuButton != null) {
            menuGroup.selectToggle(TrangchuButton);
        }

        updateMenuStyle();
    }

    /**
     * Hiển thị pane được chỉ định, ẩn tất cả các pane khác.
     * Khi chuyển đến LichsudaugiaPane, WatchlistPane, hoặc ThongkePane,
     * tự động tải dữ liệu tương ứng.
     *
     * @param paneToShow AnchorPane cần hiển thị.
     */
    private void showPane(AnchorPane paneToShow) {
        AnchorPane[] panes = {
                TrangchuPane,
                KhoPane,
                NaptienPane,
                LichsudaugiaPane,
                CaidatPane,
                WatchlistPane,
                ThongkePane,
                AdminPane
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
            if (paneToShow == WatchlistPane && WatchlistPaneController != null) {
                WatchlistPaneController.loadWatchlist();
            }
            if (paneToShow == ThongkePane && ThongkePaneController != null) {
                ThongkePaneController.loadStatistics();
            }
        }
    }

    /**
     * Cập nhật style cho các menu button: button được chọn có màu nổi bật.
     */
    private void updateMenuStyle() {
        ToggleButton[] buttons = {
                TrangchuButton,
                KhoButton,
                NaptienButton,
                BidHistory,
                WatchlistButton,
                ThongkeButton,
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

    /**
     * Định dạng số tiền BigDecimal thành chuỗi có dấu phẩy phân cách hàng nghìn.
     *
     * @param value Số tiền cần định dạng.
     * @return Chuỗi đã định dạng (vd: \"1,234,567\").
     */
    public String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value);
    }

    /**
     * Mở phòng đấu giá (FXML: auctionRoom.fxml) ở cửa sổ modal.
     * Khi đóng phòng, cập nhật số dư và tải lại danh sách.
     *
     * @param event ActionEvent kích hoạt.
     */
    @FXML
    public void openAuctionRoom(ActionEvent event) {
        openAuctionRoomForAuction(null);
    }

    /**
     * Mở màn hình phòng đấu giá cho một phiên đấu giá cụ thể (nếu có).
     *
     * @param auctionToSelect Phiên đấu giá cần được chọn tự động khi vào phòng, hoặc {@code null} nếu không chọn sẵn phiên nào.
     */
    public void openAuctionRoomForAuction(AuctionSession auctionToSelect) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionRoom.fxml"));
            Parent root = loader.load();

            AuctionRoomController auctionRoomController = loader.getController();
            auctionRoomController.setCurrentUser(currentUser);
            if (auctionToSelect != null) {
                auctionRoomController.selectAuction(auctionToSelect);
            }

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

    /**
     * Xử lý thay đổi ảnh đại diện: mở FileChooser, tải ảnh lên server.
     *
     * @param event ActionEvent kích hoạt.
     */
    @FXML
    public void changeAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.jpeg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("Tất cả", "*.*")
        );
        File file = fileChooser.showOpenDialog(getCurrentStage());
        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString());
                if (avatarImageView != null) {
                    avatarImageView.setImage(image);
                }
                Message response = networkService.updateAvatar(file.getAbsolutePath());
                if (response.getType() != Message.Type.SUCCESS) {
                    showWarning("Lỗi", "Không thể cập nhật ảnh đại diện: " + response.getContent());
                }
            } catch (Exception e) {
                showWarning("Lỗi", "Không thể tải ảnh: " + e.getMessage());
            }
        }
    }

    /**
     * Mở form tạo sản phẩm mới (modal). Sau khi đóng, tải lại trang chủ và kho.
     *
     * @param event ActionEvent kích hoạt.
     */
    public void createItems(ActionEvent event) {
        openModalFXML(CREATE_ITEM_FXML, "Tạo sản phẩm");
        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
        if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
    }

    /**
     * Mở hộp thoại phương thức (Modal Dialog) chỉnh sửa vật phẩm đã chọn.
     *
     * @param item Vật phẩm cần chỉnh sửa thông tin.
     */
    public void editItem(Item item) {
        try {
            URL resourceUrl = getClass().getResource(CREATE_ITEM_FXML);
            if (resourceUrl == null) {
                resourceUrl = Thread.currentThread().getContextClassLoader().getResource(
                        CREATE_ITEM_FXML.startsWith("/") ? CREATE_ITEM_FXML.substring(1) : CREATE_ITEM_FXML
                );
            }
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            CreateItemsController createController = loader.getController();
            connectChildController(createController);
            createController.setEditMode(item);

            Stage stage = new Stage();
            stage.setTitle("Chỉnh sửa sản phẩm");
            stage.setScene(new Scene(root));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.showAndWait();

            if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
            if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
        } catch (Exception e) {
            showError("Lỗi", "Không thể mở chỉnh sửa: " + e.getMessage());
        }
    }

    /**
     * Đăng xuất và chuyển về màn hình đăng nhập.
     *
     * @param event ActionEvent kích hoạt.
     */
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

    /**
     * Thoát chương trình sau khi xác nhận.
     *
     * @param event ActionEvent kích hoạt.
     */
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

    /**
     * Gán thông tin người dùng hiện tại, cập nhật tên hiển thị, số dư tài khoản,
     * và truyền dữ liệu cho các child controller.
     *
     * @param user Đối tượng User cần gán.
     */
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

        // Hiển thị nút Admin nếu user là admin (dùng Platform.runLater để đảm bảo UI đã render)
        final ToggleButton adminBtn = AdminButton;
        if (adminBtn != null && user != null) {
            boolean isAdmin = user.isAdmin();
            javafx.application.Platform.runLater(() -> {
                adminBtn.setManaged(isAdmin);
                adminBtn.setVisible(isAdmin);
                adminBtn.setDisable(!isAdmin);
            });
        }
    }

    /**
     * Cập nhật số dư hiển thị trên TrangchuPane và NaptienPane.
     */
    public void updateAllBalances() {
        if (TrangchuPaneController != null) TrangchuPaneController.updateBalance(soDuTaiKhoan);
        if (NaptienPaneController != null) NaptienPaneController.updateBalance(soDuTaiKhoan);
    }

    /**
     * Làm mới toàn bộ dữ liệu: cập nhật số dư, tải lại trang chủ và kho.
     */
    public void refreshAllData() {
        updateAllBalances();
        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
        if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
    }

    /**
     * Làm mới danh sách sản phẩm trên trang chủ.
     */
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

    /**
     * Trả về đối tượng NetworkService để các controller con có thể dùng.
     *
     * @return Đối tượng NetworkService singleton.
     */
    public NetworkService getNetworkService() {
        return networkService;
    }

    /**
     * Tạo một Label rỗng với style mặc định, dùng khi không có dữ liệu để hiển thị.
     *
     * @param text Nội dung thông báo trống.
     * @return Label đã được style.
     */
    public Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px;");
        return label;
    }

    /**
     * Trả về Stage hiện tại từ một trong các pane đang hiển thị.
     *
     * @return Stage hiện tại, hoặc {@code null} nếu không có pane nào được khởi tạo.
     */
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

    /**
     * Mở một FXML dưới dạng cửa sổ modal, tự động kết nối child controller nếu implement {@link LinkedController}.
     *
     * @param fxmlPath Đường dẫn tới file FXML (có thể bắt đầu bằng {@code /}).
     * @param title    Tiêu đề cửa sổ modal.
     */
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

    /**
     * Chuyển scene đến một FXML khác (dùng cho đăng xuất).
     *
     * @param fxmlPath Đường dẫn file FXML đích.
     * @param title    Tiêu đề cửa sổ mới.
     */
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

    /**
     * Kết nối child controller với UserController hiện tại nếu nó implement {@link LinkedController}.
     *
     * @param controller Đối tượng controller cần kết nối.
     */
    private void connectChildController(Object controller) {
        if (controller instanceof LinkedController linkedController) {
            linkedController.setUserController(this);
        }
    }

    /**
     * Hiển thị hộp thoại thông báo (Alert.INFORMATION).
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung thông báo.
     */
    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại cảnh báo (Alert.WARNING).
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung cảnh báo.
     */
    public void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại lỗi (Alert.ERROR).
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung lỗi.
     */
    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại xác nhận (Alert.CONFIRMATION).
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung câu hỏi xác nhận.
     * @return {@code true} nếu người dùng nhấn OK, {@code false} nếu nhấn Cancel.
     */
    public boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }

    /**
     * Interface đánh dấu các controller con (child pane) có thể được kết nối với
     * {@link UserController} để nhận tham chiếu và gọi các phương thức chung
     * (showInfo, showWarning, formatMoney, v.v.).
     */
    public interface LinkedController {
        /**
         * Gán tham chiếu đến UserController cha.
         *
         * @param userController Đối tượng UserController điều phối.
         */
        void setUserController(UserController userController);
    }
}
