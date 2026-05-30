package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục xe cộ (ô tô, xe máy…).
 * Kế thừa từ {@link Item}, tự động gán category = "VEHICLE".
 * Các trường đặc thù: brand, model, year, mileage, fuelType, transmission, color, condition.
 */
public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
private String brand;
    private String model;
    private int year;
    private int mileage;
    private String fuelType;
    private String transmission;
    private String color;
    private String condition;

    /**
     * Khởi tạo vật phẩm xe cộ với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Vehicle(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "VEHICLE";
        this.brand = "";
        this.model = "";
        this.year = 0;
        this.mileage = 0;
        this.fuelType = "";
        this.transmission = "";
        this.color = "";
        this.condition = "";
    }

    /**
     * Khởi tạo vật phẩm xe cộ với đầy đủ thông tin đặc thù.
     *
     * @param id           mã vật phẩm
     * @param name         tên vật phẩm
     * @param description  mô tả
     * @param startPrice   giá khởi điểm
     * @param sellerId     ID người bán
     * @param brand        thương hiệu xe
     * @param model        model xe
     * @param year         năm sản xuất
     * @param mileage      số km đã đi
     * @param fuelType     loại nhiên liệu (xăng, dầu, điện...)
     * @param transmission loại hộp số (tự động, sàn)
     * @param color        màu sắc
     * @param condition    tình trạng xe
     */
    public Vehicle(String id, String name, String description, double startPrice,
                      String sellerId, String brand, String model, int year,
                      int mileage, String fuelType, String transmission, String color, String condition) {
        super(id, name, description, startPrice, sellerId);
        this.category = "VEHICLE";
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.color = color;
        this.condition = condition;
    }

    /** @return thương hiệu xe */
    public String getBrand() {
        return brand;
    }

    /** @param brand thương hiệu mới */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /** @return model xe */
    public String getModel() {
        return model;
    }

    /** @return năm sản xuất */
    public int getYear() {
        return year;
    }

    /** @return số km đã đi */
    public int getMileage() {
        return mileage;
    }

    /** @return loại nhiên liệu */
    public String getFuelType() {
        return fuelType;
    }

    /** @return loại hộp số */
    public String getTransmission() {
        return transmission;
    }

    /** @return màu sắc */
    public String getColor() {
        return color;
    }

    /** @return tình trạng xe */
    public String getCondition() {
        return condition;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm xe cộ.
     *
     * @return chuỗi gồm brand, model, year, mileage, fuelType, transmission, color, condition
     */
    public String getSpecificInfo() {
        return String.format("Brand: %s %s, Year: %d, Mileage: %,d km, Fuel: %s, Transmission: %s, Color: %s, Condition: %s",
                brand, model, year, mileage, fuelType, transmission, color, condition);
    }
}
