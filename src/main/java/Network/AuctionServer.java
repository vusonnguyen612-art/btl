package Network;

import Exception.*;
import Model.*;
import Model.SearchCriteria;
import DAO.*;
import Factory.ItemFactory;
import Factory.UserFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Server TCP đa luồng xử lý các yêu cầu đấu giá. Quản lý AutoBid, penalty, notification. */
public class AuctionServer {
    private static final Map<String, List<Message>> pendingNotifications = new ConcurrentHashMap<>();
    private static final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private static final Set<String> sentWatchlistNotifications = ConcurrentHashMap.newKeySet();
    private static final int DEFAULT_PORT = 8989;
    private static final int PAYMENT_PENALTY_INITIAL_DELAY_SECONDS = 10;
    private static final int PAYMENT_PENALTY_INTERVAL_SECONDS = 30;
    private static final int WATCHLIST_INITIAL_DELAY_SECONDS = 15;
    private static final int WATCHLIST_INTERVAL_SECONDS = 10;
    private static final int WATCHLIST_WARNING_SECONDS = 300;

    private ServerSocket serverSocket;
    private ScheduledExecutorService penaltyScheduler;
    private ScheduledExecutorService watchlistScheduler;
    private final int port;
    private boolean running;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final AuctionDAO auctionDAO;
    private final AutoBidEngine autoBidEngine;

    /** @param port cổng lắng nghe kết nối */
    public AuctionServer(int port) {
        this.port = port;
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
        this.auctionDAO = new AuctionDAO();
        this.autoBidEngine = new AutoBidEngine(auctionDAO);
        System.out.println("Server initialized with DAO pattern (MySQL)");
    }

