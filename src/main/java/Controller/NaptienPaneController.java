package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

import Network.Message;
import Network.NetworkService;

public class NaptienPaneController implements UserController.LinkedController {

    @FXML private Label Sodutaikhoan;
    @FXML private TextField Nganhangnaptien;
    @FXML private TextField Sotaikhoannaptien;
    @FXML private TextField Sotiencannap;

    private UserController userController;
    private NetworkService networkService = NetworkService.getInstance();

    /** Gán UserController liên kết để truy cập thông tin người dùng và các phương thức hỗ trợ giao diện. */
    @Override
    public void setUserController(UserController uc) {
        this.userController = uc;
    }

    /** Cập nhật hiển thị số dư tài khoản trên giao diện nạp tiền. */
    public void updateBalance(BigDecimal balance) {
        if (Sodutaikhoan != null) {
            Sodutaikhoan.setText(userController.formatMoney(balance));
        }
    }

    /** Xử lý sự kiện nạp tiền: kiểm tra dữ liệu đầu vào, gửi yêu cầu nạp tiền lên server, cập nhật số dư nếu thành công. */
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

    /** Phân tích và chuyển đổi chuỗi tiền tệ thành BigDecimal, hỗ trợ định dạng có dấu phẩy, khoảng trắng, ký hiệu $ và ₫. */
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
