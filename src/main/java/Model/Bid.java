package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một lượt đặt giá (bid) trong phiên đấu giá.
 * Lưu thông tin: id, auctionId, bidderId, bidderUsername, amount, itemName, timestamp, Winner.
 */
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String auctionId;
    private String bidderId;
    private String bidderUsername;
    private double amount;
    private String itemName;
    private LocalDateTime timestamp;
    private boolean Winner;

    /**
     * Khởi tạo Bid với các thông tin cơ bản.
     *
     * @param auctionId ID phiên đấu giá
     * @param bidderId  ID người đặt giá
     * @param amount    số tiền đặt
     */
    public Bid(String auctionId, String bidderId, double amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    /** @return mã định danh của bid */
    public String getId() {
        return id;
    }

    /** @param id mã định danh mới */
    public void setId(String id) {
        this.id = id;
    }

    /** @return ID phiên đấu giá */
    public String getAuctionId() {
        return auctionId;
    }

    /** @return ID người đặt giá */
    public String getBidderId() {
        return bidderId;
    }

    /** @return tên đăng nhập người đặt giá */
    public String getBidderUsername() {
        return bidderUsername;
    }

    /** @param bidderUsername tên đăng nhập người đặt giá */
    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    /** @return tên vật phẩm */
    public String getItemName() {
        return itemName;
    }

    /** @return true nếu bid này là bid thắng cuộc */
    public boolean isWinner() { return Winner; }

    /** @param winner trạng thái thắng cuộc */
    public void setWinner(boolean winner) { Winner = winner; }

    /** @param itemName tên vật phẩm */
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    /** @return số tiền đặt */
    public double getAmount() {
        return amount;
    }

    /** @return thời điểm đặt giá */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** @param timestamp thời điểm đặt giá mới */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
