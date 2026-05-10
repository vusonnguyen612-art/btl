package Exception;

/**
 * Ngoại lệ khi xác thực người dùng thất bại (sai tài khoản/mật khẩu hoặc không tìm thấy user).
 */
public class AuthenticationException extends Exception {
    private final String username;

    /**
     * @param message  thông báo lỗi
     * @param username tên đăng nhập gây ra lỗi
     */
    public AuthenticationException(String message, String username) {
        super(message);
        this.username = username;
    }

    /** @return tên đăng nhập gây ra lỗi */
    public String getUsername() {
        return username;
    }
}
