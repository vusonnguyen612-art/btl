package Model;

/**
 * Người dùng mặc định — vừa là bidder vừa là seller.
 * <p>
 * Kế thừa {@link User}, role mặc định "BIDDER_SELLER".
 * Được tạo khi người dùng đăng ký không chọn role cụ thể.
 * </p>
 */
public class RegularUser extends User {
    private static final long serialVersionUID = 1L;

    /**
     * @param id       mã người dùng
     * @param username tên đăng nhập
     * @param password mật khẩu
     */
    public RegularUser(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRole() {
        return "BIDDER_SELLER";
    }

    @Override
    public String getSpecificInfo() {
        return "Regular user - both bidder and seller";
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
        return true;
    }
}
