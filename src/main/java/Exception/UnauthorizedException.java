package Exception;

/**
 * Ngoại lệ khi người dùng không có quyền thực hiện một hành động nào đó.
 */
public class UnauthorizedException extends Exception {
    private final String userId;
    private final String action;

    /**
     * @param message thông báo lỗi
     * @param userId  ID người dùng vi phạm
     * @param action  hành động bị từ chối
     */
    public UnauthorizedException(String message, String userId, String action) {
        super(message);
        this.userId = userId;
        this.action = action;
    }

    /** @return ID người dùng vi phạm */
    public String getUserId() {
        return userId;
    }

    /** @return hành động bị từ chối */
    public String getAction() {
        return action;
    }
}
