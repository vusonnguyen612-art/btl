package Model;

import java.util.*;

public class Auction {

    private Item item;
    private int highestBid;
    private String highestBidder;
    private boolean isOpen = true;

    private List<Bid> history;

    public Auction(Item item) {
        this.item = item;
        this.highestBid = item.getStartPrice();
        this.highestBidder = "None";
        this.history = new ArrayList<>();
    }

    public boolean placeBid(String user, int amount) {

        if (amount <= highestBid) {
            return false;
        }

        highestBid = amount;
        highestBidder = user;

        history.add(new Bid(user, amount));

        return true;
    }

    public int getHighestBid() {
        return highestBid;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public List<Bid> getHistory() {
        return history;
    }
    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        isOpen = false;
    }
}