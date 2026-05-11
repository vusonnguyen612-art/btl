package Network;

import Exception.*;
import Model.*;
import DAO.*;
import Factory.ItemFactory;
import Factory.UserFactory;

import java.io.*;
import java.net.*;
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

    private ServerSocket serverSocket;
    private ScheduledExecutorService penaltyScheduler;
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

    /** Khởi động server, lắng nghe kết nối và chạy penalty scheduler 30 giây. */
    public void start() throws IOException {
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

        /** Xử lý đăng ký: validate password, tạo user, lưu DB. */
        private Message handleRegister(Message message) {
            String passwordError = UserFactory.getPasswordError(message.getContent());
            if (passwordError != null) {
                return createErrorMessage(passwordError);
            }

            if (!(message.getData() instanceof String) || ((String) message.getData()).isBlank()) {
                return createErrorMessage("Username cannot be empty");
            }

            User newUser = UserFactory.createUser(
                ((String) message.getData()).trim(),
                message.getContent()
            );
            if (message.getSenderId() != null && !message.getSenderId().isBlank()) {
                newUser.setEmail(message.getSenderId().trim());
            }

            if (userDAO.existsByUsername(newUser.getUsername())) {
                return createErrorMessage("Username already exists");
            }

            if (!userDAO.register(newUser)) {
                return createErrorMessage("Could not create account");
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

                AuctionSession auction = new AuctionSession(
                    "AUC" + System.currentTimeMillis(),
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
                return createErrorMessage("Only bidders can place bids");
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
                    response.setContent("Bid placed successfully");
                    return response;
                }
                System.out.println("[handlePlaceBid] Manual bid FAILED");
                return createErrorMessage("Failed to place bid");
            } catch (Exception e) {
                System.out.println("[handlePlaceBid] Exception: " + e.getMessage());
                return createErrorMessage(e.getMessage());
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
            }
            processAutoBids(auctionId);
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

        /** Xử lý AutoBid theo second-price logic: tìm người tự động trả cao nhất và đặt giá phù hợp. */
        private void processAutoBids(String auctionId) {
            System.out.println("[AutoBid] processAutoBids called for auction: " + auctionId);
            int maxIterations = 100;
            for (int i = 0; i < maxIterations; i++) {
                Optional<AuctionSession> auctionOpt = auctionDAO.findAuctionById(auctionId);
                if (!auctionOpt.isPresent()) {
                    System.out.println("[AutoBid] Auction not found, breaking");
                    break;
                }
                AuctionSession auction = auctionOpt.get();
                if (!auction.isRunning()) {
                    System.out.println("[AutoBid] Auction not running, breaking");
                    break;
                }

                double currentPrice = auction.getCurrentPrice();
                String highestBidderId = auction.getHighestBidderId();
                System.out.println("[AutoBid] Iteration " + i + ": currentPrice=" + currentPrice + ", highestBidderId=" + highestBidderId);

                List<AutoBid> candidates;
                synchronized (autoBidLock) {
                    List<AutoBid> bids = autoBids.get(auctionId);
                    if (bids == null || bids.isEmpty()) {
                        System.out.println("[AutoBid] No auto-bids found for this auction");
                        return;
                    }
                    candidates = new ArrayList<>(bids);
                }

                candidates.sort(Comparator.comparingDouble(AutoBid::getMaxAmount).reversed());
                System.out.println("[AutoBid] Found " + candidates.size() + " auto-bid(s)");

                AutoBid best = null;
                for (AutoBid ab : candidates) {
                    boolean notWinner = !ab.getUserId().equals(highestBidderId);
                    boolean canOutbid = ab.getMaxAmount() > currentPrice;
                    System.out.println("[AutoBid] Candidate userId=" + ab.getUserId() + " max=" + ab.getMaxAmount() + " inc=" + ab.getIncrement() + " notWinner=" + notWinner + " canOutbid=" + canOutbid);
                    if (notWinner && canOutbid) {
                        best = ab;
                        break;
                    }
                }
                if (best == null) {
                    System.out.println("[AutoBid] No eligible auto-bidder found");
                    return;
                }

                double inc = best.getIncrement();
                double bidAmount = currentPrice + inc;
                System.out.println("[AutoBid] Best: userId=" + best.getUserId() + " max=" + best.getMaxAmount() + " inc=" + inc + " bidAmount=" + bidAmount);
                if (bidAmount > best.getMaxAmount()) {
                    System.out.println("[AutoBid] bidAmount exceeds max, returning");
                    return;
                }

                if (candidates.size() > 1) {
                    double secondMax = 0;
                    for (AutoBid ab : candidates) {
                        if (!ab.getUserId().equals(best.getUserId())) {
                            secondMax = Math.max(secondMax, ab.getMaxAmount());
                        }
                    }
                    if (secondMax > 0) {
                        double maxNeeded = Math.min(secondMax + inc, best.getMaxAmount());
                        System.out.println("[AutoBid] Second-price logic: secondMax=" + secondMax + " maxNeeded=" + maxNeeded);
                        if (bidAmount > maxNeeded) bidAmount = maxNeeded;
                    }
                }
                System.out.println("[AutoBid] Placing auto-bid: userId=" + best.getUserId() + " amount=" + bidAmount);

                boolean placed = auctionDAO.placeBid(auctionId, best.getUserId(), bidAmount);
                if (!placed) {
                    System.out.println("[AutoBid] placeBid FAILED");
                    return;
                }
                System.out.println("[AutoBid] placeBid SUCCESS");
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
            Optional<AuctionSession> auctionOpt = auctionDAO.findAuctionById(auctionId);
            if (!auctionOpt.isPresent()) {
                return createErrorMessage("Auction not found");
            }
            if (!auctionOpt.get().getSellerId().equals(currentUser.getId())) {
                return createErrorMessage("Only the seller can stop this auction");
            }
            boolean success = auctionDAO.stopAuction(auctionId);
            if (success) {
                AuctionSession auction = auctionOpt.get();
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
            return createErrorMessage("Failed to stop auction");
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
