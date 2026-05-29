package Factory;

import Model.User;
import Model.Admin;
import java.util.concurrent.atomic.AtomicInteger;

/** Factory tạo User mới và validate mật khẩu. */
public class UserFactory {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final AtomicInteger userCounter = new AtomicInteger(0);

    /** Kiểm tra mật khẩu hợp lệ (>=6 ký tự, không khoảng trắng). */
    public static boolean isValidPassword(String password) {
        return getPasswordError(password) == null;
    }

    /** Kiểm tra lỗi mật khẩu, trả về thông báo lỗi hoặc null nếu hợp lệ. */
    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least 6 characters";
        }
        if (containsWhitespace(password)) {
            return "Password cannot contain whitespace";
        }
        return null;
    }

    private static boolean containsWhitespace(String value) {
        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }

    /** Tạo user mới với ID "USRxxxx". */
    public static User createUser(String username, String password) {
        String id = "USR" + String.format("%04d", userCounter.incrementAndGet());
        return new Model.User(id, username, password);
    }

    /**
     * Tạo admin mới với ID "ADMxxxx".
     *
     * @param username tên đăng nhập
     * @param password mật khẩu
     * @return thực thể Admin được khởi tạo
     */
    public static Admin createAdmin(String username, String password) {
        String id = "ADM" + String.format("%04d", userCounter.incrementAndGet());
        return new Model.Admin(id, username, password);
    }
}
