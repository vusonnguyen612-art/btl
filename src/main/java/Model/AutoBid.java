package Model;

import java.io.Serializable;

/**
 * Lớp cấu hình tự động trả giá (Auto Bid) cho một người dùng trên một phiên đấu giá.
 * Khi giá hiện tại thấp hơn maxAmount, hệ thống tự động đặt giá với bước increment.
 *
 * Các trường: userId, auctionId, maxAmount, increment, active.
 */
public class AutoBid implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String auctionId;
    private double maxAmount;
    private double increment;

    /**
     * Khởi tạo cấu hình AutoBid với các thông số.
     *
     * @param userId    ID người dùng
     * @param auctionId ID phiên đấu giá
     * @param maxAmount số tiền tối đa có thể trả
     * @param increment bước giá mỗi lần tự động trả
     */
    public AutoBid(String userId, String auctionId, double maxAmount, double increment) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxAmount = maxAmount;
        this.increment = increment;
    }

    /** @return ID người dùng */
    public String getUserId() {
        return userId;
    }

    /** @return ID phiên đấu giá */
    public String getAuctionId() {
        return auctionId;
    }

    /** @return số tiền tối đa có thể tự động trả */
    public double getMaxAmount() {
        return maxAmount;
    }

    /** @param maxAmount số tiền tối đa mới */
    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    /** @return bước giá mỗi lần tự động tăng */
    public double getIncrement() {
        return increment;
    }

    /** @param increment bước giá mới */
    public void setIncrement(double increment) {
        this.increment = increment;
    }
}
