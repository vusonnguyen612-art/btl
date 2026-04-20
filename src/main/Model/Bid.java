package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String auctionId;
    private String bidderId;
    private double amount;
    private LocalDateTime timestamp;

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

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
