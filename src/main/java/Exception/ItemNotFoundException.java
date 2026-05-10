package Exception;

/**
 * Ngoại lệ khi không tìm thấy vật phẩm với ID tương ứng.
 */
public class ItemNotFoundException extends Exception {
    private final String itemId;

    /**
     * @param message thông báo lỗi
     * @param itemId  ID của vật phẩm không tìm thấy
     */
    public ItemNotFoundException(String message, String itemId) {
        super(message);
        this.itemId = itemId;
    }

    /** @return ID của vật phẩm không tìm thấy */
    public String getItemId() {
        return itemId;
    }
}
