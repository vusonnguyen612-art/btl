package Model;

import java.io.Serializable;

public class AutoBid implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String auctionId;
    private double maxAmount;
    private double increment;

    public AutoBid(String userId, String auctionId, double maxAmount, double increment) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxAmount = maxAmount;
        this.increment = increment;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }
}
