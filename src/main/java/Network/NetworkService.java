package Network;

import Model.*;
import java.io.*;
import java.net.*;
import java.util.List;

public class NetworkService {
    private static NetworkService instance;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String serverAddress;
    private int port;
    private User currentUser;

    private NetworkService(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService("localhost", 8989);
        }
        return instance;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverAddress, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            System.out.println("Connected to server at " + serverAddress + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                // Send logout message
                if (currentUser != null) {
                    Message logoutMsg = new Message(Message.Type.LOGOUT);
                    sendMessage(logoutMsg);
                }
                socket.close();
                System.out.println("Disconnected from server");
            }
        } catch (IOException e) {
            System.err.println("Disconnect error: " + e.getMessage());
        }
    }

    public Message sendMessage(Message message) {
        try {
            output.writeObject(message);
            output.flush();
            return (Message) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Send message error: " + e.getMessage());
            Message error = new Message(Message.Type.ERROR);
            error.setContent(e.getMessage());
            return error;
        }
    }

    public Message login(String username, String password) {
        Message message = new Message(Message.Type.LOGIN);
        message.setData(username);
        message.setContent(password);
        Message response = sendMessage(message);
        if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
            currentUser = (User) response.getData();
        }
        return response;
    }

    public Message register(String username, String password) {
        Message message = new Message(Message.Type.REGISTER);
        message.setData(username);
        message.setContent(password);
        return sendMessage(message);
    }

    public Message getAuctions() {
        return sendMessage(new Message(Message.Type.GET_AUCTIONS));
    }

    public Message getAuction(String auctionId) {
        Message message = new Message(Message.Type.GET_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    public Message createAuction(String itemId, long durationMinutes) {
        Message message = new Message(Message.Type.CREATE_AUCTION);
        message.setItemId(itemId);
        message.setContent(String.valueOf(durationMinutes));
        return sendMessage(message);
    }

    public Message startAuction(String auctionId) {
        Message message = new Message(Message.Type.START_AUCTION);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    public Message placeBid(String auctionId, double amount) {
        Message message = new Message(Message.Type.PLACE_BID);
        message.setAuctionId(auctionId);
        message.setData(amount);
        return sendMessage(message);
    }

    public Message getItems() {
        return sendMessage(new Message(Message.Type.GET_ITEMS));
    }

    public Message createItem(Item item) {
        Message message = new Message(Message.Type.CREATE_ITEM);
        message.setData(item);
        return sendMessage(message);
    }

    public Message getUserBalance() {
        return sendMessage(new Message(Message.Type.GET_USER_BALANCE));
    }

    public Message getBidHistory(String auctionId) {
        Message message = new Message(Message.Type.GET_BID_HISTORY);
        message.setAuctionId(auctionId);
        return sendMessage(message);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public Message getWinningHistory(String userId) {
        Message request = new Message(Message.Type.GET_WINNING_HISTORY, userId);

        return sendMessage(request);
    }
}
