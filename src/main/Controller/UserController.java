package Controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Model.Item;
import Model.User;
import DAO.UserDAO;
import DAO.ItemDAO;

public class UserController {

    @FXML
    private Label Name;

    @FXML
    private Label Sodutaikhoan1;

    @FXML
    private Label Sodutaikhoan;

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
    private VBox AllItems;

    @FXML
    private VBox Items;

    @FXML
    private TextField Nganhangnaptien;

    @FXML
    private TextField Sotaikhoannaptien;

    @FXML
    private TextField Sotiencannap;

    @FXML
    private TextField Matkhauhientai;

    @FXML
    private TextField Matkhaumoi;

    @FXML
    private TextField Nhaplaimatkhaumoi;

    private final ToggleGroup menuGroup = new ToggleGroup();

    private User currentUser;
    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private BigDecimal soDuTaiKhoan = new BigDecimal("300000");

    private static final String MENU_STYLE =
            "-fx-background-color: #1E1E1D;" +
                    "-fx-text-fill: #eacd8f;";

    private static final String MENU_SELECTED_STYLE =
            "-fx-background-color: #d9b15f;" +
                    "-fx-text-fill: #1E1E1D;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 20;";

    private static final String CREATE_ITEM_FXML = "/CreateItems.fxml";
    private static final String LOGIN_FXML = "/login.fxml";

