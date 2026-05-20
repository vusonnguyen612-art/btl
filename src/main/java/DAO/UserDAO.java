package DAO;

import Model.User;
import Exception.AuthenticationException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

/** DAO cho bảng users: đăng ký, đăng nhập, xác thực, quản lý số dư. */
public class UserDAO {

    /** Đăng ký người dùng mới vào bảng users. */
    public boolean register(User user) {
        String sql = "INSERT INTO users (id, username, password, email, is_seller, is_bidder) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
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
        String sql = "SELECT * FROM users WHERE BINARY username = ? AND BINARY password = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
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
        String sql = "UPDATE users SET password = ? WHERE BINARY username = ? AND BINARY password = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            stmt.setString(3, oldPassword);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("password")
        );
        user.setEmail(rs.getString("email"));
        user.setBalance(rs.getBigDecimal("balance"));
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
}
