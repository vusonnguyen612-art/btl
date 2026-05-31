package Model;

import Observer.AuctionObserver;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionObservable implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient List<AuctionObserver> observers;

    public AuctionObservable() {
        this.observers = new CopyOnWriteArrayList<>();
    }

    public void addObserver(AuctionObserver observer) {
        if (observers == null) {
            observers = new CopyOnWriteArrayList<>();
        }
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    public void notifyBidPlaced(String auctionId, String bidderId, double amount) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onBidPlaced(auctionId, bidderId, amount);
            }
        }
    }

    public void notifyAuctionStarted(String auctionId) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionStarted(auctionId);
            }
        }
    }

    public void notifyAuctionFinished(String auctionId, String winnerId, double finalPrice) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionFinished(auctionId, winnerId, finalPrice);
            }
        }
    }

    public void notifyAuctionCanceled(String auctionId, String reason) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionCanceled(auctionId, reason);
            }
        }
    }

    public boolean hasObservers() {
        return observers != null && !observers.isEmpty();
    }
}
