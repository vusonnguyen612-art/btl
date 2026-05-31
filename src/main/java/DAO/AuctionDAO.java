package DAO;

import Model.Bid;
import Model.AuctionSession;
import Model.Item;
import Model.SearchCriteria;
import Exception.*;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO cho bảng auction_sessions và bids: quản lý đấu giá, đặt giá (có Sniper Protection),
 * thanh toán, phạt quá hạn, lịch sử, và tìm kiếm nâng cao.
 * <p>Sử dụng {@link ItemDAO} để nạp thông tin vật phẩm liên kết.</p>
 */
public class AuctionDAO {
    
    private final ItemDAO itemDAO = new ItemDAO();
    
    /**
     * Thực hiện việc ghi nhận lượt đặt giá mới của người dùng cho phiên đấu giá.
     * Phương thức thực hiện các bước kiểm tra nghiệp vụ nghiêm ngặt bao gồm:
     * - Kiểm tra sự tồn tại của phiên đấu giá.
     * - Kiểm tra trạng thái hoạt động của phiên (phải là RUNNING).
     * - Kiểm tra phiên đấu giá đã kết thúc theo thời gian thực chưa.
     * - Ngăn chặn người bán tự đấu giá sản phẩm của chính mình.
     * - Kiểm tra giá đặt mới phải lớn hơn giá hiện tại và đáp ứng bước giá tối thiểu.
     * - Kiểm tra số dư tài khoản của người đặt phải lớn hơn hoặc bằng giá đặt mới.
     * Nếu tất cả hợp lệ, ghi nhận lượt đặt giá vào DB, cập nhật giá hiện tại và kích hoạt Sniper Protection nếu đặt giá trong 2 phút cuối.
     *
     * @param auctionId ID của phiên đấu giá cần đặt giá.
     * @param bidderId  ID của người dùng thực hiện đặt giá.
     * @param amount    Số tiền đặt giá mới.
     * @return {@code true} nếu đặt giá và cập nhật DB thành công, ngược lại {@code false}.
     * @throws AuctionClosedException       Ném ra nếu phiên đấu giá không tồn tại, chưa bắt đầu hoặc đã kết thúc.
     * @throws InvalidBidException          Ném ra nếu giá trị đặt không lớn hơn giá hiện tại hoặc không đáp ứng bước giá tối thiểu.
     * @throws InsufficientBalanceException Ném ra nếu số dư tài khoản người mua nhỏ hơn số tiền đặt giá.
     * @throws UnauthorizedException        Ném ra nếu người bán tự đặt giá cho sản phẩm của mình.
     */
    public synchronized boolean placeBid(String auctionId, String bidderId, double amount)
            throws AuctionClosedException, InvalidBidException, InsufficientBalanceException, UnauthorizedException {
        // 1. Kiểm tra sự tồn tại của phiên đấu giá
        AuctionSession session = findAuctionById(auctionId).orElseThrow(() -> 
            new AuctionClosedException("Phiên đấu giá không tồn tại.", auctionId)
        );

        // 2. Kiểm tra trạng thái của phiên (phải là RUNNING)
        if (session.getStatus() != AuctionSession.Status.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá không ở trạng thái hoạt động.", auctionId);
        }

        // 3. Kiểm tra thời gian kết thúc của phiên (phải sau thời điểm hiện tại)
        if (session.getEndTime() != null && java.time.LocalDateTime.now().isAfter(session.getEndTime())) {
            throw new AuctionClosedException("Phiên đấu giá đã kết thúc.", auctionId);
        }

        // 4. Kiểm tra người đặt giá (không được là người bán sản phẩm đó)
        if (bidderId.equals(session.getSellerId())) {
            throw new UnauthorizedException("Người bán không được phép đặt giá cho sản phẩm của chính mình.", bidderId, "placeBid");
        }

        // 5. Kiểm tra giá đặt mới so với giá hiện tại
        double currentPrice = session.getCurrentPrice();
        if (amount <= currentPrice) {
            throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại.", amount, currentPrice);
        }

        // 6. Kiểm tra bước giá tối thiểu
        double minInc = session.getMinIncrement();
        if (amount < currentPrice + minInc) {
            throw new InvalidBidException("Giá đặt phải tăng tối thiểu " + minInc + " so với giá hiện tại.", amount, currentPrice);
        }

        // 7. Lấy số dư tài khoản của người đặt và kiểm tra số dư
        BigDecimal balance = getUserBalance(bidderId);
        BigDecimal bidAmount = BigDecimal.valueOf(amount);
        if (balance.compareTo(bidAmount) < 0) {
            throw new InsufficientBalanceException("Số dư tài khoản không đủ để thực hiện đặt giá.", bidderId, balance, bidAmount);
        }

        // 8. Ghi nhận bid + cập nhật giá trong 1 transaction (atomic)
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);

