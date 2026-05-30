package Model;

/**
 * Lớp đại diện cho người bán hàng trong hệ thống.
 * Kế thừa từ {@link User}, role "SELLER".
 * Có thêm trường storeName để lưu tên cửa hàng.
 */
public class Seller extends User {
    private static final long serialVersionUID = 1L;
    private String storeName;

    /**
     * Khởi tạo Seller với mã, tên đăng nhập và mật khẩu.
     *
     * @param id       mã người bán
     * @param username tên đăng nhập
     * @param password mật khẩu
     */
    public Seller(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getSpecificInfo() {
        return "Seller - can list items for auction";
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public boolean isBidder() {
        return false;
    }

    @Override
    public boolean isSeller() {
        return true;
    }

    /** @return tên cửa hàng */
    public String getStoreName() {
        return storeName;
    }

    /** @param storeName tên cửa hàng mới */
    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}
