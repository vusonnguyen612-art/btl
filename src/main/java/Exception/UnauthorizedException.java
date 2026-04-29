package Exception;

public class UnauthorizedException extends Exception {
    private final String userId;
    private final String action;

    public UnauthorizedException(String message, String userId, String action) {
        super(message);
        this.userId = userId;
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }
}
