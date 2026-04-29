package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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

    @FXML
    private TextField thoiGianDauGia;

    @FXML
    private Button btn30Phut;

    @FXML
    private Button btn60Phut;

    @FXML
    private Button btn120Phut;

    @FXML
    private Button btn180Phut;

    private long selectedDuration = 60;

    private final String BUTTON_STYLE_DEFAULT =
            "-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 15;";

    private final String BUTTON_STYLE_SELECTED =
            "-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;";

    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();

    @FXML
    private void initialize() {
        thoiGianDauGia.setText("60");
    }

    @FXML
    private void setDuration30(ActionEvent event) {
        selectedDuration = 30;
        thoiGianDauGia.setText("30");
        updateDurationButtons(btn30Phut);
    }

    @FXML
    private void setDuration60(ActionEvent event) {
        selectedDuration = 60;
        thoiGianDauGia.setText("60");
        updateDurationButtons(btn60Phut);
    }

    @FXML
    private void setDuration120(ActionEvent event) {
        selectedDuration = 120;
        thoiGianDauGia.setText("120");
        updateDurationButtons(btn120Phut);
    }

    @FXML
    private void setDuration180(ActionEvent event) {
        selectedDuration = 180;
        thoiGianDauGia.setText("180");
        updateDurationButtons(btn180Phut);
    }

    private void updateDurationButtons(Button selected) {
        Button[] buttons = {btn30Phut, btn60Phut, btn120Phut, btn180Phut};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setStyle(btn == selected ? BUTTON_STYLE_SELECTED : BUTTON_STYLE_DEFAULT);
            }
        }
    }

    @FXML
    private void TaoSanPham(ActionEvent event) {
        String ten = tenSanPham.getText().trim();
        String giaStr = giaKhoiDau.getText().trim();
        String Mota = moTa.getText().trim();
        String thoiGianStr = thoiGianDauGia.getText().trim();

        if (ten.isBlank()) {
            showWarning("Loi", "Vui long nhap ten san pham.");
            return;
        }

        if (giaStr.isBlank()) {
            showWarning("Loi", "Vui long nhap gia khoi dau.");
            return;
        }

        if (thoiGianStr.isBlank()) {
            showWarning("Loi", "Vui long nhap thoi gian dau gia.");
            return;
        }

        try {
            BigDecimal gia = new BigDecimal(giaStr);
            if (gia.compareTo(BigDecimal.ZERO) <= 0) {
                showWarning("Loi", "Gia phai lon hon 0.");
                return;
            }

            long duration;
            try {
                duration = Long.parseLong(thoiGianStr);
                if (duration <= 0) {
                    showWarning("Loi", "Thoi gian phai lon hon 0 phut.");
                    return;
                }
                if (duration > 1440) {
                    showWarning("Loi", "Thoi gian toi da la 1440 phut (24 gio).");
                    return;
                }
            } catch (NumberFormatException e) {
                showWarning("Loi", "Thoi gian khong hop le.");
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
                    duration
                );
                sessionDAO.save(session);

                String durationText = duration >= 60
                        ? (duration / 60) + " gio" + (duration % 60 > 0 ? " " + (duration % 60) + " phut" : "")
                        : duration + " phut";

                showInfo("Thanh cong", "Tao san pham thanh cong!\nThoi gian dau gia: " + durationText);
                closeWindow();
            } else {
                showWarning("Loi", "Khong the tao san pham.");
            }

        } catch (NumberFormatException e) {
            showWarning("Loi", "Gia khong hop le.");
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
