package Controller;

import java.io.IOException;
import java.net.URL;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import Network.NetworkService;
import Model.User;

/**
 * Controller cho màn hình đăng nhập và đăng ký (FXML: login.fxml, signin.fxml).
 * Quản lý form đăng nhập (tên đăng nhập + mật khẩu) và form đăng ký (họ tên, username,
 * email, SĐT, mật khẩu, xác nhận mật khẩu). Sau khi đăng nhập thành công, chuyển đến
 * giao diện chính (Indivisual.fxml / UserController).
 *
 * <p>Các {@code @FXML fields} chính:
 * <ul>
 *   <li>{@code loginUsernameField}, {@code loginPasswordField} — form đăng nhập</li>
 *   <li>{@code signupNameField}, {@code signupUsernameField}, {@code signupEmailField},
 *       {@code signupPhoneField}, {@code signupPasswordField}, {@code signupConfirmPasswordField}
 *       — form đăng ký</li>
 *   <li>{@code messageLabel} — hiển thị thông báo</li>
 * </ul></p>
 *
 * <p>Sử dụng {@link Network.NetworkService} để gọi API login/register.</p>
 */
public class logincontroller {
    @FXML
    private TextField loginUsernameField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private TextField signupNameField;

    @FXML
    private TextField signupUsernameField;

    @FXML
    private TextField signupEmailField;

    @FXML
    private TextField signupPhoneField;

    @FXML
    private PasswordField signupPasswordField;

    @FXML
    private PasswordField signupConfirmPasswordField;

    @FXML
    private ToggleGroup roleToggleGroup;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    private static User currentUser;

    /** Màu sắc gốc của nút đăng nhập. */
    private static final String NORMAL_BUTTON_STYLE = "-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30;";
    /** Màu sắc cho nút admin. */
    private static final String ADMIN_BUTTON_STYLE = "-fx-background-color: #8B0000; -fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-background-radius: 30; -fx-border-color: #FFD700;";

