package Model;

import Exception.AuctionClosedException;
import Exception.InvalidBidException;
import Observer.AuctionObserver;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AuctionSession implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        OPEN,
        RUNNING,
        FINISHED,
        PAID,
        CANCELED
    }

    private String id;
    private Item item;
    private String sellerId;
    private Status status;
    private double currentPrice;
    private double startPrice;
    private String highestBidderId;
    private String winnerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMinutes;
    private List<Bid> bidHistory;
    private transient List<AuctionObserver> observers;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> autoCloseTask;
    private double minIncrement;

    public AuctionSession(String id, Item item, String sellerId, double startPrice, long durationMinutes) {
        this.id = id;
        this.item = item;
        this.sellerId = sellerId;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.durationMinutes = durationMinutes;
        this.status = Status.OPEN;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.minIncrement = 1.0;
    }

    public synchronized void start() {
        if (status != Status.OPEN) {
            return;
        }
        this.startTime = LocalDateTime.now();
        this.endTime = startTime.plusMinutes(durationMinutes);
        this.status = Status.RUNNING;
        notifyAuctionStarted();
        scheduleAutoClose();
    }

    private void scheduleAutoClose() {
        long remainingMillis = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (remainingMillis > 0) {
            autoCloseTask = scheduler.schedule(this::finish, remainingMillis, TimeUnit.MILLISECONDS);
        } else {
            finish();
        }
    }

    public synchronized void placeBid(String bidderId, double amount)
            throws AuctionClosedException, InvalidBidException {
        if (status != Status.RUNNING) {
            throw new AuctionClosedException("Auction is not running", id);
        }

        if (LocalDateTime.now().isAfter(endTime)) {
            finish();
            throw new AuctionClosedException("Auction time has expired", id);
        }

        if (bidderId.equals(sellerId)) {
            throw new InvalidBidException(
                "Seller cannot bid on their own item",
                amount,
                currentPrice
            );
        }

        if (amount <= currentPrice) {
            throw new InvalidBidException(
                String.format("Bid must be higher than current price: %.2f", currentPrice),
                amount,
                currentPrice
            );
        }

        if (amount < currentPrice + minIncrement) {
            throw new InvalidBidException(
                String.format("Bid must be at least %.2f higher than current price", minIncrement),
                amount, 
                currentPrice
            );
        }

        currentPrice = amount;
        highestBidderId = bidderId;
        
        Bid bid = new Bid(id, bidderId, amount);
        bidHistory.add(bid);
        
        notifyBidPlaced(bidderId, amount);
    }

    public synchronized void finish() {
        if (status == Status.FINISHED || status == Status.PAID || status == Status.CANCELED) {
            return;
        }

        status = Status.FINISHED;
        
        if (highestBidderId != null) {
            winnerId = highestBidderId;
        }
        
        cleanup();
        notifyAuctionFinished(winnerId, currentPrice);
    }

    public synchronized void cancel(String reason) {
        if (status == Status.PAID || status == Status.CANCELED) {
            return;
        }

        status = Status.CANCELED;
        
        cleanup();
        notifyAuctionCanceled(reason);
    }

    public synchronized boolean processPayment(String winnerId, double amount) {
        if (!winnerId.equals(this.winnerId)) {
            return false;
        }
        if (Math.abs(amount - currentPrice) < 0.01) {
            status = Status.PAID;
            return true;
        }
        return false;
    }

    private void cleanup() {
        if (autoCloseTask != null && !autoCloseTask.isDone()) {
            autoCloseTask.cancel(false);
        }
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    private void notifyBidPlaced(String bidderId, double amount) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onBidPlaced(id, bidderId, amount);
            }
        }
    }

    private void notifyAuctionStarted() {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionStarted(id);
            }
        }
    }

    private void notifyAuctionFinished(String winnerId, double finalPrice) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionFinished(id, winnerId, finalPrice);
            }
        }
    }

    private void notifyAuctionCanceled(String reason) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionCanceled(id, reason);
            }
        }
    }

    public String getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public String getSellerId() {
        return sellerId;
    }

    public Status getStatus() {
        return status;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public List<Bid> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }

    public double getMinIncrement() {
        return minIncrement;
    }

    public void setMinIncrement(double minIncrement) {
        this.minIncrement = minIncrement;
    }

    public long getRemainingTimeMillis() {
        if (endTime == null) {
            return durationMinutes * 60 * 1000;
        }
        long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
        return Math.max(0, remaining);
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    public boolean isPaid() {
        return status == Status.PAID;
    }

    public boolean isCanceled() {
        return status == Status.CANCELED;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s - Current: $%.2f - %s", 
            id, item.getName(), status, currentPrice, 
            startTime != null ? startTime.toString() : "Not started");
    }
}
