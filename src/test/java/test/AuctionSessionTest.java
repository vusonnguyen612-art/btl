package test;
import Model.*;
import Exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionSessionTest {

    private AuctionSession session;
    private Electronics item;
    private final String sellerId = "seller1";
    private final String bidderId = "bidder1";

    @BeforeEach
    void setUp() {
        item = new Electronics("ELC0001", "Laptop", "Gaming laptop",
                500.0, sellerId, "Dell", 24, "XPS", "New");
        session = new AuctionSession("AUC0001", item, sellerId, 500.0, 60);
    }

    @Test
    void testStart_TransitionsToRunning() {
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());
        session.start();
        assertEquals(AuctionSession.Status.RUNNING, session.getStatus());
        assertNotNull(session.getStartTime());
        assertNotNull(session.getEndTime());
    }

    @Test
    void testStart_AlreadyRunning_NoEffect() {
        session.start();
        AuctionSession.Status statusAfterFirstStart = session.getStatus();
        session.start();
        assertEquals(statusAfterFirstStart, session.getStatus());
    }

    @Test
    void testPlaceBid_IncreasesCurrentPrice() throws Exception {
        session.start();
        double newPrice = 600.0;
        session.placeBid(bidderId, newPrice);
        assertEquals(newPrice, session.getCurrentPrice(), 0.001);
    }

    @Test
    void testPlaceBid_SetsHighestBidderId() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        assertEquals(bidderId, session.getHighestBidderId());
    }

    @Test
    void testPlaceBid_BidLowerThanCurrent_Throws() {
        session.start();
        assertThrows(InvalidBidException.class, () -> session.placeBid(bidderId, 400.0));
    }

    @Test
    void testPlaceBid_BidEqualToCurrent_Throws() {
        session.start();
        assertThrows(InvalidBidException.class, () -> session.placeBid(bidderId, 500.0));
    }

    @Test
    void testPlaceBid_BidBelowMinIncrement_Throws() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        assertThrows(InvalidBidException.class, () -> session.placeBid("bidder2", 600.5));
    }

    @Test
    void testPlaceBid_AuctionNotRunning_Throws() {
        assertEquals(AuctionSession.Status.OPEN, session.getStatus());
        assertThrows(AuctionClosedException.class, () -> session.placeBid(bidderId, 600.0));
    }

    @Test
    void testPlaceBid_AfterEndTime_Throws() {
        session.start();
        session.setEndTime(LocalDateTime.now().minusSeconds(1));
        assertThrows(AuctionClosedException.class, () -> session.placeBid(bidderId, 600.0));
    }

    @Test
    void testPlaceBid_RecordsBidHistory() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        assertEquals(1, session.getBidHistory().size());
        assertEquals(600.0, session.getBidHistory().get(0).getAmount(), 0.001);
    }

    @Test
    void testFinish_TransitionsToFinished() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertEquals(AuctionSession.Status.FINISHED, session.getStatus());
        assertEquals(bidderId, session.getWinnerId());
    }

    @Test
    void testFinish_NoBids_NoWinner() {
        session.start();
        session.finish();
        assertEquals(AuctionSession.Status.FINISHED, session.getStatus());
        assertNull(session.getWinnerId());
    }

    @Test
    void testFinish_AlreadyFinished_NoEffect() {
        session.start();
        session.finish();
        session.finish();
        assertEquals(AuctionSession.Status.FINISHED, session.getStatus());
    }

    @Test
    void testCancel_TransitionsToCanceled() {
        session.start();
        session.cancel("Test cancellation");
        assertEquals(AuctionSession.Status.CANCELED, session.getStatus());
    }

    @Test
    void testCancel_OpenAuction() {
        session.cancel("Cancel before start");
        assertEquals(AuctionSession.Status.CANCELED, session.getStatus());
    }

    @Test
    void testProcessPayment_CorrectAmount_Success() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertTrue(session.processPayment(bidderId, 600.0));
        assertEquals(AuctionSession.Status.PAID, session.getStatus());
    }

    @Test
    void testProcessPayment_WrongWinner_Fails() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertFalse(session.processPayment("wrongUser", 600.0));
    }

    @Test
    void testProcessPayment_WrongAmount_Fails() throws Exception {
        session.start();
        session.placeBid(bidderId, 600.0);
        session.finish();
        assertFalse(session.processPayment(bidderId, 500.0));
    }

    @Test
    void testGetRemainingTimeMillis_Running() {
        session.start();
        assertTrue(session.getRemainingTimeMillis() > 0);
    }

    @Test
    void testGetRemainingTimeMillis_NotStarted() {
        long remaining = session.getRemainingTimeMillis();
        assertEquals(60 * 60 * 1000, remaining);
    }
}
