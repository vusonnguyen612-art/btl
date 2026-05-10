package Model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Lớp đại diện cho người dùng trong hệ thống.
 * Mặc định vừa là seller vừa là bidder, số dư ban đầu 300,000.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String username;
    private String password;
    private String email;
    private boolean isSeller;
    private boolean isBidder;
    private BigDecimal balance;

    /** @param id       mã người dùng
     *  @param username tên đăng nhập
     *  @param password mật khẩu */
    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isSeller = true;
        this.isBidder = true;
        this.balance = new BigDecimal("300000");
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** @return vai trò mặc định BIDDER_SELLER */
    public String getRole() {
        return "BIDDER_SELLER";
    }

    /** @return false vì User không phải Admin */
    public boolean isAdmin() {
        return false;
    }

    public boolean isSeller() {
        return isSeller;
    }

    public boolean isBidder() {
        return isBidder;
    }
}
