package DAO;

import Model.User;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class UserDAO {
    
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
    
    public Optional<User> login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
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
    
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
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
    
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ? AND password = ?";
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
    
    public boolean updateBalance(String username, BigDecimal newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";
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
}