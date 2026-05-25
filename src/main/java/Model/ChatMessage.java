package Model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Lớp đại diện cho một tin nhắn trong phòng trò chuyện trực tiếp (Live Chat) của phiên đấu giá.
 * Kế thừa {@link Serializable} để truyền tải qua Socket TCP.
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID duy nhất của tin nhắn */
    private String id;
    
    /** ID của phiên đấu giá chứa tin nhắn này */
    private String auctionId;
    
    /** ID của người gửi tin nhắn */
    private String senderId;
    
    /** Tên đăng nhập (username) của người gửi để hiển thị trên giao diện chat */
    private String senderName;
    
    /** Nội dung tin nhắn trò chuyện */
    private String message;
    
    /** Thời gian gửi tin nhắn */
    private Timestamp timestamp;

    /**
     * Khởi tạo một đối tượng ChatMessage đầy đủ thông tin.
     *
     * @param id          ID duy nhất của tin nhắn.
     * @param auctionId   ID của phiên đấu giá.
     * @param senderId    ID của người gửi.
     * @param senderName  Tên hiển thị người gửi.
     * @param message     Nội dung tin nhắn.
     * @param timestamp   Thời gian gửi.
     */
    public ChatMessage(String id, String auctionId, String senderId, String senderName, String message, Timestamp timestamp) {
        this.id = id;
        this.auctionId = auctionId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
