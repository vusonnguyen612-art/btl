package Model;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private int warrantyMonths;
    private String model;
    private String condition;

    public Electronics(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "ELECTRONICS";
        this.brand = "";
        this.warrantyMonths = 0;
        this.model = "";
        this.condition = "";
    }

    public Electronics(String id, String name, String description, double startPrice,
                       String sellerId, String brand, int warrantyMonths, String model, String condition) {
        super(id, name, description, startPrice, sellerId);
        this.category = "ELECTRONICS";
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.model = model;
        this.condition = condition;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public String getModel() {
        return model;
    }

    public String getCondition() {
        return condition;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Brand: %s, Model: %s, Warranty: %d months, Condition: %s",
                brand, model, warrantyMonths, condition);
    }
}
