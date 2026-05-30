package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp trừu tượng đại diện cho một vật phẩm đấu giá.
 * Kế thừa {@link Entity}.
 * Các lớp con: Vehicle, Electronics, Fashion, Books, Art, Music, Sports, Jewelry, Furniture.
 */
public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private double startPrice;
    private String sellerId;
    protected String category;
    private String imagePath;

    /**
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Item(String id, String name, String description, double startPrice, String sellerId) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.sellerId = sellerId;
    }

    /** @return mã định danh duy nhất của vật phẩm */
    public String getId() {
        return id;
    }

    /** @return tên của vật phẩm */
    public String getName() {
        return name;
    }

    /** @param name tên mới cho vật phẩm */
    public void setName(String name) {
        this.name = name;
    }

    /** @return mô tả chi tiết của vật phẩm */
    public String getDescription() {
        return description;
    }

    /** @param description mô tả mới cho vật phẩm */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return giá khởi điểm của vật phẩm */
    public double getStartPrice() {
        return startPrice;
    }

    /** @param startPrice giá khởi điểm mới */
    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    /** @return ID của người bán sở hữu vật phẩm */
    public String getSellerId() {
        return sellerId;
    }

    /** @param sellerId ID mới của người bán */
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    /** @return danh mục của vật phẩm (do lớp con thiết lập) */
    public String getCategory() {
        return category;
    }

    /** @return đường dẫn đến ảnh của vật phẩm */
    public String getImagePath() {
        return imagePath;
    }

    /** @param imagePath đường dẫn ảnh mới */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Lấy thông tin chi tiết đặc thù theo từng danh mục.
     * Mỗi lớp con triển khai method này để trả về chuỗi mô tả riêng.
     *
     * @return chuỗi thông tin chi tiết đặc thù danh mục
     */
    public abstract String getSpecificInfo();

    @Override
    /**
     * Trả về chuỗi biểu diễn của vật phẩm bao gồm mã, tên, mô tả và giá khởi điểm.
     *
     * @return chuỗi định dạng "[id] name - description - Start: $startPrice"
     */
    public String toString() {
        return String.format("[%s] %s - %s - Start: $%.2f", id, name, description, startPrice);
    }
}
