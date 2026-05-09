package Model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public Bid(String auctionId, String bidderId, double amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isWinner() { return Winner; }

    public void setWinner(boolean winner) { Winner = winner; }

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

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setAmount(){this.amount=amount;}

    public double getAmount(){return amount;}
}