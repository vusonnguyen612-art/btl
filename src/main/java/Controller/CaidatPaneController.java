package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class CaidatPaneController implements UserController.LinkedController {

    @FXML private TextField Matkhauhientai;
    @FXML private TextField Matkhaumoi;
    @FXML private TextField Nhaplaimatkhaumoi;

    private UserController userController;

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    @FXML
    private void doimatkhau(ActionEvent event) {
        if (userController == null || userController.getCurrentUser() == null) {
            userController.showWarning("Lỗi", "Vui lòng đăng nhập.");
            return;
        }

        String matKhauHienTai = Matkhauhientai.getText();
        String matKhauMoi = Matkhaumoi.getText();
        String nhapLaiMatKhauMoi = Nhaplaimatkhaumoi.getText();

        if (matKhauHienTai.isBlank() || matKhauMoi.isBlank() || nhapLaiMatKhauMoi.isBlank()) {
            userController.showWarning("Dữ liệu thiếu", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (!matKhauMoi.equals(nhapLaiMatKhauMoi)) {
            userController.showWarning("Lỗi", "Mật khẩu mới không khớp.");
            return;
        }

        if (matKhauMoi.length() < 6) {
            userController.showWarning("Lỗi", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }

        userController.showWarning("Chưa hỗ trợ", "Tính năng đổi mật khẩu qua Server chưa được cài đặt.");

        Matkhauhientai.clear();
        Matkhaumoi.clear();
        Nhaplaimatkhaumoi.clear();
    }

    @FXML
    private void onExit(ActionEvent event) {
        if (userController != null) {
            userController.exit(event);
        }
    }

    @FXML
    private void onDoitaikhoan(ActionEvent event) {
        if (userController != null) {
            userController.doitaikhoan(event);
        }
    }
}
