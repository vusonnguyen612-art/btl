package Network;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Type {
        LOGIN,
        LOGOUT,
        REGISTER,
        GET_AUCTIONS,
        GET_AUCTION,
        CREATE_AUCTION,
        START_AUCTION,
        PLACE_BID,
        FINISH_AUCTION,
        CANCEL_AUCTION,
        GET_ITEMS,
        GET_ITEM,
        CREATE_ITEM,
        UPDATE_ITEM,
        DELETE_ITEM,
        GET_USERS,
        GET_USER_BALANCE,
        DEPOSIT,
        SET_AUTOBID,
        REMOVE_AUTOBID,
        STOP_AUCTION,
        PROCESS_PAYMENT,
        GET_BID_HISTORY,
        NOTIFICATION,
        ERROR,
        SUCCESS,
        GET_WINNING_HISTORY
    }

    private Type type;
    private String senderId;
    private String auctionId;
    private String itemId;
    private String content;
    private Object data;
    private List<Message> notifications;
    private long timestamp;

    public Message(Type type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Message(Type type, String senderId) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<Message> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Message> notifications) {
        this.notifications = notifications;
    }
}
