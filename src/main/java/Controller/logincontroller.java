package Controller;

import java.io.IOException;
import java.net.URL;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import Network.NetworkService;
import Model.User;

/** Controller cho màn hình đăng nhập và đăng ký (FXML: login.fxml, signin.fxml). */
public class logincontroller {
    @FXML
    private TextField loginUsernameField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private TextField signupNameField;

    @FXML
    private TextField signupEmailField;

    @FXML
    private TextField signupPhoneField;

    @FXML
    private PasswordField signupPasswordField;

    @FXML
    private PasswordField signupConfirmPasswordField;

    @FXML
    private Label messageLabel;

    private static User currentUser;

    /** @return người dùng đã đăng nhập */
    public static User getCurrentUser() {
        return currentUser;
    }

    /** Xóa thông tin người dùng hiện tại. */
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
            networkService.connect();
        }

        try {
            var response = networkService.login(username, password);
            if (response.getType() == Network.Message.Type.SUCCESS && response.getData() != null) {
                currentUser = (User) response.getData();
                showMessage("Đăng nhập thành công! Xin chào! " + currentUser.getUsername());
                navigateToMain(event);
            } else {
                showMessage("Tài khoản hoặc mật khẩu không đúng.");
            }
        } catch (Exception e) {
            showMessage("Lỗi đăng nhập: " + e.getMessage());
        }
    }

    @FXML
    /** Xử lý đăng ký: kiểm tra đầu vào, gọi NetworkService.register(). */
    private void Signup(ActionEvent event) {
        String fullName = read(signupNameField);
        String email = read(signupEmailField);
        String phone = read(signupPhoneField);
        String password = read(signupPasswordField);
        String confirmPassword = read(signupConfirmPasswordField);

        if (fullName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            showMessage("Vui lòng nhập đầy đủ thông tin đăng ký.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Mật khẩu nhập lại không khớp.");
            return;
        }

        NetworkService networkService = NetworkService.getInstance();
        if (!networkService.isConnected()) {
            networkService.connect();
        }

        try {
            var response = networkService.register(fullName, password, email, phone);
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
        switchScene(event, "/signin.fxml", 450, 605);
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
        } catch (IOException e) {
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
        } catch (IOException e) {
            showMessage("Không thể mở giao diện: " + e.getMessage());
        }
    }
}