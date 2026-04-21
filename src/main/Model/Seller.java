package Model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;
    private String storeName;

    public Seller(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    @Override
    public boolean isSeller() {
        return true;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}
