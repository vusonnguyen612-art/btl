package Model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String username;
    private String password;
    private String email;
    private boolean isSeller;
    private boolean isBidder;

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isSeller = true;
        this.isBidder = true;
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

    public String getRole() {
        return "BIDDER_SELLER";
    }

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
