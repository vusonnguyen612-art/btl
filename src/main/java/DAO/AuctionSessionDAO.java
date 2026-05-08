package DAO;

import Model.AuctionSession;
import Model.Item;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionSessionDAO {
    
    private final ItemDAO itemDAO = new ItemDAO();
    
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