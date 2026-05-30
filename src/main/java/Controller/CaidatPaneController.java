package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * Controller cho pane Cài đặt (FXML: Indivisual.fxml -> CaidatPane).
 * Cung cấp chức năng đổi mật khẩu, đăng xuất (đổi tài khoản), và thoát chương trình.
 *
 * <p>Implement {@link UserController.LinkedController} để nhận tham chiếu
 * đến {@link UserController} cha và sử dụng các phương thức tiện ích
 * (showWarning, exit, doitaikhoan).</p>
 */
public class CaidatPaneController implements UserController.LinkedController {

    @FXML private TextField Matkhauhientai;
    @FXML private TextField Matkhaumoi;
    @FXML private TextField Nhaplaimatkhaumoi;

    private UserController userController;

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    /**
     * Xử lý đổi mật khẩu: kiểm tra đầu vào (không trống, khớp mật khẩu mới,
     * độ dài tối thiểu 6 ký tự), sau đó gửi yêu cầu lên server.
     *
     * @param event ActionEvent kích hoạt.
     */
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

        // Gửi yêu cầu đổi mật khẩu lên server
        Network.NetworkService ns = Network.NetworkService.getInstance();
        Network.Message response = ns.changePassword(matKhauHienTai, matKhauMoi);
        if (response.getType() == Network.Message.Type.SUCCESS) {
            userController.showWarning("Thành công", "Đổi mật khẩu thành công.");
        } else {
            userController.showWarning("Lỗi", response.getContent());
        }

        Matkhauhientai.clear();
        Matkhaumoi.clear();
        Nhaplaimatkhaumoi.clear();
    }

    /**
     * Thoát chương trình thông qua UserController cha.
     *
     * @param event ActionEvent kích hoạt.
     */
    @FXML
    private void onExit(ActionEvent event) {
        if (userController != null) {
            userController.exit(event);
        }
    }

    /**
     * Đăng xuất và chuyển về màn hình đăng nhập thông qua UserController cha.
     *
     * @param event ActionEvent kích hoạt.
     */
    @FXML
    private void onDoitaikhoan(ActionEvent event) {
        if (userController != null) {
            userController.doitaikhoan(event);
        }
    }
}
