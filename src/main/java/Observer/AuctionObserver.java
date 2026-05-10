package Observer;

/** Observer interface cho các sự kiện trong phiên đấu giá. */
public interface AuctionObserver {
    /** Được gọi khi có lượt đặt giá mới. */
    void onBidPlaced(String auctionId, String bidderId, double amount);
    /** Được gọi khi phiên đấu giá bắt đầu. */
    void onAuctionStarted(String auctionId);
    /** Được gọi khi phiên đấu giá kết thúc. */
    void onAuctionFinished(String auctionId, String winnerId, double finalPrice);
    /** Được gọi khi phiên đấu giá bị hủy. */
    void onAuctionCanceled(String auctionId, String reason);
    /** Được gọi khi trạng thái phiên thay đổi. */
    void onAuctionStatusChanged(String auctionId, String oldStatus, String newStatus);
}
