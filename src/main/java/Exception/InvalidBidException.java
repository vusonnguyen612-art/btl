package Exception;

/**
 * Ngoại lệ khi giá đặt không hợp lệ (thấp hơn hoặc bằng giá hiện tại, hoặc không đủ bước giá tối thiểu).
 */
public class InvalidBidException extends Exception {
    private final double bidAmount;
    private final double currentPrice;

    /**
     * @param message      thông báo lỗi
     * @param bidAmount    số tiền đã đặt
     * @param currentPrice giá hiện tại của phiên
     */
    public InvalidBidException(String message, double bidAmount, double currentPrice) {
        super(message);
        this.bidAmount = bidAmount;
        this.currentPrice = currentPrice;
    }

    /** @return số tiền đã đặt */
    public double getBidAmount() {
        return bidAmount;
    }

    /** @return giá hiện tại của phiên */
    public double getCurrentPrice() {
        return currentPrice;
    }
}
