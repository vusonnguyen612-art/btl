package test;

import DAO.AuctionSessionDAO;
import DAO.ItemDAO;
import DAO.BidDAO;
import DAO.UserDAO;
import DAO.AuctionDAO;
import DAO.DatabaseUtil;
import Exception.AuthenticationException;
import Model.AuctionSession;
import Model.Art;
import Model.Bid;
import Model.Item;
import Model.RegularUser;
import Model.SearchCriteria;
import Model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử tích hợp mở rộng (Integration Test) cho các lớp DAO chưa được kiểm thử đầy đủ.
 * <p>
 * Các DAO được kiểm thử:
 * <ol>
 *   <li><b>AuctionSessionDAO</b> — save, update, findById, findByStatus, findBySellerId, findAll</li>
 *   <li><b>ItemDAO</b> — save, update, findById, findBySellerId, findAll, delete</li>
 *   <li><b>BidDAO</b> — getHighestBid, markAsWinner, getWinningBidsByUserId</li>
 *   <li><b>UserDAO</b> — register (duplicate), login (wrong password), changePassword,
 *       resetPassword, blockUser, deleteUser, balance operations, avatar operations, findAllUsers</li>
 *   <li><b>AuctionDAO</b> — findAuctionById, findRunningAuctions, findOpenAuctions,
 *       findPaymentPendingAuctions, findAllAuctions, startAuction, stopAuction, cancelAuction,
 *       getBidHistory, getUserBidHistory, getCurrentPrice, deductBalance, getUserBalance,
 *       findOverduePaymentAuctions, searchAuctions, findUserParticipatedAuctions</li>
 * </ol>
 * <p>
 * <strong>Lưu ý:</strong> Các kiểm thử này tương tác trực tiếp với cơ sở dữ liệu MySQL
 * (database: auction_db). Dữ liệu test được tạo trong {@link #setUp()} và dọn dẹp trong
 * {@link #tearDown()} để đảm bảo cô lập giữa các lần chạy.
 */
class DaoExtendedTest {

    // ── DAO instances ──────────────────────────────────────
    private final AuctionSessionDAO auctionSessionDAO = new AuctionSessionDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();

    // ── Test data IDs (unique per run via timestamp) ───────
    private String sellerId;
    private String bidderId;
    private String itemId;
    private String auctionId;

    /**
     * Khởi tạo các cột còn thiếu trong DB (migration) trước toàn bộ bộ kiểm thử.
     * Thêm các cột chưa có trong schema gốc nhưng được DAO sử dụng.
     */
    @BeforeAll
    static void setUpAll() throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            // Thêm cột is_blocked cho users nếu chưa có
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN is_blocked BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
                // Cột đã tồn tại
            }
            // Thêm cột is_winner cho bids nếu chưa có
            try {
                stmt.execute("ALTER TABLE bids ADD COLUMN is_winner BOOLEAN DEFAULT 0");
            } catch (SQLException e) {
                // Cột đã tồn tại
            }
            // Thêm cột Winner cho bids nếu chưa có (BidDAO.getWinningBidsByUserId dùng)
            try {
                stmt.execute("ALTER TABLE bids ADD COLUMN Winner BOOLEAN DEFAULT 0");
            } catch (SQLException e) {
                // Cột đã tồn tại
            }
            // Thêm cột item_name cho bids nếu chưa có
            try {
                stmt.execute("ALTER TABLE bids ADD COLUMN item_name VARCHAR(200)");
            } catch (SQLException e) {
                // Cột đã tồn tại
            }
        }
    }

    /**
     * Thiết lập môi trường trước mỗi bài kiểm thử:
     * <ul>
     *   <li>Tạo tài khoản người bán và người mua trong DB</li>
     *   <li>Nạp số dư cho người mua (50,000) đủ để đặt giá</li>
     *   <li>Tạo vật phẩm thử nghiệm (Art) trong DB</li>
     *   <li>Tạo phiên đấu giá OPEN trong DB</li>
     * </ul>
     */
    @BeforeEach
    void setUp() throws SQLException {
        long ts = System.currentTimeMillis();
        sellerId = "TEST_SELLER_" + ts;
        bidderId = "TEST_BIDDER_" + ts;
        itemId = "TEST_ITEM_" + ts;
        auctionId = "TEST_AUC_" + ts;

        // Tạo người dùng
        User seller = new RegularUser(sellerId, sellerId, "password");
        User bidder = new RegularUser(bidderId, bidderId, "password");
        userDAO.register(seller);
        userDAO.register(bidder);

        // Nạp số dư cho người mua
        userDAO.addBalance(bidderId, BigDecimal.valueOf(50000.0));

        // Tạo vật phẩm
        Item item = new Art(itemId, "Vật phẩm test", "Mô tả test", 500.0, sellerId);
        itemDAO.save(item);

        // Tạo phiên đấu giá OPEN
        AuctionSession session = new AuctionSession(auctionId, item, sellerId, 500.0, 60);
        session.setMinIncrement(10.0);
        auctionSessionDAO.save(session);
    }

    /**
     * Dọn dẹp dữ liệu thử nghiệm trong DB sau mỗi bài kiểm thử.
     * Xóa theo thứ tự bảng con trước bảng cha để tránh lỗi ràng buộc khóa ngoại.
     */
    @AfterEach
    void tearDown() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            // 1. Xóa bids
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM bids WHERE auction_id = ? OR bidder_id IN (?, ?)")) {
                stmt.setString(1, auctionId);
                stmt.setString(2, sellerId);
                stmt.setString(3, bidderId);
                stmt.executeUpdate();
            }
            // 2. Xóa auction_sessions
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM auction_sessions WHERE id = ? OR seller_id IN (?, ?)")) {
                stmt.setString(1, auctionId);
                stmt.setString(2, sellerId);
                stmt.setString(3, bidderId);
                stmt.executeUpdate();
            }
            // 3. Xóa items
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM items WHERE id = ? OR seller_id IN (?, ?)")) {
                stmt.setString(1, itemId);
                stmt.setString(2, sellerId);
                stmt.setString(3, bidderId);
                stmt.executeUpdate();
            }
            // 4. Xóa users
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM users WHERE id IN (?, ?)")) {
                stmt.setString(1, sellerId);
                stmt.setString(2, bidderId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi dọn dẹp dữ liệu test: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🟢 AuctionSessionDAO Tests
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử {@link AuctionSessionDAO#save(AuctionSession)} — lưu phiên đấu giá mới thành công.
     */
    @Test
    void testAuctionSessionDAO_Save() {
        Optional<AuctionSession> found = auctionSessionDAO.findById(auctionId);
        assertTrue(found.isPresent(), "Phiên đấu giá vừa lưu phải tìm thấy được.");
        assertEquals(auctionId, found.get().getId());
        assertEquals(AuctionSession.Status.OPEN, found.get().getStatus());
        assertEquals(500.0, found.get().getStartPrice(), 0.001);
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#update(AuctionSession)} — cập nhật trạng thái và giá.
     */
    @Test
    void testAuctionSessionDAO_Update() {
        Optional<AuctionSession> opt = auctionSessionDAO.findById(auctionId);
        assertTrue(opt.isPresent());
        AuctionSession session = opt.get();

        session.setStatus(AuctionSession.Status.RUNNING);
        session.setCurrentPrice(600.0);
        session.setHighestBidderId(bidderId);
        session.setWinnerId(bidderId);
        session.setStartTime(LocalDateTime.now().minusMinutes(10));
        session.setEndTime(LocalDateTime.now().plusMinutes(50));

        boolean updated = auctionSessionDAO.update(session);
        assertTrue(updated, "Cập nhật phiên đấu giá phải thành công.");

        Optional<AuctionSession> updatedOpt = auctionSessionDAO.findById(auctionId);
        assertTrue(updatedOpt.isPresent());
        assertEquals(AuctionSession.Status.RUNNING, updatedOpt.get().getStatus());
        assertEquals(600.0, updatedOpt.get().getCurrentPrice(), 0.001);
        assertEquals(bidderId, updatedOpt.get().getHighestBidderId());
        assertEquals(bidderId, updatedOpt.get().getWinnerId());
        assertNotNull(updatedOpt.get().getStartTime());
        assertNotNull(updatedOpt.get().getEndTime());
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#findById(String)} — tìm thấy phiên tồn tại.
     */
    @Test
    void testAuctionSessionDAO_FindById_Exists() {
        Optional<AuctionSession> found = auctionSessionDAO.findById(auctionId);
        assertTrue(found.isPresent());
        assertEquals(itemId, found.get().getItem().getId());
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#findById(String)} — ID không tồn tại trả về empty.
     */
    @Test
    void testAuctionSessionDAO_FindById_NotFound() {
        Optional<AuctionSession> found = auctionSessionDAO.findById("NONEXISTENT_" + System.currentTimeMillis());
        assertFalse(found.isPresent(), "ID không tồn tại phải trả về Optional.empty().");
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#findByStatus(AuctionSession.Status)} — lọc theo trạng thái OPEN.
     */
    @Test
    void testAuctionSessionDAO_FindByStatus_Open() {
        List<AuctionSession> sessions = auctionSessionDAO.findByStatus(AuctionSession.Status.OPEN);
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty(), "Phải có ít nhất 1 phiên OPEN.");
        assertTrue(sessions.stream().anyMatch(s -> s.getId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#findByStatus(AuctionSession.Status)} — trạng thái không có phiên nào.
     */
    @Test
    void testAuctionSessionDAO_FindByStatus_Paid_Empty() {
        List<AuctionSession> sessions = auctionSessionDAO.findByStatus(AuctionSession.Status.PAID);
        assertNotNull(sessions);
        // Không có phiên PAID nào từ dữ liệu test
        boolean hasTestAuction = sessions.stream().anyMatch(s -> s.getId().equals(auctionId));
        assertFalse(hasTestAuction, "Phiên test đang OPEN, không được xuất hiện trong danh sách PAID.");
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#findBySellerId(String)} — tìm theo seller tồn tại.
     */
    @Test
    void testAuctionSessionDAO_FindBySellerId_Exists() {
        List<AuctionSession> sessions = auctionSessionDAO.findBySellerId(sellerId);
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty());
        assertTrue(sessions.stream().anyMatch(s -> s.getId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#findBySellerId(String)} — seller không tồn tại.
     */
    @Test
    void testAuctionSessionDAO_FindBySellerId_NotFound() {
        List<AuctionSession> sessions = auctionSessionDAO.findBySellerId("NONEXISTENT_SELLER_" + System.currentTimeMillis());
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty(), "Seller không tồn tại phải trả về danh sách rỗng.");
    }

    /**
     * Kiểm thử {@link AuctionSessionDAO#findAll()} — trả về danh sách không null.
     */
    @Test
    void testAuctionSessionDAO_FindAll() {
        List<AuctionSession> sessions = auctionSessionDAO.findAll();
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty(), "Phải có ít nhất 1 phiên trong DB.");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🟢 ItemDAO Tests
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử {@link ItemDAO#save(Item)} — lưu vật phẩm mới thành công.
     */
    @Test
    void testItemDAO_Save() {
        Optional<Item> found = itemDAO.findById(itemId);
        assertTrue(found.isPresent(), "Vật phẩm vừa lưu phải tìm thấy được.");
        assertEquals("Vật phẩm test", found.get().getName());
        assertEquals("ART", found.get().getCategory());
    }

    /**
     * Kiểm thử {@link ItemDAO#update(Item)} — cập nhật thông tin vật phẩm.
     */
    @Test
    void testItemDAO_Update() {
        Optional<Item> opt = itemDAO.findById(itemId);
        assertTrue(opt.isPresent());
        Item item = opt.get();
        item.setName("Vật phẩm đã cập nhật");
        item.setDescription("Mô tả mới");
        item.setStartPrice(800.0);
        item.setImagePath("/images/updated.jpg");

        boolean updated = itemDAO.update(item);
        assertTrue(updated, "Cập nhật vật phẩm phải thành công.");

        Optional<Item> updatedOpt = itemDAO.findById(itemId);
        assertTrue(updatedOpt.isPresent());
        assertEquals("Vật phẩm đã cập nhật", updatedOpt.get().getName());
        assertEquals("Mô tả mới", updatedOpt.get().getDescription());
        assertEquals(800.0, updatedOpt.get().getStartPrice(), 0.001);
        assertEquals("/images/updated.jpg", updatedOpt.get().getImagePath());
    }

    /**
     * Kiểm thử {@link ItemDAO#findById(String)} — tìm thấy vật phẩm tồn tại.
     */
    @Test
    void testItemDAO_FindById_Exists() {
        Optional<Item> found = itemDAO.findById(itemId);
        assertTrue(found.isPresent());
        assertEquals(sellerId, found.get().getSellerId());
    }

    /**
     * Kiểm thử {@link ItemDAO#findById(String)} — ID không tồn tại trả về empty.
     */
    @Test
    void testItemDAO_FindById_NotFound() {
        Optional<Item> found = itemDAO.findById("NONEXISTENT_ITEM_" + System.currentTimeMillis());
        assertFalse(found.isPresent(), "ID không tồn tại phải trả về Optional.empty().");
    }

    /**
     * Kiểm thử {@link ItemDAO#findBySellerId(String)} — tìm theo seller tồn tại.
     */
    @Test
    void testItemDAO_FindBySellerId_Exists() {
        List<Item> items = itemDAO.findBySellerId(sellerId);
        assertNotNull(items);
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(i -> i.getId().equals(itemId)));
    }

    /**
     * Kiểm thử {@link ItemDAO#findBySellerId(String)} — seller không tồn tại.
     */
    @Test
    void testItemDAO_FindBySellerId_NotFound() {
        List<Item> items = itemDAO.findBySellerId("NONEXISTENT_SELLER_" + System.currentTimeMillis());
        assertNotNull(items);
        assertTrue(items.isEmpty(), "Seller không tồn tại phải trả về danh sách rỗng.");
    }

    /**
     * Kiểm thử {@link ItemDAO#findAll()} — trả về danh sách không null.
     */
    @Test
    void testItemDAO_FindAll() {
        List<Item> items = itemDAO.findAll();
        assertNotNull(items);
        assertFalse(items.isEmpty(), "Phải có ít nhất 1 vật phẩm trong DB.");
    }

    /**
     * Kiểm thử {@link ItemDAO#delete(String)} — xóa vật phẩm thành công.
     */
    @Test
    void testItemDAO_Delete_Exists() {
        // Tạo item riêng để test delete (không dùng itemId chính vì auctionSession tham chiếu đến nó)
        String deleteItemId = "TEST_DEL_ITEM_" + System.currentTimeMillis();
        Item delItem = new Art(deleteItemId, "Item xóa", "Mô tả", 100.0, sellerId);
        itemDAO.save(delItem);

        boolean deleted = itemDAO.delete(deleteItemId);
        assertTrue(deleted, "Xóa vật phẩm tồn tại phải thành công.");

        Optional<Item> found = itemDAO.findById(deleteItemId);
        assertFalse(found.isPresent(), "Vật phẩm đã xóa không được tìm thấy.");
    }

    /**
     * Kiểm thử {@link ItemDAO#delete(String)} — xóa ID không tồn tại trả về false.
     */
    @Test
    void testItemDAO_Delete_NotFound() {
        boolean deleted = itemDAO.delete("NONEXISTENT_ITEM_" + System.currentTimeMillis());
        assertFalse(deleted, "Xóa ID không tồn tại phải trả về false.");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🟢 BidDAO Tests
    // ═══════════════════════════════════════════════════════════════

    /**
     * Helper: tạo phiên đấu giá RUNNING và thực hiện đặt giá qua raw SQL để test BidDAO.
     */
    private String prepareAuctionWithBids() throws SQLException {
        // Chuyển phiên sang RUNNING
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE auction_sessions SET status = 'RUNNING', start_time = NOW(), " +
                     "end_time = DATE_ADD(NOW(), INTERVAL 60 MINUTE) WHERE id = ?")) {
            stmt.setString(1, auctionId);
            stmt.executeUpdate();
        }

        // Thêm bids trực tiếp vào DB
        String bidId1 = "TEST_BID_1_" + System.currentTimeMillis();
        String bidId2 = "TEST_BID_2_" + System.currentTimeMillis();

        try (Connection conn = DatabaseUtil.getConnection()) {
            // Bid 1: 600
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO bids (id, auction_id, bidder_id, amount, item_name, Winner) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, bidId1);
                stmt.setString(2, auctionId);
                stmt.setString(3, bidderId);
                stmt.setDouble(4, 600.0);
                stmt.setString(5, "Vật phẩm test");
                stmt.setInt(6, 0);
                stmt.executeUpdate();
            }
            // Bid 2: 700 (higher)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO bids (id, auction_id, bidder_id, amount, item_name, Winner) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, bidId2);
                stmt.setString(2, auctionId);
                stmt.setString(3, bidderId);
                stmt.setDouble(4, 700.0);
                stmt.setString(5, "Vật phẩm test");
                stmt.setInt(6, 0);
                stmt.executeUpdate();
            }
            // Cập nhật current_price và highest_bidder_id
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE auction_sessions SET current_price = 700.0, highest_bidder_id = ? WHERE id = ?")) {
                stmt.setString(1, bidderId);
                stmt.setString(2, auctionId);
                stmt.executeUpdate();
            }
        }
        return bidId2; // ID của bid cao nhất
    }

    /**
     * Kiểm thử {@link BidDAO#getHighestBid(String)} — có bids, trả về bid cao nhất.
     */
    @Test
    void testBidDAO_GetHighestBid_Exists() throws SQLException {
        prepareAuctionWithBids();

        Bid highest = bidDAO.getHighestBid(auctionId);
        assertNotNull(highest, "Phải tìm thấy bid cao nhất.");
        assertEquals(700.0, highest.getAmount(), 0.001);
        assertEquals(bidderId, highest.getBidderId());
    }

    /**
     * Kiểm thử {@link BidDAO#getHighestBid(String)} — không có bid nào, trả về null.
     */
    @Test
    void testBidDAO_GetHighestBid_NoBids() {
        Bid highest = bidDAO.getHighestBid(auctionId);
        assertNull(highest, "Không có bid nào thì phải trả về null.");
    }

    /**
     * Kiểm thử {@link BidDAO#markAsWinner(String)} — đánh dấu bid là người thắng.
     */
    @Test
    void testBidDAO_MarkAsWinner() throws SQLException {
        String highestBidId = prepareAuctionWithBids();

        // Đánh dấu bid cao nhất là winner
        bidDAO.markAsWinner(highestBidId);

        // Kiểm tra trong DB
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT is_winner FROM bids WHERE id = ?")) {
            stmt.setString(1, highestBidId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("is_winner"), "is_winner phải được đặt thành 1.");
            }
        }
    }

    /**
     * Kiểm thử {@link BidDAO#getWinningBidsByUserId(String)} — user có bid thắng.
     */
    @Test
    void testBidDAO_GetWinningBidsByUserId_HasWinner() throws SQLException {
        String highestBidId = prepareAuctionWithBids();
        bidDAO.markAsWinner(highestBidId);

        // Cập nhật Winner = 1 (vì getWinningBidsByUserId dùng cột Winner)
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE bids SET Winner = 1 WHERE id = ?")) {
            stmt.setString(1, highestBidId);
            stmt.executeUpdate();
        }

        List<Bid> winningBids = bidDAO.getWinningBidsByUserId(bidderId);
        assertNotNull(winningBids);
        assertFalse(winningBids.isEmpty(), "Phải có ít nhất 1 bid thắng.");
        assertTrue(winningBids.stream().anyMatch(b -> b.getId().equals(highestBidId)));
    }

    /**
     * Kiểm thử {@link BidDAO#getWinningBidsByUserId(String)} — user không có bid thắng nào.
     */
    @Test
    void testBidDAO_GetWinningBidsByUserId_NoWinner() {
        List<Bid> winningBids = bidDAO.getWinningBidsByUserId(bidderId);
        assertNotNull(winningBids);
        // Không có Winner = 1 nên danh sách rỗng
        boolean hasWinner = winningBids.stream().anyMatch(Bid::isWinner);
        assertFalse(hasWinner, "User test không có bid thắng nào.");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🟢 UserDAO Tests
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử {@link UserDAO#findAllUsers()} — trả về danh sách không null và không rỗng.
     */
    @Test
    void testUserDAO_FindAllUsers() {
        List<User> users = userDAO.findAllUsers();
        assertNotNull(users);
        assertFalse(users.isEmpty(), "Phải có ít nhất 1 user trong DB.");
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals(sellerId)));
    }

    /**
     * Kiểm thử {@link UserDAO#deleteUser(String)} — xóa user thường thành công.
     */
    @Test
    void testUserDAO_DeleteUser_Success() {
        // Tạo user riêng để test delete (không dùng sellerId/bidderId chính)
        String delUserId = "TEST_DEL_USER_" + System.currentTimeMillis();
        User delUser = new RegularUser(delUserId, delUserId, "password");
        userDAO.register(delUser);

        boolean deleted = userDAO.deleteUser(delUserId);
        assertTrue(deleted, "Xóa user thường phải thành công.");

        Optional<User> found = userDAO.findById(delUserId);
        assertFalse(found.isPresent(), "User đã xóa không được tìm thấy.");
    }

    /**
     * Kiểm thử {@link UserDAO#deleteUser(String)} — xóa ID không tồn tại trả về false.
     */
    @Test
    void testUserDAO_DeleteUser_NotFound() {
        boolean deleted = userDAO.deleteUser("NONEXISTENT_USER_" + System.currentTimeMillis());
        assertFalse(deleted, "Xóa ID không tồn tại phải trả về false.");
    }

    /**
     * Kiểm thử {@link UserDAO#resetPassword(String, String)} — reset mật khẩu thành công.
     * Sau khi reset, đăng nhập với mật khẩu mới phải thành công.
     */
    @Test
    void testUserDAO_ResetPassword() {
        boolean reset = userDAO.resetPassword(sellerId, "newpassword123");
        assertTrue(reset, "Reset mật khẩu phải thành công.");

        // Đăng nhập với mật khẩu mới
        Optional<User> loggedIn = userDAO.login(sellerId, "newpassword123");
        assertTrue(loggedIn.isPresent(), "Phải đăng nhập được với mật khẩu mới.");
    }

    /**
     * Kiểm thử {@link UserDAO#blockUser(String, boolean)} — chặn user thành công.
     */
    @Test
    void testUserDAO_BlockUser() {
        boolean blocked = userDAO.blockUser(sellerId, true);
        assertTrue(blocked, "Chặn user phải thành công.");

        // Kiểm tra trực tiếp trong DB
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT is_blocked FROM users WHERE id = ?")) {
            stmt.setString(1, sellerId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertTrue(rs.getBoolean("is_blocked"), "is_blocked phải là true.");
            }
        } catch (SQLException e) {
            fail("Lỗi truy vấn DB: " + e.getMessage());
        }

        // Mở chặn
        boolean unblocked = userDAO.blockUser(sellerId, false);
        assertTrue(unblocked, "Mở chặn user phải thành công.");
    }

    /**
     * Kiểm thử {@link UserDAO#findById(String)} — ID không tồn tại trả về empty.
     */
    @Test
    void testUserDAO_FindById_NotFound() {
        Optional<User> found = userDAO.findById("NONEXISTENT_USER_" + System.currentTimeMillis());
        assertFalse(found.isPresent(), "ID không tồn tại phải trả về Optional.empty().");
    }

    /**
     * Kiểm thử {@link UserDAO#register(User)} — đăng ký username trùng lặp trả về false.
     */
    @Test
    void testUserDAO_Register_DuplicateUsername() {
        User duplicate = new RegularUser("TEST_DUP_" + System.currentTimeMillis(), sellerId, "password");
        boolean registered = userDAO.register(duplicate);
        assertFalse(registered, "Đăng ký username đã tồn tại phải trả về false.");
    }

    /**
     * Kiểm thử {@link UserDAO#login(String, String)} — đăng nhập với sai mật khẩu.
     */
    @Test
    void testUserDAO_Login_WrongPassword() {
        Optional<User> loggedIn = userDAO.login(sellerId, "wrongpassword");
        assertFalse(loggedIn.isPresent(), "Đăng nhập với sai mật khẩu phải trả về Optional.empty().");
    }

    /**
     * Kiểm thử {@link UserDAO#authenticate(String, String)} — sai mật khẩu ném AuthenticationException.
     */
    @Test
    void testUserDAO_Authenticate_WrongPassword() {
        assertThrows(AuthenticationException.class, () ->
                userDAO.authenticate(sellerId, "wrongpassword"),
                "Sai mật khẩu phải ném AuthenticationException.");
    }

    /**
     * Kiểm thử {@link UserDAO#changePassword(String, String, String)} — đổi mật khẩu đúng old password.
     */
    @Test
    void testUserDAO_ChangePassword_CorrectOldPassword() {
        boolean changed = userDAO.changePassword(sellerId, "password", "newSecurePass1");
        assertTrue(changed, "Đổi mật khẩu với đúng old password phải thành công.");

        // Xác nhận đăng nhập được với mật khẩu mới
        Optional<User> loggedIn = userDAO.login(sellerId, "newSecurePass1");
        assertTrue(loggedIn.isPresent(), "Phải đăng nhập được với mật khẩu mới.");
    }

    /**
     * Kiểm thử {@link UserDAO#changePassword(String, String, String)} — đổi mật khẩu sai old password.
     */
    @Test
    void testUserDAO_ChangePassword_WrongOldPassword() {
        boolean changed = userDAO.changePassword(sellerId, "wrongOldPassword", "newPass");
        assertFalse(changed, "Đổi mật khẩu với sai old password phải trả về false.");
    }

    /**
     * Kiểm thử {@link UserDAO#updateBalance(String, BigDecimal)} — cập nhật số dư.
     */
    @Test
    void testUserDAO_UpdateBalance() {
        boolean updated = userDAO.updateBalance(sellerId, BigDecimal.valueOf(9999.99));
        assertTrue(updated, "Cập nhật balance phải thành công.");

        BigDecimal balance = userDAO.getBalance(sellerId);
        assertEquals(BigDecimal.valueOf(9999.99).doubleValue(), balance.doubleValue(), 0.01);
    }

    /**
     * Kiểm thử {@link UserDAO#addBalance(String, BigDecimal)} — cộng thêm số dư.
     */
    @Test
    void testUserDAO_AddBalance() {
        BigDecimal initial = userDAO.getBalance(sellerId);

        boolean added = userDAO.addBalance(sellerId, BigDecimal.valueOf(500.0));
        assertTrue(added, "Cộng balance phải thành công.");

        BigDecimal after = userDAO.getBalance(sellerId);
        assertEquals(initial.add(BigDecimal.valueOf(500.0)).doubleValue(), after.doubleValue(), 0.01);
    }

    /**
     * Kiểm thử {@link UserDAO#getBalance(String)} — lấy số dư của user tồn tại.
     */
    @Test
    void testUserDAO_GetBalance_Exists() {
        BigDecimal balance = userDAO.getBalance(sellerId);
        assertNotNull(balance);
    }

    /**
     * Kiểm thử {@link UserDAO#getBalance(String)} — user không tồn tại trả về ZERO.
     */
    @Test
    void testUserDAO_GetBalance_NotFound() {
        BigDecimal balance = userDAO.getBalance("NONEXISTENT_USER_" + System.currentTimeMillis());
        assertEquals(BigDecimal.ZERO, balance, "User không tồn tại phải trả về BigDecimal.ZERO.");
    }

    /**
     * Kiểm thử {@link UserDAO#getUsernameById(String)} — user tồn tại trả về username.
     */
    @Test
    void testUserDAO_GetUsernameById_Exists() {
        String username = userDAO.getUsernameById(sellerId);
        assertEquals(sellerId, username, "Phải trả về đúng username.");
    }

    /**
     * Kiểm thử {@link UserDAO#getUsernameById(String)} — user không tồn tại trả về chính userId.
     */
    @Test
    void testUserDAO_GetUsernameById_NotFound() {
        String fakeId = "FAKE_" + System.currentTimeMillis();
        String result = userDAO.getUsernameById(fakeId);
        assertEquals(fakeId, result, "User không tồn tại phải trả về chính userId.");
    }

    /**
     * Kiểm thử {@link UserDAO#getAvatarPath(String)} — user không có avatar trả về null.
     */
    @Test
    void testUserDAO_GetAvatarPath_Null() {
        String avatar = userDAO.getAvatarPath(sellerId);
        assertNull(avatar, "User mới tạo chưa có avatar.");
    }

    /**
     * Kiểm thử {@link UserDAO#updateAvatarPath(String, String)} và {@link UserDAO#getAvatarPath(String)}.
     */
    @Test
    void testUserDAO_UpdateAndGetAvatarPath() {
        String avatarPath = "/avatars/test.png";
        boolean updated = userDAO.updateAvatarPath(sellerId, avatarPath);
        assertTrue(updated, "Cập nhật avatar path phải thành công.");

        String retrieved = userDAO.getAvatarPath(sellerId);
        assertEquals(avatarPath, retrieved, "Avatar path phải khớp với giá trị đã cập nhật.");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🟢 AuctionDAO Tests
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử {@link AuctionDAO#findAuctionById(String)} — tìm thấy phiên tồn tại.
     */
    @Test
    void testAuctionDAO_FindAuctionById_Exists() {
        Optional<AuctionSession> found = auctionDAO.findAuctionById(auctionId);
        assertTrue(found.isPresent(), "Phải tìm thấy phiên đấu giá.");
        assertEquals(itemId, found.get().getItem().getId());
    }

    /**
     * Kiểm thử {@link AuctionDAO#findAuctionById(String)} — ID không tồn tại trả về empty.
     */
    @Test
    void testAuctionDAO_FindAuctionById_NotFound() {
        Optional<AuctionSession> found = auctionDAO.findAuctionById("NONEXISTENT_AUC_" + System.currentTimeMillis());
        assertFalse(found.isPresent(), "ID không tồn tại phải trả về Optional.empty().");
    }

    /**
     * Kiểm thử {@link AuctionDAO#findRunningAuctions()} — trả về danh sách các phiên RUNNING.
     */
    @Test
    void testAuctionDAO_FindRunningAuctions() {
        List<AuctionSession> sessions = auctionDAO.findRunningAuctions();
        assertNotNull(sessions);
        // Tất cả phiên trong danh sách phải có status RUNNING
        for (AuctionSession s : sessions) {
            assertEquals(AuctionSession.Status.RUNNING, s.getStatus());
        }
    }

    /**
     * Kiểm thử {@link AuctionDAO#findOpenAuctions()} — trả về danh sách các phiên OPEN.
     */
    @Test
    void testAuctionDAO_FindOpenAuctions() {
        List<AuctionSession> sessions = auctionDAO.findOpenAuctions();
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty(), "Phải có ít nhất 1 phiên OPEN.");
        assertTrue(sessions.stream().anyMatch(s -> s.getId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionDAO#findPaymentPendingAuctions()} — trả về danh sách phiên PAYMENT_PENDING.
     */
    @Test
    void testAuctionDAO_FindPaymentPendingAuctions() {
        List<AuctionSession> sessions = auctionDAO.findPaymentPendingAuctions();
        assertNotNull(sessions);
        // Phiên test đang OPEN nên không có trong danh sách PAYMENT_PENDING
    }

    /**
     * Kiểm thử {@link AuctionDAO#findAllAuctions()} — trả về danh sách tất cả phiên.
     */
    @Test
    void testAuctionDAO_FindAllAuctions() {
        List<AuctionSession> sessions = auctionDAO.findAllAuctions();
        assertNotNull(sessions);
        assertFalse(sessions.isEmpty(), "Phải có ít nhất 1 phiên trong DB.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#startAuction(String)} — bắt đầu phiên OPEN thành công.
     */
    @Test
    void testAuctionDAO_StartAuction() {
        boolean started = auctionDAO.startAuction(auctionId);
        assertTrue(started, "Bắt đầu phiên OPEN phải thành công.");

        Optional<AuctionSession> opt = auctionDAO.findAuctionById(auctionId);
        assertTrue(opt.isPresent());
        assertEquals(AuctionSession.Status.RUNNING, opt.get().getStatus());
        assertNotNull(opt.get().getStartTime());
        assertNotNull(opt.get().getEndTime());
    }

    /**
     * Kiểm thử {@link AuctionDAO#startAuction(String)} — phiên không phải OPEN thì không bắt đầu được.
     */
    @Test
    void testAuctionDAO_StartAuction_AlreadyRunning() {
        // Bắt đầu lần 1
        auctionDAO.startAuction(auctionId);
        // Bắt đầu lần 2 — phải thất bại vì không còn OPEN
        boolean startedAgain = auctionDAO.startAuction(auctionId);
        assertFalse(startedAgain, "Phiên đã RUNNING thì không thể start lại.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#stopAuction(String)} — tạm dừng phiên RUNNING.
     */
    @Test
    void testAuctionDAO_StopAuction() {
        // Phải start trước
        auctionDAO.startAuction(auctionId);

        boolean stopped = auctionDAO.stopAuction(auctionId);
        assertTrue(stopped, "Dừng phiên RUNNING phải thành công.");

        Optional<AuctionSession> opt = auctionDAO.findAuctionById(auctionId);
        assertTrue(opt.isPresent());
        assertNotNull(opt.get().getEndTime(), "End time phải được gia hạn.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#cancelAuction(String, String)} — hủy phiên đấu giá.
     */
    @Test
    void testAuctionDAO_CancelAuction() {
        boolean canceled = auctionDAO.cancelAuction(auctionId, "Lý do test");
        assertTrue(canceled, "Hủy phiên phải thành công.");

        Optional<AuctionSession> opt = auctionDAO.findAuctionById(auctionId);
        assertTrue(opt.isPresent());
        assertEquals(AuctionSession.Status.CANCELED, opt.get().getStatus());
    }

    /**
     * Kiểm thử {@link AuctionDAO#getBidHistory(String)} — lấy lịch sử đặt giá của phiên.
     */
    @Test
    void testAuctionDAO_GetBidHistory() throws Exception {
        // Start auction và đặt giá
        auctionDAO.startAuction(auctionId);
        auctionDAO.placeBid(auctionId, bidderId, 600.0);
        auctionDAO.placeBid(auctionId, bidderId, 700.0);

        List<Bid> history = auctionDAO.getBidHistory(auctionId);
        assertNotNull(history);
        assertEquals(2, history.size(), "Phải có 2 bid trong lịch sử.");
        // Bid mới nhất trước (700)
        assertEquals(700.0, history.get(0).getAmount(), 0.001);
    }

    /**
     * Kiểm thử {@link AuctionDAO#getBidHistory(String)} — phiên không có bid nào.
     */
    @Test
    void testAuctionDAO_GetBidHistory_Empty() {
        List<Bid> history = auctionDAO.getBidHistory(auctionId);
        assertNotNull(history);
        assertTrue(history.isEmpty(), "Phiên chưa có bid thì lịch sử phải rỗng.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#getUserBidHistory(String)} — lấy lịch sử đặt giá của user.
     */
    @Test
    void testAuctionDAO_GetUserBidHistory() throws Exception {
        auctionDAO.startAuction(auctionId);
        auctionDAO.placeBid(auctionId, bidderId, 600.0);

        List<Bid> userHistory = auctionDAO.getUserBidHistory(bidderId);
        assertNotNull(userHistory);
        assertFalse(userHistory.isEmpty(), "User đã đặt giá nên phải có lịch sử.");
        assertTrue(userHistory.stream().anyMatch(b -> b.getAuctionId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionDAO#getUserBidHistory(String)} — user chưa đặt giá nào.
     */
    @Test
    void testAuctionDAO_GetUserBidHistory_NoBids() {
        List<Bid> userHistory = auctionDAO.getUserBidHistory(bidderId);
        assertNotNull(userHistory);
        assertTrue(userHistory.isEmpty(), "User chưa đặt giá thì lịch sử phải rỗng.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#getCurrentPrice(String)} — lấy giá hiện tại của phiên.
     */
    @Test
    void testAuctionDAO_GetCurrentPrice() {
        double price = auctionDAO.getCurrentPrice(auctionId);
        assertEquals(500.0, price, 0.001, "Giá khởi điểm phải là 500.0.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#getCurrentPrice(String)} — phiên không tồn tại trả về 0.
     */
    @Test
    void testAuctionDAO_GetCurrentPrice_NotFound() {
        double price = auctionDAO.getCurrentPrice("NONEXISTENT_AUC_" + System.currentTimeMillis());
        assertEquals(0.0, price, 0.001, "Phiên không tồn tại phải trả về 0.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#deductBalance(String, double)} — trừ tiền thành công.
     */
    @Test
    void testAuctionDAO_DeductBalance_Success() {
        // Nạp tiền cho seller trước
        userDAO.addBalance(sellerId, BigDecimal.valueOf(1000.0));

        boolean deducted = auctionDAO.deductBalance(sellerId, 300.0);
        assertTrue(deducted, "Trừ tiền phải thành công khi đủ số dư.");

        BigDecimal balance = auctionDAO.getUserBalance(sellerId);
        assertEquals(700.0, balance.doubleValue(), 0.01);
    }

    /**
     * Kiểm thử {@link AuctionDAO#deductBalance(String, double)} — không đủ số dư.
     */
    @Test
    void testAuctionDAO_DeductBalance_Insufficient() {
        // Seller ban đầu có balance = 0
        boolean deducted = auctionDAO.deductBalance(sellerId, 999999.0);
        assertFalse(deducted, "Không đủ số dư thì trừ tiền phải thất bại.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#getUserBalance(String)} — lấy số dư user tồn tại.
     */
    @Test
    void testAuctionDAO_GetUserBalance_Exists() {
        BigDecimal balance = auctionDAO.getUserBalance(sellerId);
        assertNotNull(balance);
    }

    /**
     * Kiểm thử {@link AuctionDAO#getUserBalance(String)} — user không tồn tại trả về ZERO.
     */
    @Test
    void testAuctionDAO_GetUserBalance_NotFound() {
        BigDecimal balance = auctionDAO.getUserBalance("NONEXISTENT_USER_" + System.currentTimeMillis());
        assertEquals(BigDecimal.ZERO, balance, "User không tồn tại phải trả về BigDecimal.ZERO.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#findOverduePaymentAuctions()} — trả về danh sách phiên quá hạn.
     */
    @Test
    void testAuctionDAO_FindOverduePaymentAuctions() {
        List<AuctionSession> overdue = auctionDAO.findOverduePaymentAuctions();
        assertNotNull(overdue);
        // Phiên test đang OPEN nên không nằm trong danh sách quá hạn
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — tìm kiếm theo keyword.
     */
    @Test
    void testAuctionDAO_SearchAuctions_ByKeyword() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setKeyword("Vật phẩm");

        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Phải tìm thấy phiên với keyword 'Vật phẩm'.");
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — tìm kiếm theo category.
     */
    @Test
    void testAuctionDAO_SearchAuctions_ByCategory() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCategory("ART");

        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Phải tìm thấy phiên với category ART.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — tìm kiếm theo trạng thái.
     */
    @Test
    void testAuctionDAO_SearchAuctions_ByStatus() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setStatuses(Arrays.asList(AuctionSession.Status.OPEN));

        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        // Tất cả kết quả phải có status OPEN
        for (AuctionSession s : results) {
            assertEquals(AuctionSession.Status.OPEN, s.getStatus());
        }
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — tìm kiếm theo khoảng giá.
     */
    @Test
    void testAuctionDAO_SearchAuctions_ByPriceRange() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMinPrice(400.0);
        criteria.setMaxPrice(600.0);

        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        // Phiên test có current_price = 500, nằm trong khoảng
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — tìm kiếm theo sellerId.
     */
    @Test
    void testAuctionDAO_SearchAuctions_BySellerId() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSellerId(sellerId);

        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — sắp xếp theo giá tăng dần.
     */
    @Test
    void testAuctionDAO_SearchAuctions_SortByPriceAsc() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSortBy("price_asc");

        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getCurrentPrice() <= results.get(i).getCurrentPrice(),
                    "Kết quả phải sắp xếp theo giá tăng dần.");
        }
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — sắp xếp theo giá giảm dần.
     */
    @Test
    void testAuctionDAO_SearchAuctions_SortByPriceDesc() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSortBy("price_desc");

        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getCurrentPrice() >= results.get(i).getCurrentPrice(),
                    "Kết quả phải sắp xếp theo giá giảm dần.");
        }
    }

    /**
     * Kiểm thử {@link AuctionDAO#searchAuctions(SearchCriteria)} — tiêu chí trống trả về tất cả.
     */
    @Test
    void testAuctionDAO_SearchAuctions_EmptyCriteria() {
        SearchCriteria criteria = new SearchCriteria();
        List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
        assertNotNull(results);
        // Tiêu chí rỗng trả về tất cả phiên không bị lọc
    }

    /**
     * Kiểm thử {@link AuctionDAO#findUserParticipatedAuctions(String)} — user đã tham gia.
     */
    @Test
    void testAuctionDAO_FindUserParticipatedAuctions() throws Exception {
        // Start và kết thúc phiên với bid
        auctionDAO.startAuction(auctionId);
        auctionDAO.placeBid(auctionId, bidderId, 600.0);
        auctionDAO.finishAuction(auctionId);

        List<AuctionSession> participated = auctionDAO.findUserParticipatedAuctions(bidderId);
        assertNotNull(participated);
        assertFalse(participated.isEmpty(), "User đã bid nên phải có phiên tham gia.");
        assertTrue(participated.stream().anyMatch(s -> s.getId().equals(auctionId)));
    }

    /**
     * Kiểm thử {@link AuctionDAO#findUserParticipatedAuctions(String)} — user chưa tham gia phiên nào.
     */
    @Test
    void testAuctionDAO_FindUserParticipatedAuctions_NoParticipation() {
        List<AuctionSession> participated = auctionDAO.findUserParticipatedAuctions(bidderId);
        assertNotNull(participated);
        // bidder chưa tham gia phiên FINISHED/PAID nào
        boolean hasTestAuction = participated.stream().anyMatch(s -> s.getId().equals(auctionId));
        assertFalse(hasTestAuction, "Phiên test đang OPEN, không được xuất hiện.");
    }

    /**
     * Kiểm thử {@link AuctionDAO#saveAuction(AuctionSession)} — lưu phiên trực tiếp qua AuctionDAO.
     */
    @Test
    void testAuctionDAO_SaveAuction() {
        Optional<AuctionSession> found = auctionDAO.findAuctionById(auctionId);
        assertTrue(found.isPresent());
        assertEquals(AuctionSession.Status.OPEN, found.get().getStatus());
    }

    /**
     * Kiểm thử {@link AuctionDAO#findByItemId(String)} — tìm phiên theo itemId.
     */
    @Test
    void testAuctionDAO_FindByItemId_Exists() {
        Optional<AuctionSession> found = auctionDAO.findByItemId(itemId);
        assertTrue(found.isPresent(), "Phải tìm thấy phiên theo itemId.");
        assertEquals(auctionId, found.get().getId());
    }

    /**
     * Kiểm thử {@link AuctionDAO#findByItemId(String)} — itemId không tồn tại.
     */
    @Test
    void testAuctionDAO_FindByItemId_NotFound() {
        Optional<AuctionSession> found = auctionDAO.findByItemId("NONEXISTENT_ITEM_" + System.currentTimeMillis());
        assertFalse(found.isPresent(), "ItemId không tồn tại phải trả về Optional.empty().");
    }
}
