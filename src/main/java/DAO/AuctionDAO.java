package DAO;

import Model.Bid;
import Model.AuctionSession;
import Model.Item;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO cho bảng auction_sessions và bids: quản lý đấu giá, thanh toán, phạt, lịch sử. */
public class AuctionDAO {
    
    private final ItemDAO itemDAO = new ItemDAO();
    
    /** Ghi nhận lượt đặt giá vào bảng bids và cập nhật current_price / highest_bidder_id. */
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
    
    /** Gia hạn phiên đấu giá thêm 5 phút. */
    public boolean stopAuction(String auctionId) {
        String sql = "UPDATE auction_sessions SET end_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE) WHERE id = ? AND status = 'RUNNING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Bắt đầu phiên: chuyển trạng thái RUNNING, đặt start_time và end_time. */
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
    
    /** Kết thúc phiên: nếu có highest_bidder -> PAYMENT_PENDING, nếu không -> FINISHED. */
    public boolean finishAuction(String auctionId) {
        String checkSql = "SELECT highest_bidder_id FROM auction_sessions WHERE id = ? AND status = 'RUNNING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, auctionId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getString("highest_bidder_id") != null) {
                    String updateSql = "UPDATE auction_sessions SET status = 'PAYMENT_PENDING', winner_id = highest_bidder_id WHERE id = ? AND status = 'RUNNING'";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, auctionId);
                        return updateStmt.executeUpdate() > 0;
                    }
                } else {
                    String updateSql = "UPDATE auction_sessions SET status = 'FINISHED' WHERE id = ? AND status = 'RUNNING'";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, auctionId);
                        return updateStmt.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Xử lý thanh toán trong transaction: trừ tiền người thắng -> cộng cho người bán -> chuyển PAID.
     * Rollback nếu số dư không đủ hoặc lỗi.
     */
    public boolean processPayment(String auctionId) {
        String sql = "SELECT current_price, highest_bidder_id, seller_id FROM auction_sessions WHERE id = ? AND status = 'PAYMENT_PENDING'";
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, auctionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                    double amount = rs.getDouble("current_price");
                    String winnerId = rs.getString("highest_bidder_id");
                    String sellerId = rs.getString("seller_id");
                    if (winnerId == null || sellerId == null) { conn.rollback(); return false; }

                    String deductWinner = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?";
                    try (PreparedStatement deductStmt = conn.prepareStatement(deductWinner)) {
                        deductStmt.setDouble(1, amount);
                        deductStmt.setString(2, winnerId);
                        deductStmt.setDouble(3, amount);
                        if (deductStmt.executeUpdate() == 0) { conn.rollback(); return false; }
                    }

                    String addSeller = "UPDATE users SET balance = balance + ? WHERE id = ?";
                    try (PreparedStatement addStmt = conn.prepareStatement(addSeller)) {
                        addStmt.setDouble(1, amount);
                        addStmt.setString(2, sellerId);
                        addStmt.executeUpdate();
                    }

                    String updateStatus = "UPDATE auction_sessions SET status = 'PAID' WHERE id = ?";
                    try (PreparedStatement statusStmt = conn.prepareStatement(updateStatus)) {
                        statusStmt.setString(1, auctionId);
                        statusStmt.executeUpdate();
                    }

                    conn.commit();
                    return true;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Phạt người thắng quá hạn thanh toán 50,000 và chuyển phiên về FINISHED. */
    public boolean penalizeWinner(String auctionId) {
        String sql = "SELECT highest_bidder_id FROM auction_sessions WHERE id = ? AND status = 'PAYMENT_PENDING'";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return false;
                String winnerId = rs.getString("highest_bidder_id");
                if (winnerId != null) {
                    double penalty = 50000;
                    String deductPenalty = "UPDATE users SET balance = GREATEST(balance - ?, 0) WHERE id = ?";
                    try (PreparedStatement penaltyStmt = conn.prepareStatement(deductPenalty)) {
                        penaltyStmt.setDouble(1, penalty);
                        penaltyStmt.setString(2, winnerId);
                        penaltyStmt.executeUpdate();
                    }
                }
                String updateStatus = "UPDATE auction_sessions SET status = 'FINISHED' WHERE id = ?";
                try (PreparedStatement statusStmt = conn.prepareStatement(updateStatus)) {
                    statusStmt.setString(1, auctionId);
                    statusStmt.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Tìm các phiên PAYMENT_PENDING quá hạn >1 giờ kể từ endTime. */
    public List<AuctionSession> findOverduePaymentAuctions() {
        String sql = "SELECT * FROM auction_sessions WHERE status = 'PAYMENT_PENDING' AND end_time IS NOT NULL AND DATE_ADD(end_time, INTERVAL 1 HOUR) <= NOW()";
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
    
    /** Hủy phiên đấu giá. */
    public boolean cancelAuction(String auctionId, String content) {
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
    
    /** Tìm phiên đấu giá theo ID (join với items). */
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
    
    /** Tìm các phiên đang chạy (RUNNING) sắp kết thúc trước. */
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
    
    /** Tìm các phiên mở (OPEN) mới nhất trước. */
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
    
    /** Tìm các phiên chờ thanh toán (PAYMENT_PENDING). */
    public List<AuctionSession> findPaymentPendingAuctions() {
        String sql = "SELECT * FROM auction_sessions WHERE status = 'PAYMENT_PENDING' ORDER BY end_time ASC";
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

    /** Lấy tất cả phiên đấu giá, mới nhất trước. */
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
    
    /** Lấy lịch sử đặt giá của một phiên, mới nhất trước. */
    public List<Bid> getBidHistory(String auctionId) {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY timestamp DESC";
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
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) bid.setTimestamp(ts.toLocalDateTime());
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
    
    /** Lấy lịch sử đặt giá của một người dùng (join với auction_sessions để lấy item_id). */
    public List<Bid> getUserBidHistory(String userId) {
        String sql = """
                SELECT b.*, i.name AS item_name,
                       CASE WHEN a.winner_id = b.bidder_id THEN 1 ELSE 0 END AS is_winner
                FROM bids b
                JOIN auction_sessions a ON b.auction_id = a.id
                JOIN items i ON a.item_id = i.id
                WHERE b.bidder_id = ?
                ORDER BY b.timestamp DESC
                """;
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
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) bid.setTimestamp(ts.toLocalDateTime());
                    bid.setItemName(rs.getString("item_name"));
                    bid.setWinner(rs.getInt("is_winner") == 1);
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
    
    /** Lấy giá hiện tại của phiên. */
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
    
    /** Trừ tiền từ tài khoản người dùng (kiểm tra số dư >= amount). */
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
    
    /** Lấy số dư người dùng (đơn vị BigDecimal). */
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

    /** Lưu phiên đấu giá mới với thông tin cơ bản. */
    public void saveAuction(AuctionSession auction) {
        String sql = "INSERT INTO auction_sessions (id, item_id, seller_id, status, current_price, start_price, duration_minutes, min_increment) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItem().getId());
            stmt.setString(3, auction.getSellerId());
            stmt.setString(4, auction.getStatus().name());
            stmt.setDouble(5, auction.getCurrentPrice());
            stmt.setDouble(6, auction.getStartPrice());
            stmt.setLong(7, auction.getDurationMinutes());
            stmt.setDouble(8, auction.getMinIncrement());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
