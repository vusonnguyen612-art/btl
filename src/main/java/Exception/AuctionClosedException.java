package Exception;

/**
 * Ngoại lệ khi thao tác trên phiên đấu giá đã đóng hoặc không ở trạng thái cho phép.
 */
public class AuctionClosedException extends Exception {
    private final String auctionId;

    /**
     * @param message   thông báo lỗi
     * @param auctionId ID phiên đấu giá
     */
    public AuctionClosedException(String message, String auctionId) {
        super(message);
        this.auctionId = auctionId;
    }

    /** @return ID phiên đấu giá */
    public String getAuctionId() {
        return auctionId;
    }
}
