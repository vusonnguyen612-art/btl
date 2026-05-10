package Model;

/** Vật phẩm danh mục nội thất (bàn ghế, tủ…). */
public class Furniture extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String material;
    private String color;
    private String dimensions;
    private String condition;

    public Furniture(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "FURNITURE";
        this.brand = "";
        this.material = "";
        this.color = "";
        this.dimensions = "";
        this.condition = "";
    }

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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getMaterial() {
        return material;
    }

    public String getColor() {
        return color;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getCondition() {
        return condition;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Brand: %s, Material: %s, Color: %s, Dimensions: %s, Condition: %s",
                brand, material, color, dimensions, condition);
    }
}
