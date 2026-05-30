package DAO;

import Model.AuctionSession;
import Model.Item;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO cho bảng auction_sessions: thêm, sửa, xóa, tìm kiếm phiên đấu giá.
 * <p>Phối hợp với {@link ItemDAO} để nạp thông tin vật phẩm liên kết.</p>
 */
public class AuctionSessionDAO {
    
    private final ItemDAO itemDAO = new ItemDAO();

    /**
     * Lưu phiên đấu giá mới vào bảng auction_sessions.
     *
     * @param session Đối tượng {@link AuctionSession} cần lưu.
     * @return {@code true} nếu lưu thành công, ngược lại {@code false}.
     */
    public boolean save(AuctionSession session) {
        String sql = "INSERT INTO auction_sessions (id, item_id, seller_id, status, current_price, start_price, duration_minutes, min_increment) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, session.getId());
            stmt.setString(2, session.getItem().getId());
            stmt.setString(3, session.getSellerId());
            stmt.setString(4, session.getStatus().name());
            stmt.setDouble(5, session.getCurrentPrice());
            stmt.setDouble(6, session.getStartPrice());
            stmt.setLong(7, session.getDurationMinutes());
            stmt.setDouble(8, session.getMinIncrement());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cập nhật trạng thái, giá hiện tại, người đặt cao nhất, winner_id, và thời gian của phiên đấu giá.
     *
     * @param session Đối tượng {@link AuctionSession} chứa dữ liệu cập nhật.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
    public boolean update(AuctionSession session) {
        String sql = "UPDATE auction_sessions SET status = ?, current_price = ?, highest_bidder_id = ?, winner_id = ?, start_time = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, session.getStatus().name());
            stmt.setDouble(2, session.getCurrentPrice());
            stmt.setString(3, session.getHighestBidderId());
            stmt.setString(4, session.getWinnerId());
            stmt.setTimestamp(5, session.getStartTime() != null ? Timestamp.valueOf(session.getStartTime()) : null);
            stmt.setTimestamp(6, session.getEndTime() != null ? Timestamp.valueOf(session.getEndTime()) : null);
            stmt.setString(7, session.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Tìm phiên đấu giá theo ID.
     *
     * @param id ID của phiên đấu giá cần tìm.
     * @return {@link Optional} chứa {@link AuctionSession} nếu tìm thấy, ngược lại {@link Optional#empty()}.
     */
    public Optional<AuctionSession> findById(String id) {
        String sql = "SELECT * FROM auction_sessions WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    /**
     * Tìm danh sách phiên đấu giá theo trạng thái.
     *
     * @param status Trạng thái cần lọc ({@link AuctionSession.Status}).
     * @return Danh sách các phiên đấu giá có trạng thái tương ứng.
     */
    public List<AuctionSession> findByStatus(AuctionSession.Status status) {
        String sql = "SELECT * FROM auction_sessions WHERE status = ?";
        List<AuctionSession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }
    
    /**
     * Tìm danh sách phiên đấu giá theo ID người bán.
     *
     * @param sellerId ID của người bán cần tra cứu.
     * @return Danh sách các phiên đấu giá thuộc về người bán đó.
     */
    public List<AuctionSession> findBySellerId(String sellerId) {
        String sql = "SELECT * FROM auction_sessions WHERE seller_id = ?";
        List<AuctionSession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }
    
    /**
     * Lấy tất cả phiên đấu giá trong hệ thống.
     *
     * @return Danh sách toàn bộ các phiên đấu giá.
     */
    public List<AuctionSession> findAll() {
        String sql = "SELECT * FROM auction_sessions";
        List<AuctionSession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                sessions.add(mapResultSetToSession(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }
    
    /**
     * Ánh xạ một dòng dữ liệu từ ResultSet thành đối tượng {@link AuctionSession}.
     * Tự động nạp thông tin {@link Item} từ {@link ItemDAO#findById(String)}.
     * Nếu không tìm thấy Item, tạo placeholder để tránh lỗi NullPointerException.
     *
     * @param rs ResultSet đang trỏ tới dòng dữ liệu cần ánh xạ.
     * @return Đối tượng {@link AuctionSession} đã được khởi tạo đầy đủ.
     * @throws SQLException Nếu có lỗi khi đọc dữ liệu từ ResultSet.
     */
    private AuctionSession mapResultSetToSession(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("item_id");
        String sellerId = rs.getString("seller_id");
        AuctionSession.Status status = AuctionSession.Status.valueOf(rs.getString("status"));
        double currentPrice = rs.getDouble("current_price");
        double startPrice = rs.getDouble("start_price");
        long duration = rs.getLong("duration_minutes");
        double minInc = rs.getDouble("min_increment");
        String highestBidderId = rs.getString("highest_bidder_id");
        String winnerId = rs.getString("winner_id");
        Timestamp startTime = rs.getTimestamp("start_time");
        Timestamp endTime = rs.getTimestamp("end_time");
        
        Item item = itemDAO.findById(itemId).orElse(null);
        AuctionSession session = new AuctionSession(id, item, sellerId, startPrice, duration);
        session.setMinIncrement(minInc);
        session.setStatus(status);
        session.setCurrentPrice(currentPrice);
        session.setHighestBidderId(highestBidderId);
        session.setWinnerId(winnerId);
        if (startTime != null) session.setStartTime(startTime.toLocalDateTime());
        if (endTime != null) session.setEndTime(endTime.toLocalDateTime());
        return session;
    }
}