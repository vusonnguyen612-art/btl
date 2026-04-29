package Exception;

public class InvalidBidException extends Exception {
    private final double bidAmount;
    private final double currentPrice;

    public InvalidBidException(String message, double bidAmount, double currentPrice) {
        super(message);
        this.bidAmount = bidAmount;
        this.currentPrice = currentPrice;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }
}
