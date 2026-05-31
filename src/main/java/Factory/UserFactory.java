package Factory;

import Model.User;
import Model.Admin;
import DAO.DatabaseUtil;

import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UserFactory {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final AtomicInteger userCounter = new AtomicInteger(0);
    private static boolean initialized = false;

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
                    String numPart = id.substring(3);
                    try {
                        long num = Long.parseLong(numPart);
                        if (num > maxId) maxId = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not initialize user counter: " + e.getMessage());
        }
        userCounter.set((int) Math.min(maxId, Integer.MAX_VALUE));
        initialized = true;
        System.out.println("UserFactory counter initialized to: " + maxId);
    }

    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) return "Password cannot be empty";
        if (password.length() < MIN_PASSWORD_LENGTH) return "Password must be at least 6 characters";
        if (containsWhitespace(password)) return "Password cannot contain whitespace";
        return null;
    }

    public static boolean isValidPassword(String password) {
        return getPasswordError(password) == null;
    }

    private static boolean containsWhitespace(String value) {
        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c)) return true;
        }
        return false;
    }

    public static User createUser(String username, String password) {
        if (!initialized) initializeCounter();
        String id = "USR" + String.format("%04d", userCounter.incrementAndGet());
        return new User(id, username, password);
    }

    public static Admin createAdmin(String username, String password) {
        if (!initialized) initializeCounter();
        String id = "ADM" + String.format("%04d", userCounter.incrementAndGet());
        return new Admin(id, username, password);
    }
}
