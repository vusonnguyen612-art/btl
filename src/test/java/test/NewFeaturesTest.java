package test;

import DAO.AuctionDAO;
import DAO.ChatDAO;
import DAO.DatabaseUtil;
import DAO.ItemDAO;
import DAO.UserDAO;
import DAO.WatchlistDAO;
import Exception.AuctionClosedException;
import Exception.InsufficientBalanceException;
import Exception.InvalidBidException;
import Exception.UnauthorizedException;
import Model.AuctionSession;
import Model.ChatMessage;
import Model.Item;
import Model.User;
import Model.Admin;
import Model.RegularUser;
import Model.Bidder;
import Model.Seller;
import Factory.UserFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử tích hợp (Integration Test) cho các tính năng mới của hệ thống đấu giá.
 * <p>
 * Các tính năng được kiểm thử:
 * <ol>
 *   <li><b>Sniper Protection (Soft Close)</b> — Tự động gia hạn thời gian đấu giá thêm
 *       2 phút khi có bid được đặt trong 2 phút cuối, ngăn chặn chiến thuật "sniping".</li>
 *   <li><b>Live Chat</b> — Lưu tin nhắn chat và truy vấn lịch sử trò chuyện theo phiên đấu giá.</li>
 *   <li><b>Watchlist</b> — Thêm/xóa phiên đấu giá vào danh sách theo dõi của người dùng,
 *       truy vấn danh sách watchers.</li>
 *   <li><b>Admin Mapping</b> — Tạo và xác thực tài khoản Admin qua DB,
 *       đảm bảo ánh xạ đúng kiểu {@link Admin} với role và quyền isAdmin().</li>
 * </ol>
 * <p>
 * <strong>Lưu ý:</strong> Các kiểm thử này tương tác trực tiếp với cơ sở dữ liệu MySQL
 * thông qua các lớp DAO. Dữ liệu test được tạo trong {@link #setUp()} và dọn dẹp trong
 * {@link #tearDown()} để đảm bảo cô lập giữa các lần chạy.
 */
class NewFeaturesTest {

    /**
     * Khởi tạo các bảng cơ sở dữ liệu cần thiết (chat_messages, watchlist) nếu chưa tồn tại
     * trước khi bắt đầu toàn bộ bộ kiểm thử.
     */
    @BeforeAll
    static void setUpAll() throws SQLException {
        String createChatMessagesTable = 
            "CREATE TABLE IF NOT EXISTS chat_messages (" +
            "    id VARCHAR(50) PRIMARY KEY," +
            "    auction_id VARCHAR(50) NOT NULL," +
            "    sender_id VARCHAR(50) NOT NULL," +
            "    message TEXT NOT NULL," +
            "    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id) ON DELETE CASCADE," +
            "    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createWatchlistTable = 
            "CREATE TABLE IF NOT EXISTS watchlist (" +
            "    id VARCHAR(50) PRIMARY KEY," +
            "    user_id VARCHAR(50) NOT NULL," +
            "    auction_id VARCHAR(50) NOT NULL," +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "    UNIQUE KEY unique_user_auction (user_id, auction_id)," +
            "    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
            "    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id) ON DELETE CASCADE" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = DatabaseUtil.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(createChatMessagesTable);
            stmt.execute(createWatchlistTable);
            // Migration: thêm cột role nếu chưa có
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'BIDDER_SELLER'");
            } catch (SQLException e) {
                // Column already exists
            }
        }
    }

    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ChatDAO chatDAO = new ChatDAO();
    private final WatchlistDAO watchlistDAO = new WatchlistDAO();

    private final String sellerId = "TEST_SELLER_" + System.currentTimeMillis();
    private final String bidderId = "TEST_BIDDER_" + System.currentTimeMillis();
    private final String itemId = "TEST_ITEM_" + System.currentTimeMillis();
    private final String auctionId = "TEST_AUC_" + System.currentTimeMillis();

    /**
     * Thiết lập môi trường trước mỗi bài kiểm thử:
     * <ul>
     *   <li>Tạo tài khoản người bán và người mua trong DB</li>
     *   <li>Nạp số dư cho người mua (10,000) đủ để đặt giá</li>
     *   <li>Tạo vật phẩm thử nghiệm (Art) trong DB</li>
     *   <li>Tạo phiên đấu giá RUNNING với thời gian còn 5 phút, min increment = 10</li>
     * </ul>
     */
    @BeforeEach
    void setUp() throws SQLException {
        // Tạo người dùng thử nghiệm
        User seller = new RegularUser(sellerId, sellerId, "password");
        User bidder = new RegularUser(bidderId, bidderId, "password");
        userDAO.register(seller);
        userDAO.register(bidder);

        // Nạp số dư cho người mua
        userDAO.addBalance(bidderId, BigDecimal.valueOf(10000.0));

        // Tạo vật phẩm thử nghiệm
        Item item = new Model.Art(itemId, "Sản phẩm test", "Mô tả test", 500.0, sellerId);
        itemDAO.save(item);

        // Tạo phiên đấu giá thử nghiệm
        AuctionSession session = new AuctionSession(auctionId, item, sellerId, 500.0, 10);
        session.setStatus(AuctionSession.Status.RUNNING);
        session.setStartTime(LocalDateTime.now().minusMinutes(5));
        session.setEndTime(LocalDateTime.now().plusMinutes(5));
        session.setMinIncrement(10.0);
        auctionDAO.saveAuction(session);
    }

    /**
     * Dọn dẹp dữ liệu thử nghiệm trong DB sau mỗi bài kiểm thử.
     * Xóa theo thứ tự bảng con trước bảng cha để tránh lỗi ràng buộc khóa ngoại (Foreign Key).
     */
    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    /**
     * Thực hiện xóa toàn bộ dữ liệu thử nghiệm trong DB theo thứ tự:
     * watchlist → chat_messages → bids → auction_sessions → items → users.
     */
    private void deleteTestData() {
        String[] sqlQueries = {
            "DELETE FROM watchlist WHERE user_id IN (?, ?)",
            "DELETE FROM chat_messages WHERE sender_id IN (?, ?) OR auction_id = ?",
            "DELETE FROM bids WHERE bidder_id IN (?, ?) OR auction_id = ?",
            "DELETE FROM auction_sessions WHERE id = ?",
            "DELETE FROM items WHERE id = ?",
            "DELETE FROM users WHERE id IN (?, ?)"
        };

        try (Connection conn = DatabaseUtil.getConnection()) {
            for (String sql : sqlQueries) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    if (sql.contains("watchlist")) {
                        stmt.setString(1, sellerId);
                        stmt.setString(2, bidderId);
                    } else if (sql.contains("chat_messages") || sql.contains("bids")) {
                        stmt.setString(1, sellerId);
                        stmt.setString(2, bidderId);
                        stmt.setString(3, auctionId);
                    } else if (sql.contains("auction_sessions") || sql.contains("items")) {
                        stmt.setString(1, sql.contains("items") ? itemId : auctionId);
                    } else if (sql.contains("users")) {
                        stmt.setString(1, sellerId);
                        stmt.setString(2, bidderId);
                    }
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi dọn dẹp dữ liệu test: " + e.getMessage());
        }
    }

    // ── Sniper Protection ───────────────────────────────────

    /**
     * Kiểm thử tính năng Sniper Protection (Soft Close):
     * <p>
     * Khi đặt giá trong vòng 2 phút cuối của phiên đấu giá, thời gian kết thúc (end_time)
     * phải tự động gia hạn thêm 2 phút để ngăn chặn chiến thuật đặt giá giây cuối (sniping).
     * <p>
     * Quy trình:
     * <ol>
     *   <li>Đặt end_time của phiên còn 1 phút (trong cửa sổ 2 phút)</li>
     *   <li>Thực hiện đặt giá hợp lệ (600.0)</li>
     *   <li>Lấy lại phiên từ DB — end_time mới phải ≈ end_time cũ + 2 phút</li>
     * </ol>
     *
     * @throws Exception nếu đặt giá thất bại
     */
    @Test
    void testSniperProtection() throws Exception {
        // Cập nhật end_time của phiên đấu giá thử nghiệm còn 1 phút nữa kết thúc (nằm trong khoảng 2 phút cuối)
        LocalDateTime originalEndTime = LocalDateTime.now().plusMinutes(1);
        setAuctionEndTimeInDB(originalEndTime);

        // Thực hiện đặt giá hợp lệ
        double bidAmount = 600.0;
        boolean success = auctionDAO.placeBid(auctionId, bidderId, bidAmount);
        assertTrue(success, "Đặt giá thử nghiệm phải thành công.");

        // Lấy lại phiên đấu giá từ DB và xác nhận thời gian kết thúc đã tăng lên
        Optional<AuctionSession> sessionOpt = auctionDAO.findAuctionById(auctionId);
        assertTrue(sessionOpt.isPresent());
        AuctionSession updatedSession = sessionOpt.get();

        assertNotNull(updatedSession.getEndTime());
        // Thời gian kết thúc mới phải gần bằng originalEndTime + 2 phút (chênh lệch dưới 5 giây do sai số thời gian chạy)
        LocalDateTime expectedEndTime = originalEndTime.plusMinutes(2);
        java.time.Duration diff = java.time.Duration.between(updatedSession.getEndTime(), expectedEndTime);
        assertTrue(Math.abs(diff.getSeconds()) < 5, "Thời gian kết thúc phải được kéo dài thêm đúng 2 phút.");
    }

    /**
     * Cập nhật thời gian kết thúc của phiên đấu giá trong DB.
     * <p>
     * Phương thức helper dùng để mô phỏng kịch bản "2 phút cuối" cho kiểm thử Sniper Protection.
     *
     * @param endTime thời gian kết thúc mới
     * @throws SQLException nếu truy vấn thất bại
     */
    private void setAuctionEndTimeInDB(LocalDateTime endTime) throws SQLException {
        String sql = "UPDATE auction_sessions SET end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(endTime));
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        }
    }

    // ── Live Chat ───────────────────────────────────────────

    /**
     * Kiểm thử tính năng Live Chat:
     * <ul>
     *   <li>Lưu tin nhắn chat thành công qua {@link ChatDAO#saveChatMessage}</li>
     *   <li>Truy vấn lịch sử chat qua {@link ChatDAO#getChatHistory} — không rỗng</li>
     *   <li>Tin nhắn cuối cùng khớp nội dung đã gửi, đúng senderId, có senderName</li>
     * </ul>
     */
    @Test
    void testLiveChat() {
        String chatMsgContent = "Xin chào, đây là tin nhắn kiểm thử!";

        // Lưu tin nhắn chat
        boolean saved = chatDAO.saveChatMessage(auctionId, bidderId, chatMsgContent);
        assertTrue(saved, "Lưu tin nhắn chat phải thành công.");

        // Lấy lịch sử chat
        List<ChatMessage> chatHistory = chatDAO.getChatHistory(auctionId);
        assertNotNull(chatHistory);
        assertFalse(chatHistory.isEmpty(), "Lịch sử chat không được rỗng.");

        // Xác nhận tin nhắn cuối cùng khớp với tin nhắn vừa gửi
        ChatMessage lastMessage = chatHistory.get(chatHistory.size() - 1);
        assertEquals(chatMsgContent, lastMessage.getMessage());
        assertEquals(bidderId, lastMessage.getSenderId());
        assertNotNull(lastMessage.getSenderName(), "Tên hiển thị người gửi không được null.");
    }

    // ── Watchlist ───────────────────────────────────────────

    /**
     * Kiểm thử tính năng Watchlist (danh sách theo dõi):
     * <ol>
     *   <li>Thêm phiên đấu giá vào danh sách theo dõi — thành công</li>
     *   <li>Truy vấn danh sách theo dõi của người dùng — chứa phiên vừa thêm</li>
     *   <li>Truy vấn danh sách watchers của phiên — chứa ID người dùng</li>
     *   <li>Xóa phiên khỏi danh sách theo dõi — thành công</li>
     *   <li>Xác nhận danh sách không còn chứa phiên đã xóa</li>
     * </ol>
     */
    @Test
    void testWatchlist() {
        // Thêm vào danh sách theo dõi
        boolean added = watchlistDAO.addWatchlist(bidderId, auctionId);
        assertTrue(added, "Thêm vào danh sách theo dõi phải thành công.");

        // Kiểm tra lấy danh sách theo dõi của người dùng
        List<AuctionSession> watchlist = watchlistDAO.getWatchlist(bidderId);
        assertNotNull(watchlist);
        boolean containsAuction = watchlist.stream().anyMatch(a -> a.getId().equals(auctionId));
        assertTrue(containsAuction, "Danh sách theo dõi phải chứa phiên đấu giá.");

        // Kiểm tra lấy danh sách watchers của phiên đấu giá
        List<String> watchers = watchlistDAO.getWatchers(auctionId);
        assertNotNull(watchers);
        assertTrue(watchers.contains(bidderId), "Danh sách người theo dõi phải chứa ID người dùng.");

        // Xóa khỏi danh sách theo dõi
        boolean removed = watchlistDAO.removeWatchlist(bidderId, auctionId);
        assertTrue(removed, "Xóa khỏi danh sách theo dõi phải thành công.");

        // Xác nhận đã xóa thành công
        List<AuctionSession> watchlistAfterRemove = watchlistDAO.getWatchlist(bidderId);
        boolean containsAuctionAfter = watchlistAfterRemove.stream().anyMatch(a -> a.getId().equals(auctionId));
        assertFalse(containsAuctionAfter, "Danh sách theo dõi không được chứa phiên đấu giá sau khi xóa.");
    }

    // ── Admin Mapping ───────────────────────────────────────

    /**
     * Kiểm thử tính năng nhận diện tài khoản Admin từ DB:
     * <ol>
     *   <li>Tạo Admin với ID tiền tố "ADM" bằng {@link UserFactory#createAdmin}</li>
     *   <li>Đăng ký Admin vào DB qua {@link UserDAO#register}</li>
     *   <li>Truy vấn Admin từ DB bằng {@link UserDAO#findById} — trả về đúng kiểu Admin</li>
     *   <li>Xác thực đăng nhập bằng {@link UserDAO#authenticate} — trả về đúng kiểu Admin</li>
     *   <li>{@code isAdmin()} trả về true, role là "ADMIN"</li>
     *   <li>Dọn dẹp tài khoản admin sau khi test</li>
     * </ol>
     *
     * @throws Exception nếu đăng ký hoặc xác thực thất bại
     */
    @Test
    void testAdminMapping() throws Exception {
        String adminUsername = "test_admin_" + System.currentTimeMillis();
        Admin admin = UserFactory.createAdmin(adminUsername, "adminpass123");
        admin.setEmail("admin_test@example.com");

        try {
            // Đăng ký Admin
            boolean registered = userDAO.register(admin);
            assertTrue(registered, "Đăng ký tài khoản Admin phải thành công.");

            // Truy vấn theo ID và kiểm tra
            java.util.Optional<User> foundOpt = userDAO.findById(admin.getId());
            assertTrue(foundOpt.isPresent(), "Phải tìm thấy Admin theo ID.");
            User foundUser = foundOpt.get();
            assertTrue(foundUser instanceof Admin, "Đối tượng trả về phải thuộc lớp Admin.");
            assertTrue(foundUser.isAdmin(), "isAdmin() của Admin phải trả về true.");
            assertEquals("ADMIN", foundUser.getRole(), "Role của Admin phải là ADMIN.");

            // Xác thực đăng nhập và kiểm tra
            User authedUser = userDAO.authenticate(adminUsername, "adminpass123");
            assertNotNull(authedUser);
            assertTrue(authedUser instanceof Admin, "Đối tượng đăng nhập thành công phải thuộc lớp Admin.");
            assertTrue(authedUser.isAdmin(), "isAdmin() của Admin đăng nhập thành công phải trả về true.");
        } finally {
            // Dọn dẹp tài khoản admin vừa tạo
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                stmt.setString(1, admin.getId());
                stmt.executeUpdate();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔴 / 🟡 NEGATIVE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử Sniper Protection tại boundary 2 phút:
     * Khi end_time còn hơn 2 phút, bid không được gia hạn thêm.
     * Dùng buffer +5 giây để tránh flaky test do timing (giữa lúc ghi DB
     * và lúc placeBid kiểm tra, thời gian thực tế đã trôi).
     *
     * @throws Exception nếu đặt giá thất bại
     */
    @Test
    void testSniperProtection_Boundary_Exactly2Minutes() throws Exception {
        // Đặt end_time cách hiện tại 2 phút + 5 giây buffer
        // để sau khi xử lý DB, remaining time vẫn > 120 giây
        LocalDateTime boundaryEndTime = LocalDateTime.now().plusMinutes(2).plusSeconds(5);
        setAuctionEndTimeInDB(boundaryEndTime);

        double bidAmount = 600.0;
        boolean success = auctionDAO.placeBid(auctionId, bidderId, bidAmount);
        assertTrue(success, "Đặt giá phải thành công.");

        Optional<AuctionSession> sessionOpt = auctionDAO.findAuctionById(auctionId);
        assertTrue(sessionOpt.isPresent());
        AuctionSession updatedSession = sessionOpt.get();

        assertNotNull(updatedSession.getEndTime());
        // End_time không thay đổi (chênh lệch < 5 giây so với gốc)
        java.time.Duration diff = java.time.Duration.between(updatedSession.getEndTime(), boundaryEndTime);
        assertTrue(Math.abs(diff.getSeconds()) < 5,
                "Khi còn hơn 2 phút, bid không được kích hoạt sniper protection.");
    }

    /**
     * Kiểm thử Live Chat với tin nhắn rỗng — phải trả về {@code false} hoặc ném exception.
     */
    @Test
    void testLiveChat_EmptyMessage_Fails() {
        assertFalse(chatDAO.saveChatMessage(auctionId, bidderId, ""),
                "Tin nhắn rỗng không được lưu.");
    }

    /**
     * Kiểm thử Live Chat với tin nhắn null — phải trả về {@code false} hoặc ném exception.
     */
    @Test
    void testLiveChat_NullMessage_Fails() {
        assertFalse(chatDAO.saveChatMessage(auctionId, bidderId, null),
                "Tin nhắn null không được lưu.");
    }

    /**
     * Kiểm thử Live Chat với auction không tồn tại — lịch sử chat phải rỗng.
     */
    @Test
    void testLiveChat_NonExistentAuction_ReturnsEmpty() {
        List<ChatMessage> history = chatDAO.getChatHistory("NONEXISTENT_AUCTION");
        assertNotNull(history);
        assertTrue(history.isEmpty(), "Auction không tồn tại phải trả về danh sách rỗng.");
    }

    /**
     * Kiểm thử Watchlist thêm trùng lặp — lần thứ hai phải trả về {@code false}
     * do ràng buộc UNIQUE (user_id, auction_id).
     */
    @Test
    void testWatchlist_DuplicateEntry_Fails() {
        assertTrue(watchlistDAO.addWatchlist(bidderId, auctionId), "Lần thêm đầu phải thành công.");
        assertFalse(watchlistDAO.addWatchlist(bidderId, auctionId), "Lần thêm trùng phải thất bại.");
    }

    /**
     * Kiểm thử Watchlist xóa entry không tồn tại — trả về {@code false}.
     */
    @Test
    void testWatchlist_RemoveNonExistent_Fails() {
        assertFalse(watchlistDAO.removeWatchlist(bidderId, auctionId),
                "Xóa watchlist chưa tồn tại phải trả về false.");
    }

    /**
     * Kiểm thử Watchlist với user không tồn tại — danh sách theo dõi phải rỗng.
     */
    @Test
    void testWatchlist_NonExistentUser_ReturnsEmpty() {
        List<AuctionSession> watchlist = watchlistDAO.getWatchlist("NONEXISTENT_USER");
        assertNotNull(watchlist);
        assertTrue(watchlist.isEmpty(), "User không tồn tại phải trả về danh sách rỗng.");
    }
}
