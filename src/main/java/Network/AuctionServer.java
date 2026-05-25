package Network;

import Exception.*;
import Model.*;
import Model.SearchCriteria;
import DAO.*;
import Factory.ItemFactory;
import Factory.UserFactory;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Server TCP đa luồng xử lý các yêu cầu đấu giá. Quản lý AutoBid, penalty, notification. */
public class AuctionServer {
    private static final Map<String, List<AutoBid>> autoBids = new ConcurrentHashMap<>();
    private static final Object autoBidLock = new Object();
    private static final Map<String, List<Message>> pendingNotifications = new ConcurrentHashMap<>();
    private static final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private static final Set<String> sentWatchlistNotifications = ConcurrentHashMap.newKeySet();

    private ServerSocket serverSocket;
    private ScheduledExecutorService penaltyScheduler;
    private ScheduledExecutorService watchlistScheduler;
    private int port;
    private boolean running;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    /** @param port cổng lắng nghe kết nối */
    public AuctionServer(int port) {
        this.port = port;
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
        this.auctionDAO = new AuctionDAO();
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
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Server started on port " + port);

        penaltyScheduler = Executors.newSingleThreadScheduledExecutor();
        penaltyScheduler.scheduleAtFixedRate(() -> {
            try {
                List<AuctionSession> overdue = auctionDAO.findOverduePaymentAuctions();
                for (AuctionSession auction : overdue) {
                    System.out.println("Penalizing winner for overdue auction: " + auction.getId());
                    auctionDAO.penalizeWinner(auction.getId());
                }
            } catch (Exception e) {
                System.err.println("Penalty check error: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);

        watchlistScheduler = Executors.newSingleThreadScheduledExecutor();
        watchlistScheduler.scheduleAtFixedRate(() -> {
            try {
                List<AuctionSession> runningSessions = auctionDAO.findRunningAuctions();
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                WatchlistDAO watchlistDAO = new WatchlistDAO();
                for (AuctionSession session : runningSessions) {
                    if (session.getEndTime() == null) continue;
                    java.time.Duration duration = java.time.Duration.between(now, session.getEndTime());
                    long secondsLeft = duration.getSeconds();
                    if (secondsLeft > 0 && secondsLeft <= 300) {
                        List<String> watchers = watchlistDAO.getWatchers(session.getId());
                        String itemName = session.getItem() != null ? session.getItem().getName() : session.getId();
                        for (String watcherId : watchers) {
                            String key = session.getId() + "_" + watcherId;
                            if (!sentWatchlistNotifications.contains(key)) {
                                sentWatchlistNotifications.add(key);
                                Message notification = new Message(Message.Type.NOTIFICATION);
                                notification.setContent("Phiên đấu giá sản phẩm \"" + itemName + "\" mà bạn theo dõi sắp kết thúc trong vòng 5 phút!");
                                notification.setAuctionId(session.getId());
                                pendingNotifications.computeIfAbsent(watcherId, k -> new ArrayList<>()).add(notification);
                                System.out.println("[Watchlist Notification] Queued ending warning for user " + watcherId + " on auction " + session.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Watchlist notify check error: " + e.getMessage());
            }
        }, 15, 10, TimeUnit.SECONDS);

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
        private Socket socket;
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

                    if (response.getType() == Message.Type.ERROR) {
                        break;
                    }
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
                Message response;
                switch (message.getType()) {
                    case LOGIN:
                        response = handleLogin(message);
                        break;
                    case REGISTER:
                        response = handleRegister(message);
                        break;
                    case GET_AUCTIONS:
                        response = handleGetAuctions();
                        break;
                    case GET_AUCTION:
                        response = handleGetAuction(message);
                        break;
                    case CREATE_AUCTION:
                        response = handleCreateAuction(message);
                        break;
                    case START_AUCTION:
                        response = handleStartAuction(message);
                        break;
                    case PLACE_BID:
                        response = handlePlaceBid(message);
                        break;
                    case FINISH_AUCTION:
                        response = handleFinishAuction(message);
                        break;
                    case CANCEL_AUCTION:
                        response = handleCancelAuction(message);
                        break;
                    case GET_ITEMS:
                        response = handleGetItems();
                        break;
                    case CREATE_ITEM:
                        response = handleCreateItem(message);
                        break;
                    case GET_USER_BALANCE:
                        response = handleGetUserBalance(message);
                        break;
                    case DEPOSIT:
                        response = handleDeposit(message);
                        break;
                    case SET_AUTOBID:
                        response = handleSetAutoBid(message);
                        break;
                    case REMOVE_AUTOBID:
                        response = handleRemoveAutoBid(message);
                        break;
                    case STOP_AUCTION:
                        response = handleStopAuction(message);
                        break;
                    case PROCESS_PAYMENT:
                        response = handleProcessPayment(message);
                        break;
                    case LOGOUT:
                        if (currentUser != null) {
                            activeClients.remove(currentUser.getId());
                            pendingNotifications.remove(currentUser.getId());
                        }
                        currentUser = null;
                        response = new Message(Message.Type.SUCCESS);
                        response.setContent("Logged out");
                        break;
                    case GET_BID_HISTORY:
                        response = handleGetBidHistory(message);
                        break;
                    case SEARCH_AUCTIONS:
                        response = handleSearchAuctions(message);
                        break;
                    case UPDATE_ITEM:
                        response = handleUpdateItem(message);
                        break;
                    case GET_USER_AUCTIONS:
                        response = handleGetUserAuctions(message);
                        break;
                    case UPDATE_AVATAR:
                        response = handleUpdateAvatar(message);
                        break;
                    case GET_AVATAR:
                        response = handleGetAvatar();
                        break;
                    case SEND_CHAT_MESSAGE:
                        response = handleSendChatMessage(message);
                        break;
                    case GET_CHAT_HISTORY:
                        response = handleGetChatHistory(message);
                        break;
                    case ADD_WATCHLIST:
                        response = handleAddWatchlist(message);
                        break;
                    case REMOVE_WATCHLIST:
                        response = handleRemoveWatchlist(message);
                        break;
                    case GET_WATCHLIST:
                        response = handleGetWatchlist();
                        break;
                    default:
                        return createErrorMessage("Unknown message type");
                }
                if (response.getType() == Message.Type.SUCCESS && currentUser != null) {
                    List<Message> notifs = pendingNotifications.remove(currentUser.getId());
                    if (notifs != null && !notifs.isEmpty()) {
                        response.setNotifications(notifs);
                    }
                }
                return response;
            } catch (Exception e) {
                return createErrorMessage(e.getMessage());
            }
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
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Login successful");
                response.setData(user);
                return response;
            } catch (AuthenticationException e) {
                return createErrorMessage(e.getMessage());
            }
        }

        /** Xử lý đăng ký: validate password, kiểm tra trùng username, tạo user, lưu DB. */
        private Message handleRegister(Message message) {
            String passwordError = UserFactory.getPasswordError(message.getContent());
            if (passwordError != null) {
                return createErrorMessage(passwordError);
            }

            User userData = (User) message.getData();
            String username = userData.getUsername();
            String email = userData.getEmail();
            String password = message.getContent();

            if (userDAO.existsByUsername(username)) {
                return createErrorMessage("Tên đăng nhập đã tồn tại.");
            }

            User newUser = UserFactory.createUser(username, password);
            newUser.setEmail(email);

            boolean registered = userDAO.register(newUser);
            if (!registered) {
                return createErrorMessage("Đăng ký thất bại, vui lòng thử lại.");
            }

            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Registration successful");
            response.setData(newUser);
            return response;
        }

        /** Lấy danh sách tất cả phiên đấu giá. */
        private Message handleGetAuctions() {
            List<AuctionSession> auctions = auctionDAO.findAllAuctions();
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(auctions);
            return response;
        }

        /** Lấy chi tiết một phiên. */
        private Message handleGetAuction(Message message) {
            Optional<AuctionSession> auction = auctionDAO.findAuctionById(message.getAuctionId());
            if (auction.isPresent()) {
                Message response = new Message(Message.Type.SUCCESS);
                response.setData(auction.get());
                return response;
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
                if (!itemOpt.isPresent()) {
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
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Auction created");
                response.setData(auction);
                return response;
            } catch (Exception e) {
                return createErrorMessage(e.getMessage());
            }
        }

        /** Bắt đầu phiên đấu giá. */
        private Message handleStartAuction(Message message) {
            auctionDAO.startAuction(message.getAuctionId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Auction started");
            return response;
        }

        /** Xử lý đặt giá: kiểm tra quyền, ghi nhận, kích hoạt AutoBid. */
        private Message handlePlaceBid(Message message) {
            if (currentUser == null || !currentUser.isBidder()) {
                return createErrorMessage("Chỉ người mua mới có quyền đặt giá.");
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
                    processAutoBids(message.getAuctionId());
                    Message response = new Message(Message.Type.SUCCESS);
                    response.setContent("Đặt giá thành công!");
                    return response;
                }
                System.out.println("[handlePlaceBid] Manual bid FAILED");
                return createErrorMessage("Đã xảy ra lỗi không xác định khi đặt giá.");
            } catch (AuctionClosedException | InvalidBidException | InsufficientBalanceException | UnauthorizedException e) {
                System.out.println("[handlePlaceBid] Business Exception: " + e.getMessage());
                return createErrorMessage(e.getMessage());
            } catch (Exception e) {
                System.out.println("[handlePlaceBid] System Exception: " + e.getMessage());
                return createErrorMessage("Lỗi hệ thống: " + e.getMessage());
            }
        }

        /** Kết thúc phiên đấu giá. */
        private Message handleFinishAuction(Message message) {
            auctionDAO.finishAuction(message.getAuctionId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Auction finished");
            return response;
        }

        /** Hủy phiên đấu giá. */
        private Message handleCancelAuction(Message message) {
            auctionDAO.cancelAuction(message.getAuctionId(), message.getContent());
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Auction canceled");
            return response;
        }

        /** Lấy danh sách tất cả vật phẩm. */
        private Message handleGetItems() {
            List<Item> items = itemDAO.findAll();
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(items);
            return response;
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

            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Item created");
            response.setData(item);
            return response;
        }

        /** Lấy số dư tài khoản. */
        private Message handleGetUserBalance(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            java.math.BigDecimal balance = userDAO.getBalance(currentUser.getId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(balance);
            return response;
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
            System.out.println("[handleSetAutoBid] User=" + currentUser.getId() + " auction=" + auctionId + " max=" + maxAmount + " inc=" + increment);
            synchronized (autoBidLock) {
                List<AutoBid> bids = autoBids.computeIfAbsent(auctionId, k -> new ArrayList<>());
                bids.removeIf(ab -> ab.getUserId().equals(currentUser.getId()));
                bids.add(new AutoBid(currentUser.getId(), auctionId, maxAmount, increment));
                processAutoBids(auctionId);
            }
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("AutoBid set");
            return response;
        }

        /** Gỡ AutoBid của user khỏi phiên. */
        private Message handleRemoveAutoBid(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            String auctionId = message.getAuctionId();
            synchronized (autoBidLock) {
                List<AutoBid> bids = autoBids.get(auctionId);
                if (bids != null) {
                    bids.removeIf(ab -> ab.getUserId().equals(currentUser.getId()));
                }
            }
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("AutoBid removed");
            return response;
        }

        /** Xử lý AutoBid theo second-price logic 1 lần duy nhất (không loop). */
        private void processAutoBids(String auctionId) {
            System.out.println("[AutoBid] processAutoBids called for auction: " + auctionId);

            // 1. Đọc trạng thái hiện tại của phiên
            Optional<AuctionSession> auctionOpt = auctionDAO.findAuctionById(auctionId);
            if (!auctionOpt.isPresent() || !auctionOpt.get().isRunning()) {
                System.out.println("[AutoBid] Auction not found or not running");
                return;
            }

            AuctionSession auction = auctionOpt.get();
            double currentPrice = auction.getCurrentPrice();
            String highestBidderId = auction.getHighestBidderId();

            // 2. Lấy danh sách AutoBid
            List<AutoBid> candidates;
            synchronized (autoBidLock) {
                List<AutoBid> bids = autoBids.get(auctionId);
                if (bids == null || bids.isEmpty()) {
                    System.out.println("[AutoBid] No auto-bids found");
                    return;
                }
                candidates = new ArrayList<>(bids);
            }

            // 3. Sắp xếp giảm dần theo giá trần (maxAmount)
            candidates.sort(Comparator.comparingDouble(AutoBid::getMaxAmount).reversed());
            System.out.println("[AutoBid] Found " + candidates.size() + " auto-bid(s)");

            // 4. Tìm người có maxAmount cao nhất (và không phải người đang thắng)
            AutoBid best = null;
            for (AutoBid ab : candidates) {
                boolean notWinner = !ab.getUserId().equals(highestBidderId);
                boolean canOutbid = ab.getMaxAmount() > currentPrice;
                System.out.println("[AutoBid] Candidate userId=" + ab.getUserId()
                    + " max=" + ab.getMaxAmount() + " notWinner=" + notWinner + " canOutbid=" + canOutbid);
                if (notWinner && canOutbid) {
                    best = ab;
                    break;
                }
            }
            if (best == null) {
                System.out.println("[AutoBid] No eligible auto-bidder found");
                return;
            }

            // 5. Tìm maxAmount cao thứ 2 (second-price)
            double secondMax = 0;
            for (AutoBid ab : candidates) {
                if (!ab.getUserId().equals(best.getUserId())) {
                    secondMax = Math.max(secondMax, ab.getMaxAmount());
                }
            }
            System.out.println("[AutoBid] Best=" + best.getUserId() + " (max=" + best.getMaxAmount()
                + ") secondMax=" + secondMax + " currentPrice=" + currentPrice);

            // 6. Tính giá trong 1 lần duy nhất
            double increment = best.getIncrement();
            double bidAmount;

            if (secondMax > 0) {
                // Second-Price: chỉ trả = người thứ 2 + increment, nhưng không quá max của mình
                bidAmount = Math.min(best.getMaxAmount(), secondMax + increment);
            } else {
                // Chỉ 1 người dùng AutoBid: trả = currentPrice + increment
                bidAmount = currentPrice + increment;
            }

            // Đảm bảo bidAmount > currentPrice và <= max
            if (bidAmount <= currentPrice) {
                bidAmount = Math.min(currentPrice + increment, best.getMaxAmount());
            }
            if (bidAmount <= currentPrice || bidAmount > best.getMaxAmount()) {
                System.out.println("[AutoBid] Invalid bidAmount=" + bidAmount + ", returning");
                return;
            }

            // 7. Đặt giá 1 lần duy nhất
            System.out.println("[AutoBid] Placing ONE-SHOT bid: userId=" + best.getUserId()
                + " amount=" + bidAmount);
            try {
                boolean placed = auctionDAO.placeBid(auctionId, best.getUserId(), bidAmount);
                System.out.println("[AutoBid] placeBid " + (placed ? "SUCCESS" : "FAILED"));
            } catch (AuctionClosedException | InvalidBidException
                    | InsufficientBalanceException | UnauthorizedException e) {
                System.err.println("[AutoBid] Error: " + e.getMessage());
                if (e instanceof InsufficientBalanceException) {
                    synchronized (autoBidLock) {
                        List<AutoBid> bids = autoBids.get(auctionId);
                        if (bids != null) bids.remove(best);
                    }
                }
            }
        }

        /** Xử lý nạp tiền vào tài khoản. */
        private Message handleDeposit(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            java.math.BigDecimal amount = (java.math.BigDecimal) message.getData();
            if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return createErrorMessage("Invalid deposit amount");
            }
            boolean success = userDAO.addBalance(currentUser.getId(), amount);
            if (success) {
                java.math.BigDecimal newBalance = userDAO.getBalance(currentUser.getId());
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Deposit successful");
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
                if (!auctionOpt.isPresent()) {
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
            Message notification = new Message(Message.Type.NOTIFICATION);
            notification.setContent("Phiên đấu giá \"" + itemName + "\" đã được người bán gia hạn thêm 5 phút!");
            notification.setAuctionId(auctionId);
            for (Map.Entry<String, ClientHandler> entry : activeClients.entrySet()) {
                String uid = entry.getKey();
                if (!uid.equals(currentUser.getId())) {
                    pendingNotifications.computeIfAbsent(uid, k -> new ArrayList<>()).add(notification);
                }
            }
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Auction stopped, 5 minutes remaining");
            return response;
        }

        /** Xử lý thanh toán (chỉ người thắng mới được phép). */
        private Message handleProcessPayment(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            String auctionId = message.getAuctionId();
            Optional<AuctionSession> auctionOpt = auctionDAO.findAuctionById(auctionId);
            if (!auctionOpt.isPresent()) {
                return createErrorMessage("Auction not found");
            }
            AuctionSession auction = auctionOpt.get();
            if (!currentUser.getId().equals(auction.getHighestBidderId())) {
                return createErrorMessage("Only the winner can process payment");
            }
            boolean success = auctionDAO.processPayment(auctionId);
            if (success) {
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Payment successful");
                return response;
            }
            return createErrorMessage("Payment failed");
        }

        /** Lấy lịch sử đặt giá của phiên. */
        private Message handleGetBidHistory(Message message) {
            List<Bid> bids = auctionDAO.getBidHistory(message.getAuctionId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(bids);
            return response;
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
                return createErrorMessage("Bạn không phải người tạo sản phẩm này");
            }

            Optional<AuctionSession> auctionOpt = auctionDAO.findByItemId(updatedItem.getId());
            if (auctionOpt.isPresent()) {
                AuctionSession.Status status = auctionOpt.get().getStatus();
                if (status != AuctionSession.Status.OPEN) {
                    return createErrorMessage("Không thể chỉnh sửa sản phẩm sau khi phiên đấu giá đã bắt đầu");
                }
            }

            itemDAO.update(updatedItem);
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Item updated");
            return response;
        }

        private Message handleGetUserAuctions(Message message) {
            List<AuctionSession> auctions = auctionDAO.findUserParticipatedAuctions(message.getContent());
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(auctions);
            return response;
        }

        private Message handleUpdateAvatar(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            boolean success = userDAO.updateAvatarPath(currentUser.getId(), message.getContent());
            if (success) {
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Avatar updated");
                return response;
            }
            return createErrorMessage("Failed to update avatar");
        }

        private Message handleGetAvatar() {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            String avatarPath = userDAO.getAvatarPath(currentUser.getId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent(avatarPath);
            return response;
        }

        private Message handleSearchAuctions(Message message) {
            SearchCriteria criteria = (SearchCriteria) message.getData();
            List<AuctionSession> results = auctionDAO.searchAuctions(criteria);
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(results);
            return response;
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
                return createErrorMessage("Vui lòng đăng nhập để gửi tin nhắn.");
            }
            String auctionId = message.getAuctionId();
            String content = message.getContent();
            if (auctionId == null || content == null || content.trim().isEmpty()) {
                return createErrorMessage("Dữ liệu không hợp lệ.");
            }
            ChatDAO chatDAO = new ChatDAO();
            boolean success = chatDAO.saveChatMessage(auctionId, currentUser.getId(), content);
            if (success) {
                return new Message(Message.Type.SUCCESS);
            }
            return createErrorMessage("Lưu tin nhắn thất bại.");
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
                return createErrorMessage("Dữ liệu không hợp lệ.");
            }
            ChatDAO chatDAO = new ChatDAO();
            List<ChatMessage> history = chatDAO.getChatHistory(auctionId);
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(history);
            return response;
        }

        /**
         * Xử lý yêu cầu thêm một phiên đấu giá vào danh sách theo dõi của người dùng.
         *
         * @param message Thông điệp chứa ID phiên đấu giá cần theo dõi.
         * @return Phản hồi SUCCESS kèm thông báo thành công hoặc ERROR nếu thất bại.
         */
        private Message handleAddWatchlist(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Vui lòng đăng nhập để theo dõi sản phẩm.");
            }
            String auctionId = message.getAuctionId();
            if (auctionId == null) {
                return createErrorMessage("Dữ liệu không hợp lệ.");
            }
            WatchlistDAO watchlistDAO = new WatchlistDAO();
            boolean success = watchlistDAO.addWatchlist(currentUser.getId(), auctionId);
            if (success) {
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Đã thêm vào danh sách theo dõi.");
                return response;
            }
            return createErrorMessage("Thêm vào danh sách theo dõi thất bại.");
        }

        /**
         * Xử lý yêu cầu xóa một phiên đấu giá khỏi danh sách theo dõi của người dùng.
         *
         * @param message Thông điệp chứa ID phiên đấu giá cần hủy theo dõi.
         * @return Phản hồi SUCCESS kèm thông báo hoặc ERROR nếu thất bại.
         */
        private Message handleRemoveWatchlist(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Vui lòng đăng nhập.");
            }
            String auctionId = message.getAuctionId();
            if (auctionId == null) {
                return createErrorMessage("Dữ liệu không hợp lệ.");
            }
            WatchlistDAO watchlistDAO = new WatchlistDAO();
            boolean success = watchlistDAO.removeWatchlist(currentUser.getId(), auctionId);
            if (success) {
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Đã xóa khỏi danh sách theo dõi.");
                return response;
            }
            return createErrorMessage("Xóa khỏi danh sách theo dõi thất bại.");
        }

        /**
         * Xử lý yêu cầu lấy danh sách các phiên đấu giá đang được theo dõi của người dùng đang đăng nhập.
         *
         * @return Phản hồi SUCCESS chứa danh sách AuctionSession được theo dõi.
         */
        private Message handleGetWatchlist() {
            if (currentUser == null) {
                return createErrorMessage("Vui lòng đăng nhập.");
            }
            WatchlistDAO watchlistDAO = new WatchlistDAO();
            List<AuctionSession> list = watchlistDAO.getWatchlist(currentUser.getId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(list);
            return response;
        }

        private Message createErrorMessage(String error) {
            Message response = new Message(Message.Type.ERROR);
            response.setContent(error);
            return response;
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
        int port = 8989;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default 8989");
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
