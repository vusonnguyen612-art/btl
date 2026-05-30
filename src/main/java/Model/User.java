package Model;

import java.math.BigDecimal;

/**
 * Lớp trừu tượng đại diện cho người dùng trong hệ thống đấu giá.
 * <p>
 * Kế thừa {@link Entity}, cung cấp các trường chung: username, password, email, balance, avatarPath.
 * </p>
 * <p>
 * Các lớp con triển khai cụ thể hóa vai trò:
 * <ul>
 *   <li>{@link RegularUser} — vừa là bidder vừa là seller (mặc định)</li>
 *   <li>{@link Bidder} — chỉ tham gia đấu giá</li>
 *   <li>{@link Seller} — chỉ đăng sản phẩm</li>
 *   <li>{@link Admin} — quản trị viên hệ thống</li>
 * </ul>
 * </p>
 */
public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String email;
    private BigDecimal balance;
    private String avatarPath;
    private boolean banned;

    /**
     * @param id       mã người dùng
     * @param username tên đăng nhập
     * @param password mật khẩu
     */
    public User(String id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
        this.balance = BigDecimal.ZERO;
        this.banned = false;
    }

    // ── Abstract methods ────────────────────────────────────

    /** @return chuỗi vai trò (VD: "BIDDER_SELLER", "ADMIN", "SELLER", "BIDDER") */
    @Override
    public abstract String getSpecificInfo();

    /** @return vai trò người dùng */
    public abstract String getRole();

    /** @return true nếu là quản trị viên */
    public abstract boolean isAdmin();

    /** @return true nếu có quyền đấu giá */
    public abstract boolean isBidder();

    /** @return true nếu có quyền bán */
    public abstract boolean isSeller();

    // ── Getters / Setters ───────────────────────────────────

    /** @return số dư tài khoản */
    public BigDecimal getBalance() { return balance; }

    /** @param balance số dư mới */
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    /** @return tên đăng nhập */
    public String getUsername() { return username; }

    /** @param username tên đăng nhập mới */
    public void setUsername(String username) { this.username = username; }

    /** @return mật khẩu */
    public String getPassword() { return password; }

    /** @param password mật khẩu mới */
    public void setPassword(String password) { this.password = password; }

    /** @return email */
    public String getEmail() { return email; }

    /** @param email email mới */
    public void setEmail(String email) { this.email = email; }

    /** @return đường dẫn ảnh đại diện */
    public String getAvatarPath() { return avatarPath; }

    /** @param avatarPath đường dẫn ảnh đại diện mới */
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    /** @return true nếu tài khoản bị khóa */
    public boolean isBanned() { return banned; }

    /** @param banned trạng thái khóa tài khoản */
    public void setBanned(boolean banned) { this.banned = banned; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", getId(), getUsername(), getRole());
    }
}
