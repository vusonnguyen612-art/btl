package test;

import Model.*;
import Factory.UserFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử bao phủ toàn bộ các lớp Model chưa được kiểm thử hoặc có độ phủ thấp.
 *
 * <p>Danh sách lớp được kiểm thử:
 * <ul>
 *   <li>{@link SearchCriteria} — 0% coverage: constructor, getters/setters cho 7 trường</li>
 *   <li>{@link AutoBid} — 0% coverage: constructor, getters/setters cho userId, auctionId, maxAmount, increment</li>
 *   <li>{@link Seller} — 0% coverage: constructor, getSpecificInfo, getRole, isAdmin/isBidder/isSeller, storeName</li>
 *   <li>{@link Bidder} — 0% coverage: constructor, getSpecificInfo, getRole, isAdmin/isBidder/isSeller, biddingBalance, addBalance</li>
 *   <li>{@link Bid} — 30.4% coverage: constructor, getters/setters cho tất cả các trường (id, auctionId, bidderId, bidderUsername, amount, itemName, timestamp, winner)</li>
 *   <li>{@link ChatMessage} — 42.3% coverage: constructor, getters/setters cho id, auctionId, senderId, senderName, message, timestamp</li>
 *   <li>{@link Entity} — 42.9% coverage: constructor, getId/setId, getCreatedAt, toString, equals, hashCode, getSpecificInfo</li>
 *   <li>{@link Admin} — 60% coverage: getAdminLevel/setAdminLevel, constructor, getRole, isAdmin/isBidder/isSeller, getSpecificInfo</li>
 *   <li>{@link UserFactory} — 71.4% coverage: kiểm thử createUser và createAdmin với các edge case</li>
 *   <li>{@link RegularUser} — 85.7% coverage: constructor, getRole, getSpecificInfo, isAdmin/isBidder/isSeller</li>
 * </ul>
 *
 * @author test
 * @version 1.0
 */
@DisplayName("ModelCoverageTest - Bao phủ toàn bộ Model classes")
class ModelCoverageTest {

    // ═══════════════════════════════════════════════════════════════
    // 1. SearchCriteria Tests (0/15 lines, 0% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SearchCriteria - Kiểm thử constructor và getter/setter")
    class SearchCriteriaTests {

        @Test
        @DisplayName("Default constructor: tất cả các trường đều null")
        void testDefaultConstructor_AllFieldsNull() {
            SearchCriteria criteria = new SearchCriteria();
            assertNull(criteria.getKeyword());
            assertNull(criteria.getCategory());
            assertNull(criteria.getStatuses());
            assertNull(criteria.getMinPrice());
            assertNull(criteria.getMaxPrice());
            assertNull(criteria.getSellerId());
            assertNull(criteria.getSortBy());
        }

        @Test
        @DisplayName("setKeyword và getKeyword hoạt động chính xác")
        void testKeyword_GetSet() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setKeyword("iPhone");
            assertEquals("iPhone", criteria.getKeyword());

            criteria.setKeyword(null);
            assertNull(criteria.getKeyword());

            criteria.setKeyword("");
            assertEquals("", criteria.getKeyword());
        }

        @Test
        @DisplayName("setCategory và getCategory hoạt động chính xác")
        void testCategory_GetSet() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setCategory("Electronics");
            assertEquals("Electronics", criteria.getCategory());

