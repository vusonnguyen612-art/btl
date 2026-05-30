package DAO;

import Model.Bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng bids: truy vấn giá đặt cao nhất, đánh dấu người thắng,
 * và lấy danh sách giá đặt thắng của người dùng.
 */
public class BidDAO {

    /**
     * Lấy lượt đặt giá có số tiền cao nhất cho một phiên đấu giá.
     *
     * @param auctionId ID của phiên đấu giá cần tra cứu.
     * @return Đối tượng {@link Bid} có giá cao nhất, hoặc {@code null} nếu chưa có lượt đặt nào.
     */
    public Bid getHighestBid(String auctionId) {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC LIMIT 1";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Bid bid = new Bid(
                        rs.getString("auction_id"),
                        rs.getString("bidder_id"),
                        rs.getDouble("amount")
                );
                bid.setId(rs.getString("id"));
                return bid;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Đánh dấu một lượt đặt giá là người thắng cuộc (is_winner = 1).
     *
     * @param bidId ID của lượt đặt giá cần đánh dấu.
     */
    public void markAsWinner(String bidId) {
        String sql = "UPDATE bids SET is_winner = 1 WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy danh sách các lượt đặt giá thắng của một người dùng.
     *
     * @param userId ID của người dùng cần tra cứu.
     * @return Danh sách các đối tượng {@link Bid} có is_winner = 1.
     */
    public List<Bid> getWinningBidsByUserId(String userId) {
        List<Bid> winningBids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE bidder_id = ? AND Winner = 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Bid bid = new Bid(
                        rs.getString("auction_id"),
                        rs.getString("bidder_id"),
                        rs.getDouble("amount")
                );
                bid.setId(rs.getString("id"));
                bid.setItemName(rs.getString("item_name"));
                bid.setWinner(true);
                winningBids.add(bid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return winningBids;
    }
}
