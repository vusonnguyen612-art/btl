package Service;

import Model.*;
import Exception.*;
import Observer.AuctionObserver;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private transient UserService userService;
    private transient ItemService itemService;
    private transient DataService dataService;

    private AuctionManager() {
        this.users = new ConcurrentHashMap<>();
        this.items = new ConcurrentHashMap<>();
        this.auctions = new ConcurrentHashMap<>();
        this.globalObservers = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.auctionCounter = 0;
        initServices();
    }

    private void initServices() {
        this.userService = new UserService(users, lock);
        this.itemService = new ItemService(items, lock);
        this.dataService = new DataService();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public static synchronized void setInstance(AuctionManager manager) {
        instance = manager;
        if (instance != null) {
            instance.initServices();
        }
    }

    public static synchronized void resetInstance() {
        instance = new AuctionManager();
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

    public void addItem(Item item) {
        itemService.addItem(item);
    }

    public Item getItem(String itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return itemService.getItem(itemId);
    }

    public List<Item> getAllItems() {
        return itemService.getAllItems();
    }

    public boolean updateItem(String itemId, String name, String description, Double startPrice) {
        return itemService.updateItem(itemId, name, description, startPrice);
    }

    public boolean deleteItem(String itemId) {
        return itemService.deleteItem(itemId);
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

    public void addUser(User user) {
        userService.addUser(user);
    }

    public User getUser(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return userService.getUser(userId);
    }

    public User authenticate(String username, String password) throws AuthenticationException {
        return userService.authenticate(username, password);
    }

    /** Đăng ký observer toàn cục. */
    public void addGlobalObserver(AuctionObserver observer) {
        globalObservers.add(observer);
    }

    /** Hủy đăng ký observer toàn cục. */
    public void removeGlobalObserver(AuctionObserver observer) {
        globalObservers.remove(observer);
    }

    public void saveData() throws IOException {
        lock.readLock().lock();
        try {
            dataService.saveData(this);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static AuctionManager loadData() throws IOException, ClassNotFoundException {
        return new DataService().loadData();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (globalObservers == null) {
            globalObservers = new CopyOnWriteArrayList<>();
        }
        initServices();
    }
}
