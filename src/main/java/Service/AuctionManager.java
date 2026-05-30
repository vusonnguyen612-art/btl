package Service;

import Model.*;
import Exception.*;
import Observer.AuctionObserver;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Quản lý toàn bộ dữ liệu hệ thống (users, items, auctions) in-memory với ReadWriteLock. Singleton. */
public class AuctionManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static AuctionManager instance;
    
    private final Map<String, User> users;
    private final Map<String, Item> items;
    private final Map<String, AuctionSession> auctions;
    private transient List<AuctionObserver> globalObservers;
    private final ReentrantReadWriteLock lock;
    private int auctionCounter;
    private static final String DATA_FILE = "auction_data.ser";

    /**
     * Khởi tạo đối tượng {@code AuctionManager} với các cấu trúc dữ liệu rỗng.
     * <p>Được gọi từ {@link #getInstance()} lần đầu tiên hoặc {@link #resetInstance()}.</p>
     * <ul>
     *   <li>{@code users} — ConcurrentHashMap lưu trữ người dùng theo ID.</li>
     *   <li>{@code items} — ConcurrentHashMap lưu trữ vật phẩm theo ID.</li>
     *   <li>{@code auctions} — ConcurrentHashMap lưu trữ phiên đấu giá theo ID.</li>
     *   <li>{@code globalObservers} — danh sách observer toàn cục (CopyOnWriteArrayList).</li>
     *   <li>{@code lock} — ReadWriteLock đảm bảo đồng bộ đọc-ghi.</li>
     *   <li>{@code auctionCounter} — bộ đếm tạo mã phiên tự động tăng.</li>
     * </ul>
     */
    private AuctionManager() {
        this.users = new ConcurrentHashMap<>();
        this.items = new ConcurrentHashMap<>();
        this.auctions = new ConcurrentHashMap<>();
        this.globalObservers = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.auctionCounter = 0;
    }

    /** Lấy instance singleton (tạo mới nếu chưa có). */
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    /** Reset instance về trạng thái mới. */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance = new AuctionManager();
        }
    }

    /** Tạo phiên đấu giá mới từ itemId. */
    public AuctionSession createAuction(String itemId, long durationMinutes) 
            throws ItemNotFoundException {
        lock.writeLock().lock();
        try {
            Item item = items.get(itemId);
            if (item == null) {
                throw new ItemNotFoundException("Item not found: " + itemId, itemId);
            }

            String auctionId = "AUC" + String.format("%05d", ++auctionCounter);
            AuctionSession session = new AuctionSession(
                auctionId, 
                item, 
                item.getSellerId(),
                item.getStartPrice(),
                durationMinutes
            );

            auctions.put(auctionId, session);
            return session;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Đặt giá cho phiên và thông báo cho tất cả globalObservers. */
    public synchronized String placeBid(String auctionId, String bidderId, double amount)
            throws AuctionClosedException, InvalidBidException {
        lock.writeLock().lock();
        try {
            AuctionSession session = auctions.get(auctionId);
            if (session == null) {
                throw new AuctionClosedException("Auction not found: " + auctionId, auctionId);
            }
            
            session.placeBid(bidderId, amount);
            
            for (AuctionObserver observer : globalObservers) {
                observer.onBidPlaced(auctionId, bidderId, amount);
            }
            
            return String.format("Bid placed: %s bid %.2f", bidderId, amount);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Bắt đầu phiên đấu giá. */
    public void startAuction(String auctionId) {
        lock.writeLock().lock();
        try {
            AuctionSession session = auctions.get(auctionId);
            if (session != null && session.isOpen()) {
                session.start();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Kết thúc phiên đấu giá. */
    public void finishAuction(String auctionId) {
        lock.writeLock().lock();
        try {
            AuctionSession session = auctions.get(auctionId);
            if (session != null) {
                session.finish();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Hủy phiên đấu giá với lý do. */
    public void cancelAuction(String auctionId, String reason) {
        lock.writeLock().lock();
        try {
            AuctionSession session = auctions.get(auctionId);
            if (session != null) {
                session.cancel(reason);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Xử lý thanh toán cho phiên. */
    public boolean processPayment(String auctionId, String winnerId, double amount) {
        lock.writeLock().lock();
        try {
            AuctionSession session = auctions.get(auctionId);
            if (session != null) {
                return session.processPayment(winnerId, amount);
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Thêm vật phẩm vào danh sách. */
    public void addItem(Item item) {
        lock.writeLock().lock();
        try {
            items.put(item.getId(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Lấy vật phẩm theo ID. */
    public Item getItem(String itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        lock.readLock().lock();
        try {
            return items.get(itemId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Lấy tất cả vật phẩm. */
    public List<Item> getAllItems() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(items.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Cập nhật thông tin vật phẩm. */
    public boolean updateItem(String itemId, String name, String description, Double startPrice) {
        lock.writeLock().lock();
        try {
            Item item = items.get(itemId);
            if (item == null) return false;
            
            if (name != null) item.setName(name);
            if (description != null) item.setDescription(description);
            if (startPrice != null) item.setStartPrice(startPrice);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Xóa vật phẩm theo ID. */
    public boolean deleteItem(String itemId) {
        lock.writeLock().lock();
        try {
            return items.remove(itemId) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Lấy phiên đấu giá theo ID. */
    public AuctionSession getAuction(String auctionId) {
        lock.readLock().lock();
        try {
            return auctions.get(auctionId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Lấy tất cả phiên đấu giá. */
    public List<AuctionSession> getAllAuctions() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(auctions.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Lọc phiên theo trạng thái. */
    public List<AuctionSession> getAuctionsByStatus(AuctionSession.Status status) {
        lock.readLock().lock();
        try {
            List<AuctionSession> result = new ArrayList<>();
            for (AuctionSession session : auctions.values()) {
                if (session.getStatus() == status) {
                    result.add(session);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Thêm người dùng. */
    public void addUser(User user) {
        lock.writeLock().lock();
        try {
            users.put(user.getId(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Lấy người dùng theo ID. */
    public User getUser(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        lock.readLock().lock();
        try {
            return users.get(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Xác thực username/password. */
    public User authenticate(String username, String password) throws AuthenticationException {
        lock.readLock().lock();
        try {
            for (User user : users.values()) {
                if (user.getUsername().equals(username)) {
                    if (user.getPassword().equals(password)) {
                        return user;
                    }
                    throw new AuthenticationException("Invalid password", username);
                }
            }
            throw new AuthenticationException("User not found", username);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Đăng ký observer toàn cục. */
    public void addGlobalObserver(AuctionObserver observer) {
        globalObservers.add(observer);
    }

    /** Hủy đăng ký observer toàn cục. */
    public void removeGlobalObserver(AuctionObserver observer) {
        globalObservers.remove(observer);
    }

    /** Lưu toàn bộ dữ liệu ra file auction_data.ser. */
    public void saveData() throws IOException {
        lock.readLock().lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Tải dữ liệu từ file auction_data.ser. */
    public static AuctionManager loadData() throws IOException, ClassNotFoundException {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return getInstance();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            instance = (AuctionManager) ois.readObject();
            return instance;
        }
    }

    /**
     * Khôi phục đối tượng sau khi deserialize từ file {@code auction_data.ser}.
     * <p>Đảm bảo trường transient {@code globalObservers} được khởi tạo lại
     * (dùng {@link java.util.concurrent.CopyOnWriteArrayList}) nếu đang là {@code null},
     * vì observer không được serialize.</p>
     *
     * @param in luồng ObjectInputStream chứa dữ liệu đã serialize.
     * @throws IOException            nếu có lỗi đọc luồng.
     * @throws ClassNotFoundException nếu class của đối tượng không tìm thấy.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (globalObservers == null) {
            globalObservers = new CopyOnWriteArrayList<>();
        }
    }
}
