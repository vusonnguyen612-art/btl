package Factory;

import Model.User;
import Model.Admin;
import Model.Bidder;
import Model.Seller;
import Model.RegularUser;
import DAO.DatabaseUtil;
import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Factory tạo User mới và validate mật khẩu. */
public class UserFactory {
    private static final AtomicInteger userCounter = new AtomicInteger(0);
    private static boolean initialized = false;

    /**
     * Khởi tạo counter từ giá trị lớn nhất trong database.
     * Cần gọi một lần khi server khởi động để tránh trùng ID.
     */
    public static synchronized void initializeCounter() {
        if (initialized) return;
        long maxId = 0;
        String sql = "SELECT id FROM users WHERE id LIKE 'USR%' OR id LIKE 'ADM%'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                if (id != null && id.length() > 3) {
                    // Trích xuất số từ ID dạng "USR0001" hoặc "ADM0002"
                    String numPart = id.substring(3);
                    try {
                        long num = Long.parseLong(numPart);
                        if (num > maxId) {
                            maxId = num;
                        }
                    } catch (NumberFormatException e) {
                        // Bỏ qua ID không đúng định dạng
                        System.out.println("Skipping non-standard ID: " + id);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not initialize user counter from DB: " + e.getMessage());
        }
        userCounter.set((int) Math.min(maxId, Integer.MAX_VALUE));
        initialized = true;
        System.out.println("UserFactory counter initialized to: " + maxId);
    }

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
            return "Mật khẩu không được để trống.";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }
        if (password.contains(" ")) {
            return "Mật khẩu không được chứa khoảng trắng.";
        }
        return null;
    }

    /** Tạo user mới mặc định (BIDDER_SELLER) với ID "USRxxxx". */
    public static User createUser(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        if (!initialized) {
            initializeCounter();
        }
        String id = "USR" + String.format("%04d", userCounter.incrementAndGet());
        return new RegularUser(id, username, password);
    }

    /**
     * Tạo user với vai trò cụ thể.
     *
     * @param username tên đăng nhập
     * @param password mật khẩu
     * @param role     vai trò: "BIDDER", "SELLER", "BIDDER_SELLER" (mặc định), "ADMIN"
     * @return thực thể User tương ứng với vai trò
     */
    public static User createUserWithRole(String username, String password, String role) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        if (!initialized) {
            initializeCounter();
        }
        String id = "USR" + String.format("%04d", userCounter.incrementAndGet());
        switch (role != null ? role.toUpperCase() : "") {
            case "BIDDER":
                return new Bidder(id, username, password);
            case "SELLER":
                return new Seller(id, username, password);
            case "ADMIN":
                String adminId = "ADM" + String.format("%04d", userCounter.incrementAndGet());
                return new Admin(adminId, username, password);
            default:
                return new RegularUser(id, username, password);
        }
    }

    /**
     * Tạo admin mới với ID "ADMxxxx".
     *
     * @param username tên đăng nhập
     * @param password mật khẩu
     * @return thực thể Admin được khởi tạo
     */
    public static Admin createAdmin(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        if (!initialized) {
            initializeCounter();
        }
        String id = "ADM" + String.format("%04d", userCounter.incrementAndGet());
        return new Model.Admin(id, username, password);
    }
}