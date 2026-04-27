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

import Network.AuctionClient;
import Network.Message;
import Model.User;
import DAO.UserDAO;
import java.util.UUID;

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

    private static AuctionClient client;
    private static User currentUser;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8989;
    private final UserDAO userDAO = new UserDAO();

    private boolean ensureConnection() {
        if (client == null) {
            client = new AuctionClient(SERVER_ADDRESS, SERVER_PORT);
        }
        try {
            if (client.connect()) {
                return true;
            }
        } catch (Exception e) {
            showMessage("Khong the ket noi toi server: " + e.getMessage());
        }
        showMessage("Khong the ket noi toi server.");
        return false;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static AuctionClient getClient() {
        return client;
    }

    public static void logout() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        currentUser = null;
    }

    @FXML
    private void Login(ActionEvent event) {
        String username = read(loginUsernameField);
        String password = read(loginPasswordField);

        if (username.isBlank() || password.isBlank()) {
            showMessage("Vui long nhap day du tai khoan va mat khau.");
            return;
        }

        try {
            var userOpt = userDAO.login(username, password);
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                showMessage("Dang nhap thanh cong! Xin chao " + currentUser.getUsername());
                navigateToMain(event);
            } else {
                showMessage("Tai khoan hoac mat khau khong dung.");
            }
        } catch (Exception e) {
            showMessage("Loi dang nhap: " + e.getMessage());
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
            showMessage("Vui long nhap day du thong tin dang ky.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Mat khau nhap lai khong khop.");
            return;
        }

        try {
            if (userDAO.existsByUsername(fullName)) {
                showMessage("Tai khoan da ton tai.");
                return;
            }

            String userId = UUID.randomUUID().toString();
            User newUser = new User(userId, fullName, password);
            newUser.setEmail(email);
            
            if (userDAO.register(newUser)) {
                showMessage("Dang ky thanh cong! Vui long dang nhap.");
                ComeLogin(event);
            } else {
                showMessage("Dang ky that bai.");
            }
        } catch (Exception e) {
            showMessage("Loi dang ky: " + e.getMessage());
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

    private void navigateToMain(ActionEvent event) {
        switchScene(event, "/Indivisual.fxml", 900, 600);
    }
}