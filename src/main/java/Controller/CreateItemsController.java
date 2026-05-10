package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import Model.Item;
import Model.AuctionSession;
import DAO.ItemDAO;
import DAO.AuctionSessionDAO;
import Factory.ItemFactory;
import Model.User;
import java.math.BigDecimal;
import java.util.UUID;

/** Controller cho form tạo sản phẩm mới (FXML: CreateItems.fxml). */
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
    private ComboBox<String> categoryComboBox;

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
    /** Khởi tạo ComboBox danh mục và giá trị mặc định. */
    private void initialize() {
        thoiGianDauGia.setText("60");
        categoryComboBox.getItems().addAll("Art", "Electronics", "Vehicle", "Fashion", "Books", "Sports", "Jewelry", "Music", "Furniture");
        categoryComboBox.setValue("Art");
    }

    @FXML
    /** Chọn thời gian 30 phút. */
    private void setDuration30(ActionEvent event) {
        selectedDuration = 30;
        thoiGianDauGia.setText("30");
        updateDurationButtons(btn30Phut);
    }

    @FXML
    /** Chọn thời gian 60 phút (mặc định). */
    private void setDuration60(ActionEvent event) {
        selectedDuration = 60;
        thoiGianDauGia.setText("60");
        updateDurationButtons(btn60Phut);
    }

    @FXML
    /** Chọn thời gian 120 phút. */
    private void setDuration120(ActionEvent event) {
        selectedDuration = 120;
        thoiGianDauGia.setText("120");
        updateDurationButtons(btn120Phut);
    }

    @FXML
    /** Chọn thời gian 180 phút. */
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
    /** Tạo sản phẩm + phiên đấu giá: validate input, lưu DB qua DAO, đóng cửa sổ. */
    private void TaoSanPham(ActionEvent event) {
        String ten = tenSanPham.getText().trim();
        String giaStr = giaKhoiDau.getText().trim();
        String Mota = moTa.getText().trim();
        String thoiGianStr = thoiGianDauGia.getText().trim();

        if (ten.isBlank()) {
            showWarning("Lỗi", "Vui lòng nhập tên sản phẩm.");
            return;
        }

        if (giaStr.isBlank()) {
            showWarning("Lỗi", "Vui lòng nhập giá khởi đầu.");
            return;
        }

        if (thoiGianStr.isBlank()) {
            showWarning("Lỗi", "Vui lòng nhập thời gian đấu giá.");
            return;
        }

        try {
            BigDecimal gia = new BigDecimal(giaStr);
            if (gia.compareTo(BigDecimal.ZERO) <= 0) {
                showWarning("Lỗi", "Giá phải lớn hơn 0.");
                return;
            }

            long duration;
            try {
                duration = Long.parseLong(thoiGianStr);
                if (duration <= 0) {
                    showWarning("Lỗi", "Thời gian phải lớn hơn 0 phút.");
                    return;
                }
                if (duration > 1440) {
                    showWarning("Loi", "Thời gian tôí đa là 1440 phút (24 giờ).");
                    return;
                }
            } catch (NumberFormatException e) {
                showWarning("Loi", "Thời gian không hợp lệ.");
                return;
            }

            String category = categoryComboBox.getValue();
            Item item = ItemFactory.createItem(category, ten, Mota, gia.doubleValue(), currentUser.getId());
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
                        ? (duration / 60) + " giờ" + (duration % 60 > 0 ? " " + (duration % 60) + " phút" : "")
                        : duration + " phút";

                showInfo("Thành công", "Tạo sản phẩm thành công!\nThời gian đấu giá: " + durationText);
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
