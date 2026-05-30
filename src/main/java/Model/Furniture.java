package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục nội thất (bàn ghế, tủ…).
 * Kế thừa từ {@link Item}, tự động gán category = "FURNITURE".
 * Các trường đặc thù: brand, material, color, dimensions, condition.
 */
public class Furniture extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String material;
    private String color;
    private String dimensions;
    private String condition;

    /**
     * Khởi tạo vật phẩm nội thất với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Furniture(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "FURNITURE";
        this.brand = "";
        this.material = "";
        this.color = "";
        this.dimensions = "";
        this.condition = "";
    }

    /**
     * Khởi tạo vật phẩm nội thất với đầy đủ thông tin đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     * @param brand       thương hiệu
     * @param material    chất liệu (gỗ, kim loại...)
     * @param color       màu sắc
     * @param dimensions  kích thước (dài x rộng x cao)
     * @param condition   tình trạng
     */
    public Furniture(String id, String name, String description, double startPrice,
                     String sellerId, String brand, String material, String color, String dimensions, String condition) {
        super(id, name, description, startPrice, sellerId);
        this.category = "FURNITURE";
        this.brand = brand;
        this.material = material;
        this.color = color;
        this.dimensions = dimensions;
        this.condition = condition;
    }

    /** @return thương hiệu */
    public String getBrand() {
        return brand;
    }

    /** @param brand thương hiệu mới */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /** @return chất liệu */
    public String getMaterial() {
        return material;
    }

    /** @return màu sắc */
    public String getColor() {
        return color;
    }

    /** @return kích thước */
    public String getDimensions() {
        return dimensions;
    }

    /** @return tình trạng */
    public String getCondition() {
        return condition;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm nội thất.
     *
     * @return chuỗi gồm brand, material, color, dimensions, condition
     */
    public String getSpecificInfo() {
        return String.format("Brand: %s, Material: %s, Color: %s, Dimensions: %s, Condition: %s",
                brand, material, color, dimensions, condition);
    }
}
