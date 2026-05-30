package Model;

import java.math.BigDecimal;

/**
 * Lớp đại diện cho người tham gia đấu giá (bidder).
 * Kế thừa từ {@link User}, role "BIDDER".
 */
public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private BigDecimal biddingBalance;

    /**
     * Khởi tạo Bidder với mã, tên đăng nhập và mật khẩu.
     *
     * @param id       mã người đấu giá
     * @param username tên đăng nhập
     * @param password mật khẩu
     */
    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getSpecificInfo() {
        return "Bidder - can participate in auctions";
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public boolean isBidder() {
        return true;
    }

    @Override
    public boolean isSeller() {
        return false;
    }

    /** @return số dư dành riêng cho đấu giá */
    public BigDecimal getBiddingBalance() {
        return biddingBalance;
    }

    /** @param biddingBalance số dư đấu giá mới */
    public void setBiddingBalance(BigDecimal biddingBalance) {
        this.biddingBalance = biddingBalance;
    }

    /**
     * Cộng thêm số tiền vào số dư đấu giá.
     *
     * @param amount số tiền cần thêm (có thể âm để trừ)
     */
    public void addBalance(BigDecimal amount) {
        if (this.biddingBalance == null) {
            this.biddingBalance = BigDecimal.ZERO;
        }
        this.biddingBalance = this.biddingBalance.add(amount);
    }
}
