package Model;

/**
 * Lớp đại diện cho quản trị viên hệ thống.
 * Kế thừa từ {@link User}, ghi đè {@link #isAdmin()} trả về true và {@link #getRole()} trả về "ADMIN".
 * Có thêm trường adminLevel để phân cấp quản trị.
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;
    private String adminLevel;

    /**
     * Khởi tạo Admin với mã, tên đăng nhập và mật khẩu.
     *
     * @param id       mã quản trị viên
     * @param username tên đăng nhập
     * @param password mật khẩu
     */
    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getSpecificInfo() {
        return "Administrator - system management";
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    @Override
    public boolean isBidder() {
        return false;
    }

    @Override
    public boolean isSeller() {
        return false;
    }

    /** @return cấp quản trị (super, moderator...) */
    public String getAdminLevel() {
        return adminLevel;
    }

    /** @param adminLevel cấp quản trị mới */
    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }
}
