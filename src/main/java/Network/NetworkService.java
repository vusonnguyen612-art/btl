package Network;

import Model.*;
import Model.SearchCriteria;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.util.List;
import java.util.function.Consumer;

/** Singleton quản lý kết nối socket tới AuctionServer, gửi/nhận Message. */
public class NetworkService {
    private static NetworkService instance;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String serverAddress;
    private int port;
    private volatile User currentUser;
    private volatile boolean connected;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;
    private Consumer<List<Message>> onNotifications;

    private NetworkService(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    /** Lấy instance singleton (mặc định localhost:8989). */
    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService("localhost", 8989);
        }
        return instance;
    }

    /** Kết nối tới server. */
    public boolean connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverAddress, port), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;
            System.out.println("Connected to server at " + serverAddress + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    /** Ngắt kết nối (gửi LOGOUT trước khi đóng socket). */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                if (currentUser != null) {
                    Message logoutMsg = new Message(Message.Type.LOGOUT);
                    sendMessage(logoutMsg);
                }
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Disconnect error: " + e.getMessage());
        } finally {
            connected = false;
            currentUser = null;
            System.out.println("Disconnected from server");
        }
    }

    /** Gửi message và nhận response. Tự động xử lý notification nếu có. */
    public Message sendMessage(Message message) {
        try {
            output.writeObject(message);
            output.flush();
            Message response = (Message) input.readObject();
            if (response.getNotifications() != null && !response.getNotifications().isEmpty() && onNotifications != null) {
                onNotifications.accept(response.getNotifications());
            }
            return response;
        } catch (SocketTimeoutException e) {
            System.err.println("Send message timeout: " + e.getMessage());
            closeQuietly();
            Message error = new Message(Message.Type.ERROR);
            error.setContent("Kết nối đến server bị timeout, vui lòng thử lại.");
            return error;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Send message error: " + e.getMessage());
            closeQuietly();
            Message error = new Message(Message.Type.ERROR);
            error.setContent("Lỗi kết nối: " + e.getMessage());
            return error;
        }
    }

    /** Đóng socket im lặng và đánh dấu ngắt kết nối. */
    private void closeQuietly() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {}
        socket = null;
        output = null;
        input = null;
    }

    /** Đăng ký callback nhận notification từ server. */
    public void setOnNotifications(Consumer<List<Message>> listener) {
        this.onNotifications = listener;
    }

    /** Gửi yêu cầu đăng nhập, tự động reconnect nếu mất kết nối. */
    public Message login(String username, String password) {
        if (!isConnected()) {
            connect();
        }
        Message message = new Message(Message.Type.LOGIN);
        message.setData(username);
        message.setContent(password);
        Message response = sendMessage(message);
        if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
            currentUser = (User) response.getData();
        }
        return response;
    }

    /** Gửi yêu cầu đăng ký tài khoản mới, tự động reconnect nếu mất kết nối. */
    public Message register(String username, String password, String email, String phone) {
        if (!isConnected()) {
            connect();
        }
        User user = new User(null, username, password);
        user.setEmail(email);
        Message message = new Message(Message.Type.REGISTER);
        message.setData(user);
        message.setContent(password);
        return sendMessage(message);
    }

    /** Lấy danh sách tất cả phiên đấu giá. */
    public Message getAuctions() {
        return sendMessage(new Message(Message.Type.GET_AUCTIONS));
    }

    /** Lấy thông tin một phiên đấu giá. */
    public Message getAuction(String auctionId) {
        Message message = new Message(Message.Type.GET_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Yêu cầu tạo phiên đấu giá mới. */
    public Message createAuction(String itemId, long durationMinutes) {
        Message message = new Message(Message.Type.CREATE_AUCTION);
        message.setItemId(itemId);
        message.setContent(String.valueOf(durationMinutes));
        return sendMessage(message);
    }

    /** Yêu cầu bắt đầu phiên đấu giá. */
    public Message startAuction(String auctionId) {
        Message message = new Message(Message.Type.START_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Gửi yêu cầu đặt giá. */
    public Message placeBid(String auctionId, double amount) {
        Message message = new Message(Message.Type.PLACE_BID);
        message.setAuctionId(auctionId);
        message.setData(amount);
        return sendMessage(message);
    }

    /** Lấy danh sách tất cả vật phẩm. */
    public Message getItems() {
        return sendMessage(new Message(Message.Type.GET_ITEMS));
    }

    /** Gửi yêu cầu tạo vật phẩm mới. */
    public Message createItem(Item item) {
        Message message = new Message(Message.Type.CREATE_ITEM);
        message.setData(item);
        return sendMessage(message);
    }

    /** Lấy số dư tài khoản. */
    public Message getUserBalance() {
        return sendMessage(new Message(Message.Type.GET_USER_BALANCE));
    }

    /** Cài đặt tự động trả giá. */
    public Message setAutoBid(String auctionId, double maxAmount, double increment) {
        Message message = new Message(Message.Type.SET_AUTOBID);
        message.setAuctionId(auctionId);
        message.setData(maxAmount);
        message.setContent(String.valueOf(increment));
        return sendMessage(message);
    }

    /** Gỡ cài đặt tự động trả giá. */
    public Message removeAutoBid(String auctionId) {
        Message message = new Message(Message.Type.REMOVE_AUTOBID);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Yêu cầu dừng/gia hạn phiên đấu giá. */
    public Message stopAuction(String auctionId) {
        Message message = new Message(Message.Type.STOP_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Gửi yêu cầu thanh toán cho phiên. */
    public Message processPayment(String auctionId) {
        Message message = new Message(Message.Type.PROCESS_PAYMENT);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Gửi yêu cầu nạp tiền. */
    public Message deposit(BigDecimal amount) {
        Message message = new Message(Message.Type.DEPOSIT);
        message.setData(amount);
        return sendMessage(message);
    }

    /** Lấy lịch sử đặt giá của phiên. */
    public Message getBidHistory(String auctionId) {
        Message message = new Message(Message.Type.GET_BID_HISTORY);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Lấy danh sách phiên đã kết thúc mà user tham gia. */
    public Message getUserAuctions(String userId) {
        Message message = new Message(Message.Type.GET_USER_AUCTIONS);
        message.setContent(userId);
        return sendMessage(message);
    }

    /** Tìm kiếm phiên đấu giá theo tiêu chí (keyword, category, status, price range, seller). */
    public Message searchAuctions(SearchCriteria criteria) {
        Message message = new Message(Message.Type.SEARCH_AUCTIONS);
        message.setData(criteria);
        return sendMessage(message);
    }

    /** Gửi yêu cầu cập nhật thông tin vật phẩm. */
    public Message updateItem(Item item) {
        Message message = new Message(Message.Type.UPDATE_ITEM);
        message.setData(item);
        return sendMessage(message);
    }

    /** @return người dùng hiện tại */
    public Message getUserBidHistory(String userId) {
        Message message = new Message(Message.Type.GET_USER_BID_HISTORY);
        message.setSenderId(userId);
        return sendMessage(message);
    }

    /** Gửi yêu cầu cập nhật ảnh đại diện. */
    public Message updateAvatar(String avatarPath) {
        Message message = new Message(Message.Type.UPDATE_AVATAR);
        message.setContent(avatarPath);
        Message response = sendMessage(message);
        if (response.getType() == Message.Type.SUCCESS && currentUser != null) {
            currentUser.setAvatarPath(avatarPath);
        }
        return response;
    }

    /** Lấy đường dẫn ảnh đại diện từ server. */
    public Message getAvatarPath() {
        return sendMessage(new Message(Message.Type.GET_AVATAR));
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /** Ghi đè người dùng hiện tại. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /** Kiểm tra trạng thái kết nối. */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
