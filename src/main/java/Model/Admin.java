package Model;

/** Quản trị viên hệ thống, kế thừa User, có cấp quản trị riêng. */
public class Admin extends User {
    private static final long serialVersionUID = 1L;
    private String adminLevel;

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }
}
