package Controller;

import Controller.utils.AlertUtils;
import Controller.utils.FormatUtils;
import Controller.utils.ResponseUtils;
import Controller.utils.UIUtils;
import Model.AuctionSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

import Model.Item;
import Model.User;
import Network.Message;
import Network.NetworkService;

public class UserController {

    @FXML
    private Label Name;

    @FXML
    private ToggleButton TrangchuButton;

    @FXML
    private ToggleButton NaptienButton;

    @FXML
    private ToggleButton CaidatButton;

    @FXML
    private AnchorPane TrangchuPane;

    @FXML
    private AnchorPane NaptienPane;

    @FXML
    private AnchorPane CaidatPane;

    @FXML
    private TrangchuPaneController TrangchuPaneController;

    @FXML
    private NaptienPaneController NaptienPaneController;

    @FXML
    private CaidatPaneController CaidatPaneController;

    @FXML
    private ToggleButton WatchlistButton;

    @FXML
    private ToggleButton ThongkeButton;

    @FXML
    private AnchorPane WatchlistPane;

    @FXML
    private AnchorPane ThongkePane;

    @FXML
    private WatchlistPaneController WatchlistPaneController;

    @FXML
    private ThongkePaneController ThongkePaneController;

    @FXML
    private ToggleButton KhoButton;

    @FXML
    private AnchorPane KhoPane;

    @FXML
    private KhoPaneController KhoPaneController;

    @FXML
    private ToggleButton LichsudaugiaButton;

    @FXML
    private AnchorPane LichsudaugiaPane;

    @FXML
    private LichsudaugiaPaneController LichsudaugiaPaneController;

    @FXML
    private ToggleButton AdminButton;

    @FXML
    private AnchorPane AdminPane;

    @FXML
    private AdminPaneController AdminPaneController;

    @FXML
    private ImageView avatarImageView;

    private ToggleGroup menuGroup = new ToggleGroup();
    private User currentUser;
    private NetworkService networkService = NetworkService.getInstance();
    private BigDecimal soDuTaiKhoan = BigDecimal.ZERO;

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

    private record MenuItem(ToggleButton button, AnchorPane pane, Runnable onShow) {
    }

    private record LoadedView(Parent root, Object controller) {
    }

    /**
     * Khởi tạo controller chính của giao diện người dùng.
     * Phương thức này được gọi tự động sau khi FXML được load.
     * Nó thiết lập các nút menu, màn hình mặc định và tải danh sách vật phẩm.
     */
    @FXML
    private void initialize() {
        for (Object controller : childControllers()) {
            connectChildController(controller);
        }

        setupMenuButtons();
        setupDefaultScreen();

        refreshItemLists();
    }

    /**
     * Thiết lập tất cả các nút menu trong giao diện.
     * Duyệt qua danh sách menu items và gắn sự kiện cho từng nút.
     */
    private void setupMenuButtons() {
        for (MenuItem item : menuItems()) {
            bindMenuButton(item);
        }
    }

    /**
     * Gắn sự kiện và kiểu cho một nút menu cụ thể.
     * Khi nút được nhấn, pane tương ứng sẽ được hiển thị và kiểu nút được cập nhật.
     *
     * @param item MenuItem chứa thông tin về nút, pane và hành động khi hiển thị
     */
    private void bindMenuButton(MenuItem item) {
        if (item.button() == null || item.pane() == null) {
            return;
        }

        item.button().setToggleGroup(menuGroup);
        item.button().setStyle(MENU_STYLE);

        item.button().setOnAction(event -> {
            showPane(item.pane());
            menuGroup.selectToggle(item.button());
            updateMenuStyle();
        });
    }

    /**
     * Thiết lập màn hình mặc định khi ứng dụng khởi động.
     * Hiển thị pane Trang chủ và chọn nút Trang chủ trong menu.
     */
    private void setupDefaultScreen() {
        showPane(TrangchuPane);

        if (TrangchuButton != null) {
            menuGroup.selectToggle(TrangchuButton);
        }

        updateMenuStyle();
    }

