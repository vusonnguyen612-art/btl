package test;

import Exception.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void testAuctionClosedException() {
        AuctionClosedException e = new AuctionClosedException("Auction is closed", "AUC001");
        assertEquals("Auction is closed", e.getMessage());
        assertEquals("AUC001", e.getAuctionId());
    }

    @Test
    void testInvalidBidException() {
        InvalidBidException e = new InvalidBidException("Bid too low", 100.0, 200.0);
        assertEquals("Bid too low", e.getMessage());
        assertEquals(100.0, e.getBidAmount(), 0.001);
        assertEquals(200.0, e.getCurrentPrice(), 0.001);
    }

    @Test
    void testAuthenticationException() {
        AuthenticationException e = new AuthenticationException("Wrong password", "john");
        assertEquals("Wrong password", e.getMessage());
        assertEquals("john", e.getUsername());
    }

    @Test
    void testItemNotFoundException() {
        ItemNotFoundException e = new ItemNotFoundException("Item not found", "ELC001");
        assertEquals("Item not found", e.getMessage());
        assertEquals("ELC001", e.getItemId());
    }

    @Test
    void testUnauthorizedException() {
        UnauthorizedException e = new UnauthorizedException("Access denied", "USR001", "DELETE_ITEM");
        assertEquals("Access denied", e.getMessage());
        assertEquals("USR001", e.getUserId());
        assertEquals("DELETE_ITEM", e.getAction());
    }
}