    @FXML
    private void initialize() {
        setupMenuButtons();
        setupDefaultScreen();

        syncBalanceLabels();

        loadHomeItems();
        loadWarehouseItems();
        loadBidHistory();
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

    private void syncBalanceLabels() {
        String formattedBalance = formatMoney(soDuTaiKhoan);

        if (Sodutaikhoan1 != null) {
            Sodutaikhoan1.setText(formattedBalance);
        }

        if (Sodutaikhoan != null) {
            Sodutaikhoan.setText(formattedBalance);
        }
    }

    private String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value);
    }

    private BigDecimal parseMoney(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền cần nạp.");
        }

        String normalized = rawText
                .trim()
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")
                .replace("$", "")
                .replace("₫", "");

        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("Số tiền không hợp lệ.");
        }

        BigDecimal amount = new BigDecimal(normalized);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        }

        return amount;
    }

    @FXML
    private void Naptien(ActionEvent event) {
        try {
            String nganHang = Nganhangnaptien.getText().trim();
            String soTaiKhoan = Sotaikhoannaptien.getText().trim();
            String soTienRaw = Sotiencannap.getText().trim();

            if (nganHang.isEmpty()) {
                showWarning("Thiếu thông tin", "Vui lòng nhập tên ngân hàng.");
                return;
            }

            if (soTaiKhoan.isEmpty()) {
                showWarning("Thiếu thông tin", "Vui lòng nhập số tài khoản.");
                return;
            }

            BigDecimal soTienNap = parseMoney(soTienRaw);

            soDuTaiKhoan = soDuTaiKhoan.add(soTienNap);
            
            if (currentUser != null) {
                userDAO.updateBalance(currentUser.getUsername(), soDuTaiKhoan);
            }
            
            syncBalanceLabels();

            Nganhangnaptien.clear();
            Sotaikhoannaptien.clear();
            Sotiencannap.clear();

            showInfo("Nạp tiền thành công", "Bạn đã nạp thêm " + formatMoney(soTienNap) + " $ vào tài khoản.");

        } catch (IllegalArgumentException e) {
            showWarning("Dữ liệu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            showError("Lỗi", "Không thể nạp tiền. Chi tiết: " + e.getMessage());
        }
    }

    @FXML
    private void Doimatkhau(ActionEvent event) {
        if (currentUser == null) {
            showWarning("Lỗi", "Vui lòng đăng nhập.");
            return;
        }

        String matKhauHienTai = Matkhauhientai.getText();
        String matKhauMoi = Matkhaumoi.getText();
        String nhapLaiMatKhauMoi = Nhaplaimatkhaumoi.getText();

        if (matKhauHienTai.isBlank() || matKhauMoi.isBlank() || nhapLaiMatKhauMoi.isBlank()) {
            showWarning("Dữ liệu thiếu", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (!matKhauMoi.equals(nhapLaiMatKhauMoi)) {
            showWarning("Lỗi", "Mật khẩu mới không khớp.");
            return;
        }

        if (matKhauMoi.length() < 6) {
            showWarning("Lỗi", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }

        boolean success = userDAO.changePassword(currentUser.getUsername(), matKhauHienTai, matKhauMoi);

        if (success) {
            Matkhauhientai.clear();
            Matkhaumoi.clear();
            Nhaplaimatkhaumoi.clear();
            showInfo("Thành công", "Đổi mật khẩu thành công!");
        } else {
            showWarning("Lỗi", "Mật khẩu hiện tại không đúng.");
        }
    }

    @FXML
    private void CreateItems(ActionEvent event) {
        openModalFXML(CREATE_ITEM_FXML, "Tạo sản phẩm");

        loadHomeItems();
        loadWarehouseItems();
    }

    @FXML
    private void Doitaikhoan(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Đổi tài khoản",
                "Bạn có chắc muốn đăng xuất và đổi tài khoản không?"
        );

        if (!confirmed) {
            return;
        }

        switchScene(LOGIN_FXML, "Đăng nhập");
    }

    @FXML
    private void Exit(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Thoát",
                "Bạn có chắc muốn thoát chương trình không?"
        );

        if (!confirmed) {
            return;
        }

        Stage stage = getCurrentStage();
        if (stage != null) {
            stage.close();
        }
    }

    public void setUserData(User user, BigDecimal balance) {
        this.currentUser = user;
        
        if (user != null && user.getUsername() != null) {
            Name.setText(user.getUsername());
        }

        if (user != null && user.getBalance() != null) {
            this.soDuTaiKhoan = user.getBalance();
        } else if (balance != null) {
            this.soDuTaiKhoan = balance;
        }

        updateBalanceLabels();
        syncBalanceLabels();
        loadHomeItems();
        loadWarehouseItems();
        loadBidHistory();
    }

    private void updateBalanceLabels() {
        String balanceText = formatMoney(soDuTaiKhoan);

        if (Sodutaikhoan != null) {
            Sodutaikhoan.setText(balanceText);
        }

        if (Sodutaikhoan1 != null) {
            Sodutaikhoan1.setText(balanceText);
        }
    }

    public BigDecimal getSoDuTaiKhoan() {
        return soDuTaiKhoan;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setSoDuTaiKhoan(BigDecimal soDuTaiKhoan) {
        if (soDuTaiKhoan == null) {
            return;
        }

        this.soDuTaiKhoan = soDuTaiKhoan;
        syncBalanceLabels();
    }

    public void refreshAllData() {
        syncBalanceLabels();
        loadHomeItems();
        loadWarehouseItems();
        loadBidHistory();
    }

    private void loadHomeItems() {
        if (AllItems == null) {
            return;
        }

        AllItems.getChildren().clear();

        try {
            List<Model.Item> allItems = itemDAO.findAll();
            if (allItems.isEmpty()) {
                Label emptyLabel = createEmptyLabel("Chưa có sản phẩm đấu giá nào.");
                AllItems.getChildren().add(emptyLabel);
            } else {
                for (Model.Item item : allItems) {
                    HBox itemBox = createItemCard(item);
                    AllItems.getChildren().add(itemBox);
                }
            }
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Lỗi tải sản phẩm.");
            AllItems.getChildren().add(errorLabel);
        }
    }

    private HBox createItemCard(Model.Item item) {
        HBox hbox = new HBox(10);
        hbox.setStyle("-fx-background-color: #111111; -fx-border-color: #d4af5a; -fx-padding: 10;");

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 16px;");

        BigDecimal price = new BigDecimal(String.valueOf(item.getStartPrice()));
        Label priceLabel = new Label(formatMoney(price) + " $");
        priceLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        hbox.getChildren().addAll(nameLabel, priceLabel);
        return hbox;
    }

    private void loadWarehouseItems() {
        if (Items == null) {
            return;
        }

        Items.getChildren().clear();

        Label emptyLabel = createEmptyLabel("Kho của bạn hiện chưa có sản phẩm.");
        Items.getChildren().add(emptyLabel);

        /*
         * Nếu bạn đã có danh sách sản phẩm của user:
         *
         * List<Item> userItems = itemService.getItemsByUser(currentUserId);
         * for (Item item : userItems) {
         *     Parent card = loadItemCard(item);
         *     Items.getChildren().add(card);
         * }
         */
    }

    private void loadBidHistory() {
        if (LichsudaugiaPane == null) {
            return;
        }

        if (!LichsudaugiaPane.getChildren().isEmpty()) {
            return;
        }

        Label label = createEmptyLabel("Chưa có lịch sử đấu giá.");
        label.setLayoutX(220);
        label.setLayoutY(250);

        LichsudaugiaPane.getChildren().add(label);

        /*
         * Nếu bạn có BidHistoryController riêng, bạn có thể load FXML vào pane này:
         *
         * loadFXMLIntoPane("/view/BidHistory.fxml", LichsudaugiaPane);
         */
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px;");
        return label;
    }

    private void openModalFXML(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
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
            showError(
                    "Không thể mở giao diện",
                    "Không tìm thấy hoặc không load được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
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
                    "Không tìm thấy hoặc không load được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private void loadFXMLIntoPane(String fxmlPath, AnchorPane targetPane) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object childController = loader.getController();
            connectChildController(childController);

            targetPane.getChildren().clear();
            targetPane.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);

        } catch (IOException e) {
            showError(
                    "Không thể load giao diện",
                    "Không load được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private void connectChildController(Object controller) {
        if (controller instanceof LinkedController linkedController) {
            linkedController.setUserController(this);
        }
    }

    private Stage getCurrentStage() {
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showConfirm(String title, String message) {
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