    /**
     * Khởi tạo tự động các bảng mới trong cơ sở dữ liệu nếu chúng chưa tồn tại.
     * Bảng bao gồm: chat_messages và watchlist.
     */
    private void initializeDatabaseTables() {
        System.out.println("Initializing database tables...");
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
             Statement stmt = conn.createStatement()) {
            stmt.execute(createChatMessagesTable);
            stmt.execute(createWatchlistTable);
            System.out.println("Database tables checked/initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Lỗi khởi tạo bảng cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Khởi động server, lắng nghe kết nối và chạy penalty scheduler 30 giây. */
    public void start() throws IOException {
        initializeDatabaseTables();
        seedDefaultAdmin();
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Server started on port " + port);

        startSchedulers();

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                new ClientHandler(clientSocket).start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    /** Tạo/reset tài khoản admin mặc định với password đã hash đúng. */
    private void seedDefaultAdmin() {
        String adminUsername = "admin";
        String adminPassword = "admin123";

        String checkSql = "SELECT id FROM users WHERE BINARY username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, adminUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    userDAO.resetPassword(adminUsername, adminPassword);
                    System.out.println("Admin password reset. username=" + adminUsername + ", password=" + adminPassword);
                } else {
                    Admin admin = UserFactory.createAdmin(adminUsername, adminPassword);
                    admin.setEmail("admin@auction.com");
                    if (userDAO.register(admin)) {
                        System.out.println("Admin account created. username=" + adminUsername + ", password=" + adminPassword);
                    } else {
                        System.err.println("Failed to create admin account.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error seeding admin account: " + e.getMessage());
        }
    }

    private void startSchedulers() {
        penaltyScheduler = Executors.newSingleThreadScheduledExecutor();
        penaltyScheduler.scheduleAtFixedRate(
            this::penalizeOverduePayments,
            PAYMENT_PENALTY_INITIAL_DELAY_SECONDS,
            PAYMENT_PENALTY_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        watchlistScheduler = Executors.newSingleThreadScheduledExecutor();
        watchlistScheduler.scheduleAtFixedRate(
            this::notifyWatchersBeforeAuctionEnds,
            WATCHLIST_INITIAL_DELAY_SECONDS,
            WATCHLIST_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    private void penalizeOverduePayments() {
        try {
            List<AuctionSession> overdue = auctionDAO.findOverduePaymentAuctions();
            for (AuctionSession auction : overdue) {
                System.out.println("Penalizing winner for overdue auction: " + auction.getId());
                auctionDAO.penalizeWinner(auction.getId());
            }
        } catch (Exception e) {
            System.err.println("Penalty check error: " + e.getMessage());
        }
    }

    private void notifyWatchersBeforeAuctionEnds() {
        try {
            List<AuctionSession> runningSessions = auctionDAO.findRunningAuctions();
            LocalDateTime now = LocalDateTime.now();
            WatchlistDAO watchlistDAO = new WatchlistDAO();
            for (AuctionSession session : runningSessions) {
                queueEndingSoonNotification(session, now, watchlistDAO);
            }
        } catch (Exception e) {
            System.err.println("Watchlist notify check error: " + e.getMessage());
        }
    }

    private void queueEndingSoonNotification(AuctionSession session, LocalDateTime now, WatchlistDAO watchlistDAO) {
        if (session.getEndTime() == null) {
            return;
        }

        long secondsLeft = Duration.between(now, session.getEndTime()).getSeconds();
        if (secondsLeft <= 0 || secondsLeft > WATCHLIST_WARNING_SECONDS) {
            return;
        }

        String itemName = session.getItem() != null ? session.getItem().getName() : session.getId();
        for (String watcherId : watchlistDAO.getWatchers(session.getId())) {
            String key = session.getId() + "_" + watcherId;
            if (sentWatchlistNotifications.add(key)) {
                Message notification = createNotification(
                    "Phiên đấu giá sản phẩm \"" + itemName + "\" mà bạn theo dõi sắp kết thúc trong vòng 5 phút!",
                    session.getId()
                );
                queueNotification(watcherId, notification);
                System.out.println("[Watchlist Notification] Queued ending warning for user " + watcherId + " on auction " + session.getId());
            }
        }
    }

    private static Message createNotification(String content, String auctionId) {
        return MessageFactory.notification(content, auctionId);
    }

    private static void queueNotification(String userId, Message notification) {
        pendingNotifications
            .computeIfAbsent(userId, key -> Collections.synchronizedList(new ArrayList<>()))
            .add(notification);
    }

    private static List<Message> drainPendingNotifications(String userId) {
        List<Message> notifications = pendingNotifications.remove(userId);
        if (notifications == null || notifications.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (notifications) {
            return new ArrayList<>(notifications);
        }
    }

    /** Dừng server và giải phóng tài nguyên. */
    public void stop() {
        running = false;
        if (penaltyScheduler != null) {
            penaltyScheduler.shutdown();
        }
        if (watchlistScheduler != null) {
            watchlistScheduler.shutdown();
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    /** Xử lý kết nối từ một client riêng biệt. */
    private class ClientHandler extends Thread {
        private final Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private User currentUser;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = new ObjectInputStream(socket.getInputStream());
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();

                Message message;
                while ((message = (Message) input.readObject()) != null) {
                    Message response = processMessage(message);
                    output.writeObject(response);
                    output.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                close();
            }
        }

        /** Phân loại message và gọi handler tương ứng. */
        private Message processMessage(Message message) {
            try {
                Message response = switch (message.getType()) {
                    case LOGIN -> handleLogin(message);
                    case REGISTER -> handleRegister(message);
                    case GET_AUCTIONS -> handleGetAuctions();
                    case GET_AUCTION -> handleGetAuction(message);
                    case CREATE_AUCTION -> handleCreateAuction(message);
                    case START_AUCTION -> handleStartAuction(message);
                    case PLACE_BID -> handlePlaceBid(message);
                    case FINISH_AUCTION -> handleFinishAuction(message);
                    case CANCEL_AUCTION -> handleCancelAuction(message);
                    case GET_ITEMS -> handleGetItems();
                    case CREATE_ITEM -> handleCreateItem(message);
                    case GET_USER_BALANCE -> handleGetUserBalance(message);
                    case DEPOSIT -> handleDeposit(message);
                    case SET_AUTOBID -> handleSetAutoBid(message);
                    case REMOVE_AUTOBID -> handleRemoveAutoBid(message);
                    case STOP_AUCTION -> handleStopAuction(message);
                    case PROCESS_PAYMENT -> handleProcessPayment(message);
                    case LOGOUT -> handleLogout();
                    case GET_BID_HISTORY -> handleGetBidHistory(message);
                    case SEARCH_AUCTIONS -> handleSearchAuctions(message);
                    case UPDATE_ITEM -> handleUpdateItem(message);
                    case GET_USER_AUCTIONS -> handleGetUserAuctions(message);
                    case UPDATE_AVATAR -> handleUpdateAvatar(message);
                    case GET_AVATAR -> handleGetAvatar();
                    case SEND_CHAT_MESSAGE -> handleSendChatMessage(message);
                    case GET_CHAT_HISTORY -> handleGetChatHistory(message);
                    case ADD_WATCHLIST -> handleAddWatchlist(message);
                    case REMOVE_WATCHLIST -> handleRemoveWatchlist(message);
                    case GET_WATCHLIST -> handleGetWatchlist();
                    case GET_USERS -> handleGetUsers();
                    case DELETE_USER -> handleDeleteUser(message);
                    case BLOCK_USER -> handleBlockUser(message);
                    case DELETE_ITEM -> handleDeleteItem(message);
                    case CHANGE_PASSWORD -> handleChangePassword(message);
                    default -> createErrorMessage("Unknown message type");
                };
                if (response.getType() == Message.Type.SUCCESS && currentUser != null) {
                    List<Message> notifications = drainPendingNotifications(currentUser.getId());
                    if (!notifications.isEmpty()) {
                        response.setNotifications(notifications);
                    }
                }
                return response;
            } catch (Exception e) {
                return createErrorMessage(e.getMessage());
            }
        }

        private Message handleLogout() {
            if (currentUser != null) {
                activeClients.remove(currentUser.getId());
                pendingNotifications.remove(currentUser.getId());
            }
            currentUser = null;
            return createSuccessMessage("Logged out");
        }

        /** Xử lý đăng nhập: xác thực user, đăng ký notification. */
        private Message handleLogin(Message message) {
            try {
                User user = userDAO.authenticate(
                    (String) message.getData(),
                    message.getContent()
                );
                currentUser = user;
                activeClients.put(user.getId(), this);
                System.out.println("[Login] User " + user.getId() + " registered for notifications");
                Message response = createSuccessMessage("Login successful");
                response.setData(user);
                return response;
            } catch (AuthenticationException e) {
                return createErrorMessage(e.getMessage());
            }
        }

        /** Xử lý đăng ký: validate password, kiểm tra trùng username, tạo user, lưu DB. */
        private Message handleRegister(Message message) {
            String content = message.getContent();
            int separatorIndex = content.lastIndexOf('|');
            String password;
            String role = "BIDDER_SELLER";
            if (separatorIndex > 0 && separatorIndex < content.length() - 1) {
                password = content.substring(0, separatorIndex);
                role = content.substring(separatorIndex + 1);
            } else {
                password = content;
            }

            String passwordError = UserFactory.getPasswordError(password);
            if (passwordError != null) {
                return createErrorMessage(passwordError);
            }

            User userData = (User) message.getData();
            String username = userData.getUsername();
            String email = userData.getEmail();

            if (userDAO.existsByUsername(username)) {
                return createErrorMessage("TÃªn Ä‘Äƒng nháº­p Ä‘Ã£ tá»“n táº¡i.");
            }

            User newUser = UserFactory.createUser(username, password);
            newUser.setEmail(email);
            applyRole(newUser, role);

            boolean registered = userDAO.register(newUser);
            if (!registered) {
                return createErrorMessage("ÄÄƒng kÃ½ tháº¥t báº¡i, vui lÃ²ng thá»­ láº¡i.");
            }

            Message response = createSuccessMessage("Registration successful");
            response.setData(newUser);
            return response;
        }

        private void applyRole(User user, String role) {
            switch (role) {
                case "BIDDER":
                    user.setSeller(false);
                    user.setBidder(true);
                    break;
                case "SELLER":
                    user.setSeller(true);
                    user.setBidder(false);
                    break;
                default:
                    user.setSeller(true);
                    user.setBidder(true);
                    break;
            }
        }

        /** Lấy danh sách tất cả phiên đấu giá. */
        private Message handleGetAuctions() {
            return createSuccessMessage(auctionDAO.findAllAuctions());
        }

        /** Lấy chi tiết một phiên. */
        private Message handleGetAuction(Message message) {
            Optional<AuctionSession> auction = auctionDAO.findAuctionById(message.getAuctionId());
            if (auction.isPresent()) {
                return createSuccessMessage(auction.get());
            }
            return createErrorMessage("Auction not found");
        }

        /** Tạo phiên đấu giá mới (seller only). */
        private Message handleCreateAuction(Message message) {
            if (currentUser == null || !currentUser.isSeller()) {
                return createErrorMessage("Only sellers can create auctions");
            }

            try {
                Optional<Item> itemOpt = itemDAO.findById(message.getItemId());
                if (itemOpt.isEmpty()) {
                    return createErrorMessage("Item not found");
                }
                Item item = itemOpt.get();

                String auctionId = "AUC" + System.currentTimeMillis() + "_" + String.format("%03d", (int)(Math.random() * 1000));
                AuctionSession auction = new AuctionSession(
                    auctionId,
                    item,
                    currentUser.getId(),
                    item.getStartPrice(),
                    Long.parseLong(message.getContent())
                );
                auctionDAO.saveAuction(auction);
                Message response = createSuccessMessage("Auction created");
                response.setData(auction);
                return response;
            } catch (Exception e) {
                return createErrorMessage(e.getMessage());
            }
        }

        /** Bắt đầu phiên đấu giá. */
        private Message handleStartAuction(Message message) {
            auctionDAO.startAuction(message.getAuctionId());
            return createSuccessMessage("Auction started");
        }

        /** Xử lý đặt giá: kiểm tra quyền, ghi nhận, kích hoạt AutoBid. */
        private Message handlePlaceBid(Message message) {
            if (currentUser == null || !currentUser.isBidder()) {
                return createErrorMessage("Chá»‰ ngÆ°á»i mua má»›i cÃ³ quyá»n Ä‘áº·t giÃ¡.");
            }

            try {
                double amount = (Double) message.getData();
                System.out.println("[handlePlaceBid] User=" + currentUser.getId() + " auction=" + message.getAuctionId() + " amount=" + amount);
                boolean success = auctionDAO.placeBid(
                    message.getAuctionId(),
                    currentUser.getId(),
                    amount
                );
                if (success) {
                    System.out.println("[handlePlaceBid] Manual bid SUCCESS, calling processAutoBids");
                    autoBidEngine.processAutoBids(message.getAuctionId());
                    return createSuccessMessage("Äáº·t giÃ¡ thÃ nh cÃ´ng!");
                }
                System.out.println("[handlePlaceBid] Manual bid FAILED");
                return createErrorMessage("ÄÃ£ xáº£y ra lá»—i khÃ´ng xÃ¡c Ä‘á»‹nh khi Ä‘áº·t giÃ¡.");
            } catch (AuctionClosedException | InvalidBidException | InsufficientBalanceException | UnauthorizedException e) {
                System.out.println("[handlePlaceBid] Business Exception: " + e.getMessage());
                return createErrorMessage(e.getMessage());
            } catch (Exception e) {
                System.out.println("[handlePlaceBid] System Exception: " + e.getMessage());
                return createErrorMessage("Lá»—i há»‡ thá»‘ng: " + e.getMessage());
            }
        }

        /** Kết thúc phiên đấu giá. */
        private Message handleFinishAuction(Message message) {
            auctionDAO.finishAuction(message.getAuctionId());
            return createSuccessMessage("Auction finished");
        }

        /** Hủy phiên đấu giá. */
        private Message handleCancelAuction(Message message) {
            auctionDAO.cancelAuction(message.getAuctionId(), message.getContent());
            return createSuccessMessage("Auction canceled");
        }

        /** Lấy danh sách tất cả vật phẩm. */
        private Message handleGetItems() {
            return createSuccessMessage(itemDAO.findAll());
        }

        /** Tạo vật phẩm mới (seller only). */
        private Message handleCreateItem(Message message) {
            if (currentUser == null || !currentUser.isSeller()) {
                return createErrorMessage("Only sellers can create items");
            }

            Item item = (Item) message.getData();
            item = ItemFactory.createItem(
                item.getCategory(),
                item.getName(),
                item.getDescription(),
                item.getStartPrice(),
                currentUser.getId()
            );
            itemDAO.save(item);

            Message response = createSuccessMessage("Item created");
            response.setData(item);
            return response;
        }

        /** Lấy số dư tài khoản. */
        private Message handleGetUserBalance(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            return createSuccessMessage(userDAO.getBalance(currentUser.getId()));
        }

        /** Cài đặt AutoBid và chạy xử lý ngay. */
        private Message handleSetAutoBid(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            String auctionId = message.getAuctionId();
            double maxAmount = (Double) message.getData();
            if (maxAmount <= 0) {
                return createErrorMessage("Invalid max amount");
            }
            double increment = 1.0;
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                try {
                    increment = Double.parseDouble(message.getContent());
                    if (increment <= 0) increment = 1.0;
                } catch (NumberFormatException e) {
                    increment = 1.0;
                }
            }
            autoBidEngine.setAutoBid(currentUser.getId(), auctionId, maxAmount, increment);
            return createSuccessMessage("AutoBid set");
        }

        /** Gỡ AutoBid của user khỏi phiên. */
        private Message handleRemoveAutoBid(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            autoBidEngine.removeAutoBid(currentUser.getId(), message.getAuctionId());
            return createSuccessMessage("AutoBid removed");
        }

        /** Xử lý nạp tiền vào tài khoản. */
        private Message handleDeposit(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            BigDecimal amount = (BigDecimal) message.getData();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return createErrorMessage("Invalid deposit amount");
            }
            boolean success = userDAO.addBalance(currentUser.getId(), amount);
            if (success) {
                BigDecimal newBalance = userDAO.getBalance(currentUser.getId());
                Message response = createSuccessMessage("Deposit successful");
                response.setData(newBalance);
                return response;
            }
            return createErrorMessage("Deposit failed");
        }

        /** Dừng phiên đấu giá (seller only) và gửi notification đến các client khác. */
        private Message handleStopAuction(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            String auctionId = message.getAuctionId();
            AuctionSession auction;
            synchronized (auctionDAO) {
                Optional<AuctionSession> auctionOpt = auctionDAO.findAuctionById(auctionId);
                if (auctionOpt.isEmpty()) {
                    return createErrorMessage("Auction not found");
                }
                if (!auctionOpt.get().getSellerId().equals(currentUser.getId())) {
                    return createErrorMessage("Only the seller can stop this auction");
                }
                auction = auctionOpt.get();
                boolean success = auctionDAO.stopAuction(auctionId);
                if (!success) {
                    return createErrorMessage("Failed to stop auction");
                }
            }
            String itemName = auction.getItem() != null ? auction.getItem().getName() : auctionId;
            Message notification = createNotification(
                "Phiên đấu giá \"" + itemName + "\" đã được người bán gia hạn thêm 5 phút!",
                auctionId
            );
            for (Map.Entry<String, ClientHandler> entry : activeClients.entrySet()) {
                String uid = entry.getKey();
                if (!uid.equals(currentUser.getId())) {
                    queueNotification(uid, notification);
                }
            }
            return createSuccessMessage("Auction stopped, 5 minutes remaining");
        }

        /** Xử lý thanh toán (chỉ người thắng mới được phép). */
        private Message handleProcessPayment(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            String auctionId = message.getAuctionId();
            Optional<AuctionSession> auctionOpt = auctionDAO.findAuctionById(auctionId);
            if (auctionOpt.isEmpty()) {
                return createErrorMessage("Auction not found");
            }
            AuctionSession auction = auctionOpt.get();
            if (!currentUser.getId().equals(auction.getHighestBidderId())) {
                return createErrorMessage("Only the winner can process payment");
            }
            boolean success = auctionDAO.processPayment(auctionId);
            if (success) {
                return createSuccessMessage("Payment successful");
            }
            return createErrorMessage("Payment failed");
        }

        /** Lấy lịch sử đặt giá của phiên. */
        private Message handleGetBidHistory(Message message) {
            return createSuccessMessage(auctionDAO.getBidHistory(message.getAuctionId()));
        }

        /** Xử lý cập nhật vật phẩm: chỉ người bán và phiên chưa bắt đầu (OPEN) mới được sửa. */
        private Message handleUpdateItem(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            Item updatedItem = (Item) message.getData();
            if (updatedItem == null || updatedItem.getId() == null) {
                return createErrorMessage("Invalid item data");
            }

            Optional<Item> existingOpt = itemDAO.findById(updatedItem.getId());
            if (existingOpt.isEmpty()) {
                return createErrorMessage("Item not found");
            }
            if (!existingOpt.get().getSellerId().equals(currentUser.getId())) {
                return createErrorMessage("Báº¡n khÃ´ng pháº£i ngÆ°á»i táº¡o sáº£n pháº©m nÃ y");
            }

            Optional<AuctionSession> auctionOpt = auctionDAO.findByItemId(updatedItem.getId());
            if (auctionOpt.isPresent()) {
                AuctionSession.Status status = auctionOpt.get().getStatus();
                if (status != AuctionSession.Status.OPEN) {
                    return createErrorMessage("KhÃ´ng thá»ƒ chá»‰nh sá»­a sáº£n pháº©m sau khi phiÃªn Ä‘áº¥u giÃ¡ Ä‘Ã£ báº¯t Ä‘áº§u");
                }
            }

            itemDAO.update(updatedItem);
            return createSuccessMessage("Item updated");
        }

        private Message handleGetUserAuctions(Message message) {
            return createSuccessMessage(auctionDAO.findUserParticipatedAuctions(message.getContent()));
        }

        private Message handleUpdateAvatar(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            boolean success = userDAO.updateAvatarPath(currentUser.getId(), message.getContent());
            if (success) {
                return createSuccessMessage("Avatar updated");
            }
            return createErrorMessage("Failed to update avatar");
        }

        private Message handleGetAvatar() {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            String avatarPath = userDAO.getAvatarPath(currentUser.getId());
            return createSuccessMessage(avatarPath);
        }

        private Message handleSearchAuctions(Message message) {
            SearchCriteria criteria = (SearchCriteria) message.getData();
            return createSuccessMessage(auctionDAO.searchAuctions(criteria));
        }

        /**
         * Xử lý thông điệp gửi tin nhắn chat của người dùng trong phòng đấu giá.
         * Xác thực đăng nhập, kiểm tra dữ liệu và lưu vào cơ sở dữ liệu qua ChatDAO.
         *
         * @param message Thông điệp chứa ID phòng đấu giá và nội dung tin nhắn.
         * @return Phản hồi SUCCESS nếu lưu tin nhắn thành công, ngược lại phản hồi ERROR.
         */
        private Message handleSendChatMessage(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Vui lÃ²ng Ä‘Äƒng nháº­p Ä‘á»ƒ gá»­i tin nháº¯n.");
            }
            String auctionId = message.getAuctionId();
            String content = message.getContent();
            if (auctionId == null || content == null || content.trim().isEmpty()) {
                return createErrorMessage("Dá»¯ liá»‡u khÃ´ng há»£p lá»‡.");
            }
            ChatDAO chatDAO = new ChatDAO();
            boolean success = chatDAO.saveChatMessage(auctionId, currentUser.getId(), content);
            if (success) {
                return createSuccessMessage();
            }
            return createErrorMessage("LÆ°u tin nháº¯n tháº¥t báº¡i.");
        }

        /**
         * Xử lý thông điệp yêu cầu lấy lịch sử tin nhắn trò chuyện của một phiên đấu giá.
         *
         * @param message Thông điệp chứa ID phiên đấu giá.
         * @return Phản hồi SUCCESS chứa danh sách các ChatMessage trong phần dữ liệu.
         */
        private Message handleGetChatHistory(Message message) {
            String auctionId = message.getAuctionId();
            if (auctionId == null) {
                return createErrorMessage("Dá»¯ liá»‡u khÃ´ng há»£p lá»‡.");
            }
            ChatDAO chatDAO = new ChatDAO();
            List<ChatMessage> history = chatDAO.getChatHistory(auctionId);
            return createSuccessMessage(history);
        }

        /**
         * Xử lý yêu cầu thêm một phiên đấu giá vào danh sách theo dõi của người dùng.
         *
         * @param message Thông điệp chứa ID phiên đấu giá cần theo dõi.
         * @return Phản hồi SUCCESS kèm thông báo thành công hoặc ERROR nếu thất bại.
         */
        private Message handleAddWatchlist(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Vui lÃ²ng Ä‘Äƒng nháº­p Ä‘á»ƒ theo dÃµi sáº£n pháº©m.");
            }
            String auctionId = message.getAuctionId();
            if (auctionId == null) {
                return createErrorMessage("Dá»¯ liá»‡u khÃ´ng há»£p lá»‡.");
            }
            WatchlistDAO watchlistDAO = new WatchlistDAO();
            boolean success = watchlistDAO.addWatchlist(currentUser.getId(), auctionId);
            if (success) {
                return createSuccessMessage("ÄÃ£ thÃªm vÃ o danh sÃ¡ch theo dÃµi.");
            }
            return createErrorMessage("ThÃªm vÃ o danh sÃ¡ch theo dÃµi tháº¥t báº¡i.");
        }

        /**
         * Xử lý yêu cầu xóa một phiên đấu giá khỏi danh sách theo dõi của người dùng.
         *
         * @param message Thông điệp chứa ID phiên đấu giá cần hủy theo dõi.
         * @return Phản hồi SUCCESS kèm thông báo hoặc ERROR nếu thất bại.
         */
        private Message handleRemoveWatchlist(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Vui lÃ²ng Ä‘Äƒng nháº­p.");
            }
            String auctionId = message.getAuctionId();
            if (auctionId == null) {
                return createErrorMessage("Dá»¯ liá»‡u khÃ´ng há»£p lá»‡.");
            }
            WatchlistDAO watchlistDAO = new WatchlistDAO();
            boolean success = watchlistDAO.removeWatchlist(currentUser.getId(), auctionId);
            if (success) {
                return createSuccessMessage("ÄÃ£ xÃ³a khá»i danh sÃ¡ch theo dÃµi.");
            }
            return createErrorMessage("XÃ³a khá»i danh sÃ¡ch theo dÃµi tháº¥t báº¡i.");
        }

        /**
         * Xử lý yêu cầu lấy danh sách các phiên đấu giá đang được theo dõi của người dùng đang đăng nhập.
         *
         * @return Phản hồi SUCCESS chứa danh sách AuctionSession được theo dõi.
         */
        private Message handleGetWatchlist() {
            if (currentUser == null) {
                return createErrorMessage("Vui lÃ²ng Ä‘Äƒng nháº­p.");
            }
            WatchlistDAO watchlistDAO = new WatchlistDAO();
            return createSuccessMessage(watchlistDAO.getWatchlist(currentUser.getId()));
        }

        private Message handleGetUsers() {
            if (currentUser == null || !currentUser.isAdmin()) {
                return createErrorMessage("Chi admin moi co quyen xem danh sach.");
            }
            return createSuccessMessage(userDAO.findAllUsers());
        }

        private Message handleDeleteUser(Message message) {
            if (currentUser == null || !currentUser.isAdmin()) {
                return createErrorMessage("Chi admin moi co quyen xoa nguoi dung.");
            }
            String userId = message.getContent();
            if (userId == null || userId.isBlank()) {
                return createErrorMessage("ID khong hop le.");
            }
            boolean success = userDAO.deleteUser(userId);
            if (success) return createSuccessMessage("Da xoa nguoi dung.");
            return createErrorMessage("Khong the xoa nguoi dung.");
        }

        private Message handleBlockUser(Message message) {
            if (currentUser == null || !currentUser.isAdmin()) {
                return createErrorMessage("Chi admin moi co quyen chan nguoi dung.");
            }
            String userId = message.getContent();
            boolean blocked = (Boolean) message.getData();
            if (userId == null || userId.isBlank()) {
                return createErrorMessage("ID khong hop le.");
            }
            boolean success = userDAO.blockUser(userId, blocked);
            if (success) {
                return createSuccessMessage(blocked ? "Da chan nguoi dung." : "Da bo chan nguoi dung.");
            }
            return createErrorMessage("Khong the thay doi trang thai.");
        }

        private Message handleDeleteItem(Message message) {
            if (currentUser == null || !currentUser.isAdmin()) {
                return createErrorMessage("Chi admin moi co quyen xoa vat pham.");
            }
            String itemId = message.getContent();
            if (itemId == null || itemId.isBlank()) {
                return createErrorMessage("ID khong hop le.");
            }
            boolean success = itemDAO.delete(itemId);
            if (success) return createSuccessMessage("Da xoa vat pham.");
            return createErrorMessage("Khong the xoa vat pham.");
        }

        private Message handleChangePassword(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Vui long dang nhap.");
            }
            String content = message.getContent();
            if (content == null || !content.contains("|")) {
                return createErrorMessage("Du lieu khong hop le.");
            }
            int sep = content.indexOf("|");
            String oldPassword = content.substring(0, sep);
            String newPassword = content.substring(sep + 1);
            boolean success = userDAO.changePassword(currentUser.getUsername(), oldPassword, newPassword);
            if (success) {
                return createSuccessMessage("Doi mat khau thanh cong.");
            }
            return createErrorMessage("Mat khau cu khong dung.");
        }

        private Message createErrorMessage(String error) {
            return MessageFactory.error(error);
        }

        private Message createSuccessMessage(String content) {
            return MessageFactory.success(content);
        }

        private Message createSuccessMessage() {
            return MessageFactory.success();
        }

        private Message createSuccessMessage(Object data) {
            return MessageFactory.success(data);
        }

        private void close() {
            if (currentUser != null) {
                activeClients.remove(currentUser.getId());
                pendingNotifications.remove(currentUser.getId());
                System.out.println("[Disconnect] User " + currentUser.getId() + " removed from notifications");
            }
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client handler: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default " + DEFAULT_PORT);
            }
        }
        AuctionServer server = new AuctionServer(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
