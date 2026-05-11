package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Đại diện cho một lượt đặt giá trong phiên đấu giá. */
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

    /** @param auctionId ID phiên đấu giá
     *  @param bidderId  ID người đặt giá
     *  @param amount    số tiền đặt */
    public Bid(String auctionId, String bidderId, double amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    public String getItemName() {
        return itemName;
    }

    public boolean isWinner() { return Winner; }

    public void setWinner(boolean winner) { Winner = winner; }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
