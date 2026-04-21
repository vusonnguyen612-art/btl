package Network;

import Exception.*;
import Model.*;
import Service.AuctionManager;
import Observer.AuctionObserver;
import Factory.ItemFactory;
import Factory.UserFactory;

import java.io.*;
import java.net.*;
import java.util.List;

public class AuctionServer {
    private ServerSocket serverSocket;
    private int port;
    private boolean running;
    private AuctionManager auctionManager;

    public AuctionServer(int port) {
        this.port = port;
        try {
            this.auctionManager = AuctionManager.loadData();
            System.out.println("Data loaded from file");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No data file found, starting fresh");
            this.auctionManager = AuctionManager.getInstance();
        }
        auctionManager.addGlobalObserver(new ServerAuctionObserver());
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Server started on port " + port);
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                new ClientHandler(clientSocket, auctionManager).start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            auctionManager.saveData();
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private AuctionManager auctionManager;
        private User currentUser;

        public ClientHandler(Socket socket, AuctionManager auctionManager) {
            this.socket = socket;
            this.auctionManager = auctionManager;
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

        private Message processMessage(Message message) {
            try {
                switch (message.getType()) {
                    case LOGIN:
                        return handleLogin(message);
                    case REGISTER:
                        return handleRegister(message);
                    case GET_AUCTIONS:
                        return handleGetAuctions();
                    case GET_AUCTION:
                        return handleGetAuction(message);
                    case CREATE_AUCTION:
                        return handleCreateAuction(message);
                    case START_AUCTION:
                        return handleStartAuction(message);
                    case PLACE_BID:
                        return handlePlaceBid(message);
                    case FINISH_AUCTION:
                        return handleFinishAuction(message);
                    case CANCEL_AUCTION:
                        return handleCancelAuction(message);
                    case GET_ITEMS:
                        return handleGetItems();
                    case CREATE_ITEM:
                        return handleCreateItem(message);
                    case GET_USERS:
                        return handleGetUsers();
                    default:
                        return new Message(Message.Type.ERROR, "Unknown message type");
                }
            } catch (Exception e) {
                return createErrorMessage(e.getMessage());
            }
        }

        private Message handleLogin(Message message) {
            try {
                User user = auctionManager.authenticate(
                    (String) message.getData(),
                    message.getContent()
                );
                currentUser = user;
                Message response = new Message(Message.Type.SUCCESS, "Login successful");
                response.setData(user);
                return response;
            } catch (AuthenticationException e) {
                return createErrorMessage(e.getMessage());
            }
        }

        private Message handleRegister(Message message) {
            String passwordError = UserFactory.getPasswordError(message.getContent());
            if (passwordError != null) {
                return createErrorMessage(passwordError);
            }

            User newUser = UserFactory.createUser(
                (String) message.getData(),
                message.getContent()
            );
            auctionManager.addUser(newUser);
            try {
                auctionManager.saveData();
            } catch (IOException e) {
                System.err.println("Error saving data: " + e.getMessage());
            }
            Message response = new Message(Message.Type.SUCCESS, "Registration successful");
            response.setData(newUser);
            return response;
        }

        private Message handleGetAuctions() {
            List<AuctionSession> auctions = auctionManager.getAllAuctions();
            String auctionList = auctions.isEmpty() ? "No auctions" : auctions.toString();
            Message response = new Message(Message.Type.SUCCESS, auctionList);
            response.setData(auctions);
            return response;
        }

        private Message handleGetAuction(Message message) {
            AuctionSession auction = auctionManager.getAuction(message.getAuctionId());
            if (auction != null) {
                Message response = new Message(Message.Type.SUCCESS);
                response.setData(auction);
                return response;
            }
            return createErrorMessage("Auction not found");
        }

        private Message handleCreateAuction(Message message) {
            if (currentUser == null || !currentUser.isSeller()) {
                return createErrorMessage("Only sellers can create auctions");
            }
            
            try {
                AuctionSession auction = auctionManager.createAuction(
                    message.getItemId(),
                    Long.parseLong(message.getContent())
                );
                Message response = new Message(Message.Type.SUCCESS, "Auction created");
                response.setData(auction);
                return response;
            } catch (ItemNotFoundException e) {
                return createErrorMessage(e.getMessage());
            }
        }

        private Message handleStartAuction(Message message) {
            auctionManager.startAuction(message.getAuctionId());
            return new Message(Message.Type.SUCCESS, "Auction started");
        }

        private Message handlePlaceBid(Message message) {
            if (currentUser == null || !currentUser.isBidder()) {
                return createErrorMessage("Only bidders can place bids");
            }
            
            try {
                String result = auctionManager.placeBid(
                    message.getAuctionId(),
                    currentUser.getId(),
                    (Double) message.getData()
                );
                return new Message(Message.Type.SUCCESS, result);
            } catch (AuctionClosedException | InvalidBidException e) {
                return createErrorMessage(e.getMessage());
            }
        }

        private Message handleFinishAuction(Message message) {
            auctionManager.finishAuction(message.getAuctionId());
            return new Message(Message.Type.SUCCESS, "Auction finished");
        }

        private Message handleCancelAuction(Message message) {
            auctionManager.cancelAuction(message.getAuctionId(), message.getContent());
            return new Message(Message.Type.SUCCESS, "Auction canceled");
        }

        private Message handleGetItems() {
            List<Item> items = auctionManager.getAllItems();
            String itemList = items.isEmpty() ? "No items" : items.toString();
            Message response = new Message(Message.Type.SUCCESS, itemList);
            response.setData(items);
            return response;
        }

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
            auctionManager.addItem(item);
            
            Message response = new Message(Message.Type.SUCCESS, "Item created");
            response.setData(item);
            return response;
        }

        private Message handleGetUsers() {
            if (currentUser == null || !currentUser.isAdmin()) {
                return createErrorMessage("Only admins can view all users");
            }
            
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(auctionManager.getAllAuctions());
            return response;
        }

        private Message createErrorMessage(String error) {
            Message response = new Message(Message.Type.ERROR);
            response.setContent(error);
            return response;
        }

        private void close() {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client handler: " + e.getMessage());
            }
        }
    }

    private static class ServerAuctionObserver implements AuctionObserver {
        @Override
        public void onBidPlaced(String auctionId, String bidderId, double amount) {
            System.out.println("[NOTIFICATION] New bid on " + auctionId + ": " + bidderId + " - " + amount);
        }

        @Override
        public void onAuctionStarted(String auctionId) {
            System.out.println("[NOTIFICATION] Auction " + auctionId + " started");
        }

        @Override
        public void onAuctionFinished(String auctionId, String winnerId, double finalPrice) {
            System.out.println("[NOTIFICATION] Auction " + auctionId + " finished. Winner: " + winnerId + " - " + finalPrice);
        }

        @Override
        public void onAuctionCanceled(String auctionId, String reason) {
            System.out.println("[NOTIFICATION] Auction " + auctionId + " canceled: " + reason);
        }

        @Override
        public void onAuctionStatusChanged(String auctionId, String oldStatus, String newStatus) {
            System.out.println("[NOTIFICATION] Auction " + auctionId + " status changed: " + oldStatus + " -> " + newStatus);
        }
    }

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer(8989);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
