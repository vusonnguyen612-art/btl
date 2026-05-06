package Network;

import Exception.*;
import Model.*;
import DAO.*;
import Factory.ItemFactory;
import Factory.UserFactory;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Optional;

public class AuctionServer {
    private ServerSocket serverSocket;
    private int port;
    private boolean running;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    public AuctionServer(int port) {
        this.port = port;
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
        this.auctionDAO = new AuctionDAO();
        System.out.println("Server initialized with DAO pattern (MySQL)");
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Server started on port " + port);

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

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

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
                    case GET_USER_BALANCE:
                        return handleGetUserBalance(message);
                    case GET_BID_HISTORY:
                        return handleGetBidHistory(message);
                    default:
                        return createErrorMessage("Unknown message type");
                }
            } catch (Exception e) {
                return createErrorMessage(e.getMessage());
            }
        }

        private Message handleLogin(Message message) {
            try {
                User user = userDAO.authenticate(
                    (String) message.getData(),
                    message.getContent()
                );
                currentUser = user;
                Message response = new Message(Message.Type.SUCCESS);
                response.setContent("Login successful");
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
            userDAO.register(newUser);
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Registration successful");
            response.setData(newUser);
            return response;
        }

        private Message handleGetAuctions() {
            List<AuctionSession> auctions = auctionDAO.findAllAuctions();
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(auctions);
            return response;
        }

        private Message handleGetAuction(Message message) {
            Optional<AuctionSession> auction = auctionDAO.findAuctionById(message.getAuctionId());
            if (auction.isPresent()) {
                Message response = new Message(Message.Type.SUCCESS);
                response.setData(auction.get());
                return response;
            }
            return createErrorMessage("Auction not found");
        }

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

        private Message handleStartAuction(Message message) {
            auctionDAO.startAuction(message.getAuctionId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Auction started");
            return response;
        }

        private Message handlePlaceBid(Message message) {
            if (currentUser == null || !currentUser.isBidder()) {
                return createErrorMessage("Only bidders can place bids");
            }

            try {
                boolean success = auctionDAO.placeBid(
                    message.getAuctionId(),
                    currentUser.getId(),
                    (Double) message.getData()
                );
                if (success) {
                    Message response = new Message(Message.Type.SUCCESS);
                    response.setContent("Bid placed successfully");
                    return response;
                }
                return createErrorMessage("Failed to place bid");
            } catch (Exception e) {
                return createErrorMessage(e.getMessage());
            }
        }

        private Message handleFinishAuction(Message message) {
            auctionDAO.finishAuction(message.getAuctionId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Auction finished");
            return response;
        }

        private Message handleCancelAuction(Message message) {
            auctionDAO.cancelAuction(message.getAuctionId(), message.getContent());
            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Auction canceled");
            return response;
        }

        private Message handleGetItems() {
            List<Item> items = itemDAO.findAll();
            Message response = new Message(Message.Type.SUCCESS);
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
            itemDAO.save(item);

            Message response = new Message(Message.Type.SUCCESS);
            response.setContent("Item created");
            response.setData(item);
            return response;
        }

        private Message handleGetUserBalance(Message message) {
            if (currentUser == null) {
                return createErrorMessage("Not logged in");
            }
            java.math.BigDecimal balance = userDAO.getBalance(currentUser.getId());
            Message response = new Message(Message.Type.SUCCESS);
            response.setData(balance);
            return response;
        }

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
