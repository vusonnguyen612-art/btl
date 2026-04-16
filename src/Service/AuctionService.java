package Service;

import Model.*;

public class AuctionService {

    private Auction auction;
    private String SellerName;

    public void createAuction(String item, int price,String sellerName) {
        auction = new Auction(item, price);
    }

    public synchronized String placeBid(String user, int amount) {

        if (auction == null) {
            return "❌ Chưa có auction";
        }

        if (!auction.isOpen()) {
            return "❌ Auction đã đóng";
        }

        if (amount <= auction.getHighestBid()) {
            return "❌ Bid phải > " + auction.getHighestBid();
        }

        boolean ok = auction.placeBid(user, amount);

        if (ok) {
            return "🔥 " + user + " bid " + amount;
        }

        return "❌ Bid fail";
    }

    public String endAuction(String requesterName) {
        if (auction == null) return "Không có auction";
        if (requesterName.equals(this.SellerName))
        auction.close();

        return "🏁 Winner: " + auction.getHighestBidder() +
                " | Giá: " + auction.getHighestBid();
    }
}