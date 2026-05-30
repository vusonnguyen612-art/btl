package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

import Network.Message;
import Network.NetworkService;

/**
 * Controller cho pane Nạp tiền (FXML: Indivisual.fxml -> NaptienPane).
 * Cho phép người dùng nhập thông tin ngân hàng, số tài khoản, số tiền cần nạp
 * và gửi yêu cầu nạp tiền lên server.
 *
 * <p>Implement {@link UserController.LinkedController} để nhận tham chiếu
 * đến {@link UserController} cha.</p>
 */
public class NaptienPaneController implements UserController.LinkedController {

    @FXML private Label Sodutaikhoan;
    @FXML private TextField Nganhangnaptien;
    @FXML private TextField Sotaikhoannaptien;
    @FXML private TextField Sotiencannap;

    private UserController userController;
    private NetworkService networkService = NetworkService.getInstance();

    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    /**
     * Cập nhật số dư hiển thị trên Label Sodutaikhoan.
     *
     * @param balance Số dư mới của người dùng.
     */
    public void updateBalance(BigDecimal balance) {
        if (Sodutaikhoan != null) {
            Sodutaikhoan.setText(userController.formatMoney(balance));
        }
    }

    /**
     * Xử lý nạp tiền: kiểm tra thông tin ngân hàng, số tài khoản, số tiền,
     * gọi NetworkService.deposit() và cập nhật số dư.
     *
     * @param event ActionEvent kích hoạt.
     */
    @FXML
    private void naptien(ActionEvent event) {
        try {
            String nganHang = Nganhangnaptien.getText().trim();
            String soTaiKhoan = Sotaikhoannaptien.getText().trim();
            String soTienRaw = Sotiencannap.getText().trim();

            if (nganHang.isEmpty()) {
                userController.showWarning("Thiếu thông tin!", "Vui lòng nhập tên ngân hàng!");
                return;
            }

            if (soTaiKhoan.isEmpty()) {
                userController.showWarning("Thiếu thông tin!", "Vui lòng nhập số tài khoản!");
                return;
            }

            BigDecimal soTienNap = parseMoney(soTienRaw);

            Message response = networkService.deposit(soTienNap);
            if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
                BigDecimal newBalance = (BigDecimal) response.getData();
                userController.setSoDuTaiKhoan(newBalance);
                userController.updateAllBalances();

                Nganhangnaptien.clear();
                Sotaikhoannaptien.clear();
                Sotiencannap.clear();

                userController.showInfo("Nạp tiền thành công", "Bạn đã nạp thêm " + userController.formatMoney(soTienNap) + " $ vào tài khoản.");
            } else {
                userController.showError("Lỗi", "Nạp tiền thất bại: " + response.getContent());
            }

        } catch (IllegalArgumentException e) {
            userController.showWarning("Dữ liệu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            userController.showError("Lỗi", "Không thể nạp tiền. Chi tiết: " + e.getMessage());
        }
    }

    /**
     * Chuyển đổi chuỗi số tiền thô (có thể chứa dấu cách, dấu phẩy, ký tự $, ₫)
     * thành {@link BigDecimal}. Ném {@link IllegalArgumentException} nếu đầu vào không hợp lệ.
     *
     * @param rawText Chuỗi số tiền từ giao diện.
     * @return Số tiền dạng BigDecimal.
     * @throws IllegalArgumentException nếu chuỗi rỗng, không phải số, hoặc nhỏ hơn hoặc bằng 0.
     */
    private BigDecimal parseMoney(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền cần nạp!");
        }

        String normalized = rawText
                .trim()
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")
                .replace("$", "")
                .replace("\u20ab", "");

        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("Số tiền không hợp lệ!.");
        }

        BigDecimal amount = new BigDecimal(normalized);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0!");
        }

        return amount;
    }
}
