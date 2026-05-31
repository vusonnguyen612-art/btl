package Service;

import Model.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ItemService {
    private final Map<String, Item> items;
    private final ReentrantReadWriteLock lock;

    public ItemService(Map<String, Item> items, ReentrantReadWriteLock lock) {
        this.items = items;
        this.lock = lock;
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
}
