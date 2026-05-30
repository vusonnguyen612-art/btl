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

/**
 * Quản lý một phiên đấu giá với state machine: OPEN → RUNNING → FINISHED / CANCELED / PAID.
 * <p>
 * Các trạng thái:
 * <ul>
 *   <li>{@link Status#OPEN} — phiên mới tạo, chưa bắt đầu</li>
 *   <li>{@link Status#RUNNING} — đang diễn ra, có thể đặt giá</li>
 *   <li>{@link Status#PAYMENT_PENDING} — chờ xử lý thanh toán</li>
 *   <li>{@link Status#FINISHED} — đã kết thúc</li>
 *   <li>{@link Status#PAID} — đã thanh toán</li>
 *   <li>{@link Status#CANCELED} — đã hủy</li>
 * </ul>
 * Sử dụng {@link java.util.concurrent.ScheduledExecutorService} để tự động kết thúc phiên theo thời gian.
 * Áp dụng Observer pattern thông qua {@link Observer.AuctionObserver} để thông báo bid/status change.
 * </p>
 *
 * Các trường chính: id, item, sellerId, status, currentPrice, highestBidderId, winnerId,
 * startTime, endTime, durationMinutes, bidHistory, minIncrement.
 */
public class AuctionSession implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Các trạng thái của phiên đấu giá.
     * <ul>
     *   <li>{@link #OPEN} — phiên mới, chưa bắt đầu</li>
     *   <li>{@link #RUNNING} — đang đấu giá</li>
     *   <li>{@link #PAYMENT_PENDING} — chờ thanh toán</li>
     *   <li>{@link #FINISHED} — đã kết thúc</li>
     *   <li>{@link #PAID} — đã thanh toán</li>
     *   <li>{@link #CANCELED} — đã hủy</li>
     * </ul>
     */
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

    /**
     * Lên lịch tự động kết thúc phiên khi hết thời gian.
     * Sử dụng {@link ScheduledExecutorService} để chạy {@link #finish()} sau khoảng thời gian còn lại.
     */
    private void scheduleAutoClose() {
        long remainingMillis = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (remainingMillis > 0) {
            autoCloseTask = scheduler.schedule(this::finish, remainingMillis, TimeUnit.MILLISECONDS);
        } else {
            finish();
        }
    }

    /**
     * Đặt giá cho phiên đấu giá.
     * Chỉ hiệu lực khi phiên ở trạng thái {@link Status#RUNNING} và chưa hết thời gian.
     *
     * @param bidderId ID người đặt giá
     * @param amount   số tiền muốn đặt
     * @throws AuctionClosedException nếu phiên không ở trạng thái RUNNING hoặc đã hết giờ
     * @throws InvalidBidException    nếu amount &le; currentPrice hoặc không đủ mức tăng tối thiểu
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

    /**
     * Kết thúc phiên đấu giá.
     * Chuyển trạng thái sang {@link Status#FINISHED}, ghi nhận winnerId nếu có,
     * dọn dẹp scheduler và thông báo cho các observer.
     */
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

    /**
     * Hủy phiên đấu giá với lý do cụ thể.
     * Chuyển trạng thái sang {@link Status#CANCELED}, dọn dẹp scheduler và thông báo observer.
     *
     * @param reason lý do hủy phiên
     */
    public synchronized void cancel(String reason) {
        if (status == Status.PAID || status == Status.CANCELED) {
            return;
        }

        status = Status.CANCELED;
        
        cleanup();
        notifyAuctionCanceled(reason);
    }

    /**
     * Xử lý thanh toán cho phiên đấu giá.
     * Kiểm tra winnerId và số tiền, nếu hợp lệ thì chuyển trạng thái sang {@link Status#PAID}.
     *
     * @param winnerId ID người thắng cần xác thực
     * @param amount   số tiền thanh toán
     * @return true nếu thanh toán thành công, false nếu thất bại (sai winnerId, sai số tiền hoặc đã PAID)
     */
    public synchronized boolean processPayment(String winnerId, double amount) {
        if (status == Status.PAID) {
            return false; // Tránh double payment
        }
        if (!winnerId.equals(this.winnerId)) {
            return false;
        }
        if (Math.abs(amount - currentPrice) < 0.01) {
            status = Status.PAID;
            return true;
        }
        return false;
    }

    /**
     * Dọn dẹp tài nguyên: hủy autoCloseTask nếu đang chạy, shutdown scheduler.
     */
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

    /**
     * Đăng ký observer để nhận thông báo từ phiên đấu giá.
     *
     * @param observer observer cần đăng ký
     */
    public void addObserver(AuctionObserver observer) {
        if (observers == null) {
            observers = new CopyOnWriteArrayList<>();
        }
        observers.add(observer);
    }

    /**
     * Hủy đăng ký observer khỏi phiên đấu giá.
     *
     * @param observer observer cần hủy
     */
    public void removeObserver(AuctionObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    /**
     * Thông báo cho tất cả observer khi có bid mới được đặt.
     *
     * @param bidderId ID người đặt bid
     * @param amount   số tiền bid
     */
    private void notifyBidPlaced(String bidderId, double amount) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onBidPlaced(id, bidderId, amount);
            }
        }
    }

    /**
     * Thông báo cho tất cả observer khi phiên bắt đầu.
     */
    private void notifyAuctionStarted() {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionStarted(id);
            }
        }
    }

    /**
     * Thông báo cho tất cả observer khi phiên kết thúc.
     *
     * @param winnerId   ID người thắng (có thể null nếu không ai đặt giá)
     * @param finalPrice giá cuối cùng
     */
    private void notifyAuctionFinished(String winnerId, double finalPrice) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionFinished(id, winnerId, finalPrice);
            }
        }
    }

    /**
     * Thông báo cho tất cả observer khi phiên bị hủy.
     *
     * @param reason lý do hủy
     */
    private void notifyAuctionCanceled(String reason) {
        if (observers != null) {
            for (AuctionObserver observer : observers) {
                observer.onAuctionCanceled(id, reason);
            }
        }
    }

    /** @return mã phiên đấu giá */
    public String getId() {
        return id;
    }

    /** @return vật phẩm đang được đấu giá */
    public Item getItem() {
        return item;
    }

    /** @return ID người bán */
    public String getSellerId() {
        return sellerId;
    }

    /** @return trạng thái hiện tại của phiên */
    public Status getStatus() {
        return status;
    }

    /** @return giá hiện tại */
    public double getCurrentPrice() {
        return currentPrice;
    }

    /** @return giá khởi điểm */
    public double getStartPrice() {
        return startPrice;
    }

    /** @return ID người đặt giá cao nhất hiện tại */
    public String getHighestBidderId() {
        return highestBidderId;
    }

    /** @return ID người thắng cuộc (chỉ có sau khi phiên kết thúc) */
    public String getWinnerId() {
        return winnerId;
    }

    /** @return thời điểm bắt đầu phiên */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /** @return thời điểm kết thúc phiên */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /** @return thời lượng phiên (phút) */
    public long getDurationMinutes() {
        return durationMinutes;
    }

    /** @return danh sách lịch sử các bid đã đặt (bản sao) */
    public List<Bid> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }

    /** @return mức tăng tối thiểu giữa các bid */
    public double getMinIncrement() {
        return minIncrement;
    }

    /** @param minIncrement mức tăng tối thiểu mới */
    public void setMinIncrement(double minIncrement) {
        this.minIncrement = minIncrement;
    }

    /** @param status trạng thái mới cho phiên */
    public void setStatus(Status status) {
        this.status = status;
    }

    /** @param currentPrice giá hiện tại mới */
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    /** @param highestBidderId ID người đặt giá cao nhất mới */
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    /** @param winnerId ID người thắng cuộc mới */
    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    /** @param startTime thời điểm bắt đầu mới */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /** @param endTime thời điểm kết thúc mới */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Tính thời gian còn lại của phiên đấu giá.
     *
     * @return số milliseconds còn lại (luôn &ge; 0)
     */
    public long getRemainingTimeMillis() {
        if (endTime == null) {
            return durationMinutes * 60 * 1000;
        }
        long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
        return Math.max(0, remaining);
    }

    /** @return true nếu phiên đang ở trạng thái {@link Status#OPEN} */
    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** @return true nếu phiên đang ở trạng thái {@link Status#RUNNING} */
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    /** @return true nếu phiên đang ở trạng thái {@link Status#PAYMENT_PENDING} */
    public boolean isPaymentPending() {
        return status == Status.PAYMENT_PENDING;
    }

    /** @return true nếu phiên đang ở trạng thái {@link Status#FINISHED} */
    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    /** @return true nếu phiên đang ở trạng thái {@link Status#PAID} */
    public boolean isPaid() {
        return status == Status.PAID;
    }

    /** @return true nếu phiên đang ở trạng thái {@link Status#CANCELED} */
    public boolean isCanceled() {
        return status == Status.CANCELED;
    }

    @Override
    /**
     * Trả về chuỗi biểu diễn của phiên đấu giá.
     *
     * @return chuỗi định dạng "[id] name - status - Current: $currentPrice - startTime"
     */
    public String toString() {
        return String.format("[%s] %s - %s - Current: $%.2f - %s", 
            id, item.getName(), status, currentPrice, 
            startTime != null ? startTime.toString() : "Not started");
    }
}
