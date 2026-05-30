package Network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import Model.*;
import Factory.*;

/**
 * CLI client console tương tác với {@link AuctionServer} qua menu text.
 * <p>
 * Quản lý kết nối TCP socket, gửi/nhận {@link Message} dạng Object serialization.
 * Cung cấp các phương thức tiện ích cho từng loại request (login, register,
 * getAuctions, placeBid, ...) và một {@link #main(String[])} entry point
 * hiển thị menu CLI cho người dùng thao tác trực tiếp.
 * </p>
 */
public class AuctionClient {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String serverAddress;
    private int port;

    /** @param serverAddress địa chỉ server
     *  @param port          cổng kết nối */
    public AuctionClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    /**
     * Kết nối tới server qua TCP socket.
     *
     * @return true nếu kết nối thành công, false nếu thất bại.
     */
    public boolean connect() {
        try {
            socket = new Socket(serverAddress, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            System.out.println("Connected to server");
            return true;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ngắt kết nối khỏi server và đóng socket.
     */
    public void disconnect() {
        try {
            if (socket != null) socket.close();
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Disconnect error: " + e.getMessage());
        }
    }

    /**
     * Gửi message đến server và nhận response.
     *
     * @param message Đối tượng Message cần gửi.
     * @return Message phản hồi từ server, hoặc Error message nếu lỗi kết nối.
     */
    public Message sendMessage(Message message) {
        try {
            output.writeObject(message);
            output.flush();
            return (Message) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Send message error: " + e.getMessage());
            return createErrorMessage(e.getMessage());
        }
    }

    /**
     * Gửi yêu cầu đăng nhập đến server.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return Message phản hồi từ server.
     */
    public Message login(String username, String password) {
        Message message = new Message(Message.Type.LOGIN);
        message.setData(username);
        message.setContent(password);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu đăng ký tài khoản mới.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return Message phản hồi từ server.
     */
    public Message register(String username, String password) {
        Message message = new Message(Message.Type.REGISTER);
        message.setData(username);
        message.setContent(password);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu lấy danh sách tất cả phiên đấu giá.
     *
     * @return Message phản hồi chứa danh sách AuctionSession trong data.
     */
    public Message getAuctions() {
        return sendMessage(new Message(Message.Type.GET_AUCTIONS));
    }

    /**
     * Gửi yêu cầu lấy thông tin chi tiết một phiên đấu giá.
     *
     * @param auctionId ID của phiên đấu giá cần xem.
     * @return Message phản hồi chứa AuctionSession trong data.
     */
    public Message getAuction(String auctionId) {
        Message message = new Message(Message.Type.GET_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu tạo phiên đấu giá mới (yêu cầu quyền seller).
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
     * Gửi yêu cầu bắt đầu phiên đấu giá.
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
     * Gửi yêu cầu kết thúc phiên đấu giá.
     *
     * @param auctionId ID phiên cần kết thúc.
     * @return Message phản hồi từ server.
     */
    public Message finishAuction(String auctionId) {
        Message message = new Message(Message.Type.FINISH_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu hủy phiên đấu giá kèm lý do.
     *
     * @param auctionId ID phiên cần hủy.
     * @param reason    Lý do hủy phiên.
     * @return Message phản hồi từ server.
     */
    public Message cancelAuction(String auctionId, String reason) {
        Message message = new Message(Message.Type.CANCEL_AUCTION);
        message.setAuctionId(auctionId);
        message.setContent(reason);
        return sendMessage(message);
    }

    /**
     * Gửi yêu cầu lấy danh sách tất cả vật phẩm.
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
    public Message createItem(Model.Item item) {
        Message message = new Message(Message.Type.CREATE_ITEM);
        message.setData(item);
        return sendMessage(message);
    }

    /**
     * Tạo message lỗi nội bộ khi kết nối thất bại.
     *
     * @param error Nội dung lỗi.
     * @return Message loại ERROR.
     */
    private Message createErrorMessage(String error) {
        Message message = new Message(Message.Type.ERROR);
        message.setContent(error);
        return message;
    }

    /**
     * Entry point CLI. Hiển thị menu text, cho phép đăng nhập/đăng ký/xem danh sách/tạo item/tạo phiên/đặt giá.
     *
     * @param args Tham số dòng lệnh (không dùng).
     */
    public static void main(String[] args) {
        AuctionClient client = new AuctionClient("0.tcp.ap.ngrok.io", 19274);
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Auction Client ===");
        
        if (!client.connect()) {
            System.out.println("Failed to connect to server. Make sure server is running.");
            return;
        }
        
        currentUser = null;
        
        while (true) {
            printMenu();
            System.out.print("Choose option: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    handleRegister(client, scanner);
                    break;
                case "2":
                    handleLogin(client, scanner);
                    break;
                case "3":
                    handleLogout(client);
                    break;
                case "4":
                    handleViewItems(client);
                    break;
                case "5":
                    handleCreateItem(client, scanner);
                    break;
                case "6":
                    handleViewAuctions(client);
                    break;
                case "7":
                    handleCreateAuction(client, scanner);
                    break;
                case "8":
                    handlePlaceBid(client, scanner);
                    break;
                case "0":
                    client.disconnect();
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }
    
    private static User currentUser;
    
    /**
     * In menu lựa chọn CLI ra console.
     */
    private static void printMenu() {
        System.out.println("\n--- Menu ---");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Logout");
        System.out.println("4. View Items");
        System.out.println("5. Create Item (Seller only)");
        System.out.println("6. View Auctions");
        System.out.println("7. Create Auction (Seller only)");
        System.out.println("8. Place Bid (Bidder only)");
        System.out.println("0. Exit");
    }
    
    /**
     * Xử lý đăng ký từ CLI: nhập username, kiểm tra password, gửi lên server.
     *
     * @param client  Đối tượng AuctionClient để gửi request.
     * @param scanner Scanner để đọc input từ bàn phím.
     */
    private static void handleRegister(AuctionClient client, Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        
        String password;
        while (true) {
            System.out.print("Enter password (min 6 chars, no whitespace): ");
            password = scanner.nextLine();
            String error = UserFactory.getPasswordError(password);
            if (error != null) {
                System.out.println("Error: " + error);
                continue;
            }
            break;
        }
        
        Message response = client.register(username, password);
        System.out.println(response.getType() + ": " + response.getContent());
    }
    
    /**
     * Xử lý đăng nhập từ CLI: nhập username/password, gửi lên server, lưu currentUser.
     *
     * @param client  Đối tượng AuctionClient để gửi request.
     * @param scanner Scanner để đọc input từ bàn phím.
     */
    private static void handleLogin(AuctionClient client, Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();
        
        Message response = client.login(username, password);
        System.out.println(response.getType() + ": " + response.getContent());
        if (response.getType() == Message.Type.SUCCESS) {
            currentUser = (User) response.getData();
            System.out.println("Welcome, " + currentUser.getUsername() + " (" + currentUser.getClass().getSimpleName() + ")");
        }
    }
    
    /**
     * Đăng xuất: xóa currentUser.
     *
     * @param client Đối tượng AuctionClient.
     */
    private static void handleLogout(AuctionClient client) {
        currentUser = null;
        System.out.println("Logged out.");
    }

    /**
     * Xem danh sách tất cả vật phẩm từ server.
     *
     * @param client Đối tượng AuctionClient để gửi request.
     */
    private static void handleViewItems(AuctionClient client) {
        Message response = client.getItems();
        System.out.println(response.getType() + ": " + response.getContent());
        if (response.getData() != null) {
            System.out.println(response.getData());
        }
    }
    
    /**
     * Tạo vật phẩm mới từ CLI (yêu cầu đăng nhập với quyền seller).
     *
     * @param client  Đối tượng AuctionClient để gửi request.
     * @param scanner Scanner để đọc input từ bàn phím.
     */
    private static void handleCreateItem(AuctionClient client, Scanner scanner) {
        if (currentUser == null || !currentUser.isSeller()) {
            System.out.println("Only sellers can create items. Please login as seller.");
            return;
        }
        
        System.out.print("Enter category (Electronics/Art/Vehicle): ");
        String category = scanner.nextLine().trim();
        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter description: ");
        String description = scanner.nextLine().trim();
        System.out.print("Enter start price: ");
        double startPrice = Double.parseDouble(scanner.nextLine().trim());
        
        Item item = ItemFactory.createItem(category, name, description, startPrice, currentUser.getId());
        Message response = client.createItem(item);
        System.out.println(response.getType() + ": " + response.getContent());
    }
    
    /**
     * Xem danh sách phiên đấu giá từ server.
     *
     * @param client Đối tượng AuctionClient để gửi request.
     */
    private static void handleViewAuctions(AuctionClient client) {
        Message response = client.getAuctions();
        System.out.println(response.getType() + ": " + response.getContent());
        if (response.getData() != null) {
            System.out.println(response.getData());
        }
    }

    /**
     * Tạo phiên đấu giá mới từ CLI (yêu cầu đăng nhập với quyền seller).
     *
     * @param client  Đối tượng AuctionClient để gửi request.
     * @param scanner Scanner để đọc input từ bàn phím.
     */
    private static void handleCreateAuction(AuctionClient client, Scanner scanner) {
        if (currentUser == null || !currentUser.isSeller()) {
            System.out.println("Only sellers can create auctions. Please login as seller.");
            return;
        }
        
        System.out.print("Enter item ID: ");
        String itemId = scanner.nextLine().trim();
        System.out.print("Enter duration (minutes): ");
        long duration = Long.parseLong(scanner.nextLine().trim());
        
        Message response = client.createAuction(itemId, duration);
        System.out.println(response.getType() + ": " + response.getContent());
    }
    
    /**
     * Xử lý đặt giá từ CLI (yêu cầu đăng nhập với quyền bidder).
     *
     * @param client  Đối tượng AuctionClient để gửi request.
     * @param scanner Scanner để đọc input từ bàn phím.
     */
    private static void handlePlaceBid(AuctionClient client, Scanner scanner) {
        if (currentUser == null || !currentUser.isBidder()) {
            System.out.println("Only bidders can place bids. Please login as bidder.");
            return;
        }
        
        System.out.print("Enter auction ID: ");
        String auctionId = scanner.nextLine().trim();
        System.out.print("Enter bid amount: ");
        double amount = Double.parseDouble(scanner.nextLine().trim());
        
        Message response = client.placeBid(auctionId, amount);
        System.out.println(response.getType() + ": " + response.getContent());
    }
}
