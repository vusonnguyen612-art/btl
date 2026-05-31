package Network;

/** Factory tạo Message chuẩn hóa, loại bỏ duplication giữa NetworkService, AuctionClient, AuctionServer. */
public final class MessageFactory {

    private MessageFactory() {}

    public static Message error(String content) {
        Message msg = new Message(Message.Type.ERROR);
        msg.setContent(content);
        return msg;
    }

    public static Message success() {
        return new Message(Message.Type.SUCCESS);
    }

    public static Message success(String content) {
        Message msg = new Message(Message.Type.SUCCESS);
        msg.setContent(content);
        return msg;
    }

    public static Message success(Object data) {
        Message msg = new Message(Message.Type.SUCCESS);
        msg.setData(data);
        return msg;
    }

    public static Message notification(String content, String auctionId) {
        Message msg = new Message(Message.Type.NOTIFICATION);
        msg.setContent(content);
        msg.setAuctionId(auctionId);
        return msg;
    }
}
