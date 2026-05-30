package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục điện tử.
 * Kế thừa từ {@link Item}, tự động gán category = "ELECTRONICS".
 * Các trường đặc thù: brand, warrantyMonths, model, condition.
 */
public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private int warrantyMonths;
    private String model;
    private String condition;

    /**
     * Khởi tạo vật phẩm điện tử với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Electronics(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "ELECTRONICS";
        this.brand = "";
        this.warrantyMonths = 0;
        this.model = "";
        this.condition = "";
    }

    /**
     * Khởi tạo vật phẩm điện tử với đầy đủ thông tin đặc thù.
     *
     * @param id             mã vật phẩm
     * @param name           tên vật phẩm
     * @param description    mô tả
     * @param startPrice     giá khởi điểm
     * @param sellerId       ID người bán
     * @param brand          thương hiệu
     * @param warrantyMonths số tháng bảo hành
     * @param model          model/mã sản phẩm
     * @param condition      tình trạng (mới, đã qua sử dụng...)
     */
    public Electronics(String id, String name, String description, double startPrice,
                       String sellerId, String brand, int warrantyMonths, String model, String condition) {
        super(id, name, description, startPrice, sellerId);
        this.category = "ELECTRONICS";
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.model = model;
        this.condition = condition;
    }

    /** @return thương hiệu của thiết bị */
    public String getBrand() {
        return brand;
    }

    /** @param brand thương hiệu mới */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /** @return số tháng bảo hành */
    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    /** @return model/mã sản phẩm */
    public String getModel() {
        return model;
    }

    /** @return tình trạng thiết bị (mới, đã qua sử dụng...) */
    public String getCondition() {
        return condition;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm điện tử.
     *
     * @return chuỗi gồm brand, model, warrantyMonths, condition
     */
    public String getSpecificInfo() {
        return String.format("Brand: %s, Model: %s, Warranty: %d months, Condition: %s",
                brand, model, warrantyMonths, condition);
    }
}
