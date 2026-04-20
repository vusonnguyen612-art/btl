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
    private final List<AuctionObserver> globalObservers;
    private final ReentrantReadWriteLock lock;
    private int auctionCounter;
    private static final String DATA_FILE = "auction_data.ser";

    private AuctionManager() {
        this.users = new ConcurrentHashMap<>();
        this.items = new ConcurrentHashMap<>();
        this.auctions = new ConcurrentHashMap<>();
        this.globalObservers = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.auctionCounter = 0;
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance = new AuctionManager();
        }
    }

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
        lock.writeLock().lock();
        try {
            items.put(item.getId(), item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Item getItem(String itemId) {
        lock.readLock().lock();
        try {
            return items.get(itemId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Item> getAllItems() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(items.values());
        } finally {
            lock.readLock().unlock();
        }
    }

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

    public boolean deleteItem(String itemId) {
        lock.writeLock().lock();
        try {
            return items.remove(itemId) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public AuctionSession getAuction(String auctionId) {
        lock.readLock().lock();
        try {
            return auctions.get(auctionId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<AuctionSession> getAllAuctions() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(auctions.values());
        } finally {
            lock.readLock().unlock();
        }
    }

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
        lock.writeLock().lock();
        try {
            users.put(user.getId(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User getUser(String userId) {
        lock.readLock().lock();
        try {
            return users.get(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

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

    public void addGlobalObserver(AuctionObserver observer) {
        globalObservers.add(observer);
    }

    public void removeGlobalObserver(AuctionObserver observer) {
        globalObservers.remove(observer);
    }

    public void saveData() throws IOException {
        lock.readLock().lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } finally {
            lock.readLock().unlock();
        }
    }

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
}
