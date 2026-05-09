package Model;

public class Sports extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String sportType;
    private String condition;
    private String material;

    public Sports(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "SPORTS";
        this.brand = "";
        this.sportType = "";
        this.condition = "";
        this.material = "";
    }

    public Sports(String id, String name, String description, double startPrice,
                  String sellerId, String brand, String sportType, String condition, String material) {
        super(id, name, description, startPrice, sellerId);
        this.category = "SPORTS";
        this.brand = brand;
        this.sportType = sportType;
        this.condition = condition;
        this.material = material;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSportType() {
        return sportType;
    }

    public String getCondition() {
        return condition;
    }

    public String getMaterial() {
        return material;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Brand: %s, Sport: %s, Condition: %s, Material: %s",
                brand, sportType, condition, material);
    }
}
