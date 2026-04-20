package Exception;

public class ItemNotFoundException extends Exception {
    private final String itemId;

    public ItemNotFoundException(String message, String itemId) {
        super(message);
        this.itemId = itemId;
    }

    public String getItemId() {
        return itemId;
    }
}
