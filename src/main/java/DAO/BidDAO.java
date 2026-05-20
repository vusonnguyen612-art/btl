package DAO;

import Model.Bid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

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
