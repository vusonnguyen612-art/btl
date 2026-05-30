package test;
import Model.*;
import Exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử đơn vị cho {@link AuctionSession}.
 * <p>
 * Kiểm thử máy trạng thái (state machine) của một phiên đấu giá:
 * <ul>
 *   <li>Chuyển trạng thái: OPEN → RUNNING → FINISHED / CANCELED</li>
 *   <li>Đặt giá: hợp lệ, không hợp lệ (thấp hơn/bằng giá hiện tại, dưới mức tối thiểu)</li>
 *   <li>Đặt giá khi phiên chưa bắt đầu hoặc đã hết thời gian</li>
 *   <li>Lưu lịch sử bid</li>
 *   <li>Kết thúc phiên có/không có bid — xác định người thắng</li>
 *   <li>Gọi finish/cancel nhiều lần — không gây tác dụng phụ</li>
 *   <li>Xử lý thanh toán: đúng người, đúng số tiền</li>
 *   <li>Thời gian còn lại ({@link AuctionSession#getRemainingTimeMillis()})</li>
 * </ul>
 */
class AuctionSessionTest {

    private AuctionSession session;
    private Electronics item;
    private final String sellerId = "seller1";
    private final String bidderId = "bidder1";

    /**
     * Thiết lập trước mỗi bài kiểm thử.
     * Tạo vật phẩm điện tử mẫu và phiên đấu giá với giá khởi điểm 500.0, thời gian 60 phút.
     */
    @BeforeEach
    void setUp() {
        item = new Electronics("ELC0001", "Laptop", "Gaming laptop",
                500.0, sellerId, "Dell", 24, "XPS", "New");
        session = new AuctionSession("AUC0001", item, sellerId, 500.0, 60);
    }

    /**
     * Kiểm thử chuyển trạng thái từ OPEN sang RUNNING khi gọi {@code start()}.
     * Thời gian bắt đầu và kết thúc phải được thiết lập.
     */
    @Test
    void testStart_TransitionsToRunning() {
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());
        session.start();
        assertEquals(AuctionSession.Status.RUNNING, session.getStatus());
        assertNotNull(session.getStartTime());
        assertNotNull(session.getEndTime());
    }

    /**
     * Kiểm thử gọi {@code start()} hai lần — lần thứ hai không làm thay đổi trạng thái.
     */
    @Test
    void testStart_AlreadyRunning_NoEffect() {
        session.start();
        AuctionSession.Status statusAfterFirstStart = session.getStatus();
        session.start();
        assertEquals(statusAfterFirstStart, session.getStatus());
    }

    /**
     * Kiểm thử đặt giá hợp lệ làm tăng giá hiện tại của phiên.
     *
     * @throws Exception nếu bid không hợp lệ (không xảy ra trong test này)
     */
    @Test
    void testPlaceBid_IncreasesCurrentPrice() throws Exception {
        session.start();
        double newPrice = 600.0;
        session.placeBid(bidderId, newPrice);
        assertEquals(newPrice, session.getCurrentPrice(), 0.001);
    }

    /**
     * Kiểm thử đặt giá hợp lệ ghi nhận người đặt giá cao nhất.
     *
     * @throws Exception nếu bid không hợp lệ (không xảy ra trong test này)
     */
    @Test
    void testPlaceBid_SetsHighestBidderId() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        assertEquals(bidderId, session.getHighestBidderId());
    }

    /**
     * Kiểm thử đặt giá thấp hơn giá hiện tại — phải ném {@link InvalidBidException}.
     */
    @Test
    void testPlaceBid_BidLowerThanCurrent_Throws() {
        session.start();
        assertThrows(InvalidBidException.class, () -> session.placeBid(bidderId, 400.0));
    }

    /**
     * Kiểm thử đặt giá bằng giá hiện tại — phải ném {@link InvalidBidException}.
     */
    @Test
    void testPlaceBid_BidEqualToCurrent_Throws() {
        session.start();
        assertThrows(InvalidBidException.class, () -> session.placeBid(bidderId, 500.0));
    }

    /**
     * Kiểm thử đặt giá dưới mức tăng tối thiểu (min increment) so với bid hiện tại — phải ném {@link InvalidBidException}.
     *
     * @throws Exception nếu bid đầu tiên không hợp lệ (không xảy ra trong test này)
     */
    @Test
    void testPlaceBid_BidBelowMinIncrement_Throws() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        assertThrows(InvalidBidException.class, () -> session.placeBid("bidder2", 600.5));
    }

    /**
     * Kiểm thử đặt giá khi phiên chưa bắt đầu (trạng thái OPEN) — phải ném {@link AuctionClosedException}.
     */
    @Test
    void testPlaceBid_AuctionNotRunning_Throws() {
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());
        assertThrows(AuctionClosedException.class, () -> session.placeBid(bidderId, 600.0));
    }

    /**
     * Kiểm thử đặt giá sau thời gian kết thúc — phải ném {@link AuctionClosedException}.
     */
    @Test
    void testPlaceBid_AfterEndTime_Throws() {
        session.start();
        session.setEndTime(LocalDateTime.now().minusSeconds(1));
        assertThrows(AuctionClosedException.class, () -> session.placeBid(bidderId, 600.0));
    }

    /**
     * Kiểm thử đặt giá ghi lại lịch sử bid — lịch sử phải có 1 bản ghi với số tiền chính xác.
     *
     * @throws Exception nếu bid không hợp lệ (không xảy ra trong test này)
     */
    @Test
    void testPlaceBid_RecordsBidHistory() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        assertEquals(1, session.getBidHistory().size());
        assertEquals(600.0, session.getBidHistory().get(0).getAmount(), 0.001);
    }

    /**
     * Kiểm thử kết thúc phiên có bid: trạng thái chuyển sang FINISHED,
     * người thắng cuộc là người đặt giá cao nhất.
     *
     * @throws Exception nếu bid không hợp lệ (không xảy ra trong test này)
     */
    @Test
    void testFinish_TransitionsToFinished() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertEquals(AuctionSession.Status.FINISHED, session.getStatus());
        assertEquals(bidderId, session.getWinnerId());
    }

    /**
     * Kiểm thử kết thúc phiên không có bid nào — trạng thái FINISHED nhưng không có người thắng.
     */
    @Test
    void testFinish_NoBids_NoWinner() {
        session.start();
        session.finish();
        assertEquals(AuctionSession.Status.FINISHED, session.getStatus());
        assertNull(session.getWinnerId());
    }

    /**
     * Kiểm thử gọi {@code finish()} hai lần — lần thứ hai không làm thay đổi trạng thái.
     */
    @Test
    void testFinish_AlreadyFinished_NoEffect() {
        session.start();
        session.finish();
        session.finish();
        assertEquals(AuctionSession.Status.FINISHED, session.getStatus());
    }

    /**
     * Kiểm thử hủy phiên đang chạy — trạng thái chuyển sang CANCELED.
     */
    @Test
    void testCancel_TransitionsToCanceled() {
        session.start();
        session.cancel("Test cancellation");
        assertEquals(AuctionSession.Status.CANCELED, session.getStatus());
    }

    /**
     * Kiểm thử hủy phiên ở trạng thái OPEN (chưa bắt đầu) — vẫn chuyển sang CANCELED.
     */
    @Test
    void testCancel_OpenAuction() {
        session.cancel("Cancel before start");
        assertEquals(AuctionSession.Status.CANCELED, session.getStatus());
    }

    /**
     * Kiểm thử xử lý thanh toán thành công:
     * đúng người thắng, đúng số tiền, trạng thái chuyển sang PAID.
     *
     * @throws Exception nếu bid hoặc finish gặp lỗi (không xảy ra trong test này)
     */
    @Test
    void testProcessPayment_CorrectAmount_Success() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertTrue(session.processPayment(bidderId, 600.0));
        assertEquals(AuctionSession.Status.PAID, session.getStatus());
    }

    /**
     * Kiểm thử thanh toán với sai người dùng — trả về {@code false}.
     *
     * @throws Exception nếu bid hoặc finish gặp lỗi (không xảy ra trong test này)
     */
    @Test
    void testProcessPayment_WrongWinner_Fails() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertFalse(session.processPayment("wrongUser", 600.0));
    }

    /**
     * Kiểm thử thanh toán với sai số tiền — trả về {@code false}.
     *
     * @throws Exception nếu bid hoặc finish gặp lỗi (không xảy ra trong test này)
     */
    @Test
    void testProcessPayment_WrongAmount_Fails() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertFalse(session.processPayment(bidderId, 500.0));
    }

    /**
     * Kiểm thử {@link AuctionSession#getRemainingTimeMillis()} khi phiên đang chạy —
     * thời gian còn lại phải lớn hơn 0.
     */
    @Test
    void testGetRemainingTimeMillis_Running() {
        session.start();
        assertTrue(session.getRemainingTimeMillis() > 0);
    }

    /**
     * Kiểm thử {@link AuctionSession#getRemainingTimeMillis()} khi phiên chưa bắt đầu —
     * trả về tổng thời gian phiên (60 phút = 60 * 60 * 1000 ms).
     */
    @Test
    void testGetRemainingTimeMillis_NotStarted() {
        long remaining = session.getRemainingTimeMillis();
        assertEquals(60 * 60 * 1000, remaining);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔴 NEGATIVE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử hủy phiên đã FINISHED — không được throw exception (idempotent hoặc no-op).
     */
    @Test
    void testCancel_AlreadyFinished_DoesNotThrow() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertDoesNotThrow(() -> session.cancel("Cancel after finish"));
    }

    /**
     * Kiểm thử hủy phiên đã CANCELED — gọi cancel 2 lần, lần thứ hai không gây lỗi.
     */
    @Test
    void testCancel_AlreadyCanceled_NoEffect() {
        session.cancel("First cancel");
        AuctionSession.Status statusAfterFirst = session.getStatus();
        session.cancel("Second cancel");
        assertEquals(statusAfterFirst, session.getStatus());
    }

    /**
     * Kiểm thử thanh toán cho phiên đã bị hủy — trả về {@code false}.
     */
    @Test
    void testProcessPayment_CanceledAuction_Fails() {
        session.cancel("Cancel reason");
        assertFalse(session.processPayment(bidderId, 600.0));
        assertEquals(AuctionSession.Status.CANCELED, session.getStatus());
    }

    /**
     * Kiểm thử thanh toán 2 lần (double payment) — lần thứ hai phải trả về {@code false}
     * vì trạng thái đã là PAID.
     */
    @Test
    void testProcessPayment_DoublePayment_Fails() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertTrue(session.processPayment(bidderId, 600.0));
        assertEquals(AuctionSession.Status.PAID, session.getStatus());

        // Thanh toán lần 2 — phải thất bại
        assertFalse(session.processPayment(bidderId, 600.0));
    }

    /**
     * Kiểm thử đặt giá với số tiền âm — phải ném {@link InvalidBidException}.
     */
    @Test
    void testPlaceBid_NegativeAmount_Throws() {
        session.start();
        assertThrows(InvalidBidException.class, () -> session.placeBid(bidderId, -50.0));
    }

    /**
     * Kiểm thử đặt giá với số tiền bằng 0 — phải ném {@link InvalidBidException}.
     */
    @Test
    void testPlaceBid_ZeroAmount_Throws() {
        session.start();
        assertThrows(InvalidBidException.class, () -> session.placeBid(bidderId, 0.0));
    }
}
