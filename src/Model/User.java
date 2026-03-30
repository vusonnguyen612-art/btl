package Model;

public abstract class User {
    protected String username;
    protected String id;

    public User(String username, String id) {
        this.username = username;
        this.id = id;
    }

    public String getUsername() {
        return username;
    }
}
