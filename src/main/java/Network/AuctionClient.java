package Network;

import java.util.Scanner;

import Model.*;
import Factory.*;

/** CLI client console tương tác với AuctionServer qua NetworkService. */
public class AuctionClient {
    private final NetworkService networkService;
    private User currentUser;

    /** @param serverAddress địa chỉ server
     *  @param port          cổng kết nối */
    public AuctionClient(String serverAddress, int port) {
        this.networkService = NetworkService.getInstance();
    }

    /** Kết nối tới server. */
    public boolean connect() {
        return networkService.connect();
    }

    /** Ngắt kết nối. */
    public void disconnect() {
        networkService.disconnect();
    }

    /** Gửi yêu cầu đăng nhập. */
    public Message login(String username, String password) {
        Message response = networkService.login(username, password);
        if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
            currentUser = (User) response.getData();
        }
        return response;
    }

    /** Gửi yêu cầu đăng ký. */
    public Message register(String username, String password, String email, String phone) {
        return networkService.register(username, password, email, phone);
    }

    /** Lấy danh sách phiên đấu giá. */
    public Message getAuctions() {
        return networkService.getAuctions();
    }

    /** Lấy thông tin một phiên. */
    public Message getAuction(String auctionId) {
        return networkService.getAuction(auctionId);
    }

    /** Tạo phiên đấu giá mới. */
    public Message createAuction(String itemId, long durationMinutes) {
        return networkService.createAuction(itemId, durationMinutes);
    }

    /** Bắt đầu phiên đấu giá. */
    public Message startAuction(String auctionId) {
        return networkService.startAuction(auctionId);
    }

    /** Đặt giá. */
    public Message placeBid(String auctionId, double amount) {
        return networkService.placeBid(auctionId, amount);
    }

    /** Lấy danh sách vật phẩm. */
    public Message getItems() {
        return networkService.getItems();
    }

    /** Tạo vật phẩm mới. */
    public Message createItem(Item item) {
        return networkService.createItem(item);
    }

    public static void main(String[] args) {
        AuctionClient client = new AuctionClient("localhost", 8989);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Auction Client ===");

        if (!client.connect()) {
            System.out.println("Failed to connect to server. Make sure server is running.");
            return;
        }

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

        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Enter phone: ");
        String phone = scanner.nextLine().trim();

        Message response = client.register(username, password, email, phone);
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
            System.out.println("Welcome, " + client.currentUser.getUsername());
        }
    }

    private static void handleLogout(AuctionClient client) {
        client.currentUser = null;
        client.disconnect();
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
        if (client.currentUser == null || !client.currentUser.isSeller()) {
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

        Item item = ItemFactory.createItem(category, name, description, startPrice, client.currentUser.getId());
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
        if (client.currentUser == null || !client.currentUser.isSeller()) {
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
        if (client.currentUser == null || !client.currentUser.isBidder()) {
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
