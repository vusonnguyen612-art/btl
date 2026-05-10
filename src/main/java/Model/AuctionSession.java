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

/** Quản lý một phiên đấu giá: trạng thái, giá, lịch sử đặt giá, observer, auto-close timer. */
public class AuctionSession implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Các trạng thái của phiên đấu giá. */
    public enum Status {
        OPEN,
        RUNNING,
        PAYMENT_PENDING,
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

    /** @param id              mã phiên
     *  @param item            vật phẩm đấu giá
     *  @param sellerId        ID người bán
     *  @param startPrice      giá khởi điểm
     *  @param durationMinutes thời lượng (phút) */
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

    /** Bắt đầu phiên đấu giá: chuyển trạng thái RUNNING, đặt startTime/endTime, lên lịch tự động kết thúc. */
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

    /**
     * Đặt giá cho phiên. Chỉ hiệu lực khi phiên RUNNING và chưa hết giờ.
     *
     * @param bidderId ID người đặt giá
     * @param amount   số tiền muốn đặt
     * @throws AuctionClosedException nếu phiên không ở trạng thái RUNNING
     * @throws InvalidBidException    nếu amount <= currentPrice hoặc không đủ minIncrement
     */
    public synchronized void placeBid(String bidderId, double amount) 
            throws AuctionClosedException, InvalidBidException {
        if (status != Status.RUNNING) {
            throw new AuctionClosedException("Auction is not running", id);
        }

        if (LocalDateTime.now().isAfter(endTime)) {
            finish();
            throw new AuctionClosedException("Auction time has expired", id);
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

    /** Kết thúc phiên: chuyển FINISHED, ghi nhận winnerId, dọn dẹp scheduler, thông báo observer. */
    public synchronized void finish() {
        if (status == Status.PAYMENT_PENDING || status == Status.FINISHED || status == Status.PAID || status == Status.CANCELED) {
            return;
        }

        status = Status.FINISHED;
        
        if (highestBidderId != null) {
            winnerId = highestBidderId;
        }
        
        cleanup();
        notifyAuctionFinished(winnerId, currentPrice);
    }

    /** Hủy phiên với lý do cụ thể. */
    public synchronized void cancel(String reason) {
        if (status == Status.PAID || status == Status.CANCELED) {
            return;
        }

        status = Status.CANCELED;
        
        cleanup();
        notifyAuctionCanceled(reason);
    }

    /** Xử lý thanh toán: kiểm tra winnerId và amount, chuyển trạng thái PAID. */
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /** @return số milliseconds còn lại trước khi phiên kết thúc */
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

    public boolean isPaymentPending() {
        return status == Status.PAYMENT_PENDING;
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
