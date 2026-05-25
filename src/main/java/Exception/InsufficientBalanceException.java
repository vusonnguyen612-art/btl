package Exception;

import java.math.BigDecimal;

/**
 * Ngoại lệ khi người dùng không đủ số dư tài khoản để đặt giá.
 */
public class InsufficientBalanceException extends Exception {
    private final String userId;
    private final BigDecimal balance;
    private final BigDecimal bidAmount;

    /**
     * @param message   thông báo lỗi
     * @param userId    ID người dùng
     * @param balance   số dư hiện tại
     * @param bidAmount số tiền muốn đặt
     */
    public InsufficientBalanceException(String message, String userId, BigDecimal balance, BigDecimal bidAmount) {
        super(message);
        this.userId = userId;
        this.balance = balance;
        this.bidAmount = bidAmount;
    }

    /** @return ID người dùng */
    public String getUserId() {
        return userId;
    }

    /** @return số dư hiện tại */
    public BigDecimal getBalance() {
        return balance;
    }

    /** @return số tiền muốn đặt */
    public BigDecimal getBidAmount() {
        return bidAmount;
    }
}
