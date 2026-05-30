package DAO;

import Model.ChatMessage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DAO quản lý việc đọc/ghi tin nhắn trò chuyện trực tiếp (Live Chat) vào cơ sở dữ liệu MySQL.
 */
public class ChatDAO {

    /**
     * Lưu một tin nhắn trò chuyện mới của người dùng vào cơ sở dữ liệu.
     *
     * @param auctionId ID của phiên đấu giá mà tin nhắn thuộc về.
     * @param senderId  ID của người gửi tin nhắn.
     * @param message   Nội dung tin nhắn trò chuyện.
     * @return {@code true} nếu lưu thành công vào DB, ngược lại {@code false}.
     */
    public boolean saveChatMessage(String auctionId, String senderId, String message) {
        // Validate: không cho phép tin nhắn rỗng hoặc null
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO chat_messages (id, auction_id, sender_id, message, timestamp) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String messageId = "MSG" + System.currentTimeMillis() + (int)(Math.random() * 1000);
            stmt.setString(1, messageId);
            stmt.setString(2, auctionId);
            stmt.setString(3, senderId);
            stmt.setString(4, message);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi lưu tin nhắn chat: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy toàn bộ lịch sử trò chuyện của một phiên đấu giá cụ thể, sắp xếp theo thời gian tăng dần.
     * Thực hiện truy vấn JOIN với bảng users để lấy tên hiển thị (username) của người gửi.
     *
     * @param auctionId ID của phiên đấu giá cần lấy lịch sử trò chuyện.
     * @return Danh sách các đối tượng {@link ChatMessage}.
     */
    public List<ChatMessage> getChatHistory(String auctionId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT c.id, c.auction_id, c.sender_id, u.username AS sender_name, c.message, c.timestamp " +
                     "FROM chat_messages c JOIN users u ON c.sender_id = u.id " +
                     "WHERE c.auction_id = ? ORDER BY c.timestamp ASC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChatMessage chatMsg = new ChatMessage(
                        rs.getString("id"),
                        rs.getString("auction_id"),
                        rs.getString("sender_id"),
                        rs.getString("sender_name"),
                        rs.getString("message"),
                        rs.getTimestamp("timestamp")
                    );
                    list.add(chatMsg);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy lịch sử trò chuyện: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}
