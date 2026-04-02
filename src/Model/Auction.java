package Model;

import java.util.*;

public class Auction {

    private String itemName;
    private int highestBid;
    private String highestBidder;
    private boolean open = true;
    private long endTime;

    private List<Bid> history = new ArrayList<>();

    public Auction(String itemName, int startPrice) {
        this.itemName = itemName;
        this.highestBid = startPrice;
        this.highestBidder = "None";
    }

    public synchronized boolean placeBid(String user, int amount) {

        if (!open) return false;

        if (amount <= highestBid) return false;

        highestBid = amount;
        highestBidder = user;

        history.add(new Bid(user, amount));

        return true;
    }

    public synchronized void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public int getHighestBid() {
        return highestBid;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public String getItemName() {
        return itemName;
    }
}