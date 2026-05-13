package test;
import Model.*;
import Exception.*;
import Service.AuctionManager;
import Observer.AuctionObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager manager;
    private Electronics testItem;
    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        AuctionManager.resetInstance();
        manager = AuctionManager.getInstance();

        testItem = new Electronics("ELC_TEST_1", "Laptop", "Gaming laptop",
                500.0, "seller1", "Dell", 24, "XPS", "New");
        testUser = new User("USR001", "john", "pass123");
        testUser2 = new User("USR002", "jane", "pass456");
    }

    @Test
    void testGetInstance_Singleton() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testResetInstance() {
        AuctionManager.resetInstance();
        AuctionManager freshManager = AuctionManager.getInstance();
        assertNotSame(manager, freshManager);
    }

    @Test
    void testAddAndGetItem() {
        manager.addItem(testItem);
        Item retrieved = manager.getItem("ELC_TEST_1");
        assertNotNull(retrieved);
        assertEquals("Laptop", retrieved.getName());
    }

    @Test
    void testGetAllItems() {
        manager.addItem(testItem);
        Electronics item2 = new Electronics("ELC_TEST_2", "Phone", "Smartphone",
                999.0, "seller1", "Apple", 12, "iPhone", "New");
        manager.addItem(item2);

        List<Item> allItems = manager.getAllItems();
        assertEquals(2, allItems.size());
    }

    @Test
    void testUpdateItem() {
        manager.addItem(testItem);
        assertTrue(manager.updateItem("ELC_TEST_1", "Updated Laptop", "Better laptop", 600.0));
        Item updated = manager.getItem("ELC_TEST_1");
        assertEquals("Updated Laptop", updated.getName());
        assertEquals("Better laptop", updated.getDescription());
        assertEquals(600.0, updated.getStartPrice(), 0.001);
    }

    @Test
    void testUpdateItem_NonExistent_ReturnsFalse() {
        assertFalse(manager.updateItem("NONEXISTENT", "Name", "Desc", 100.0));
    }

    @Test
    void testDeleteItem() {
        manager.addItem(testItem);
        assertTrue(manager.deleteItem("ELC_TEST_1"));
        assertNull(manager.getItem("ELC_TEST_1"));
    }

    @Test
    void testDeleteItem_NonExistent_ReturnsFalse() {
        assertFalse(manager.deleteItem("NONEXISTENT"));
    }

    @Test
    void testAddAndGetUser() {
        manager.addUser(testUser);
        User retrieved = manager.getUser("USR001");
        assertNotNull(retrieved);
        assertEquals("john", retrieved.getUsername());
    }

    @Test
    void testAuthenticate_Success() throws AuthenticationException {
        manager.addUser(testUser);
        User authenticated = manager.authenticate("john", "pass123");
        assertNotNull(authenticated);
        assertEquals("john", authenticated.getUsername());
    }

    @Test
    void testAuthenticate_WrongPassword_Throws() {
        manager.addUser(testUser);
        assertThrows(AuthenticationException.class,
                () -> manager.authenticate("john", "wrongpass"));
    }

    @Test
    void testAuthenticate_UserNotFound_Throws() {
        assertThrows(AuthenticationException.class,
                () -> manager.authenticate("unknown", "pass123"));
    }

    @Test
    void testCreateAuction_Success() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        assertNotNull(session);
        assertEquals(testItem, session.getItem());
        assertEquals(500.0, session.getCurrentPrice(), 0.001);
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());
    }

    @Test
    void testCreateAuction_ItemNotFound_Throws() {
        assertThrows(ItemNotFoundException.class,
                () -> manager.createAuction("NONEXISTENT", 60));
    }

    @Test
    void testPlaceBid_Valid() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());

        manager.placeBid(session.getId(), "bidder1", 600.0);
        assertEquals(600.0, session.getCurrentPrice(), 0.001);
        assertEquals("bidder1", session.getHighestBidderId());
    }

    @Test
    void testStartAuction() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());

        manager.startAuction(session.getId());
        assertEquals(AuctionSession.Status.RUNNING, session.getStatus());
    }

    @Test
    void testFinishAuction() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());
        manager.placeBid(session.getId(), "bidder1", 600.0);

        manager.finishAuction(session.getId());
        assertEquals(AuctionSession.Status.FINISHED, session.getStatus());
        assertEquals("bidder1", session.getWinnerId());
    }

    @Test
    void testCancelAuction() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.cancelAuction(session.getId(), "Cancel reason");
        assertEquals(AuctionSession.Status.CANCELED, session.getStatus());
    }

    @Test
    void testProcessPayment() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());
        manager.placeBid(session.getId(), "bidder1", 600.0);
        manager.finishAuction(session.getId());

        assertTrue(manager.processPayment(session.getId(), "bidder1", 600.0));
        assertEquals(AuctionSession.Status.PAID, session.getStatus());
    }

    @Test
    void testGetAuctionsByStatus() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session1 = manager.createAuction("ELC_TEST_1", 60);
        Item item2 = new Electronics("ELC_TEST_2", "Phone", "Smartphone",
                999.0, "seller2", "Apple", 12, "iPhone", "New");
        manager.addItem(item2);
        AuctionSession session2 = manager.createAuction("ELC_TEST_2", 30);

        manager.startAuction(session1.getId());
        manager.startAuction(session2.getId());
        manager.finishAuction(session1.getId());

        List<AuctionSession> finished = manager.getAuctionsByStatus(AuctionSession.Status.FINISHED);
        List<AuctionSession> running = manager.getAuctionsByStatus(AuctionSession.Status.RUNNING);

        assertEquals(1, finished.size());
        assertEquals(1, running.size());
    }

    @Test
    void testGlobalObserver_NotifiedOnBid() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());

        AtomicInteger callCount = new AtomicInteger(0);
        AuctionObserver observer = new AuctionObserver() {
            @Override public void onBidPlaced(String auctionId, String bidderId, double amount) {
                callCount.incrementAndGet();
            }
            @Override public void onAuctionStarted(String auctionId) {}
            @Override public void onAuctionFinished(String auctionId, String winnerId, double finalPrice) {}
            @Override public void onAuctionCanceled(String auctionId, String reason) {}
            @Override public void onAuctionStatusChanged(String auctionId, String oldStatus, String newStatus) {}
        };

        manager.addGlobalObserver(observer);
        manager.placeBid(session.getId(), "bidder1", 600.0);
        assertEquals(1, callCount.get());
    }

    @Test
    void testGlobalObserver_Removed() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());

        AtomicInteger callCount = new AtomicInteger(0);
        AuctionObserver observer = new AuctionObserver() {
            @Override public void onBidPlaced(String auctionId, String bidderId, double amount) {
                callCount.incrementAndGet();
            }
            @Override public void onAuctionStarted(String auctionId) {}
            @Override public void onAuctionFinished(String auctionId, String winnerId, double finalPrice) {}
            @Override public void onAuctionCanceled(String auctionId, String reason) {}
            @Override public void onAuctionStatusChanged(String auctionId, String oldStatus, String newStatus) {}
        };

        manager.addGlobalObserver(observer);
        manager.removeGlobalObserver(observer);
        manager.placeBid(session.getId(), "bidder1", 600.0);
        assertEquals(0, callCount.get());
    }

    @Test
    void testNonExistentAuction_Throws() {
        assertThrows(AuctionClosedException.class,
                () -> manager.placeBid("NONEXISTENT", "bidder1", 600.0));
    }
}
