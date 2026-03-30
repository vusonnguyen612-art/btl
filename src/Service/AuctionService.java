package Service;
import Model.*;

public class AuctionService {
    private Auction auction;

    public AuctionService(Auction auction) {
        this.auction = auction;
    }

    public boolean placeBid(String user, int amount) {
        if (!auction.isOpen()) {
            System.out.println("Auction đã đóng!");
            return false;
        }

        if (amount <= auction.getHighestBid()) {
            System.out.println("Bid phải cao hơn!");
            return false;
        }
        boolean success = auction.placeBid(user, amount);

        if (success) {
            System.out.println( user + " bid " + amount);
        }

        return success;
    }
    public void createAuction(Item item) {
        this.auction = new Auction(item);
        System.out.println("Tạo auction: " + item.getName());
    }
    public void endAuction() {
        auction.close();
        System.out.println("Winner: " + auction.getHighestBidder());
    }
}
