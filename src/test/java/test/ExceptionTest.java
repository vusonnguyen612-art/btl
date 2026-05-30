package test;

import Exception.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử đơn vị cho các lớp ngoại lệ tùy chỉnh (custom exception).
 * <p>
 * Xác nhận mỗi exception lưu trữ đúng các trường dữ liệu bổ sung:
 * <ul>
 *   <li>{@link AuctionClosedException} — auctionId</li>
 *   <li>{@link InvalidBidException} — bidAmount, currentPrice</li>
 *   <li>{@link AuthenticationException} — username</li>
 *   <li>{@link ItemNotFoundException} — itemId</li>
 *   <li>{@link UnauthorizedException} — userId, action</li>
 *   <li>{@link InsufficientBalanceException} — userId, balance, bidAmount</li>
 * </ul>
 */
class ExceptionTest {

    /**
     * Kiểm thử {@link AuctionClosedException}: lưu đúng message và auctionId.
     */
    @Test
    void testAuctionClosedException() {
        AuctionClosedException e = new AuctionClosedException("Auction is closed", "AUC001");
        assertEquals("Auction is closed", e.getMessage());
        assertEquals("AUC001", e.getAuctionId());
    }

    /**
     * Kiểm thử {@link InvalidBidException}: lưu đúng message, bidAmount và currentPrice.
     */
    @Test
    void testInvalidBidException() {
        InvalidBidException e = new InvalidBidException("Bid too low", 100.0, 200.0);
        assertEquals("Bid too low", e.getMessage());
        assertEquals(100.0, e.getBidAmount(), 0.001);
        assertEquals(200.0, e.getCurrentPrice(), 0.001);
    }

    /**
     * Kiểm thử {@link AuthenticationException}: lưu đúng message và username.
     */
    @Test
    void testAuthenticationException() {
        AuthenticationException e = new AuthenticationException("Wrong password", "john");
        assertEquals("Wrong password", e.getMessage());
        assertEquals("john", e.getUsername());
    }

    /**
     * Kiểm thử {@link ItemNotFoundException}: lưu đúng message và itemId.
     */
    @Test
    void testItemNotFoundException() {
        ItemNotFoundException e = new ItemNotFoundException("Item not found", "ELC001");
        assertEquals("Item not found", e.getMessage());
        assertEquals("ELC001", e.getItemId());
    }

    /**
     * Kiểm thử {@link UnauthorizedException}: lưu đúng message, userId và action.
     */
    @Test
    void testUnauthorizedException() {
        UnauthorizedException e = new UnauthorizedException("Access denied", "USR001", "DELETE_ITEM");
        assertEquals("Access denied", e.getMessage());
        assertEquals("USR001", e.getUserId());
        assertEquals("DELETE_ITEM", e.getAction());
    }

    /**
     * Kiểm thử {@link InsufficientBalanceException}: lưu đúng message, userId, balance và bidAmount.
     */
    @Test
    void testInsufficientBalanceException() {
        java.math.BigDecimal balance = java.math.BigDecimal.valueOf(150.0);
        java.math.BigDecimal bidAmount = java.math.BigDecimal.valueOf(200.0);
        InsufficientBalanceException e = new InsufficientBalanceException("Số dư không đủ", "USR001", balance, bidAmount);
        assertEquals("Số dư không đủ", e.getMessage());
        assertEquals("USR001", e.getUserId());
        assertEquals(balance, e.getBalance());
        assertEquals(bidAmount, e.getBidAmount());
    }
}
