package Exception;

public class AuctionClosedException extends Exception {
    private final String auctionId;

    public AuctionClosedException(String message, String auctionId) {
        super(message);
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}
