package Observer;

public interface AuctionObserver {
    void onBidPlaced(String auctionId, String bidderId, double amount);
    void onAuctionStarted(String auctionId);
    void onAuctionFinished(String auctionId, String winnerId, double finalPrice);
    void onAuctionCanceled(String auctionId, String reason);
    void onAuctionStatusChanged(String auctionId, String oldStatus, String newStatus);
}
