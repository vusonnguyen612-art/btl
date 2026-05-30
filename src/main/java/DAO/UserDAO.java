package DAO;

import Model.User;
import Model.Admin;
import Model.Bidder;
import Model.Seller;
import Model.RegularUser;
import Exception.AuthenticationException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * DAO cho bảng users: đăng ký, đăng nhập, xác thực, quản lý số dư, đổi mật khẩu, avatar.
 * <p>Sử dụng SHA-256 + salt để hash mật khẩu. Hỗ trợ phân biệt User/Admin qua tiền tố ID (USR/ADM).</p>
 */
public class UserDAO {

    /**
     * Đăng ký người dùng mới vào bảng users. Tự động hash mật khẩu trước khi lưu.
     *
     * @param user Đối tượng {@link User} chứa thông tin cần đăng ký.
     * @return {@code true} nếu đăng ký thành công (có dòng được thêm), ngược lại {@code false}.
     */
    public boolean register(User user) {
        String hashedPassword = hashPassword(user.getPassword());
        String role = user.getRole();
        String sql = "INSERT INTO users (id, username, password, email, is_seller, is_bidder, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, hashedPassword);
            stmt.setString(4, user.getEmail());
            stmt.setBoolean(5, user.isSeller());
            stmt.setBoolean(6, user.isBidder());
            stmt.setString(7, role);
            int rows = stmt.executeUpdate();
            System.out.println("Registered user: " + user.getUsername() + ", rows affected: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Register error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đăng nhập với username/password, trả về Optional chứa {@link User} nếu hợp lệ.
     * So sánh phân biệt hoa thường (BINARY). Tự động hash password trước khi truy vấn.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu dạng plain text (sẽ được hash trong phương thức).
     * @return {@link Optional} chứa {@link User} nếu đăng nhập thành công, ngược lại {@link Optional#empty()}.
     */
    public Optional<User> login(String username, String password) {
        String sql = "SELECT * FROM users WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (storedHash == null) return Optional.empty();

                    // Thử PBKDF2 mới
                    String newHash = hashPassword(password);
                    if (newHash.equals(storedHash)) {
                        return Optional.of(mapResultSetToUser(rs));
                    }

                    // Thử SHA-256 cũ, nâng cấp nếu đúng
                    String oldHash = legacySha256Hash(password);
                    if (oldHash.equals(storedHash)) {
                        upgradePasswordHash(username, newHash);
                        return Optional.of(mapResultSetToUser(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Xác thực đăng nhập với username/password.
     * Hỗ trợ cả PBKDF2 (mới) và SHA-256 (cũ, tự động nâng cấp).
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu dạng plain text.
     * @return Đối tượng {@link User} nếu xác thực thành công.
     * @throws AuthenticationException Nếu sai tài khoản hoặc mật khẩu.
     */
    public User authenticate(String username, String password) throws AuthenticationException {
        // Delegates to the new implementation below
        return authenticateWithMigration(username, password);
    }

    // ── New methods ───────────────────────────────────────

    /**
     * Tìm người dùng theo ID.
     *
     * @param id ID của người dùng (USRxxxx hoặc ADMxxxx).
     * @return {@link Optional} chứa {@link User} nếu tìm thấy, ngược lại {@link Optional#empty()}.
     */
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

    /**
     * Lấy danh sách tất cả người dùng trong hệ thống.
     * Chỉ admin mới có quyền gọi phương thức này.
     *
     * @return Danh sách tất cả {@link User}.
     */
    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
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

    /**
     * Xóa người dùng theo ID (dành cho admin).
     *
     * @param userId ID của người dùng cần xóa.
     * @return {@code true} nếu xóa thành công.
     */
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

    /**
     * Nâng cấp user thường thành Admin (đổi ID từ USRxxxx → ADMxxxx).
     * Dùng khi seed admin mặc định mà username "admin" đã tồn tại.
     *
     * @param username Tên đăng nhập cần nâng cấp.
     * @return {@code true} nếu nâng cấp thành công.
     */
    public boolean upgradeToAdmin(String username) {
        String selectSql = "SELECT id FROM users WHERE BINARY username = ?";
        String updateSql = "UPDATE users SET id = ? WHERE BINARY username = ? AND id LIKE 'USR%'";
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            String oldId;
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                    oldId = rs.getString("id");
                }
            }
            if (oldId == null || !oldId.startsWith("USR")) {
                conn.rollback();
                return false;
            }
            String newId = "ADM" + oldId.substring(3);
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, newId);
                stmt.setString(2, username);
                int rows = stmt.executeUpdate();
                conn.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra tên đăng nhập đã tồn tại trong hệ thống hay chưa (phân biệt hoa thường).
     *
     * @param username Tên đăng nhập cần kiểm tra.
     * @return {@code true} nếu username đã tồn tại, ngược lại {@code false}.
     */
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

    /**
     * Đổi mật khẩu người dùng (yêu cầu mật khẩu cũ chính xác).
     *
     * @param username    Tên đăng nhập.
     * @param oldPassword Mật khẩu cũ dạng plain text.
     * @param newPassword Mật khẩu mới dạng plain text.
     * @return {@code true} nếu đổi mật khẩu thành công, ngược lại {@code false}.
     */
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

    /**
     * Ánh xạ một dòng dữ liệu từ ResultSet thành đối tượng {@link User} (hoặc {@link Admin} nếu ID bắt đầu bằng ADM).
     *
     * @param rs ResultSet đang trỏ tới dòng dữ liệu.
     * @return Đối tượng {@link User} hoặc {@link Admin} tương ứng.
     * @throws SQLException Nếu có lỗi đọc dữ liệu.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        boolean isSeller = rs.getBoolean("is_seller");
        boolean isBidder = rs.getBoolean("is_bidder");
        // Đọc cột role (có thể NULL với user cũ)
        String role = null;
        try {
            role = rs.getString("role");
        } catch (SQLException e) {
            // Cột role chưa tồn tại — bỏ qua
        }
        
        User user;
        if (role != null && role.equals("ADMIN")) {
            user = new Admin(id, username, password);
        } else if (id != null && id.startsWith("ADM")) {
            user = new Admin(id, username, password);
        } else if (isSeller && isBidder) {
            user = new RegularUser(id, username, password);
        } else if (isBidder) {
            user = new Bidder(id, username, password);
        } else if (isSeller) {
            user = new Seller(id, username, password);
        } else {
            user = new RegularUser(id, username, password);
        }
        user.setEmail(rs.getString("email"));
        user.setBalance(rs.getBigDecimal("balance"));
        return user;
    }

    /**
     * Cập nhật số dư của người dùng (ghi đè giá trị cũ).
     *
     * @param username   Tên đăng nhập của người dùng.
     * @param newBalance Giá trị số dư mới.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
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

    /**
     * Cộng thêm số dư cho người dùng (balance = balance + amount).
     *
     * @param userId ID của người dùng.
     * @param amount Số tiền cần cộng thêm.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
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

    /**
     * Lấy số dư hiện tại của người dùng.
     *
     * @param userId ID của người dùng.
     * @return Số dư hiện tại dạng {@link BigDecimal}, trả về {@link BigDecimal#ZERO} nếu không tìm thấy hoặc có lỗi.
     */
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

    /**
     * Lấy tên đăng nhập từ ID người dùng.
     *
     * @param userId ID của người dùng.
     * @return Tên đăng nhập nếu tìm thấy, hoặc trả lại chính {@code userId} nếu không tìm thấy.
     */
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

    /**
     * Lấy đường dẫn ảnh đại diện của người dùng.
     *
     * @param userId ID của người dùng.
     * @return Đường dẫn ảnh đại diện (String), hoặc {@code null} nếu chưa có hoặc không tìm thấy.
     */
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

    /**
     * Cập nhật đường dẫn ảnh đại diện của người dùng.
     *
     * @param userId     ID của người dùng.
     * @param avatarPath Đường dẫn ảnh đại diện mới.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
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

    /**
     * Hash mật khẩu bằng PBKDF2WithHmacSHA256 (65536 iterations) với salt cố định.
     * Đầu ra 64 ký tự hex (tương thích kích thước cột password VARCHAR(64) trong DB).
     * Không lưu trữ mật khẩu dạng plain text trong database.
     *
     * @param password Mật khẩu dạng plain text cần hash.
     * @return Chuỗi hex 64 ký tự.
     */
    private String hashPassword(String password) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                "AuCtIoNaPpSaLt!#".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                65536, 256);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 not available, falling back to SHA-256", e);
        }
    }

    /**
     * Hash SHA-256 cũ (giữ lại để migration backward-compatible).
     * Chỉ dùng để xác thực user đã đăng ký từ trước khi nâng cấp.
     */
    private String legacySha256Hash(String password) {
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

    /**
     * Xác thực mật khẩu: thử PBKDF2 mới trước, fallback SHA-256 cũ.
     * Nếu user dùng hash cũ, tự động nâng cấp lên PBKDF2.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu dạng plain text.
     * @return Đối tượng User nếu xác thực thành công.
     * @throws AuthenticationException Nếu sai tài khoản hoặc mật khẩu.
     */
    public User authenticateWithMigration(String username, String password) throws AuthenticationException {
        String sql = "SELECT * FROM users WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (storedHash == null) {
                        throw new AuthenticationException("Invalid username or password", username);
                    }

                    // Thử PBKDF2 mới trước
                    String newHash = hashPassword(password);
                    if (newHash.equals(storedHash)) {
                        return mapResultSetToUser(rs);
                    }

                    // Thử SHA-256 cũ, nâng cấp nếu đúng
                    String oldHash = legacySha256Hash(password);
                    if (oldHash.equals(storedHash)) {
                        upgradePasswordHash(username, newHash);
                        System.out.println("[Auth] User " + username + " upgraded from SHA-256 to PBKDF2.");
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new AuthenticationException("Invalid username or password", username);
    }

    /** Nâng cấp hash mật khẩu từ SHA-256 → PBKDF2. */
    private void upgradePasswordHash(String username, String newPbkdf2Hash) {
        String sql = "UPDATE users SET password = ? WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPbkdf2Hash);
            stmt.setString(2, username);
            stmt.executeUpdate();
            System.out.println("[Password Upgrade] User " + username + " upgraded to PBKDF2.");
        } catch (SQLException e) {
            System.err.println("[Password Upgrade] Failed to upgrade " + username + ": " + e.getMessage());
        }
    }
}
