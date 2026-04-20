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

    @FXML
    private void Login(ActionEvent event) {
        String username = read(loginUsernameField);
        String password = read(loginPasswordField);

        if (username.isBlank() || password.isBlank()) {
            showMessage("Vui long nhap day du tai khoan va mat khau.");
            return;
        }

        showMessage("Dang nhap hop le. Hay ket noi logic backend tai day.");
    }

    @FXML
    private void Signup(ActionEvent event) {
        String fullName = read(signupNameField);
        String email = read(signupEmailField);
        String phone = read(signupPhoneField);
        String password = read(signupPasswordField);
        String confirmPassword = read(signupConfirmPasswordField);

        if (fullName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            showMessage("Vui long nhap day du thong tin dang ky.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Mat khau nhap lai khong khop.");
            return;
        }

        showMessage("Dang ky hop le. Hay ket noi logic luu tai khoan tai day.");
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
                showMessage("Khong tim thay giao dien: " + resourcePath);
                return;
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, width, height));
            stage.show();
        } catch (IOException e) {
            showMessage("Khong the mo giao dien: " + e.getMessage());
        }
    }
}