            String bidId = "BID" + System.currentTimeMillis() + (int)(Math.random() * 1000);
            String insertSql = "INSERT INTO bids (id, auction_id, bidder_id, amount, timestamp) VALUES (?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, bidId);
                stmt.setString(2, auctionId);
                stmt.setString(3, bidderId);
                stmt.setDouble(4, amount);
                int rows = stmt.executeUpdate();
                if (rows == 0) { conn.rollback(); return false; }
            }

            updateAuctionPrice(conn, auctionId, amount, bidderId);

            // Sniper Protection: gia hạn 2 phút nếu bid trong 2 phút cuối
            java.time.LocalDateTime endTime = session.getEndTime();
            if (endTime != null) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.Duration duration = java.time.Duration.between(now, endTime);
                long secondsRemaining = duration.getSeconds();
                if (secondsRemaining > 0 && secondsRemaining < 120) {
                    updateAuctionEndTime(conn, auctionId);
                    System.out.println("[Sniper Protection] Gia hạn phiên đấu giá " + auctionId + " thêm 2 phút.");
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    /**
     * Cập nhật giá hiện tại và người đặt giá cao nhất của phiên đấu giá trong cùng transaction.
     *
     * @param conn      Kết nối JDBC (đã thiết lập autoCommit=false).
     * @param auctionId ID của phiên đấu giá.
     * @param amount    Giá trị đặt giá mới (sẽ trở thành currentPrice).
     * @param bidderId  ID của người đặt giá mới.
     * @throws SQLException Nếu có lỗi khi thực thi câu lệnh SQL.
     */
    private void updateAuctionPrice(Connection conn, String auctionId, double amount, String bidderId) throws SQLException {
        String sql = "UPDATE auction_sessions SET current_price = ?, highest_bidder_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, bidderId);
            stmt.setString(3, auctionId);
            stmt.executeUpdate();
        }
    }

    /**
     * Gia hạn thời gian kết thúc của phiên đấu giá thêm 2 phút (Sniper Protection).
     * Được gọi tự động khi có lượt đặt giá trong 2 phút cuối của phiên.
     *
     * @param conn      Kết nối JDBC (đã thiết lập autoCommit=false).
     * @param auctionId ID của phiên đấu giá cần gia hạn.
     * @throws SQLException Nếu có lỗi khi thực thi câu lệnh SQL.
     */
    private void updateAuctionEndTime(Connection conn, String auctionId) throws SQLException {
        String sql = "UPDATE auction_sessions SET end_time = DATE_ADD(end_time, INTERVAL 2 MINUTE) WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Tạm dừng phiên đấu giá: gia hạn thêm 5 phút kể từ thời điểm hiện tại.
     * Chỉ áp dụng cho phiên đang ở trạng thái RUNNING.
     *
     * @param auctionId ID của phiên đấu giá cần tạm dừng.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
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

    /**
     * Bắt đầu phiên đấu giá: chuyển trạng thái từ OPEN sang RUNNING,
     * đặt start_time = NOW() và end_time = NOW() + duration_minutes.
     *
     * @param auctionId ID của phiên đấu giá cần bắt đầu.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
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
    
    /**
     * Kết thúc phiên đấu giá đang chạy.
     * Nếu có highest_bidder_id, chuyển sang PAYMENT_PENDING và gán winner_id.
     * Nếu không có ai đặt giá, chuyển thẳng sang FINISHED.
     *
     * @param auctionId ID của phiên đấu giá cần kết thúc.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
    public synchronized boolean finishAuction(String auctionId) {
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
    public synchronized boolean processPayment(String auctionId) {
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

    /**
     * Phạt người thắng thanh toán quá hạn 50,000 VND và chuyển phiên về FINISHED.
     * Số dư người thắng sẽ bị trừ tối đa đến 0 (GREATEST(balance - 50000, 0)).
     *
     * @param auctionId ID của phiên đấu giá cần phạt.
     * @return {@code true} nếu xử lý thành công, ngược lại {@code false}.
     */
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

    /**
     * Tìm các phiên đang ở trạng thái PAYMENT_PENDING quá hạn > 1 giờ kể từ endTime.
     *
     * @return Danh sách các phiên đấu giá quá hạn thanh toán.
     */
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
    
    /**
     * Hủy phiên đấu giá: chuyển trạng thái sang CANCELED.
     *
     * @param auctionId ID của phiên đấu giá cần hủy.
     * @param content   Lý do hủy phiên (ghi log).
     * @return {@code true} nếu hủy thành công, ngược lại {@code false}.
     */
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
    
    /**
     * Tìm phiên đấu giá theo ID, kết hợp với thông tin Item từ bảng items.
     *
     * @param auctionId ID của phiên đấu giá cần tìm.
     * @return {@link Optional} chứa {@link AuctionSession} nếu tìm thấy, ngược lại {@link Optional#empty()}.
     */
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
    
    /**
     * Tìm các phiên đang chạy (RUNNING) sắp kết thúc trước.
     *
     * @return Danh sách các phiên đấu giá đang hoạt động, sắp xếp theo thời gian kết thúc tăng dần.
     */
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
        String sql = "SELECT b.*, u.username FROM bids b JOIN users u ON b.bidder_id = u.id WHERE b.auction_id = ? ORDER BY b.timestamp DESC, b.amount DESC";
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
        String sql = "SELECT b.*, a.item_id, u.username FROM bids b JOIN auction_sessions a ON b.auction_id = a.id JOIN users u ON b.bidder_id = u.id WHERE b.bidder_id = ? ORDER BY b.timestamp DESC, b.amount DESC";
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
    
    /** Lấy danh sách phiên đã kết thúc (FINISHED/PAID) mà user đã tham gia (bidder hoặc seller). */
    public List<AuctionSession> findUserParticipatedAuctions(String userId) {
        String sql = "SELECT DISTINCT a.* FROM auction_sessions a " +
                     "LEFT JOIN bids b ON a.id = b.auction_id " +
                     "WHERE (b.bidder_id = ? OR a.seller_id = ?) " +
                     "AND a.status IN ('FINISHED', 'PAID', 'PAYMENT_PENDING') " +
                     "ORDER BY a.end_time DESC";
        List<AuctionSession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, userId);
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
    
    /**
     * Ánh xạ một dòng kết quả từ ResultSet thành đối tượng {@link AuctionSession}.
     * <p>Tự động nạp thông tin {@link Item} từ {@link ItemDAO#findById(String)}.
     * Nếu Item không tồn tại trong DB, tạo một placeholder với tên "[Deleted: itemId]".</p>
     *
     * @param rs ResultSet đang trỏ đến dòng dữ liệu cần ánh xạ.
     * @return đối tượng AuctionSession đã được gán đầy đủ các trường.
     * @throws SQLException nếu có lỗi đọc dữ liệu từ ResultSet.
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
        if (item == null) {
            System.err.println("[WARNING] Item " + itemId + " not found for auction " + id + ". Using placeholder.");
            item = new Model.Art(id, "[Deleted: " + itemId + "]", "Item has been deleted", startPrice, sellerId);
        }
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

    /** Tìm kiếm phiên đấu giá theo nhiều tiêu chí (keyword, category, status, price range, seller). */
    public List<AuctionSession> searchAuctions(SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT a.* FROM auction_sessions a ");
        sql.append("JOIN items i ON a.item_id = i.id ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            sql.append("AND (i.name LIKE ? OR i.description LIKE ?) ");
            String kw = "%" + criteria.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        if (criteria.getCategory() != null && !criteria.getCategory().isEmpty()) {
            sql.append("AND i.category = ? ");
            params.add(criteria.getCategory());
        }

        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            sql.append("AND a.status IN (");
            for (int i = 0; i < criteria.getStatuses().size(); i++) {
                sql.append(i > 0 ? ",?" : "?");
            }
            sql.append(") ");
            for (AuctionSession.Status s : criteria.getStatuses()) {
                params.add(s.name());
            }
        }

        if (criteria.getMinPrice() != null) {
            sql.append("AND a.current_price >= ? ");
            params.add(criteria.getMinPrice());
        }

        if (criteria.getMaxPrice() != null) {
            sql.append("AND a.current_price <= ? ");
            params.add(criteria.getMaxPrice());
        }

        if (criteria.getSellerId() != null && !criteria.getSellerId().isEmpty()) {
            sql.append("AND a.seller_id = ? ");
            params.add(criteria.getSellerId());
        }

        if (criteria.getSortBy() != null) {
            switch (criteria.getSortBy()) {
                case "price_asc":
                    sql.append("ORDER BY a.current_price ASC ");
                    break;
                case "price_desc":
                    sql.append("ORDER BY a.current_price DESC ");
                    break;
                case "oldest":
                    sql.append("ORDER BY a.created_at ASC ");
                    break;
                case "name":
                    sql.append("ORDER BY i.name ASC ");
                    break;
                default:
                    sql.append("ORDER BY a.created_at DESC ");
                    break;
            }
        } else {
            sql.append("ORDER BY a.created_at DESC ");
        }

        List<AuctionSession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

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

    /** Tìm phiên đấu giá theo item_id. */
    public Optional<AuctionSession> findByItemId(String itemId) {
        String sql = "SELECT * FROM auction_sessions WHERE item_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
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