            criteria.setCategory(null);
            assertNull(criteria.getCategory());
        }

        @Test
        @DisplayName("setStatuses và getStatuses với List<AuctionSession.Status>")
        void testStatuses_GetSet() {
            SearchCriteria criteria = new SearchCriteria();
            List<AuctionSession.Status> statuses = Arrays.asList(
                    AuctionSession.Status.OPEN,
                    AuctionSession.Status.RUNNING
            );
            criteria.setStatuses(statuses);
            assertEquals(2, criteria.getStatuses().size());
            assertTrue(criteria.getStatuses().contains(AuctionSession.Status.OPEN));
            assertTrue(criteria.getStatuses().contains(AuctionSession.Status.RUNNING));

            // Empty list
            criteria.setStatuses(Collections.emptyList());
            assertTrue(criteria.getStatuses().isEmpty());

            // Null list
            criteria.setStatuses(null);
            assertNull(criteria.getStatuses());
        }

        @Test
        @DisplayName("setMinPrice và getMinPrice hoạt động chính xác")
        void testMinPrice_GetSet() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setMinPrice(100.0);
            assertEquals(100.0, criteria.getMinPrice(), 0.001);

            criteria.setMinPrice(0.0);
            assertEquals(0.0, criteria.getMinPrice(), 0.001);

            criteria.setMinPrice(null);
            assertNull(criteria.getMinPrice());
        }

        @Test
        @DisplayName("setMaxPrice và getMaxPrice hoạt động chính xác")
        void testMaxPrice_GetSet() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setMaxPrice(1000.0);
            assertEquals(1000.0, criteria.getMaxPrice(), 0.001);

            criteria.setMaxPrice(null);
            assertNull(criteria.getMaxPrice());
        }

        @Test
        @DisplayName("setSellerId và getSellerId hoạt động chính xác")
        void testSellerId_GetSet() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setSellerId("SELLER001");
            assertEquals("SELLER001", criteria.getSellerId());

            criteria.setSellerId(null);
            assertNull(criteria.getSellerId());
        }

        @Test
        @DisplayName("setSortBy và getSortBy hoạt động chính xác")
        void testSortBy_GetSet() {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setSortBy("price");
            assertEquals("price", criteria.getSortBy());

            criteria.setSortBy("date");
            assertEquals("date", criteria.getSortBy());
        }

        @Test
        @DisplayName("Tất cả các trường được set đồng thời và giữ nguyên giá trị")
        void testAllFieldsSetTogether() {
            SearchCriteria criteria = new SearchCriteria();
            List<AuctionSession.Status> statuses = Arrays.asList(
                    AuctionSession.Status.OPEN,
                    AuctionSession.Status.FINISHED,
                    AuctionSession.Status.PAID
            );

            criteria.setKeyword("laptop");
            criteria.setCategory("Computers");
            criteria.setStatuses(statuses);
            criteria.setMinPrice(500.0);
            criteria.setMaxPrice(2000.0);
            criteria.setSellerId("SELLER042");
            criteria.setSortBy("price_desc");

            assertEquals("laptop", criteria.getKeyword());
            assertEquals("Computers", criteria.getCategory());
            assertEquals(3, criteria.getStatuses().size());
            assertEquals(500.0, criteria.getMinPrice(), 0.001);
            assertEquals(2000.0, criteria.getMaxPrice(), 0.001);
            assertEquals("SELLER042", criteria.getSellerId());
            assertEquals("price_desc", criteria.getSortBy());
        }

        @Test
        @DisplayName("Statuses bao gồm tất cả các enum value của AuctionSession.Status")
        void testStatuses_AllEnumValues() {
            SearchCriteria criteria = new SearchCriteria();
            List<AuctionSession.Status> allStatuses = Arrays.asList(AuctionSession.Status.values());
            criteria.setStatuses(allStatuses);
            assertEquals(AuctionSession.Status.values().length, criteria.getStatuses().size());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. AutoBid Tests (0/14 lines, 0% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AutoBid - Kiểm thử constructor và getter/setter")
    class AutoBidTests {

        @Test
        @DisplayName("Constructor khởi tạo đúng tất cả các trường")
        void testConstructor_SetsAllFields() {
            AutoBid autoBid = new AutoBid("user1", "auction1", 1000.0, 50.0);
            assertEquals("user1", autoBid.getUserId());
            assertEquals("auction1", autoBid.getAuctionId());
            assertEquals(1000.0, autoBid.getMaxAmount(), 0.001);
            assertEquals(50.0, autoBid.getIncrement(), 0.001);
        }

        @Test
        @DisplayName("getUserId trả về đúng ID người dùng")
        void testGetUserId() {
            AutoBid autoBid = new AutoBid("bidder99", "auc42", 500.0, 10.0);
            assertEquals("bidder99", autoBid.getUserId());
        }

        @Test
        @DisplayName("getAuctionId trả về đúng ID phiên đấu giá")
        void testGetAuctionId() {
            AutoBid autoBid = new AutoBid("user1", "auctionX", 500.0, 10.0);
            assertEquals("auctionX", autoBid.getAuctionId());
        }

        @Test
        @DisplayName("getMaxAmount và setMaxAmount hoạt động chính xác")
        void testMaxAmount_GetSet() {
            AutoBid autoBid = new AutoBid("u1", "a1", 100.0, 5.0);
            assertEquals(100.0, autoBid.getMaxAmount(), 0.001);

            autoBid.setMaxAmount(250.75);
            assertEquals(250.75, autoBid.getMaxAmount(), 0.001);

            autoBid.setMaxAmount(0.0);
            assertEquals(0.0, autoBid.getMaxAmount(), 0.001);

            autoBid.setMaxAmount(-1.0);
            assertEquals(-1.0, autoBid.getMaxAmount(), 0.001);
        }

        @Test
        @DisplayName("getIncrement và setIncrement hoạt động chính xác")
        void testIncrement_GetSet() {
            AutoBid autoBid = new AutoBid("u1", "a1", 100.0, 5.0);
            assertEquals(5.0, autoBid.getIncrement(), 0.001);

            autoBid.setIncrement(25.5);
            assertEquals(25.5, autoBid.getIncrement(), 0.001);

            autoBid.setIncrement(0.0);
            assertEquals(0.0, autoBid.getIncrement(), 0.001);
        }

        @Test
        @DisplayName("Constructor với giá trị 0 và số âm")
        void testConstructor_ZeroAndNegativeValues() {
            AutoBid autoBid = new AutoBid("u", "a", 0.0, 0.0);
            assertEquals(0.0, autoBid.getMaxAmount(), 0.001);
            assertEquals(0.0, autoBid.getIncrement(), 0.001);
        }

        @Test
        @DisplayName("Constructor với userId và auctionId null")
        void testConstructor_NullIds() {
            AutoBid autoBid = new AutoBid(null, null, 100.0, 10.0);
            assertNull(autoBid.getUserId());
            assertNull(autoBid.getAuctionId());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. Seller Tests (0/10 lines, 0% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Seller - Kiểm thử constructor, getters, role methods")
    class SellerTests {

        @Test
        @DisplayName("Constructor khởi tạo Seller với id, username, password")
        void testConstructor() {
            Seller seller = new Seller("S001", "seller1", "pass123");
            assertEquals("S001", seller.getId());
            assertEquals("seller1", seller.getUsername());
            assertEquals("pass123", seller.getPassword());
        }

        @Test
        @DisplayName("getSpecificInfo trả về thông tin đặc thù của Seller")
        void testGetSpecificInfo() {
            Seller seller = new Seller("S001", "seller1", "pass123");
            assertEquals("Seller - can list items for auction", seller.getSpecificInfo());
        }

        @Test
        @DisplayName("getRole trả về \"SELLER\"")
        void testGetRole() {
            Seller seller = new Seller("S001", "seller1", "pass123");
            assertEquals("SELLER", seller.getRole());
        }

        @Test
        @DisplayName("isAdmin=false, isBidder=false, isSeller=true")
        void testIsMethods() {
            Seller seller = new Seller("S001", "seller1", "pass123");
            assertFalse(seller.isAdmin());
            assertFalse(seller.isBidder());
            assertTrue(seller.isSeller());
        }

        @Test
        @DisplayName("getStoreName ban đầu là null")
        void testGetStoreName_NullInitially() {
            Seller seller = new Seller("S001", "seller1", "pass123");
            assertNull(seller.getStoreName());
        }

        @Test
        @DisplayName("setStoreName và getStoreName hoạt động chính xác")
        void testStoreName_GetSet() {
            Seller seller = new Seller("S001", "seller1", "pass123");
            seller.setStoreName("Awesome Store");
            assertEquals("Awesome Store", seller.getStoreName());

            seller.setStoreName(null);
            assertNull(seller.getStoreName());

            seller.setStoreName("");
            assertEquals("", seller.getStoreName());
        }

        @Test
        @DisplayName("Seller kế thừa các phương thức từ User")
        void testInheritedMethods() {
            Seller seller = new Seller("S001", "seller1", "pass123");
            // Balance từ User
            assertEquals(BigDecimal.ZERO, seller.getBalance());
            seller.setBalance(new BigDecimal("500.00"));
            assertEquals(new BigDecimal("500.00"), seller.getBalance());

            // Email từ User
            assertNull(seller.getEmail());
            seller.setEmail("seller@example.com");
            assertEquals("seller@example.com", seller.getEmail());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. Bidder Tests (0/14 lines, 0% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Bidder - Kiểm thử constructor, getters, role methods, balance")
    class BidderTests {

        @Test
        @DisplayName("Constructor khởi tạo Bidder với id, username, password")
        void testConstructor() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            assertEquals("B001", bidder.getId());
            assertEquals("bidder1", bidder.getUsername());
            assertEquals("pass123", bidder.getPassword());
        }

        @Test
        @DisplayName("getSpecificInfo trả về thông tin đặc thù của Bidder")
        void testGetSpecificInfo() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            assertEquals("Bidder - can participate in auctions", bidder.getSpecificInfo());
        }

        @Test
        @DisplayName("getRole trả về \"BIDDER\"")
        void testGetRole() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            assertEquals("BIDDER", bidder.getRole());
        }

        @Test
        @DisplayName("isAdmin=false, isBidder=true, isSeller=false")
        void testIsMethods() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            assertFalse(bidder.isAdmin());
            assertTrue(bidder.isBidder());
            assertFalse(bidder.isSeller());
        }

        @Test
        @DisplayName("getBiddingBalance ban đầu là null")
        void testGetBiddingBalance_NullInitially() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            assertNull(bidder.getBiddingBalance());
        }

        @Test
        @DisplayName("setBiddingBalance và getBiddingBalance hoạt động chính xác")
        void testBiddingBalance_GetSet() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            bidder.setBiddingBalance(new BigDecimal("1000.00"));
            assertEquals(new BigDecimal("1000.00"), bidder.getBiddingBalance());

            bidder.setBiddingBalance(BigDecimal.ZERO);
            assertEquals(BigDecimal.ZERO, bidder.getBiddingBalance());

            bidder.setBiddingBalance(null);
            assertNull(bidder.getBiddingBalance());
        }

        @Test
        @DisplayName("addBalance khi biddingBalance đang null: khởi tạo về ZERO rồi cộng")
        void testAddBalance_WhenNull() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            assertNull(bidder.getBiddingBalance());

            bidder.addBalance(new BigDecimal("500.00"));
            assertEquals(new BigDecimal("500.00"), bidder.getBiddingBalance());
        }

        @Test
        @DisplayName("addBalance khi biddingBalance đã có giá trị: cộng dồn")
        void testAddBalance_WhenNotNull() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            bidder.setBiddingBalance(new BigDecimal("100.00"));

            bidder.addBalance(new BigDecimal("50.00"));
            assertEquals(new BigDecimal("150.00"), bidder.getBiddingBalance());

            bidder.addBalance(new BigDecimal("25.50"));
            assertEquals(new BigDecimal("175.50"), bidder.getBiddingBalance());
        }

        @Test
        @DisplayName("addBalance với số âm: trừ tiền khỏi balance")
        void testAddBalance_Negative() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            bidder.setBiddingBalance(new BigDecimal("200.00"));

            bidder.addBalance(new BigDecimal("-50.00"));
            assertEquals(new BigDecimal("150.00"), bidder.getBiddingBalance());
        }

        @Test
        @DisplayName("addBalance nhiều lần từ null: kiểm tra cộng dồn chính xác")
        void testAddBalance_MultipleFromNull() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            bidder.addBalance(new BigDecimal("100.00"));
            bidder.addBalance(new BigDecimal("200.00"));
            bidder.addBalance(new BigDecimal("300.00"));
            assertEquals(new BigDecimal("600.00"), bidder.getBiddingBalance());
        }

        @Test
        @DisplayName("Bidder kế thừa các phương thức từ User")
        void testInheritedMethods() {
            Bidder bidder = new Bidder("B001", "bidder1", "pass123");
            assertEquals(BigDecimal.ZERO, bidder.getBalance());
            bidder.setBalance(new BigDecimal("300.00"));
            assertEquals(new BigDecimal("300.00"), bidder.getBalance());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. Bid Tests (7/23 lines, 30.4% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Bid - Kiểm thử constructor, getters/setters toàn bộ các trường")
    class BidTests {

        @Test
        @DisplayName("Constructor khởi tạo auctionId, bidderId, amount và timestamp")
        void testConstructor_SetsFields() {
            Bid bid = new Bid("auction1", "bidder1", 500.0);
            assertEquals("auction1", bid.getAuctionId());
            assertEquals("bidder1", bid.getBidderId());
            assertEquals(500.0, bid.getAmount(), 0.001);
            assertNotNull(bid.getTimestamp());
        }

        @Test
        @DisplayName("getId ban đầu null, setId/getId hoạt động")
        void testId_GetSet() {
            Bid bid = new Bid("a1", "b1", 100.0);
            assertNull(bid.getId());

            bid.setId("BID001");
            assertEquals("BID001", bid.getId());

            bid.setId(null);
            assertNull(bid.getId());
        }

        @Test
        @DisplayName("getAuctionId trả về giá trị từ constructor")
        void testGetAuctionId() {
            Bid bid = new Bid("aucX", "bidderY", 250.0);
            assertEquals("aucX", bid.getAuctionId());
        }

        @Test
        @DisplayName("getBidderId trả về giá trị từ constructor")
        void testGetBidderId() {
            Bid bid = new Bid("aucX", "bidderY", 250.0);
            assertEquals("bidderY", bid.getBidderId());
        }

        @Test
        @DisplayName("getBidderUsername ban đầu null, setBidderUsername/getBidderUsername")
        void testBidderUsername_GetSet() {
            Bid bid = new Bid("a1", "b1", 100.0);
            assertNull(bid.getBidderUsername());

            bid.setBidderUsername("john_doe");
            assertEquals("john_doe", bid.getBidderUsername());

            bid.setBidderUsername(null);
            assertNull(bid.getBidderUsername());
        }

        @Test
        @DisplayName("getItemName ban đầu null, setItemName/getItemName")
        void testItemName_GetSet() {
            Bid bid = new Bid("a1", "b1", 100.0);
            assertNull(bid.getItemName());

            bid.setItemName("Vintage Watch");
            assertEquals("Vintage Watch", bid.getItemName());

            bid.setItemName(null);
            assertNull(bid.getItemName());
        }

        @Test
        @DisplayName("isWinner ban đầu false, setWinner/isWinner")
        void testWinner_GetSet() {
            Bid bid = new Bid("a1", "b1", 100.0);
            assertFalse(bid.isWinner());

            bid.setWinner(true);
            assertTrue(bid.isWinner());

            bid.setWinner(false);
            assertFalse(bid.isWinner());
        }

        @Test
        @DisplayName("getAmount trả về giá trị từ constructor")
        void testGetAmount() {
            Bid bid = new Bid("a1", "b1", 999.99);
            assertEquals(999.99, bid.getAmount(), 0.001);
        }

        @Test
        @DisplayName("getTimestamp trả về thời điểm tạo, setTimestamp cập nhật")
        void testTimestamp_GetSet() {
            Bid bid = new Bid("a1", "b1", 100.0);
            LocalDateTime original = bid.getTimestamp();
            assertNotNull(original);

            LocalDateTime newTime = LocalDateTime.of(2025, 1, 15, 10, 30, 0);
            bid.setTimestamp(newTime);
            assertEquals(newTime, bid.getTimestamp());
            assertNotEquals(original, bid.getTimestamp());
        }

        @Test
        @DisplayName("Toàn bộ các trường được set và đọc chính xác")
        void testAllFieldsTogether() {
            Bid bid = new Bid("auction42", "bidder7", 1500.0);
            bid.setId("BID999");
            bid.setBidderUsername("alice_wonder");
            bid.setItemName("Antique Vase");
            bid.setWinner(true);
            LocalDateTime timestamp = LocalDateTime.of(2025, 6, 1, 12, 0, 0);
            bid.setTimestamp(timestamp);

            assertEquals("BID999", bid.getId());
            assertEquals("auction42", bid.getAuctionId());
            assertEquals("bidder7", bid.getBidderId());
            assertEquals("alice_wonder", bid.getBidderUsername());
            assertEquals("Antique Vase", bid.getItemName());
            assertEquals(1500.0, bid.getAmount(), 0.001);
            assertTrue(bid.isWinner());
            assertEquals(timestamp, bid.getTimestamp());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. ChatMessage Tests (11/26 lines, 42.3% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ChatMessage - Kiểm thử constructor, getters/setters toàn bộ các trường")
    class ChatMessageTests {

        private final Timestamp sampleTimestamp = new Timestamp(System.currentTimeMillis());

        @Test
        @DisplayName("Constructor khởi tạo đúng tất cả các trường")
        void testConstructor_SetsAllFields() {
            ChatMessage msg = new ChatMessage("msg1", "auc1", "sender1", "Alice", "Hello!", sampleTimestamp);
            assertEquals("msg1", msg.getId());
            assertEquals("auc1", msg.getAuctionId());
            assertEquals("sender1", msg.getSenderId());
            assertEquals("Alice", msg.getSenderName());
            assertEquals("Hello!", msg.getMessage());
            assertEquals(sampleTimestamp, msg.getTimestamp());
        }

        @Test
        @DisplayName("getId và setId hoạt động chính xác")
        void testId_GetSet() {
            ChatMessage msg = new ChatMessage("msg1", "auc1", "s1", "Alice", "Hi", sampleTimestamp);
            assertEquals("msg1", msg.getId());

            msg.setId("msgNew");
            assertEquals("msgNew", msg.getId());

            msg.setId(null);
            assertNull(msg.getId());
        }

        @Test
        @DisplayName("getAuctionId và setAuctionId hoạt động chính xác")
        void testAuctionId_GetSet() {
            ChatMessage msg = new ChatMessage("msg1", "auc1", "s1", "Alice", "Hi", sampleTimestamp);
            assertEquals("auc1", msg.getAuctionId());

            msg.setAuctionId("aucNew");
            assertEquals("aucNew", msg.getAuctionId());

            msg.setAuctionId(null);
            assertNull(msg.getAuctionId());
        }

        @Test
        @DisplayName("getSenderId và setSenderId hoạt động chính xác")
        void testSenderId_GetSet() {
            ChatMessage msg = new ChatMessage("msg1", "auc1", "sender1", "Alice", "Hi", sampleTimestamp);
            assertEquals("sender1", msg.getSenderId());

            msg.setSenderId("senderNew");
            assertEquals("senderNew", msg.getSenderId());

            msg.setSenderId(null);
            assertNull(msg.getSenderId());
        }

        @Test
        @DisplayName("getSenderName và setSenderName hoạt động chính xác")
        void testSenderName_GetSet() {
            ChatMessage msg = new ChatMessage("msg1", "auc1", "s1", "Alice", "Hi", sampleTimestamp);
            assertEquals("Alice", msg.getSenderName());

            msg.setSenderName("Bob");
            assertEquals("Bob", msg.getSenderName());

            msg.setSenderName(null);
            assertNull(msg.getSenderName());

            msg.setSenderName("");
            assertEquals("", msg.getSenderName());
        }

        @Test
        @DisplayName("getMessage và setMessage hoạt động chính xác")
        void testMessage_GetSet() {
            ChatMessage msg = new ChatMessage("msg1", "auc1", "s1", "Alice", "Hello!", sampleTimestamp);
            assertEquals("Hello!", msg.getMessage());

            msg.setMessage("Goodbye!");
            assertEquals("Goodbye!", msg.getMessage());

            msg.setMessage(null);
            assertNull(msg.getMessage());

            msg.setMessage("");
            assertEquals("", msg.getMessage());
        }

        @Test
        @DisplayName("getTimestamp và setTimestamp hoạt động chính xác")
        void testTimestamp_GetSet() {
            ChatMessage msg = new ChatMessage("msg1", "auc1", "s1", "Alice", "Hi", sampleTimestamp);
            assertEquals(sampleTimestamp, msg.getTimestamp());

            Timestamp newTs = new Timestamp(System.currentTimeMillis() + 10000);
            msg.setTimestamp(newTs);
            assertEquals(newTs, msg.getTimestamp());

            msg.setTimestamp(null);
            assertNull(msg.getTimestamp());
        }

        @Test
        @DisplayName("Constructor với các giá trị null")
        void testConstructor_NullValues() {
            ChatMessage msg = new ChatMessage(null, null, null, null, null, null);
            assertNull(msg.getId());
            assertNull(msg.getAuctionId());
            assertNull(msg.getSenderId());
            assertNull(msg.getSenderName());
            assertNull(msg.getMessage());
            assertNull(msg.getTimestamp());
        }

        @Test
        @DisplayName("Toàn bộ getter/setter được gọi liên tiếp")
        void testAllSettersInSequence() {
            ChatMessage msg = new ChatMessage("id1", "auc1", "send1", "Alice", "msg", sampleTimestamp);

            msg.setId("id2");
            msg.setAuctionId("auc2");
            msg.setSenderId("send2");
            msg.setSenderName("Bob");
            msg.setMessage("updated message");
            Timestamp newTs = new Timestamp(System.currentTimeMillis() + 5000);
            msg.setTimestamp(newTs);

            assertEquals("id2", msg.getId());
            assertEquals("auc2", msg.getAuctionId());
            assertEquals("send2", msg.getSenderId());
            assertEquals("Bob", msg.getSenderName());
            assertEquals("updated message", msg.getMessage());
            assertEquals(newTs, msg.getTimestamp());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. Entity Tests (6/14 lines, 42.9% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Entity - Kiểm thử lớp abstract qua anonymous subclass")
    class EntityTests {

        /**
         * Lớp con ẩn danh để kiểm thử Entity abstract.
         */
        private Entity createEntity(String id) {
            return new Entity(id) {
                @Override
                public String getSpecificInfo() {
                    return "Test entity: " + getId();
                }
            };
        }

        @Test
        @DisplayName("Constructor khởi tạo id và createdAt không null")
        void testConstructor_SetsIdAndCreatedAt() {
            Entity entity = createEntity("E001");
            assertEquals("E001", entity.getId());
            assertNotNull(entity.getCreatedAt());
        }

        @Test
        @DisplayName("getId trả về id đã khởi tạo")
        void testGetId() {
            Entity entity = createEntity("entity-42");
            assertEquals("entity-42", entity.getId());
        }

        @Test
        @DisplayName("setId cập nhật id thành công")
        void testSetId() {
            Entity entity = createEntity("E001");
            entity.setId("E002");
            assertEquals("E002", entity.getId());

            entity.setId(null);
            assertNull(entity.getId());
        }

        @Test
        @DisplayName("getCreatedAt trả về thời điểm tạo")
        void testGetCreatedAt() {
            Entity entity = createEntity("E001");
            LocalDateTime createdAt = entity.getCreatedAt();
            assertNotNull(createdAt);
            // Thời điểm tạo phải gần với hiện tại
            LocalDateTime now = LocalDateTime.now();
            assertTrue(createdAt.isBefore(now.plusSeconds(1)));
            assertTrue(createdAt.isAfter(now.minusSeconds(5)));
        }

        @Test
        @DisplayName("toString trả về định dạng [ClassName] id")
        void testToString() {
            Entity entity = createEntity("test123");
            String str = entity.toString();
            assertTrue(str.contains("test123"));
            assertTrue(str.startsWith("["));
            assertTrue(str.contains("]"));
        }

        @Test
        @DisplayName("equals: cùng object -> true")
        void testEquals_SameObject() {
            Entity entity = createEntity("E001");
            assertTrue(entity.equals(entity));
        }

        @Test
        @DisplayName("equals: cùng id -> true")
        void testEquals_SameId() {
            Entity entity1 = createEntity("sameId");
            Entity entity2 = createEntity("sameId");
            assertEquals(entity1, entity2);
        }

        @Test
        @DisplayName("equals: khác id -> false")
        void testEquals_DifferentId() {
            Entity entity1 = createEntity("id1");
            Entity entity2 = createEntity("id2");
            assertNotEquals(entity1, entity2);
        }

        @Test
        @DisplayName("equals: null -> false")
        void testEquals_Null() {
            Entity entity = createEntity("E001");
            assertNotEquals(null, entity);
        }

        @Test
        @DisplayName("equals: khác class -> false")
        void testEquals_DifferentClass() {
            Entity entity = createEntity("E001");
            assertNotEquals("someString", entity);
        }

        @Test
        @DisplayName("hashCode: cùng id -> cùng hashCode")
        void testHashCode_SameId() {
            Entity entity1 = createEntity("sameId");
            Entity entity2 = createEntity("sameId");
            assertEquals(entity1.hashCode(), entity2.hashCode());
        }

        @Test
        @DisplayName("hashCode: khác id -> khác hashCode (thường)")
        void testHashCode_DifferentId() {
            Entity entity1 = createEntity("id1");
            Entity entity2 = createEntity("id2");
            // Không bắt buộc khác nhau nhưng thường sẽ khác
            // Chỉ cần đảm bảo không throw exception
            assertDoesNotThrow(() -> entity1.hashCode());
            assertDoesNotThrow(() -> entity2.hashCode());
        }

        @Test
        @DisplayName("getSpecificInfo từ subclass hoạt động")
        void testGetSpecificInfo() {
            Entity entity = createEntity("E001");
            assertEquals("Test entity: E001", entity.getSpecificInfo());
        }

        @Test
        @DisplayName("equals và hashCode với id null")
        void testEqualsHashCode_NullId() {
            Entity entity1 = createEntity(null);
            Entity entity2 = createEntity(null);
            assertEquals(entity1, entity2);
            assertEquals(entity1.hashCode(), entity2.hashCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. Admin Tests (6/10 lines, 60% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Admin - Kiểm thử adminLevel và các phương thức thừa kế")
    class AdminTests {

        @Test
        @DisplayName("Constructor khởi tạo Admin với id, username, password")
        void testConstructor() {
            Admin admin = new Admin("ADM001", "admin1", "adminpass");
            assertEquals("ADM001", admin.getId());
            assertEquals("admin1", admin.getUsername());
            assertEquals("adminpass", admin.getPassword());
        }

        @Test
        @DisplayName("getRole trả về \"ADMIN\"")
        void testGetRole() {
            Admin admin = new Admin("ADM001", "admin1", "pass");
            assertEquals("ADMIN", admin.getRole());
        }

        @Test
        @DisplayName("isAdmin=true, isBidder=false, isSeller=false")
        void testIsMethods() {
            Admin admin = new Admin("ADM001", "admin1", "pass");
            assertTrue(admin.isAdmin());
            assertFalse(admin.isBidder());
            assertFalse(admin.isSeller());
        }

        @Test
        @DisplayName("getSpecificInfo trả về thông tin Administrator")
        void testGetSpecificInfo() {
            Admin admin = new Admin("ADM001", "admin1", "pass");
            assertEquals("Administrator - system management", admin.getSpecificInfo());
        }

        @Test
        @DisplayName("getAdminLevel ban đầu là null")
        void testGetAdminLevel_NullInitially() {
            Admin admin = new Admin("ADM001", "admin1", "pass");
            assertNull(admin.getAdminLevel());
        }

        @Test
        @DisplayName("setAdminLevel và getAdminLevel hoạt động chính xác")
        void testAdminLevel_GetSet() {
            Admin admin = new Admin("ADM001", "admin1", "pass");

            admin.setAdminLevel("super");
            assertEquals("super", admin.getAdminLevel());

            admin.setAdminLevel("moderator");
            assertEquals("moderator", admin.getAdminLevel());

            admin.setAdminLevel(null);
            assertNull(admin.getAdminLevel());
        }

        @Test
        @DisplayName("Admin kế thừa các phương thức từ User")
        void testInheritedMethods() {
            Admin admin = new Admin("ADM001", "admin1", "pass");
            assertEquals(BigDecimal.ZERO, admin.getBalance());
            admin.setBalance(new BigDecimal("100.00"));
            assertEquals(new BigDecimal("100.00"), admin.getBalance());

            assertNull(admin.getEmail());
            admin.setEmail("admin@system.com");
            assertEquals("admin@system.com", admin.getEmail());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 9. UserFactory Tests (45/63 lines, 71.4% coverage)
    //    Bổ sung edge case cho createUser và createAdmin
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UserFactory - Kiểm thử bổ sung createUser và edge case")
    class UserFactoryTests {

        @Test
        @DisplayName("createUser tạo User với role BIDDER_SELLER và balance=0")
        void testCreateUser_RoleAndBalance() {
            User user = UserFactory.createUser("testbidder", "secure123");
            assertNotNull(user);
            assertEquals("BIDDER_SELLER", user.getRole());
            assertEquals(BigDecimal.ZERO, user.getBalance());
            assertTrue(user.isBidder());
            assertTrue(user.isSeller());
            assertFalse(user.isAdmin());
        }

        @Test
        @DisplayName("createUser với username và password hợp lệ tạo User thành công")
        void testCreateUser_ValidCredentials() {
            User user = UserFactory.createUser("validUser", "validPass123");
            assertNotNull(user);
            assertEquals("validUser", user.getUsername());
            assertEquals("validPass123", user.getPassword());
        }

        @Test
        @DisplayName("createUser tạo ID định dạng USRxxxx")
        void testCreateUser_IdFormat() {
            User user = UserFactory.createUser("fmtUser", "pass1234");
            assertNotNull(user.getId());
            assertTrue(user.getId().startsWith("USR"));
            assertTrue(user.getId().length() > 3);
        }

        @Test
        @DisplayName("createUser với username/password chứa ký tự đặc biệt")
        void testCreateUser_SpecialCharacters() {
            User user = UserFactory.createUser("user@domain", "p@ssw0rd!#");
            assertNotNull(user);
            assertEquals("user@domain", user.getUsername());
        }

        @Test
        @DisplayName("createAdmin tạo Admin với role ADMIN, isAdmin=true")
        void testCreateAdmin_RoleAndPermissions() {
            Admin admin = UserFactory.createAdmin("superadmin", "adminPass1");
            assertNotNull(admin);
            assertEquals("ADMIN", admin.getRole());
            assertTrue(admin.isAdmin());
            assertFalse(admin.isBidder());
            assertFalse(admin.isSeller());
        }

        @Test
        @DisplayName("createAdmin tạo ID định dạng ADMxxxx")
        void testCreateAdmin_IdFormat() {
            Admin admin = UserFactory.createAdmin("fmtAdmin", "pass1234");
            assertNotNull(admin.getId());
            assertTrue(admin.getId().startsWith("ADM"));
            assertTrue(admin.getId().length() > 3);
        }

        @Test
        @DisplayName("createAdmin tạo các Admin khác nhau có ID khác nhau")
        void testCreateAdmin_DifferentIds() {
            Admin admin1 = UserFactory.createAdmin("adminA", "pass1234");
            Admin admin2 = UserFactory.createAdmin("adminB", "pass5678");
            assertNotEquals(admin1.getId(), admin2.getId());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 10. RegularUser Tests (6/7 lines, 85.7% coverage)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RegularUser - Kiểm thử constructor và role methods")
    class RegularUserTests {

        @Test
        @DisplayName("Constructor khởi tạo RegularUser với id, username, password")
        void testConstructor() {
            RegularUser user = new RegularUser("RU001", "reguser", "pass123");
            assertEquals("RU001", user.getId());
            assertEquals("reguser", user.getUsername());
            assertEquals("pass123", user.getPassword());
        }

        @Test
        @DisplayName("getRole trả về \"BIDDER_SELLER\"")
        void testGetRole() {
            RegularUser user = new RegularUser("RU001", "reguser", "pass");
            assertEquals("BIDDER_SELLER", user.getRole());
        }

        @Test
        @DisplayName("getSpecificInfo trả về thông tin Regular user")
        void testGetSpecificInfo() {
            RegularUser user = new RegularUser("RU001", "reguser", "pass");
            assertEquals("Regular user - both bidder and seller", user.getSpecificInfo());
        }

        @Test
        @DisplayName("isAdmin=false, isBidder=true, isSeller=true")
        void testIsMethods() {
            RegularUser user = new RegularUser("RU001", "reguser", "pass");
            assertFalse(user.isAdmin());
            assertTrue(user.isBidder());
            assertTrue(user.isSeller());
        }

        @Test
        @DisplayName("RegularUser kế thừa balance từ User (= ZERO)")
        void testInheritedBalance() {
            RegularUser user = new RegularUser("RU001", "reguser", "pass");
            assertEquals(BigDecimal.ZERO, user.getBalance());
        }

        @Test
        @DisplayName("RegularUser kế thừa setEmail/getEmail từ User")
        void testInheritedEmail() {
            RegularUser user = new RegularUser("RU001", "reguser", "pass");
            assertNull(user.getEmail());
            user.setEmail("regular@example.com");
            assertEquals("regular@example.com", user.getEmail());
        }
    }
}
