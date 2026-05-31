package Controller;

import Model.Item;
import Model.User;
import Network.Message;
import Network.NetworkService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.util.List;

public class AdminPaneController implements UserController.LinkedController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colBalance;
    @FXML private TableColumn<User, String> colBlocked;
    @FXML private Label userStatusLabel;

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> colItemId;
    @FXML private TableColumn<Item, String> colItemName;
    @FXML private TableColumn<Item, String> colItemCategory;
    @FXML private TableColumn<Item, String> colItemPrice;
    @FXML private TableColumn<Item, String> colItemSeller;
    @FXML private Label itemStatusLabel;

    private UserController userController;
    private final NetworkService networkService = NetworkService.getInstance();

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(data -> {
            User u = data.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(() -> u.isAdmin() ? "ADMIN" : u.getRole());
        });
        colBalance.setCellValueFactory(data -> {
            BigDecimal bal = data.getValue().getBalance();
            return javafx.beans.binding.Bindings.createStringBinding(() -> bal != null ? bal.toString() : "0");
        });
        colBlocked.setCellValueFactory(data -> {
            return javafx.beans.binding.Bindings.createStringBinding(() -> data.getValue().isBlocked() ? "Yes" : "");
        });

        colItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colItemCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colItemPrice.setCellValueFactory(data -> {
            return javafx.beans.binding.Bindings.createStringBinding(() -> String.format("%.0f $", data.getValue().getStartPrice()));
        });
        colItemSeller.setCellValueFactory(new PropertyValueFactory<>("sellerId"));
    }

    public void loadUsers() {
        Message response = networkService.getUsers();
        if (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) response.getData();
            userTable.setItems(FXCollections.observableArrayList(users));
            if (userStatusLabel != null) userStatusLabel.setText("Tổng: " + users.size() + " người dùng");
        } else {
            if (userController != null) userController.showError("Lỗi", response.getContent());
        }
    }

    public void loadItems() {
        Message response = networkService.getItems();
        if (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Item> items = (List<Item>) response.getData();
            itemTable.setItems(FXCollections.observableArrayList(items));
            if (itemStatusLabel != null) itemStatusLabel.setText("Tổng: " + items.size() + " vật phẩm");
        } else {
            if (userController != null) userController.showError("Lỗi", response.getContent());
        }
    }

    @FXML
    private void onRefreshUsers() { loadUsers(); }

    @FXML
    private void onRefreshItems() { loadItems(); }

    @FXML
    private void onBlockUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { userController.showWarning("Cảnh báo", "Vui lòng chọn người dùng."); return; }
        if (selected.isAdmin()) { userController.showWarning("Cảnh báo", "Không thể chặn tài khoản Admin."); return; }
        boolean blocked = selected.isBlocked();
        boolean confirmed = userController.showConfirm("Xác nhận", (blocked ? "Bỏ chặn " : "Chặn ") + selected.getUsername() + "?");
        if (!confirmed) return;
        Message response = networkService.blockUser(selected.getId(), !blocked);
        if (response.getType() == Message.Type.SUCCESS) {
            userController.showInfo("Thành công", response.getContent());
            loadUsers();
        } else {
            userController.showError("Lỗi", response.getContent());
        }
    }

    @FXML
    private void onDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { userController.showWarning("Cảnh báo", "Vui lòng chọn người dùng."); return; }
        if (selected.isAdmin()) { userController.showWarning("Cảnh báo", "Không thể xóa tài khoản Admin."); return; }
        boolean confirmed = userController.showConfirm("Xác nhận", "Xóa người dùng '" + selected.getUsername() + "'?");
        if (!confirmed) return;
        Message response = networkService.deleteUser(selected.getId());
        if (response.getType() == Message.Type.SUCCESS) {
            userController.showInfo("Thành công", response.getContent());
            loadUsers();
        } else {
            userController.showError("Lỗi", response.getContent());
        }
    }

    @FXML
    private void onDeleteItem() {
        Item selected = itemTable.getSelectionModel().getSelectedItem();
        if (selected == null) { userController.showWarning("Cảnh báo", "Vui lòng chọn vật phẩm."); return; }
        boolean confirmed = userController.showConfirm("Xác nhận", "Xóa vật phẩm '" + selected.getName() + "'?");
        if (!confirmed) return;
        Message response = networkService.deleteItem(selected.getId());
        if (response.getType() == Message.Type.SUCCESS) {
            userController.showInfo("Thành công", response.getContent());
            loadItems();
        } else {
            userController.showError("Lỗi", response.getContent());
        }
    }
}
