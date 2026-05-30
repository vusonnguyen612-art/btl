package Network;

import java.io.Serializable;
import java.util.List;

/**
 * Định dạng message trao đổi giữa client và server qua TCP socket sử dụng Java Object Serialization.
 * <p>
 * Mỗi message chứa một {@link Type} xác định hành động, kèm theo các trường dữ liệu
 * như {@code senderId}, {@code auctionId}, {@code content}, {@code data} (Object đa năng),
 * và danh sách {@code notifications} đính kèm. Timestamp được tự động gán khi khởi tạo.
 * </p>
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Các loại message hỗ trợ trong giao thức client-server.
     * <p>Bao gồm các nhóm: xác thực (LOGIN, REGISTER, LOGOUT),
     * thao tác phiên đấu giá (CREATE_AUCTION, START_AUCTION, PLACE_BID, ...),
     * quản lý vật phẩm (CREATE_ITEM, UPDATE_ITEM, ...),
     * theo dõi (ADD_WATCHLIST, REMOVE_WATCHLIST, ...),
     * trò chuyện (SEND_CHAT_MESSAGE, GET_CHAT_HISTORY),
     * và các message hệ thống (NOTIFICATION, ERROR, SUCCESS).</p>
     */
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
        GET_USER_AUCTIONS,
        SEARCH_AUCTIONS,
        NOTIFICATION,
        ERROR,
        SUCCESS,
        GET_USER_BID_HISTORY,
        UPDATE_AVATAR,
        GET_AVATAR,
        SEND_CHAT_MESSAGE,
        GET_CHAT_HISTORY,
        ADD_WATCHLIST,
        REMOVE_WATCHLIST,
        GET_WATCHLIST,
        DELETE_USER,
        CHANGE_PASSWORD,
        BAN_USER,
        UNBAN_USER,
        GET_ALL_ITEMS,
        SUSPEND_AUCTION,
    }

    private Type type;
    private String senderId;
    private String auctionId;
    private String itemId;
    private String content;
    private Object data;
    private List<Message> notifications;
    private long timestamp;

    /**
     * Khởi tạo message chỉ với loại, timestamp được gán tự động.
     *
     * @param type loại message xác định hành động giao tiếp.
     */
    public Message(Type type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Khởi tạo message với loại và ID người gửi, timestamp được gán tự động.
     *
     * @param type     loại message xác định hành động giao tiếp.
     * @param senderId ID định danh người gửi message.
     */
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
