package Model;

public class Fashion extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String size;
    private String material;
    private String condition;
    private String gender;

    public Fashion(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "FASHION";
        this.brand = "";
        this.size = "";
        this.material = "";
        this.condition = "";
        this.gender = "";
    }

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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSize() {
        return size;
    }

    public String getMaterial() {
        return material;
    }

    public String getCondition() {
        return condition;
    }

    public String getGender() {
        return gender;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Brand: %s, Size: %s, Material: %s, Condition: %s, Gender: %s",
                brand, size, material, condition, gender);
    }
}
