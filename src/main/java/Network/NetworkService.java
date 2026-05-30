package Network;

import Model.*;
import Model.SearchCriteria;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Singleton quản lý kết nối socket tới {@link AuctionServer}, gửi/nhận {@link Message}.
 * <p>
 * Cung cấp cơ chế auto-reconnect: nếu mất kết nối khi gửi message, tự động kết nối lại
 * và thực hiện relogin với thông tin đăng nhập đã lưu. Hỗ trợ callback notification
 * qua {@link #setOnNotifications(java.util.function.Consumer)}.
 * </p>
 */
public class NetworkService {
    private static NetworkService instance;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String serverAddress;
    private int port;
    private volatile User currentUser;
    private volatile boolean connected;
    private String lastUsername;
    private String lastPassword;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;
    private Consumer<List<Message>> onNotifications;

    private NetworkService(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    /**
     * Lấy instance singleton của NetworkService. Mặc định kết nối tới localhost:8989.
     *
     * @return Instance duy nhất của NetworkService.
     */
    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService("localhost", 8989);
        }
        return instance;
    }

    /**
     * Kết nối tới server qua địa chỉ và cổng đã cấu hình.
     * Sử dụng {@link #CONNECT_TIMEOUT} và {@link #READ_TIMEOUT} cho socket.
     *
     * @return true nếu kết nối thành công, false nếu thất bại.
     */
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

    /**
     * Ngắt kết nối khỏi server. Gửi message LOGOUT trước khi đóng socket
     * nếu người dùng đang đăng nhập, sau đó reset trạng thái kết nối và thông tin user.
     */
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
            lastUsername = null;
            lastPassword = null;
            System.out.println("Disconnected from server");
        }
    }

    /**
     * Gửi message đến server và nhận response.
     * <p>
     * Tự động xử lý mất kết nối (auto-reconnect + relogin) nếu socket đã đóng.
     * Nếu response có notifications đính kèm, gọi callback {@link #onNotifications} nếu đã đăng ký.
     * </p>
     *
     * @param message Message cần gửi đến server.
     * @return Message phản hồi từ server, hoặc ERROR nếu lỗi kết nối hoặc timeout.
     */
    public Message sendMessage(Message message) {
        if (!isConnected()) {
            if (message.getType() != Message.Type.LOGIN && message.getType() != Message.Type.REGISTER) {
                System.out.println("[Auto-Reconnect] Socket offline. Attempting to reconnect...");
                boolean reconnected = connect();
                if (reconnected) {
                    if (currentUser != null && lastUsername != null && lastPassword != null) {
                        System.out.println("[Auto-Reconnect] Connection re-established. Attempting auto-relogin...");
                        Message reloginMsg = new Message(Message.Type.LOGIN);
                        reloginMsg.setData(lastUsername);
                        reloginMsg.setContent(lastPassword);
                        try {
                            output.writeObject(reloginMsg);
                            output.flush();
                            Message reloginRes = (Message) input.readObject();
                            if (reloginRes.getType() == Message.Type.SUCCESS) {
                                System.out.println("[Auto-Reconnect] Auto-relogin successful.");
                                if (reloginRes.getData() != null) {
                                    currentUser = (User) reloginRes.getData();
                                }
                            } else {
                                System.err.println("[Auto-Reconnect] Auto-relogin failed: " + reloginRes.getContent());
                                closeQuietly();
                                Message error = new Message(Message.Type.ERROR);
                                error.setContent("Kết nối lại thành công nhưng không thể tự động đăng nhập: " + reloginRes.getContent());
                                return error;
                            }
                        } catch (Exception e) {
                            System.err.println("[Auto-Reconnect] Exception during auto-relogin: " + e.getMessage());
                            closeQuietly();
                            Message error = new Message(Message.Type.ERROR);
                            error.setContent("Không thể đăng nhập lại sau khi kết nối lại: " + e.getMessage());
                            return error;
                        }
                    }
                } else {
                    System.err.println("[Auto-Reconnect] Reconnection failed.");
                    Message error = new Message(Message.Type.ERROR);
                    error.setContent("Mất kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối mạng.");
                    return error;
                }
            }
        }

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

    /**
     * Đóng socket im lặng (không throw exception), đặt connected = false
     * và gán null cho các stream/socket.
     */
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

    /**
     * Đăng ký callback nhận notification từ server.
     * Callback được gọi mỗi khi response có chứa danh sách notifications.
     *
     * @param listener Consumer nhận danh sách {@link Message} notification, hoặc null để huỷ đăng ký.
     */
    public void setOnNotifications(Consumer<List<Message>> listener) {
        this.onNotifications = listener;
    }

    /**
     * Gửi yêu cầu đăng nhập đến server.
     * Tự động kết nối lại nếu mất kết nối. Lưu thông tin username/password
     * để phục vụ auto-relogin khi reconnect.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return Message phản hồi từ server.
     */
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
            this.lastUsername = username;
            this.lastPassword = password;
        }
        return response;
    }

    /**
     * Gửi yêu cầu đăng ký tài khoản mới tới server.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @param email    Địa chỉ email.
     * @param phone    Số điện thoại (hiện tại chưa dùng trong message).
     * @param role     Vai trò: "BIDDER", "SELLER", hoặc "BIDDER_SELLER" (mặc định).
     * @return Message phản hồi từ server.
     */
    public Message register(String username, String password, String email, String phone, String role) {
        if (!isConnected()) {
            connect();
        }
        if (role == null || role.isBlank()) {
            role = "BIDDER_SELLER";
        }
        User user = new RegularUser(null, username, password);
        user.setEmail(email);
        Message message = new Message(Message.Type.REGISTER);
        message.setData(user);
        // content = "password|role" để server parse
        message.setContent(password + "|" + role);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu đăng ký (overload, giữ backward compatibility).
     */
    public Message register(String username, String password, String email, String phone) {
        return register(username, password, email, phone, "BIDDER_SELLER");
    }

    /**
     * Lấy danh sách tất cả phiên đấu giá từ server.
     *
     * @return Message phản hồi chứa danh sách AuctionSession trong data.
     */
    public Message getAuctions() {
        return sendMessage(new Message(Message.Type.GET_AUCTIONS));
    }

    /**
     * Lấy thông tin chi tiết một phiên đấu giá.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi chứa AuctionSession trong data.
     */
    public Message getAuction(String auctionId) {
        Message message = new Message(Message.Type.GET_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Yêu cầu tạo phiên đấu giá mới.
     *
     * @param itemId          ID vật phẩm đem đấu giá.
     * @param durationMinutes Thời lượng đấu giá (phút).
     * @return Message phản hồi từ server.
     */
    public Message createAuction(String itemId, long durationMinutes) {
        Message message = new Message(Message.Type.CREATE_AUCTION);
        message.setItemId(itemId);
        message.setContent(String.valueOf(durationMinutes));
        return sendMessage(message);
    }

    /**
     * Yêu cầu bắt đầu phiên đấu giá.
     *
     * @param auctionId ID phiên cần bắt đầu.
     * @return Message phản hồi từ server.
     */
    public Message startAuction(String auctionId) {
        Message message = new Message(Message.Type.START_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu đặt giá cho một phiên đấu giá.
     *
     * @param auctionId ID phiên đấu giá.
     * @param amount    Số tiền muốn đặt.
     * @return Message phản hồi từ server.
     */
    public Message placeBid(String auctionId, double amount) {
        Message message = new Message(Message.Type.PLACE_BID);
        message.setAuctionId(auctionId);
        message.setData(amount);
        return sendMessage(message);
    }

    /**
     * Lấy danh sách tất cả vật phẩm từ server.
     *
     * @return Message phản hồi chứa danh sách Item trong data.
     */
    public Message getItems() {
        return sendMessage(new Message(Message.Type.GET_ITEMS));
    }

    /**
     * Gửi yêu cầu tạo vật phẩm mới.
     *
     * @param item Đối tượng Item chứa thông tin vật phẩm.
     * @return Message phản hồi từ server.
     */
    public Message createItem(Item item) {
        Message message = new Message(Message.Type.CREATE_ITEM);
        message.setData(item);
        return sendMessage(message);
    }

    /**
     * Lấy số dư tài khoản của người dùng hiện tại.
     *
     * @return Message phản hồi chứa BigDecimal số dư trong data.
     */
    public Message getUserBalance() {
        return sendMessage(new Message(Message.Type.GET_USER_BALANCE));
    }

    /**
     * Cài đặt tự động trả giá (AutoBid) cho người dùng hiện tại trong một phiên.
     *
     * @param auctionId ID phiên đấu giá.
     * @param maxAmount Giá trần tối đa cho phép trả.
     * @param increment Bước giá mỗi lần tăng.
     * @return Message phản hồi từ server.
     */
    public Message setAutoBid(String auctionId, double maxAmount, double increment) {
        Message message = new Message(Message.Type.SET_AUTOBID);
        message.setAuctionId(auctionId);
        message.setData(maxAmount);
        message.setContent(String.valueOf(increment));
        return sendMessage(message);
    }

    /**
     * Gỡ cài đặt tự động trả giá (AutoBid) khỏi một phiên.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi từ server.
     */
    public Message removeAutoBid(String auctionId) {
        Message message = new Message(Message.Type.REMOVE_AUTOBID);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Yêu cầu dừng/gia hạn phiên đấu giá.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi từ server.
     */
    public Message stopAuction(String auctionId) {
        Message message = new Message(Message.Type.STOP_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu thanh toán cho phiên đấu giá.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi từ server.
     */
    public Message processPayment(String auctionId) {
        Message message = new Message(Message.Type.PROCESS_PAYMENT);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu nạp tiền vào tài khoản.
     *
     * @param amount Số tiền cần nạp.
     * @return Message phản hồi chứa số dư mới trong data.
     */
    public Message deposit(BigDecimal amount) {
        Message message = new Message(Message.Type.DEPOSIT);
        message.setData(amount);
        return sendMessage(message);
    }

    /**
     * Đổi mật khẩu cho người dùng hiện tại.
     *
     * @param oldPassword Mật khẩu cũ.
     * @param newPassword Mật khẩu mới.
     * @return Message phản hồi từ server.
     */
    public Message changePassword(String oldPassword, String newPassword) {
        Message message = new Message(Message.Type.CHANGE_PASSWORD);
        message.setContent(oldPassword + "|" + newPassword);
        return sendMessage(message);
    }

    /**
     * Lấy lịch sử đặt giá của một phiên đấu giá.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi chứa danh sách Bid trong data.
     */
    public Message getBidHistory(String auctionId) {
        Message message = new Message(Message.Type.GET_BID_HISTORY);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Lấy danh sách phiên đấu giá mà người dùng đã tham gia.
     *
     * @param userId ID người dùng.
     * @return Message phản hồi chứa danh sách AuctionSession trong data.
     */
    public Message getUserAuctions(String userId) {
        Message message = new Message(Message.Type.GET_USER_AUCTIONS);
        message.setContent(userId);
        return sendMessage(message);
    }

    /**
     * Tìm kiếm phiên đấu giá theo tiêu chí.
     *
     * @param criteria Đối tượng SearchCriteria chứa các điều kiện tìm kiếm.
     * @return Message phản hồi chứa danh sách AuctionSession phù hợp.
     */
    public Message searchAuctions(SearchCriteria criteria) {
        Message message = new Message(Message.Type.SEARCH_AUCTIONS);
        message.setData(criteria);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu cập nhật thông tin vật phẩm.
     *
     * @param item Đối tượng Item đã được cập nhật.
     * @return Message phản hồi từ server.
     */
    public Message updateItem(Item item) {
        Message message = new Message(Message.Type.UPDATE_ITEM);
        message.setData(item);
        return sendMessage(message);
    }

    /**
     * Lấy lịch sử đặt giá của một người dùng cụ thể.
     *
     * @param userId ID người dùng cần tra lịch sử.
     * @return Message phản hồi chứa danh sách Bid trong data.
     */
    public Message getUserBidHistory(String userId) {
        Message message = new Message(Message.Type.GET_USER_BID_HISTORY);
        message.setSenderId(userId);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu cập nhật ảnh đại diện.
     * Nếu thành công, cập nhật luôn đường dẫn avatar trong bộ nhớ (currentUser).
     *
     * @param avatarPath Đường dẫn ảnh đại diện mới.
     * @return Message phản hồi từ server.
     */
    public Message updateAvatar(String avatarPath) {
        Message message = new Message(Message.Type.UPDATE_AVATAR);
        message.setContent(avatarPath);
        Message response = sendMessage(message);
        if (response.getType() == Message.Type.SUCCESS && currentUser != null) {
            currentUser.setAvatarPath(avatarPath);
        }
        return response;
    }

    /**
     * Lấy đường dẫn ảnh đại diện của người dùng hiện tại từ server.
     *
     * @return Message phản hồi chứa đường dẫn ảnh trong content.
     */
    public Message getAvatarPath() {
        return sendMessage(new Message(Message.Type.GET_AVATAR));
    }

    /**
     * Lấy thông tin người dùng hiện tại đang đăng nhập (trong bộ nhớ).
     *
     * @return Đối tượng {@link User} hiện tại, hoặc null nếu chưa đăng nhập.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Ghi đè thông tin người dùng hiện tại (dùng khi cập nhật thông tin từ bên ngoài).
     *
     * @param user Đối tượng User mới.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Gửi tin nhắn trò chuyện vào phòng đấu giá.
     *
     * @param auctionId ID phiên đấu giá.
     * @param message   Nội dung tin nhắn.
     * @return Message phản hồi từ server.
     */
    public Message sendChatMessage(String auctionId, String message) {
        Message msg = new Message(Message.Type.SEND_CHAT_MESSAGE);
        msg.setAuctionId(auctionId);
        msg.setContent(message);
        return sendMessage(msg);
    }

    /**
     * Lấy lịch sử trò chuyện của phòng đấu giá.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi chứa danh sách ChatMessage trong phần data.
     */
    public Message getChatHistory(String auctionId) {
        Message msg = new Message(Message.Type.GET_CHAT_HISTORY);
        msg.setAuctionId(auctionId);
        return sendMessage(msg);
    }

    /**
     * Thêm phiên đấu giá vào danh sách theo dõi.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi từ server.
     */
    public Message addWatchlist(String auctionId) {
        Message msg = new Message(Message.Type.ADD_WATCHLIST);
        msg.setAuctionId(auctionId);
        return sendMessage(msg);
    }

    /**
     * Xóa phiên đấu giá khỏi danh sách theo dõi.
     *
     * @param auctionId ID phiên đấu giá.
     * @return Message phản hồi từ server.
     */
    public Message removeWatchlist(String auctionId) {
        Message msg = new Message(Message.Type.REMOVE_WATCHLIST);
        msg.setAuctionId(auctionId);
        return sendMessage(msg);
    }

    /**
     * Lấy danh sách phiên đấu giá đang theo dõi của người dùng hiện tại.
     *
     * @return Message phản hồi chứa danh sách các phiên đấu giá trong phần data.
     */
    public Message getWatchlist() {
        Message msg = new Message(Message.Type.GET_WATCHLIST);
        return sendMessage(msg);
    }

    // ════════════════════════════════════════════════════════════
    //  Admin Operations
    // ════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả người dùng (yêu cầu quyền Admin).
     *
     * @return Message chứa danh sách {@link User} trong data.
     */
    public Message getUsers() {
        return sendMessage(new Message(Message.Type.GET_USERS));
    }

    /**
     * Xóa người dùng theo ID (yêu cầu quyền Admin).
     *
     * @param userId ID người dùng cần xóa.
     * @return Message phản hồi từ server.
     */
    public Message deleteUser(String userId) {
        Message msg = new Message(Message.Type.DELETE_USER);
        msg.setContent(userId);
        return sendMessage(msg);
    }

    /**
     * Kiểm tra trạng thái kết nối hiện tại tới server.
     *
     * @return true nếu socket đang mở và kết nối còn hoạt động, false nếu đã ngắt.
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
