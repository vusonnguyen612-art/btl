package Factory;

import Model.User;
import java.util.concurrent.atomic.AtomicInteger;

/** Factory tạo User mới và validate mật khẩu. */
public class UserFactory {
    private static final AtomicInteger userCounter = new AtomicInteger(0);

    /** Kiểm tra mật khẩu hợp lệ (>=6 ký tự, không khoảng trắng). */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        for (char c : password.toCharArray()) {
            if (Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    /** Kiểm tra lỗi mật khẩu, trả về thông báo lỗi hoặc null nếu hợp lệ. */
    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (password.contains(" ")) {
            return "Password cannot contain whitespace";
        }
        return null;
    }

    /** Tạo user mới với ID "USRxxxx". */
    public static User createUser(String username, String password) {
        String id = "USR" + String.format("%04d", userCounter.incrementAndGet());
        return new Model.User(id, username, password);
    }
}