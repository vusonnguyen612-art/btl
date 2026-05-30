package Controller;

import Model.User;
import Network.Message;
import Network.NetworkService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller cho pane Quản trị (dành cho Admin).
 * Hiển thị danh sách người dùng, cho phép xóa user.
 */
public class AdminPaneController implements UserController.LinkedController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> balanceColumn;
    @FXML private Label statusLabel;
    @FXML private Button refreshButton;

    private UserController userController;
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        balanceColumn.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    String.format("%,.0f₫", u.getBalance()));
        });
        userTable.setItems(userList);
        userTable.setPlaceholder(new Label("Không có người dùng nào."));
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void onRefresh(ActionEvent event) {
        loadUsers();
    }

    /** Tải danh sách user từ server. */
    public void loadUsers() {
        // Kiểm tra quyền admin
        if (userController == null || userController.getCurrentUser() == null || !userController.getCurrentUser().isAdmin()) {
            statusLabel.setText("Từ chối truy cập: chỉ Admin mới có quyền.");
            return;
        }
        statusLabel.setText("Đang tải...");
        NetworkService ns = NetworkService.getInstance();
        if (!ns.isConnected()) {
            statusLabel.setText("Chưa kết nối server.");
            return;
        }
        try {
            Message response = ns.getUsers();
            if (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<User> users = (List<User>) response.getData();
                Platform.runLater(() -> {
                    userList.setAll(users);
                    statusLabel.setText("Tổng số: " + users.size() + " người dùng.");
                });
            } else {
                statusLabel.setText("Lỗi: " + response.getContent());
            }
        } catch (Exception e) {
            statusLabel.setText("Lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteUser(ActionEvent event) {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            userController.showWarning("Chọn user", "Vui lòng chọn một người dùng để xóa.");
            return;
        }
        if (selected.isAdmin()) {
            userController.showWarning("Không thể xóa", "Không thể xóa tài khoản Admin.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa người dùng \"" + selected.getUsername() + "\" (" + selected.getId() + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                NetworkService ns = NetworkService.getInstance();
                Message response = ns.deleteUser(selected.getId());
                if (response.getType() == Message.Type.SUCCESS) {
                    userList.remove(selected);
                    statusLabel.setText("Đã xóa " + selected.getUsername());
                } else {
                    userController.showWarning("Lỗi", response.getContent());
                }
            }
        });
    }
}
