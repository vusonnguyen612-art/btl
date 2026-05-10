package Model;

/** Vật phẩm danh mục trang sức (nhẫn, vòng cổ…). */
public class Jewelry extends Item {
    private static final long serialVersionUID = 1L;
    private String material;
    private String gemstone;
    private double weight;
    private String brand;
    private String condition;

    public Jewelry(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "JEWELRY";
        this.material = "";
        this.gemstone = "";
        this.weight = 0;
        this.brand = "";
        this.condition = "";
    }

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

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getGemstone() {
        return gemstone;
    }

    public double getWeight() {
        return weight;
    }

    public String getBrand() {
        return brand;
    }

    public String getCondition() {
        return condition;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Material: %s, Gemstone: %s, Weight: %.1fg, Brand: %s, Condition: %s",
                material, gemstone, weight, brand, condition);
    }
}