    /**
     * Hiển thị một pane cụ thể và ẩn tất cả các pane khác.
     * Khi pane được hiển thị, phương thức onShow() tương ứng sẽ được thực thi.
     *
     * @param paneToShow Pane cần hiển thị
     */
    private void showPane(AnchorPane paneToShow) {
        for (AnchorPane pane : contentPanes()) {
            if (pane != null) {
                pane.setVisible(false);
                pane.setManaged(false);
            }
        }

        if (paneToShow != null) {
            paneToShow.setVisible(true);
            paneToShow.setManaged(true);
            for (MenuItem item : menuItems()) {
                if (item.pane() == paneToShow && item.onShow() != null) {
                    item.onShow().run();
                    break;
                }
            }
        }
    }

    /**
     * Cập nhật kiểu hiển thị cho tất cả các nút menu.
     * Nút được chọn sẽ có kiểu khác với nút chưa được chọn.
     */
    private void updateMenuStyle() {
        for (ToggleButton button : menuButtons()) {
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
     * Trả về danh sách tất cả các menu items trong giao diện.
     * Mỗi MenuItem chứa thông tin về nút toggle, pane tương ứng và hành động khi hiển thị.
     *
     * @return Mảng chứa tất cả các MenuItem
     */
    private MenuItem[] menuItems() {
        return new MenuItem[]{
                new MenuItem(TrangchuButton, TrangchuPane, null),
                new MenuItem(KhoButton, KhoPane, () -> {
                    if (KhoPaneController != null) KhoPaneController.loadWarehouseItems();
                }),
                new MenuItem(NaptienButton, NaptienPane, null),
                new MenuItem(WatchlistButton, WatchlistPane, () -> {
                    if (WatchlistPaneController != null) WatchlistPaneController.loadWatchlist();
                }),
                new MenuItem(LichsudaugiaButton, LichsudaugiaPane, () -> {
                    if (LichsudaugiaPaneController != null) LichsudaugiaPaneController.loadBidHistory();
                }),
                new MenuItem(ThongkeButton, ThongkePane, () -> {
                    if (ThongkePaneController != null) ThongkePaneController.loadStatistics();
                }),
                new MenuItem(CaidatButton, CaidatPane, null),
                new MenuItem(AdminButton, AdminPane, () -> {
                    if (AdminPaneController != null) {
                        AdminPaneController.loadUsers();
                        AdminPaneController.loadItems();
                    }
                })
        };
    }

    private ToggleButton[] menuButtons() {
        return new ToggleButton[]{
                TrangchuButton, KhoButton, NaptienButton, WatchlistButton,
                LichsudaugiaButton, ThongkeButton, CaidatButton, AdminButton
        };
    }

    private AnchorPane[] contentPanes() {
        return new AnchorPane[]{
                TrangchuPane, KhoPane, NaptienPane, CaidatPane,
                WatchlistPane, LichsudaugiaPane, ThongkePane, AdminPane
        };
    }

    /**
     * Trả về danh sách tất cả các controller con trong giao diện.
     *
     * @return Mảng chứa tất cả các controller con
     */
    private Object[] childControllers() {
        return new Object[]{
                TrangchuPaneController,
                KhoPaneController,
                NaptienPaneController,
                CaidatPaneController,
                WatchlistPaneController,
                LichsudaugiaPaneController,
                ThongkePaneController,
                AdminPaneController
        };
    }

    /**
     * Định dạng số tiền thành chuỗi với định dạng tiền tệ Việt Nam.
     *
     * @param value Số tiền cần định dạng
     * @return Chuỗi đã định dạng tiền tệ
     */
    public String formatMoney(BigDecimal value) {
        return FormatUtils.formatMoney(value);
    }

    /**
     * Xử lý sự kiện khi nút "Mở phòng đấu giá" được nhấn.
     * Mở phòng đấu giá mà không chọn phiên đấu giá cụ thể nào.
     *
     * @param event Sự kiện click từ nút
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
                refreshBalanceFromServer();
                refreshItemLists();
            });
            stage.show();

        } catch (Exception e) {
            showError("Lỗi", "Không thể mở phòng đấu giá: " + e.getMessage());
        }
    }

    /**
     * Xử lý sự kiện khi người dùng thay đổi ảnh đại diện.
     * Mở hộp thoại chọn file ảnh, tải ảnh lên server và cập nhật hiển thị.
     *
     * @param event Sự kiện click từ nút thay đổi ảnh
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
     * Mở hộp thoại tạo vật phẩm mới.
     * Tải giao diện CreateItems và hiển thị dưới dạng modal dialog.
     *
     * @param event Sự kiện click từ nút tạo vật phẩm
     */
    public void createItems(ActionEvent event) {
        openModalFXML(CREATE_ITEM_FXML, "Tạo sản phẩm");
        refreshItemLists();
    }

    /**
     * Mở hộp thoại phương thức (Modal Dialog) chỉnh sửa vật phẩm đã chọn.
     *
     * @param item Vật phẩm cần chỉnh sửa thông tin.
     */
    public void editItem(Item item) {
        try {
            LoadedView view = loadView(CREATE_ITEM_FXML);
            CreateItemsController createController = (CreateItemsController) view.controller();
            connectChildController(createController);
            createController.setEditMode(item);

            Stage stage = new Stage();
            stage.setTitle("Chỉnh sửa sản phẩm");
            stage.setScene(new Scene(view.root()));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.showAndWait();

            refreshItemLists();
        } catch (Exception e) {
            showError("Lỗi", "Không thể mở chỉnh sửa: " + e.getMessage());
        }
    }

    /**
     * Xử lý sự kiện khi người dùng muốn đổi tài khoản.
     * Hiển thị xác nhận và chuyển về màn hình đăng nhập nếu đồng ý.
     *
     * @param event Sự kiện click từ nút đổi tài khoản
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
     * Xử lý sự kiện khi người dùng muốn thoát ứng dụng.
     * Hiển thị xác nhận và đóng cửa sổ nếu đồng ý.
     *
     * @param event Sự kiện click từ nút thoát
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
     * Thiết lập dữ liệu người dùng cho controller và các controller con.
     * Cập nhật tên hiển thị và tải lại số dư từ server.
     *
     * @param user Đối tượng User chứa thông tin người dùng
     */
    public void setUserData(User user) {
        this.currentUser = user;

        if (user != null && user.getUsername() != null) {
            if (Name != null) {
                Name.setText(user.getUsername());
            }
        }

        refreshBalanceFromServer();
        if (TrangchuPaneController != null) TrangchuPaneController.setUserData(user);
        if (KhoPaneController != null) KhoPaneController.setUserData(user);
        if (LichsudaugiaPaneController != null) LichsudaugiaPaneController.setUserData(user);

        final ToggleButton adminBtn = AdminButton;
        if (adminBtn != null && user != null) {
            boolean isAdmin = user.isAdmin();
            javafx.application.Platform.runLater(() -> {
                adminBtn.setManaged(isAdmin);
                adminBtn.setVisible(isAdmin);
                adminBtn.setDisable(!isAdmin);

                if (NaptienButton != null) {
                    NaptienButton.setManaged(!isAdmin);
                    NaptienButton.setVisible(!isAdmin);
                }
                if (WatchlistButton != null) {
                    WatchlistButton.setManaged(!isAdmin);
                    WatchlistButton.setVisible(!isAdmin);
                }
                if (KhoButton != null) {
                    KhoButton.setManaged(!isAdmin);
                    KhoButton.setVisible(!isAdmin);
                }
                if (LichsudaugiaButton != null) {
                    LichsudaugiaButton.setManaged(!isAdmin);
                    LichsudaugiaButton.setVisible(!isAdmin);
                }

                if (isAdmin) {
                    showPane(AdminPane);
                    if (AdminButton != null) menuGroup.selectToggle(AdminButton);
                    updateMenuStyle();
                    if (AdminPaneController != null) {
                        AdminPaneController.loadUsers();
                        AdminPaneController.loadItems();
                    }
                }
            });
        }
    }

    /**
     * Cập nhật số dư tài khoản trên tất cả các giao diện liên quan.
     * Gửi số dư hiện tại đến controller Trang chủ và Nạp tiền.
     */
    public void updateAllBalances() {
        if (TrangchuPaneController != null) TrangchuPaneController.updateBalance(soDuTaiKhoan);
        if (NaptienPaneController != null) NaptienPaneController.updateBalance(soDuTaiKhoan);
    }

    /**
     * Làm mới danh sách vật phẩm trên trang chủ.
     * Gọi phương thức loadHomeItems() của controller Trang chủ.
     */
    public void refreshHomeItems() {
        if (TrangchuPaneController != null) TrangchuPaneController.loadHomeItems();
    }

    /**
     * Lấy thông tin người dùng hiện tại.
     *
     * @return Đối tượng User hiện tại, hoặc null nếu chưa đăng nhập
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Thiết lập số dư tài khoản mới.
     *
     * @param soDuTaiKhoan Số dư tài khoản mới
     */
    public void setSoDuTaiKhoan(BigDecimal soDuTaiKhoan) {
        this.soDuTaiKhoan = soDuTaiKhoan;
    }

    /**
     * Tạo một Label trống với nội dung văn bản được chỉ định.
     *
     * @param text Văn bản hiển thị trên Label
     * @return Label đã được tạo với nội dung văn bản
     */
    public Label createEmptyLabel(String text) {
        return UIUtils.createEmptyLabel(text);
    }

    /**
     * Lấy Stage (cửa sổ) hiện tại từ một trong các pane nội dung.
     *
     * @return Stage hiện tại, hoặc null nếu không tìm thấy
     */
    public Stage getCurrentStage() {
        for (AnchorPane pane : contentPanes()) {
            if (pane != null && pane.getScene() != null) {
                return (Stage) pane.getScene().getWindow();
            }
        }

        return null;
    }

    /**
     * Mở giao diện FXML dưới dạng modal dialog.
     * Tải FXML từ đường dẫn đã cho, kết nối controller con và hiển thị dưới dạng cửa sổ modal.
     *
     * @param fxmlPath Đường dẫn đến file FXML
     * @param title Tiêu đề cửa sổ modal
     */
    private void openModalFXML(String fxmlPath, String title) {
        try {
            LoadedView view = loadView(fxmlPath);
            connectChildController(view.controller());
            showModal(view.root(), title);

        } catch (Exception e) {
            showError(
                    "Không thể mở giao diện",
                    "Không tìm thấy hoặc không LOAD được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    /**
     * Chuyển đổi toàn bộ giao diện sang scene FXML mới.
     * Tải FXML, kết nối controller và thay thế scene hiện tại trong stage.
     *
     * @param fxmlPath Đường dẫn đến file FXML
     * @param title Tiêu đề cửa sổ mới
     */
    private void switchScene(String fxmlPath, String title) {
        try {
            LoadedView view = loadView(fxmlPath);
            connectChildController(view.controller());

            Stage stage = getCurrentStage();

            if (stage == null) {
                stage = new Stage();
            }

            stage.setTitle(title);
            stage.setScene(new Scene(view.root()));
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
     * Kết nối controller con với UserController chính.
     * Nếu controller thực thi giao diện LinkedController, sẽ gọi phương thức setUserController().
     *
     * @param controller Controller con cần kết nối
     */
    private void connectChildController(Object controller) {
        if (controller instanceof LinkedController linkedController) {
            linkedController.setUserController(this);
        }
    }

    /**
     * Tải giao diện FXML từ đường dẫn đã cho và trả về LoadedView chứa root và controller.
     *
     * @param fxmlPath Đường dẫn đến file FXML
     * @return Đối tượng LoadedView chứa root Parent và controller
     * @throws IOException Nếu không thể tải file FXML
     */
    private LoadedView loadView(String fxmlPath) throws IOException {
        URL resourceUrl = resolveResource(fxmlPath);
        FXMLLoader loader = new FXMLLoader(resourceUrl);
        Parent root = loader.load();
        return new LoadedView(root, loader.getController());
    }

    /**
     * Tìm và trả về URL tài nguyên từ đường dẫn FXML.
     * Thử tìm theo class loader trước, sau đó theo context class loader.
     *
     * @param fxmlPath Đường dẫn đến file FXML
     * @return URL của tài nguyên FXML
     * @throws IOException Nếu không tìm thấy file FXML
     */
    private URL resolveResource(String fxmlPath) throws IOException {
        URL resourceUrl = getClass().getResource(fxmlPath);
        if (resourceUrl != null) {
            return resourceUrl;
        }

        String normalizedPath = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
        resourceUrl = Thread.currentThread().getContextClassLoader().getResource(normalizedPath);
        if (resourceUrl == null) {
            throw new IOException("FXML not found: " + fxmlPath);
        }

        return resourceUrl;
    }

    /**
     * Hiển thị giao diện dưới dạng modal dialog.
     * Tạo Stage mới với tiêu đề đã cho và hiển thị dưới dạng cửa sổ modal.
     *
     * @param root Root parent của giao diện
     * @param title Tiêu đề cửa sổ modal
     */
    private void showModal(Parent root, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.initOwner(getCurrentStage());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.showAndWait();
    }

    /**
     * Làm mới số dư tài khoản từ server.
     * Gọi API lấy số dư và cập nhật giá trị cục bộ cũng như hiển thị trên giao diện.
     */
    private void refreshBalanceFromServer() {
        BigDecimal balance = ResponseUtils.extractBalance(networkService.getUserBalance());
        if (balance != null) {
            this.soDuTaiKhoan = balance;
            updateAllBalances();
        }
    }

    /**
     * Làm mới danh sách vật phẩm trên trang chủ và kho hàng.
     * Gọi phương thức load tương ứng cho từng controller.
     */
    private void refreshItemLists() {
        if (TrangchuPaneController != null) {
            TrangchuPaneController.loadHomeItems();
        }
        if (KhoPaneController != null) {
            KhoPaneController.loadWarehouseItems();
        }
    }

    /**
     * Hiển thị thông báo thông tin cho người dùng.
     *
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     */
    public void showInfo(String title, String message) {
        AlertUtils.showInfo(title, message);
    }

    /**
     * Hiển thị cảnh báo cho người dùng.
     *
     * @param title Tiêu đề cảnh báo
     * @param message Nội dung cảnh báo
     */
    public void showWarning(String title, String message) {
        AlertUtils.showWarning(title, message);
    }

    /**
     * Hiển thị thông báo lỗi cho người dùng.
     *
     * @param title Tiêu đề lỗi
     * @param message Nội dung lỗi
     */
    public void showError(String title, String message) {
        AlertUtils.showError(title, message);
    }

    /**
     * Hiển thị hộp thoại xác nhận cho người dùng.
     *
     * @param title Tiêu đề hộp thoại
     * @param message Nội dung yêu cầu xác nhận
     * @return true nếu người dùng chọn "Đồng ý", false nếu chọn "Hủy"
     */
    public boolean showConfirm(String title, String message) {
        return AlertUtils.showConfirm(title, message);
    }

    public interface LinkedController {
        void setUserController(UserController userController);
    }
}
