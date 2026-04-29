package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import Model.Item;
import Model.AuctionSession;
import DAO.ItemDAO;
import DAO.AuctionSessionDAO;
import Model.User;
import java.math.BigDecimal;
import java.util.UUID;

public class CreateItemsController implements UserController.LinkedController {

    private User currentUser;

    @Override
    public void setUserController(UserController userController) {
        this.currentUser = userController.getCurrentUser();
    }

    @FXML
    private TextField tenSanPham;

    @FXML
    private TextField giaKhoiDau;

    @FXML
    private TextField moTa;

    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();

    @FXML
    private void TaoSanPham(ActionEvent event) {
        String ten = tenSanPham.getText().trim();
        String giaStr = giaKhoiDau.getText().trim();
        String Mota = moTa.getText().trim();

        if (ten.isBlank()) {
            showWarning("Lỗi", "Vui lòng nhập tên sản phẩm.");
            return;
        }

        if (giaStr.isBlank()) {
            showWarning("Lỗi", "Vui lòng nhập giá khởi đầu.");
            return;
        }

        try {
            BigDecimal gia = new BigDecimal(giaStr);
            if (gia.compareTo(BigDecimal.ZERO) <= 0) {
                showWarning("Lỗi", "Giá phải lớn hơn 0.");
                return;
            }

            String itemId = UUID.randomUUID().toString();
            Item item = new Model.Art(itemId, ten, Mota, gia.doubleValue(), currentUser.getId());
            item.setSellerId(currentUser.getId());

            if (itemDAO.save(item)) {
                String sessionId = UUID.randomUUID().toString();
                AuctionSession session = new AuctionSession(
                    sessionId,
                    item,
                    currentUser.getId(),
                    gia.doubleValue(),
                    60
                );
                sessionDAO.save(session);
                
                showInfo("Thành công", "Tạo sản phẩm thành công!");
                closeWindow();
            } else {
                showWarning("Lỗi", "Không thể tạo sản phẩm.");
            }

        } catch (NumberFormatException e) {
            showWarning("Lỗi", "Giá không hợp lệ.");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tenSanPham.getScene().getWindow();
        stage.close();
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
}