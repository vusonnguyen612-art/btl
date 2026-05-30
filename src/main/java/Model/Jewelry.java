package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục trang sức (nhẫn, vòng cổ…).
 * Kế thừa từ {@link Item}, tự động gán category = "JEWELRY".
 * Các trường đặc thù: material, gemstone, weight, brand, condition.
 */
public class Jewelry extends Item {
    private static final long serialVersionUID = 1L;
    private String material;
    private String gemstone;
    private double weight;
    private String brand;
    private String condition;

    /**
     * Khởi tạo vật phẩm trang sức với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Jewelry(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "JEWELRY";
        this.material = "";
        this.gemstone = "";
        this.weight = 0;
        this.brand = "";
        this.condition = "";
    }

    /**
     * Khởi tạo vật phẩm trang sức với đầy đủ thông tin đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     * @param material    chất liệu (vàng, bạc, bạch kim...)
     * @param gemstone    loại đá quý (kim cương, ruby...)
     * @param weight      trọng lượng (gram)
     * @param brand       thương hiệu
     * @param condition   tình trạng
     */
    public Jewelry(String id, String name, String description, double startPrice,
                   String sellerId, String material, String gemstone, double weight, String brand, String condition) {
        super(id, name, description, startPrice, sellerId);
        this.category = "JEWELRY";
        this.material = material;
        this.gemstone = gemstone;
        this.weight = weight;
        this.brand = brand;
        this.condition = condition;
    }

    /** @return chất liệu (vàng, bạc...) */
    public String getMaterial() {
        return material;
    }

    /** @param material chất liệu mới */
    public void setMaterial(String material) {
        this.material = material;
    }

    /** @return loại đá quý */
    public String getGemstone() {
        return gemstone;
    }

    /** @return trọng lượng (gram) */
    public double getWeight() {
        return weight;
    }

    /** @return thương hiệu */
    public String getBrand() {
        return brand;
    }

    /** @return tình trạng */
    public String getCondition() {
        return condition;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm trang sức.
     *
     * @return chuỗi gồm material, gemstone, weight, brand, condition
     */
    public String getSpecificInfo() {
        return String.format("Material: %s, Gemstone: %s, Weight: %.1fg, Brand: %s, Condition: %s",
                material, gemstone, weight, brand, condition);
    }
}
