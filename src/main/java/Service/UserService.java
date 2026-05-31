package Service;

import Model.User;
import Exception.AuthenticationException;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserService {
    private final Map<String, User> users;
    private final ReentrantReadWriteLock lock;

    public UserService(Map<String, User> users, ReentrantReadWriteLock lock) {
        this.users = users;
        this.lock = lock;
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
}
