package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục thể thao (dụng cụ, trang phục thể thao…).
 * Kế thừa từ {@link Item}, tự động gán category = "SPORTS".
 * Các trường đặc thù: brand, sportType, condition, material.
 */
public class Sports extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String sportType;
    private String condition;
    private String material;

    /**
     * Khởi tạo vật phẩm thể thao với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Sports(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "SPORTS";
        this.brand = "";
        this.sportType = "";
        this.condition = "";
        this.material = "";
    }

    /**
     * Khởi tạo vật phẩm thể thao với đầy đủ thông tin đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     * @param brand       thương hiệu
     * @param sportType   loại thể thao (bóng đá, tennis...)
     * @param condition   tình trạng
     * @param material    chất liệu
     */
    public Sports(String id, String name, String description, double startPrice,
                  String sellerId, String brand, String sportType, String condition, String material) {
        super(id, name, description, startPrice, sellerId);
        this.category = "SPORTS";
        this.brand = brand;
        this.sportType = sportType;
        this.condition = condition;
        this.material = material;
    }

    /** @return thương hiệu */
    public String getBrand() {
        return brand;
    }

    /** @param brand thương hiệu mới */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /** @return loại thể thao */
    public String getSportType() {
        return sportType;
    }

    /** @return tình trạng */
    public String getCondition() {
        return condition;
    }

    /** @return chất liệu */
    public String getMaterial() {
        return material;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm thể thao.
     *
     * @return chuỗi gồm brand, sportType, condition, material
     */
    public String getSpecificInfo() {
        return String.format("Brand: %s, Sport: %s, Condition: %s, Material: %s",
                brand, sportType, condition, material);
    }
}
