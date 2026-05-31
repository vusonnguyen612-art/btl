package DAO;

import Model.User;
import Model.Admin;
import Exception.AuthenticationException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** DAO cho bảng users: đăng ký, đăng nhập, xác thực, quản lý số dư. */
public class UserDAO {

    /** Đăng ký người dùng mới vào bảng users. */
    public boolean register(User user) {
        String hashedPassword = hashPassword(user.getPassword());
        String sql = "INSERT INTO users (id, username, password, email, is_seller, is_bidder) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, hashedPassword);
            stmt.setString(4, user.getEmail());
            stmt.setBoolean(5, user.isSeller());
            stmt.setBoolean(6, user.isBidder());
            int rows = stmt.executeUpdate();
            System.out.println("Registered user: " + user.getUsername() + ", rows affected: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Register error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** Đăng nhập với username/password, trả về Optional<User>. */
    public Optional<User> login(String username, String password) {
        String hashedPassword = hashPassword(password);
        String sql = "SELECT id, password FROM users WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (hashedPassword.equals(storedHash)) {
                        return Optional.of(findById(rs.getString("id")).orElse(null));
                    }
                    for (String suffix : new String[]{"|BIDDER_SELLER", "|BIDDER", "|SELLER"}) {
                        if (hashPassword(password + suffix).equals(storedHash)) {
                            upgradePasswordHash(username, hashedPassword);
                            return Optional.of(findById(rs.getString("id")).orElse(null));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private void upgradePasswordHash(String username, String newHash) {
        String sql = "UPDATE users SET password = ? WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Xác thực đăng nhập, throw AuthenticationException nếu sai. */
    public User authenticate(String username, String password) throws AuthenticationException {
        Optional<User> userOpt = login(username, password);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }
        throw new AuthenticationException("Invalid username or password", username);
    }

    /** Tìm user theo ID. */
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /** Kiểm tra username đã tồn tại chưa. */
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Đổi mật khẩu (yêu cầu mật khẩu cũ). */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        String hashedNew = hashPassword(newPassword);
        String hashedOld = hashPassword(oldPassword);
        String sql = "UPDATE users SET password = ? WHERE BINARY username = ? AND BINARY password = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedNew);
            stmt.setString(2, username);
            stmt.setString(3, hashedOld);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String password = rs.getString("password");

        User user;
        if (id != null && id.startsWith("ADM")) {
            user = new Admin(id, username, password);
        } else {
            user = new User(id, username, password);
        }
        user.setEmail(rs.getString("email"));
        user.setBalance(rs.getBigDecimal("balance"));
        try {
            user.setSeller(rs.getBoolean("is_seller"));
            user.setBidder(rs.getBoolean("is_bidder"));
        } catch (SQLException e) {
        }
        try {
            user.setAvatarPath(rs.getString("avatar_path"));
        } catch (SQLException e) {
        }
        try {
            user.setBlocked(rs.getBoolean("is_blocked"));
        } catch (SQLException e) {
        }
        return user;
    }

    /** Cập nhật số dư người dùng. */
    public boolean updateBalance(String username, BigDecimal newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Cộng thêm số dư cho người dùng. */
    public boolean addBalance(String userId, BigDecimal amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Lấy số dư hiện tại. */
    public BigDecimal getBalance(String userId) {
        String sql = "SELECT balance FROM users WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    /** Lấy tên đăng nhập từ ID, trả về userId nếu không tìm thấy. */
    public String getUsernameById(String userId) {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userId;
    }

    /** Lấy đường dẫn ảnh đại diện. */
    public String getAvatarPath(String userId) {
        String sql = "SELECT avatar_path FROM users WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("avatar_path");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Cập nhật đường dẫn ảnh đại diện. */
    public boolean updateAvatarPath(String userId, String avatarPath) {
        String sql = "UPDATE users SET avatar_path = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, avatarPath);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Lấy danh sách tất cả người dùng. */
    public java.util.List<User> findAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /** Xóa người dùng theo ID (không cho xóa admin). */
    public boolean deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE id = ? AND id NOT LIKE 'ADM%'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Chặn/mở chặn người dùng. */
    public boolean blockUser(String userId, boolean blocked) {
        String sql = "UPDATE users SET is_blocked = ? WHERE id = ? AND id NOT LIKE 'ADM%'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, blocked);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Hash mật khẩu bằng SHA-256 + salt cố định, không lưu plain text. */
    private String hashPassword(String password) {
        try {
            String salted = "AuCtIoNaPpSaLt!#" + password;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(salted.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
