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
 * Lớp kiểm thử (Unit Test) cho các tính năng mới được phát triển:
 * 1. Sniper Protection (Tự động gia hạn đấu giá khi đặt giá trong 2 phút cuối).
 * 2. Live Chat (Lưu và truy vấn lịch sử trò chuyện trực tuyến).
 * 3. Watchlist (Thêm, xóa và truy vấn các phiên đấu giá theo dõi).
 */
class NewFeaturesTest {

    /**
     * Khởi tạo bảng cơ sở dữ liệu nếu chưa tồn tại trước khi bắt đầu toàn bộ kiểm thử.
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
     * - Tạo tài khoản người bán và người mua thử nghiệm trong DB.
     * - Cấp số dư cho người mua để đủ điều kiện đặt giá.
     * - Tạo sản phẩm thử nghiệm trong DB.
     * - Tạo phiên đấu giá thử nghiệm ở trạng thái RUNNING.
     */
    @BeforeEach
    void setUp() throws SQLException {
        // Tạo người dùng thử nghiệm
        User seller = new User(sellerId, sellerId, "password");
        User bidder = new User(bidderId, bidderId, "password");
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
     * Dọn dẹp cơ sở dữ liệu sau mỗi bài kiểm thử để tránh ảnh hưởng đến các lần chạy sau.
     */
    @AfterEach
    void tearDown() {
        deleteTestData();
    }

    /**
     * Thực hiện xóa dữ liệu thử nghiệm trong DB theo thứ tự để tránh lỗi ràng buộc khóa ngoại (Foreign Key).
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

    /**
     * Kiểm thử tính năng Sniper Protection (Soft Close):
     * - Khi đặt giá trong vòng 2 phút cuối của phiên đấu giá, thời gian kết thúc (end_time) phải tự động gia hạn thêm 2 phút.
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

    /**
     * Kiểm thử tính năng Live Chat:
     * - Lưu tin nhắn chat thành công.
     * - Lấy lịch sử chat chứa tin nhắn đã gửi và thông tin người gửi chính xác.
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

    /**
     * Kiểm thử tính năng Watchlist:
     * - Thêm phiên đấu giá vào danh sách theo dõi.
     * - Kiểm tra danh sách theo dõi chứa phiên đấu giá đó.
     * - Kiểm tra danh sách watchers chứa người theo dõi.
     * - Xóa phiên đấu giá khỏi danh sách theo dõi và xác nhận đã xóa.
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

    /**
     * Kiểm thử tính năng nhận diện tài khoản Admin từ DB:
     * - Tạo Admin với ID có tiền tố "ADM" bằng UserFactory.
     * - Đăng ký Admin vào DB.
     * - Lấy Admin ra từ DB bằng findById và authenticate.
     * - Xác nhận đối tượng nhận được là kiểu Admin và isAdmin() trả về true.
     * - Dọn dẹp tài khoản sau khi test xong.
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
}
