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

import java.math.BigDecimal;

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

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }

    @FXML
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
            var response = networkService.register(fullName, password);
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
    private void ComeSignup(ActionEvent event) {
        switchScene(event, "/signin.fxml", 450, 605);
    }

    @FXML
    private void ComeLogin(ActionEvent event) {
        switchScene(event, "/login.fxml", 600, 400);
    }

    private String read(TextField field) {
        return field == null ? "" : field.getText().trim();
    }

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

    private void navigateToMain(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Indivisual.fxml"));
            Parent root = loader.load();

            UserController userController = loader.getController();
            userController.setUserData(currentUser, new BigDecimal("300000"));

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.show();
        } catch (IOException e) {
            showMessage("Không thể mở giao diện: " + e.getMessage());
        }
    }
}