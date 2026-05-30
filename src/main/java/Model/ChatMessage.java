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

    /** @return ID duy nhất của tin nhắn */
    public String getId() {
        return id;
    }

    /** @param id ID duy nhất mới */
    public void setId(String id) {
        this.id = id;
    }

    /** @return ID của phiên đấu giá */
    public String getAuctionId() {
        return auctionId;
    }

    /** @param auctionId ID phiên đấu giá mới */
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    /** @return ID của người gửi */
    public String getSenderId() {
        return senderId;
    }

    /** @param senderId ID người gửi mới */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /** @return tên hiển thị người gửi */
    public String getSenderName() {
        return senderName;
    }

    /** @param senderName tên hiển thị người gửi mới */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /** @return nội dung tin nhắn */
    public String getMessage() {
        return message;
    }

    /** @param message nội dung tin nhắn mới */
    public void setMessage(String message) {
        this.message = message;
    }

    /** @return thời gian gửi tin nhắn */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /** @param timestamp thời gian gửi mới */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
