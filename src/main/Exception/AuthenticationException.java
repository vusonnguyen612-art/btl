package Exception;

public class AuthenticationException extends Exception {
    private final String username;

    public AuthenticationException(String message, String username) {
        super(message);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