    @FXML
    private void initialize() {
        // Lắng nghe sự thay đổi trên ô tên đăng nhập (chỉ tồn tại trong login.fxml)
        if (loginUsernameField != null) {
            loginUsernameField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.toUpperCase().startsWith("ADM")) {
                    // Nhập tài khoản admin → đổi nút thành Admin
                    loginButton.setText("Admin");
                    loginButton.setStyle(ADMIN_BUTTON_STYLE);
                } else {
                    // Tài khoản thường → giữ nút Đăng nhập
                    loginButton.setText("Đăng nhập");
                    loginButton.setStyle(NORMAL_BUTTON_STYLE);
                }
            });
        }
        // Gán action cho nút login (chỉ tồn tại trong login.fxml)
        if (loginButton != null) {
            loginButton.setOnAction(this::Login);
        }
    }

    /**
     * Trả về người dùng đã đăng nhập hiện tại.
     *
     * @return Đối tượng User đã đăng nhập, hoặc null nếu chưa đăng nhập.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Xóa thông tin người dùng hiện tại (đăng xuất).
     */
    public static void logout() {
        currentUser = null;
    }

    @FXML
    /** Xử lý đăng nhập: kiểm tra đầu vào, gọi NetworkService.login(), chuyển sang màn hình chính. */
    private void Login(ActionEvent event) {
        String username = read(loginUsernameField);
        String password = read(loginPasswordField);

        if (username.isBlank() || password.isBlank()) {
            showMessage("Vui lòng nhập đầy đủ tài khoản và mật khẩu" +".");
            return;
        }

        NetworkService networkService = NetworkService.getInstance();
        if (!networkService.isConnected()) {
            boolean connected = networkService.connect();
            if (!connected) {
                showMessage("Không thể kết nối đến máy chủ. Vui lòng kiểm tra xem server đã được bật chưa.");
                return;
            }
        }

        try {
            var response = networkService.login(username, password);
            if (response.getType() == Network.Message.Type.SUCCESS && response.getData() != null) {
                currentUser = (User) response.getData();
                showMessage("Đăng nhập thành công! Xin chào! " + currentUser.getUsername());
                navigateToMain(event);
            } else {
                String errMsg = response.getContent() != null ? response.getContent() : "Tài khoản hoặc mật khẩu không đúng.";
                showMessage(errMsg);
            }
        } catch (Exception e) {
            showMessage("Lỗi đăng nhập: " + e.getMessage());
        }
    }

    @FXML
    /** Xử lý đăng ký: kiểm tra đầu vào, gọi NetworkService.register(). */
    private void Signup(ActionEvent event) {
        String fullName = read(signupNameField);
        String username = read(signupUsernameField);
        String email = read(signupEmailField);
        String phone = read(signupPhoneField);
        String password = read(signupPasswordField);
        String confirmPassword = read(signupConfirmPasswordField);

        if (fullName.isBlank() || username.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            showMessage("Vui lòng nhập đầy đủ thông tin đăng ký.");
            return;
        }

        if (username.contains(" ")) {
            showMessage("Tên đăng nhập không được chứa khoảng trắng.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Mật khẩu nhập lại không khớp.");
            return;
        }

        NetworkService networkService = NetworkService.getInstance();
        if (!networkService.isConnected()) {
            boolean connected = networkService.connect();
            if (!connected) {
                showMessage("Không thể kết nối đến máy chủ. Vui lòng kiểm tra xem server đã được bật chưa.");
                return;
            }
        }

        try {
            // Xác định role từ radio button
            String role = "BIDDER_SELLER";
            if (roleToggleGroup != null) {
                Toggle selected = roleToggleGroup.getSelectedToggle();
                if (selected != null) {
                    String roleId = selected.getUserData().toString();
                    if ("bidderRadio".equals(roleId)) {
                        role = "BIDDER";
                    } else if ("sellerRadio".equals(roleId)) {
                        role = "SELLER";
                    }
                }
            }

            var response = networkService.register(username, password, email, phone, role);
            if (response.getType() == Network.Message.Type.SUCCESS) {
                showMessage("Đăng ký thành công! Vui lòng đăng nhập.");
                ComeLogin(event);
            } else {
                showMessage("Đăng ký thất bại: " + response.getContent());
            }
        } catch (Exception e) {
            showMessage("Lỗi đăng ký: " + e.getMessage());
        }
    }

    @FXML
    /** Chuyển sang form đăng ký. */
    private void ComeSignup(ActionEvent event) {
        switchScene(event, "/signin.fxml", 450, 660);
    }

    @FXML
    /** Chuyển về form đăng nhập. */
    private void ComeLogin(ActionEvent event) {
        switchScene(event, "/login.fxml", 600, 400);
    }

    /** Đọc text từ TextField, xử lý null, trim khoảng trắng. */
    private String read(TextField field) {
        return field == null ? "" : field.getText().trim();
    }

    /** Hiển thị thông báo qua Label messageLabel hoặc Alert nếu Label null. */
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Chuyển scene JavaFX với kích thước cho trước. */
    private void switchScene(ActionEvent event, String resourcePath, double width, double height) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                showMessage("Không tìm thấy giao diện: " + resourcePath);
                return;
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, width, height));
            stage.show();
        } catch (Exception e) {
            showMessage("Không thể mở giao diện: " + e.getMessage());
        }
    }

    /** Chuyển đến màn hình chính UserController sau khi đăng nhập thành công. */
    private void navigateToMain(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Indivisual.fxml"));
            Parent root = loader.load();

            UserController userController = loader.getController();
            userController.setUserData(currentUser);

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setMinWidth(823);
            stage.setMinHeight(608);
            stage.show();
        } catch (Exception e) {
            showMessage("Không thể mở giao diện: " + e.getMessage());
        }
    }
}