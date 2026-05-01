package DAO;

import Model.Bid;
import Model.AuctionSession;
import Model.Item;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionDAO {
    
    private final ItemDAO itemDAO = new ItemDAO();
    
    public boolean placeBid(String auctionId, String bidderId, double amount) {
        String sql = "INSERT INTO bids (id, auction_id, bidder_id, amount, timestamp) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String bidId = "BID" + System.currentTimeMillis() + (int)(Math.random() * 1000);
            stmt.setString(1, bidId);
            stmt.setString(2, auctionId);
            stmt.setString(3, bidderId);
            stmt.setDouble(4, amount);
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                updateAuctionPrice(conn, auctionId, amount, bidderId);
            }
            
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void updateAuctionPrice(Connection conn, String auctionId, double amount, String bidderId) throws SQLException {
        String sql = "UPDATE auction_sessions SET current_price = ?, highest_bidder_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, bidderId);
            stmt.setString(3, auctionId);
            stmt.executeUpdate();
        }
    }
    
    public boolean startAuction(String auctionId) {
        String sql = "UPDATE auction_sessions SET status = 'RUNNING', start_time = NOW(), end_time = DATE_ADD(NOW(), INTERVAL duration_minutes MINUTE) WHERE id = ? AND status = 'OPEN'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean finishAuction(String auctionId) {
        String sql = "UPDATE auction_sessions SET status = 'FINISHED', winner_id = highest_bidder_id WHERE id = ? AND status = 'RUNNING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean cancelAuction(String auctionId) {
        String sql = "UPDATE auction_sessions SET status = 'CANCELED' WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Optional<AuctionSession> findAuctionById(String auctionId) {
        String sql = "SELECT * FROM auction_sessions WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
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
    
    public List<AuctionSession> findRunningAuctions() {
        String sql = "SELECT * FROM auction_sessions WHERE status = 'RUNNING' ORDER BY end_time ASC";
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
    
    public List<AuctionSession> findOpenAuctions() {
        String sql = "SELECT * FROM auction_sessions WHERE status = 'OPEN' ORDER BY created_at DESC";
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
    
    public List<AuctionSession> findAllAuctions() {
        String sql = "SELECT * FROM auction_sessions ORDER BY created_at DESC";
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
    
    public List<Bid> getBidHistory(String auctionId) {
        String sql = "SELECT b.id, b.auction_id, b.bidder_id, u.username, b.amount, b.timestamp FROM bids b LEFT JOIN users u ON b.bidder_id = u.id WHERE b.auction_id = ? ORDER BY b.timestamp DESC";
        List<Bid> bids = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bid bid = new Bid(
                        rs.getString("auction_id"),
                        rs.getString("bidder_id"),
                        rs.getDouble("amount")
                    );
                    bid.setId(rs.getString("id"));
                    bid.setBidderUsername(rs.getString("username"));
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
    
    public List<Bid> getUserBidHistory(String userId) {
        String sql = "SELECT b.id, b.auction_id, b.bidder_id, u.username, b.amount, b.timestamp, i.name as item_name FROM bids b JOIN auction_sessions a ON b.auction_id = a.id JOIN items i ON a.item_id = i.id LEFT JOIN users u ON b.bidder_id = u.id WHERE b.bidder_id = ? ORDER BY b.timestamp DESC";
        List<Bid> bids = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bid bid = new Bid(
                        rs.getString("auction_id"),
                        rs.getString("bidder_id"),
                        rs.getDouble("amount")
                    );
                    bid.setId(rs.getString("id"));
                    bid.setBidderUsername(rs.getString("username"));
                    bid.setItemName(rs.getString("item_name"));
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
    
    public double getCurrentPrice(String auctionId) {
        String sql = "SELECT current_price FROM auction_sessions WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("current_price");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public boolean deductBalance(String userId, double amount) {
        String sql = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, userId);
            stmt.setDouble(3, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public BigDecimal getUserBalance(String userId) {
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
        
        try {
            java.lang.reflect.Field statusField = AuctionSession.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(session, status);
            
            java.lang.reflect.Field priceField = AuctionSession.class.getDeclaredField("currentPrice");
            priceField.setAccessible(true);
            priceField.set(session, currentPrice);
            
            java.lang.reflect.Field bidderField = AuctionSession.class.getDeclaredField("highestBidderId");
            bidderField.setAccessible(true);
            bidderField.set(session, highestBidderId);
            
            java.lang.reflect.Field winnerField = AuctionSession.class.getDeclaredField("winnerId");
            winnerField.setAccessible(true);
            winnerField.set(session, winnerId);
            
            if (startTime != null) {
                java.lang.reflect.Field startField = AuctionSession.class.getDeclaredField("startTime");
                startField.setAccessible(true);
                startField.set(session, startTime.toLocalDateTime());
            }
            
            if (endTime != null) {
                java.lang.reflect.Field endField = AuctionSession.class.getDeclaredField("endTime");
                endField.setAccessible(true);
                endField.set(session, endTime.toLocalDateTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return session;
    }
}
