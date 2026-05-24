package DAO;

import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Script một lần: hash toàn bộ mật khẩu plain text trong DB
 * để tương thích với bản cập nhật mã hóa password.
 *
 * Cách chạy:
 *   mvn compile exec:java -Dexec.mainClass="DAO.MigratePasswords"
 *
 * Chạy DUY NHẤT 1 lần, sau đó có thể xóa class này.
 */
public class MigratePasswords {
    private static final String SALT = "AuCtIoNaPpSaLt!#";

    public static void main(String[] args) {
        System.out.println("=== Migrate Passwords: Hash toàn bộ mật khẩu plain text ===");
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Tìm tất cả user có password chưa được hash (dài < 64 ký tự hex)
            String selectSql = "SELECT id, username, password FROM users WHERE LENGTH(password) < 64";
            try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                 ResultSet rs = stmt.executeQuery()) {

                int updated = 0;
                int skipped = 0;

                while (rs.next()) {
                    String id = rs.getString("id");
                    String username = rs.getString("username");
                    String plainPassword = rs.getString("password");

                    // Nếu password đã là SHA-256 hex 64 ký tự thì bỏ qua
                    if (plainPassword != null && plainPassword.length() == 64
                            && plainPassword.matches("[0-9a-f]{64}")) {
                        skipped++;
                        continue;
                    }

                    String hashed = hashPassword(plainPassword);

                    String updateSql = "UPDATE users SET password = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, hashed);
                        updateStmt.setString(2, id);
                        int rows = updateStmt.executeUpdate();
                        if (rows > 0) {
                            System.out.println("  ✓ Migrated: " + username + " (ID: " + id + ")");
                            updated++;
                        }
                    }
                }

                System.out.println("\n=== KẾT QUẢ ===");
                System.out.println("  Đã migrate: " + updated + " tài khoản");
                System.out.println("  Đã bỏ qua (đã hash sẵn): " + skipped + " tài khoản");
                System.out.println("  Tổng: " + (updated + skipped) + " tài khoản");
                if (updated > 0) {
                    System.out.println("\n✅ Có thể đăng nhập lại bằng mật khẩu cũ.");
                } else {
                    System.out.println("\nℹ️  Không có tài khoản nào cần migrate.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String hashPassword(String password) {
        try {
            String salted = SALT + password;
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
