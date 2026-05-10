package Network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import Model.*;
import Factory.*;

/** CLI client console tương tác với AuctionServer qua menu text. */
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

    /** Kết nối tới server. */
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

    /** Ngắt kết nối. */
    public void disconnect() {
        try {
            if (socket != null) socket.close();
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Disconnect error: " + e.getMessage());
        }
    }

    /** Gửi message và nhận response. */
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

    /** Gửi yêu cầu đăng nhập. */
    public Message login(String username, String password) {
        Message message = new Message(Message.Type.LOGIN);
        message.setData(username);
        message.setContent(password);
        return sendMessage(message);
    }

    /** Gửi yêu cầu đăng ký. */
    public Message register(String username, String password) {
        Message message = new Message(Message.Type.REGISTER);
        message.setData(username);
        message.setContent(password);
        return sendMessage(message);
    }

    /** Lấy danh sách phiên đấu giá. */
    public Message getAuctions() {
        return sendMessage(new Message(Message.Type.GET_AUCTIONS));
    }

    /** Lấy thông tin một phiên. */
    public Message getAuction(String auctionId) {
        Message message = new Message(Message.Type.GET_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Tạo phiên đấu giá mới. */
    public Message createAuction(String itemId, long durationMinutes) {
        Message message = new Message(Message.Type.CREATE_AUCTION);
        message.setItemId(itemId);
        message.setContent(String.valueOf(durationMinutes));
        return sendMessage(message);
    }

    /** Bắt đầu phiên đấu giá. */
    public Message startAuction(String auctionId) {
        Message message = new Message(Message.Type.START_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Đặt giá. */
    public Message placeBid(String auctionId, double amount) {
        Message message = new Message(Message.Type.PLACE_BID);
        message.setAuctionId(auctionId);
        message.setData(amount);
        return sendMessage(message);
    }

    /** Kết thúc phiên. */
    public Message finishAuction(String auctionId) {
        Message message = new Message(Message.Type.FINISH_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    /** Hủy phiên với lý do. */
    public Message cancelAuction(String auctionId, String reason) {
        Message message = new Message(Message.Type.CANCEL_AUCTION);
        message.setAuctionId(auctionId);
        message.setContent(reason);
        return sendMessage(message);
    }

    /** Lấy danh sách vật phẩm. */
    public Message getItems() {
        return sendMessage(new Message(Message.Type.GET_ITEMS));
    }

    /** Tạo vật phẩm mới. */
    public Message createItem(Model.Item item) {
        Message message = new Message(Message.Type.CREATE_ITEM);
        message.setData(item);
        return sendMessage(message);
    }

    private Message createErrorMessage(String error) {
        Message message = new Message(Message.Type.ERROR);
        message.setContent(error);
        return message;
    }

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
    
    private static void handleLogout(AuctionClient client) {
        currentUser = null;
        System.out.println("Logged out.");
    }
    
    private static void handleViewItems(AuctionClient client) {
        Message response = client.getItems();
        System.out.println(response.getType() + ": " + response.getContent());
        if (response.getData() != null) {
            System.out.println(response.getData());
        }
    }
    
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
    
    private static void handleViewAuctions(AuctionClient client) {
        Message response = client.getAuctions();
        System.out.println(response.getType() + ": " + response.getContent());
        if (response.getData() != null) {
            System.out.println(response.getData());
        }
    }
    
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
