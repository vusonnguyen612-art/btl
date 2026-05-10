package Model;

import java.io.Serializable;

/** Cấu hình tự động trả giá cho một người dùng trên một phiên đấu giá. */
public class AutoBid implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String auctionId;
    private double maxAmount;
    private double increment;

    /** @param userId    ID người dùng
     *  @param auctionId ID phiên đấu giá
     *  @param maxAmount số tiền tối đa có thể trả
     *  @param increment bước giá mỗi lần tự động trả */
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
