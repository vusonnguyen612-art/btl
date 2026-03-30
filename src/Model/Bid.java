package Model;

public class Bid {
    private String bidder;
    private int amount;
    private long time;

    public Bid(String bidder, int amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = System.currentTimeMillis();
    }

    public int getAmount() {
        return amount;
    }

    public String getBidder() {
        return bidder;
    }
}
