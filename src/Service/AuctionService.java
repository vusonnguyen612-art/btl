package Service;

import Model.*;

public class AuctionService {

    private Auction auction;
    private String lastAutoBidResult = "";

    public void createAuction(String item, int price, String sellerName) {
        auction = new Auction(item, sellerName, price);
        AutoBid.clearAll();
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
            processAutoBids(user);
            return "🔥 " + user + " bid " + amount + lastAutoBidResult;
        }

        return "❌ Bid fail";
    }

    private void processAutoBids(String excludeBidder) {
        lastAutoBidResult = "";
        
        if (auction == null || !auction.isOpen()) return;

        int currentHighest = auction.getHighestBid();

        for (AutoBid autoBid : AutoBid.getAllActive()) {
            if (autoBid.getBidder().equals(excludeBidder)) continue;
            if (!autoBid.canBid(currentHighest)) continue;

            int autoAmount = autoBid.getNextBidAmount(currentHighest);
            
            if (autoAmount > autoBid.getMaxBid()) continue;

            auction.placeBid(autoBid.getBidder(), autoAmount);
            currentHighest = autoAmount;
            
            lastAutoBidResult += " | 🤖 " + autoBid.getBidder() + " auto bid " + autoAmount;
        }
    }

    public synchronized String registerAutoBid(String user, int maxBid, int step) {
        if (auction == null) {
            return "❌ Chưa có auction để đăng ký auto bid";
        }

        if (!auction.isOpen()) {
            return "❌ Auction đã đóng";
        }

        if (maxBid <= auction.getHighestBid()) {
            return "❌ Max bid phải lớn hơn giá hiện tại: " + auction.getHighestBid();
        }

        if (step <= 0) {
            return "❌ Bước nhảy phải > 0";
        }

        AutoBid.register(user, maxBid, step);
        return "✅ " + user + " đã đăng ký auto bid | Max: " + maxBid + " | Step: " + step;
    }

    public String cancelAutoBid(String user) {
        if (!AutoBid.hasAutoBid(user)) {
            return "❌ Bạn chưa đăng ký auto bid";
        }

        AutoBid.unregister(user);
        return "✅ " + user + " đã hủy auto bid";
    }

    public String endAuction(String requesterName) {
        if (auction == null) return "❌ Không có auction";

        if (!auction.getSeller().equals(requesterName)) {
            return "❌ Chỉ người bán mới có thể kết thúc auction";
        }

        auction.close();

        String result = "🏁 Winner: " + auction.getHighestBidder() +
                " | Giá: " + auction.getHighestBid();
        
        AutoBid.clearAll();

        return result;
    }
}