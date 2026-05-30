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

/**
 * Bộ kiểm thử đơn vị cho {@link AuctionManager}.
 * <p>
 * Kiểm thử toàn bộ vòng đời quản lý đấu giá thông qua lớp Service singleton:
 * <ul>
 *   <li>Singleton pattern ({@link #testGetInstance_Singleton()}, {@link #testResetInstance()})</li>
 *   <li>Quản lý vật phẩm (thêm, sửa, xóa, truy vấn)</li>
 *   <li>Quản lý người dùng & xác thực</li>
 *   <li>Vòng đời phiên đấu giá (tạo, bắt đầu, đặt giá, kết thúc, hủy)</li>
 *   <li>Xử lý thanh toán sau khi đấu giá kết thúc</li>
 *   <li>Observer pattern — thông báo toàn cục khi có bid</li>
 *   <li>Xử lý ngoại lệ cho các thao tác không hợp lệ</li>
 * </ul>
 */
class AuctionManagerTest {

    private AuctionManager manager;
    private Electronics testItem;
    private User testUser;
    private User testUser2;

    /**
     * Thiết lập trước mỗi bài kiểm thử.
     * Reset singleton {@link AuctionManager}, tạo vật phẩm điện tử và hai người dùng mẫu.
     */
    @BeforeEach
    void setUp() {
        AuctionManager.resetInstance();
        manager = AuctionManager.getInstance();

        testItem = new Electronics("ELC_TEST_1", "Laptop", "Gaming laptop",
                500.0, "seller1", "Dell", 24, "XPS", "New");
        testUser = new RegularUser("USR001", "john", "pass123");
        testUser2 = new RegularUser("USR002", "jane", "pass456");
    }

