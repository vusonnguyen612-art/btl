package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục thời trang (quần áo, phụ kiện…).
 * Kế thừa từ {@link Item}, tự động gán category = "FASHION".
 * Các trường đặc thù: brand, size, material, condition, gender.
 */
public class Fashion extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String size;
    private String material;
    private String condition;
    private String gender;

    /**
     * Khởi tạo vật phẩm thời trang với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Fashion(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "FASHION";
        this.brand = "";
        this.size = "";
        this.material = "";
        this.condition = "";
        this.gender = "";
    }

    /**
     * Khởi tạo vật phẩm thời trang với đầy đủ thông tin đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     * @param brand       thương hiệu
     * @param size        kích cỡ (S, M, L, XL...)
     * @param material    chất liệu vải
     * @param condition   tình trạng (mới, đã qua sử dụng...)
     * @param gender      giới tính (nam, nữ, unisex)
     */
    public Fashion(String id, String name, String description, double startPrice,
                   String sellerId, String brand, String size, String material, String condition, String gender) {
        super(id, name, description, startPrice, sellerId);
        this.category = "FASHION";
        this.brand = brand;
        this.size = size;
        this.material = material;
        this.condition = condition;
        this.gender = gender;
    }

    /** @return thương hiệu thời trang */
    public String getBrand() {
        return brand;
    }

    /** @param brand thương hiệu mới */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /** @return kích cỡ */
    public String getSize() {
        return size;
    }

    /** @return chất liệu */
    public String getMaterial() {
        return material;
    }

    /** @return tình trạng */
    public String getCondition() {
        return condition;
    }

    /** @return giới tính (nam, nữ, unisex) */
    public String getGender() {
        return gender;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm thời trang.
     *
     * @return chuỗi gồm brand, size, material, condition, gender
     */
    public String getSpecificInfo() {
        return String.format("Brand: %s, Size: %s, Material: %s, Condition: %s, Gender: %s",
                brand, size, material, condition, gender);
    }
}
