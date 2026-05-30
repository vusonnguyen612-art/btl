package DAO;

import Model.AuctionSession;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lớp DAO quản lý danh sách sản phẩm theo dõi (Watchlist) của người dùng trong cơ sở dữ liệu.
 */
public class WatchlistDAO {

    private final AuctionDAO auctionDAO = new AuctionDAO();

    /**
     * Thêm một phiên đấu giá vào danh sách theo dõi của người dùng.
     * Sử dụng cấu trúc INSERT IGNORE hoặc kiểm tra trùng lặp để tránh lỗi khóa duy nhất (Unique Key).
     *
     * @param userId    ID của người dùng muốn theo dõi.
     * @param auctionId ID của phiên đấu giá cần theo dõi.
     * @return {@code true} nếu thêm thành công hoặc đã theo dõi, ngược lại {@code false}.
     */
    public boolean addWatchlist(String userId, String auctionId) {
        String sql = "INSERT IGNORE INTO watchlist (id, user_id, auction_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String watchId = "WCH" + System.currentTimeMillis() + (int)(Math.random() * 1000);
            stmt.setString(1, watchId);
            stmt.setString(2, userId);
            stmt.setString(3, auctionId);
            
            // Execute update: trả về 0 nếu bản ghi đã tồn tại (do INSERT IGNORE)
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi thêm vào watchlist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa một phiên đấu giá khỏi danh sách theo dõi của người dùng.
     *
     * @param userId    ID của người dùng.
     * @param auctionId ID của phiên đấu giá cần hủy theo dõi.
     * @return {@code true} nếu xóa thành công, ngược lại {@code false}.
     */
    public boolean removeWatchlist(String userId, String auctionId) {
        String sql = "DELETE FROM watchlist WHERE user_id = ? AND auction_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, auctionId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi xóa khỏi watchlist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy toàn bộ danh sách các phiên đấu giá mà một người dùng đang theo dõi.
     * Tái sử dụng {@link AuctionDAO#findAuctionById(String)} để đảm bảo nạp đầy đủ thông tin Item liên kết.
     *
     * @param userId ID của người dùng.
     * @return Danh sách các phiên đấu giá {@link AuctionSession} đang theo dõi.
     */
    public List<AuctionSession> getWatchlist(String userId) {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT auction_id FROM watchlist WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String auctionId = rs.getString("auction_id");
                    Optional<AuctionSession> sessionOpt = auctionDAO.findAuctionById(auctionId);
                    sessionOpt.ifPresent(list::add);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy danh sách watchlist: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy danh sách các ID người dùng đang theo dõi một phiên đấu giá cụ thể.
     * Thường dùng để quét và gửi thông báo nhắc nhở khi phiên đấu giá sắp kết thúc.
     *
     * @param auctionId ID của phiên đấu giá.
     * @return Danh sách các ID người dùng.
     */
    public List<String> getWatchers(String auctionId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT user_id FROM watchlist WHERE auction_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy danh sách watchers: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}