    /**
     * Kiểm thử tính singleton: hai lần gọi {@code getInstance()} phải trả về cùng một đối tượng.
     */
    @Test
    void testGetInstance_Singleton() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();
        assertSame(instance1, instance2);
    }

    /**
     * Kiểm thử {@code resetInstance()}: sau khi reset, instance mới phải khác instance cũ.
     */
    @Test
    void testResetInstance() {
        AuctionManager.resetInstance();
        AuctionManager freshManager = AuctionManager.getInstance();
        assertNotSame(manager, freshManager);
    }

    /**
     * Kiểm thử thêm vật phẩm và truy vấn theo ID.
     * Vật phẩm sau khi thêm phải lấy ra được và có tên chính xác.
     */
    @Test
    void testAddAndGetItem() {
        manager.addItem(testItem);
        Item retrieved = manager.getItem("ELC_TEST_1");
        assertNotNull(retrieved);
        assertEquals("Laptop", retrieved.getName());
    }

    /**
     * Kiểm thử {@code getAllItems()} trả về đúng số lượng vật phẩm đã thêm.
     */
    @Test
    void testGetAllItems() {
        manager.addItem(testItem);
        Electronics item2 = new Electronics("ELC_TEST_2", "Phone", "Smartphone",
                999.0, "seller1", "Apple", 12, "iPhone", "New");
        manager.addItem(item2);

        List<Item> allItems = manager.getAllItems();
        assertEquals(2, allItems.size());
    }

    /**
     * Kiểm thử cập nhật thông tin vật phẩm: tên, mô tả, giá khởi điểm đều phải thay đổi.
     */
    @Test
    void testUpdateItem() {
        manager.addItem(testItem);
        assertTrue(manager.updateItem("ELC_TEST_1", "Updated Laptop", "Better laptop", 600.0));
        Item updated = manager.getItem("ELC_TEST_1");
        assertEquals("Updated Laptop", updated.getName());
        assertEquals("Better laptop", updated.getDescription());
        assertEquals(600.0, updated.getStartPrice(), 0.001);
    }

    /**
     * Kiểm thử cập nhật vật phẩm không tồn tại — phải trả về {@code false}.
     */
    @Test
    void testUpdateItem_NonExistent_ReturnsFalse() {
        assertFalse(manager.updateItem("NONEXISTENT", "Name", "Desc", 100.0));
    }

    /**
     * Kiểm thử xóa vật phẩm thành công và xác nhận không còn tồn tại.
     */
    @Test
    void testDeleteItem() {
        manager.addItem(testItem);
        assertTrue(manager.deleteItem("ELC_TEST_1"));
        assertNull(manager.getItem("ELC_TEST_1"));
    }

    /**
     * Kiểm thử xóa vật phẩm không tồn tại — phải trả về {@code false}.
     */
    @Test
    void testDeleteItem_NonExistent_ReturnsFalse() {
        assertFalse(manager.deleteItem("NONEXISTENT"));
    }

    /**
     * Kiểm thử thêm người dùng và truy vấn theo ID.
     */
    @Test
    void testAddAndGetUser() {
        manager.addUser(testUser);
        User retrieved = manager.getUser("USR001");
        assertNotNull(retrieved);
        assertEquals("john", retrieved.getUsername());
    }

    /**
     * Kiểm thử xác thực đăng nhập thành công với đúng mật khẩu.
     *
     * @throws AuthenticationException nếu xác thực thất bại (không xảy ra trong test này)
     */
    @Test
    void testAuthenticate_Success() throws AuthenticationException {
        manager.addUser(testUser);
        User authenticated = manager.authenticate("john", "pass123");
        assertNotNull(authenticated);
        assertEquals("john", authenticated.getUsername());
    }

    /**
     * Kiểm thử xác thực với sai mật khẩu — phải ném {@link AuthenticationException}.
     */
    @Test
    void testAuthenticate_WrongPassword_Throws() {
        manager.addUser(testUser);
        assertThrows(AuthenticationException.class,
                () -> manager.authenticate("john", "wrongpass"));
    }

    /**
     * Kiểm thử xác thực với người dùng không tồn tại — phải ném {@link AuthenticationException}.
     */
    @Test
    void testAuthenticate_UserNotFound_Throws() {
        assertThrows(AuthenticationException.class,
                () -> manager.authenticate("unknown", "pass123"));
    }

    /**
     * Kiểm thử tạo phiên đấu giá thành công từ vật phẩm có sẵn.
     * Phiên phải ở trạng thái {@link AuctionSession.Status#OPEN}, giá khởi điểm khớp với vật phẩm.
     *
     * @throws ItemNotFoundException nếu vật phẩm không tồn tại (không xảy ra trong test này)
     */
    @Test
    void testCreateAuction_Success() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        assertNotNull(session);
        assertEquals(testItem, session.getItem());
        assertEquals(500.0, session.getCurrentPrice(), 0.001);
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());
    }

    /**
     * Kiểm thử tạo phiên đấu giá với vật phẩm không tồn tại — phải ném {@link ItemNotFoundException}.
     */
    @Test
    void testCreateAuction_ItemNotFound_Throws() {
        assertThrows(ItemNotFoundException.class,
                () -> manager.createAuction("NONEXISTENT", 60));
    }

    /**
     * Kiểm thử đặt giá hợp lệ: giá hiện tại và người đặt giá cao nhất phải được cập nhật.
     *
     * @throws Exception nếu có lỗi trong quy trình (không xảy ra trong test này)
     */
    @Test
    void testPlaceBid_Valid() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());

        manager.placeBid(session.getId(), "bidder1", 600.0);
        assertEquals(600.0, session.getCurrentPrice(), 0.001);
        assertEquals("bidder1", session.getHighestBidderId());
    }

    /**
     * Kiểm thử bắt đầu phiên đấu giá: trạng thái chuyển từ {@code OPEN} sang {@code RUNNING}.
     *
     * @throws ItemNotFoundException nếu vật phẩm không tồn tại (không xảy ra trong test này)
     */
    @Test
    void testStartAuction() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());

        manager.startAuction(session.getId());
        assertEquals(AuctionSession.Status.RUNNING, session.getStatus());
    }

    /**
     * Kiểm thử kết thúc phiên đấu giá: trạng thái chuyển sang {@code FINISHED},
     * người thắng cuộc là người đặt giá cao nhất.
     *
     * @throws Exception nếu có lỗi trong quy trình (không xảy ra trong test này)
     */
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

    /**
     * Kiểm thử hủy phiên đấu giá: trạng thái chuyển sang {@code CANCELED}.
     *
     * @throws ItemNotFoundException nếu vật phẩm không tồn tại (không xảy ra trong test này)
     */
    @Test
    void testCancelAuction() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.cancelAuction(session.getId(), "Cancel reason");
        assertEquals(AuctionSession.Status.CANCELED, session.getStatus());
    }

    /**
     * Kiểm thử xử lý thanh toán sau đấu giá:
     * thanh toán đúng số tiền và chuyển trạng thái sang {@code PAID}.
     *
     * @throws Exception nếu có lỗi trong quy trình (không xảy ra trong test này)
     */
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

    /**
     * Kiểm thử lọc phiên đấu giá theo trạng thái.
     * Tạo hai phiên, kết thúc một phiên — phiên FINISHED có 1, phiên RUNNING có 1.
     *
     * @throws ItemNotFoundException nếu vật phẩm không tồn tại (không xảy ra trong test này)
     */
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

    /**
     * Kiểm thử Observer toàn cục: khi đặt giá, observer nhận được thông báo {@code onBidPlaced}.
     *
     * @throws Exception nếu có lỗi trong quy trình (không xảy ra trong test này)
     */
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

    /**
     * Kiểm thử gỡ bỏ Observer toàn cục: sau khi remove, observer không còn nhận thông báo.
     *
     * @throws Exception nếu có lỗi trong quy trình (không xảy ra trong test này)
     */
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

    /**
     * Kiểm thử đặt giá vào phiên không tồn tại — phải ném {@link AuctionClosedException}.
     */
    @Test
    void testNonExistentAuction_Throws() {
        assertThrows(AuctionClosedException.class,
                () -> manager.placeBid("NONEXISTENT", "bidder1", 600.0));
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔴 NEGATIVE TESTS — State transitions không hợp lệ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử start một phiên đã FINISHED — hành vi không xác định, nhưng không được ném exception lạ.
     */
    @Test
    void testStartAuction_AlreadyFinished_DoesNotThrow() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());
        manager.placeBid(session.getId(), "bidder1", 600.0);
        manager.finishAuction(session.getId());

        // Start lại phiên đã kết thúc — không throw là chấp nhận được (no-op)
        assertDoesNotThrow(() -> manager.startAuction(session.getId()));
    }

    /**
     * Kiểm thử start một phiên đã CANCELED — không được throw exception.
     */
    @Test
    void testStartAuction_AlreadyCanceled_DoesNotThrow() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.cancelAuction(session.getId(), "Cancel reason");

        assertDoesNotThrow(() -> manager.startAuction(session.getId()));
    }

    /**
     * Kiểm thử hủy phiên đã FINISHED — không được throw exception.
     */
    @Test
    void testCancelAuction_AlreadyFinished_DoesNotThrow() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());
        manager.placeBid(session.getId(), "bidder1", 600.0);
        manager.finishAuction(session.getId());

        assertDoesNotThrow(() -> manager.cancelAuction(session.getId(), "Cancel after finish"));
    }

    /**
     * Kiểm thử finish phiên đã CANCELED — không được throw exception.
     */
    @Test
    void testFinishAuction_AlreadyCanceled_DoesNotThrow() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.cancelAuction(session.getId(), "Cancel reason");

        assertDoesNotThrow(() -> manager.finishAuction(session.getId()));
    }

    /**
     * Kiểm thử đặt giá sau khi phiên đã bị hủy — phải ném {@link AuctionClosedException}.
     */
    @Test
    void testPlaceBid_AfterAuctionCanceled_Throws() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.cancelAuction(session.getId(), "Cancel reason");

        assertThrows(AuctionClosedException.class,
                () -> manager.placeBid(session.getId(), "bidder1", 600.0));
    }

    /**
     * Kiểm thử đặt giá sau khi phiên đã kết thúc — phải ném {@link AuctionClosedException}.
     */
    @Test
    void testPlaceBid_AfterAuctionFinished_Throws() throws Exception {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());
        manager.placeBid(session.getId(), "bidder1", 600.0);
        manager.finishAuction(session.getId());

        assertThrows(AuctionClosedException.class,
                () -> manager.placeBid(session.getId(), "bidder2", 700.0));
    }

    /**
     * Kiểm thử thanh toán cho phiên đã bị hủy — phải trả về {@code false}.
     */
    @Test
    void testProcessPayment_CanceledAuction_Fails() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.cancelAuction(session.getId(), "Cancel reason");

        assertFalse(manager.processPayment(session.getId(), "bidder1", 600.0));
    }

    /**
     * Kiểm thử lấy vật phẩm với ID null — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testGetItem_NullId_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.getItem(null));
    }

    /**
     * Kiểm thử lấy người dùng với ID null — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testGetUser_NullId_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.getUser(null));
    }

    /**
     * Kiểm thử đặt giá với số tiền âm — phải ném {@link InvalidBidException}.
     */
    @Test
    void testPlaceBid_NegativeAmount_Throws() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());

        assertThrows(InvalidBidException.class,
                () -> manager.placeBid(session.getId(), "bidder1", -100.0));
    }

    /**
     * Kiểm thử đặt giá với số tiền bằng 0 — phải ném {@link InvalidBidException}.
     */
    @Test
    void testPlaceBid_ZeroAmount_Throws() throws ItemNotFoundException {
        manager.addItem(testItem);
        AuctionSession session = manager.createAuction("ELC_TEST_1", 60);
        manager.startAuction(session.getId());

        assertThrows(InvalidBidException.class,
                () -> manager.placeBid(session.getId(), "bidder1", 0.0));
    }
}
