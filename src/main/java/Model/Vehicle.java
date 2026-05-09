package Model;

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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public int getMileage() {
        return mileage;
    }

    public String getFuelType() {
        return fuelType;
    }

    public String getTransmission() {
        return transmission;
    }

    public String getColor() {
        return color;
    }

    public String getCondition() {
        return condition;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Brand: %s %s, Year: %d, Mileage: %,d km, Fuel: %s, Transmission: %s, Color: %s, Condition: %s",
                brand, model, year, mileage, fuelType, transmission, color, condition);
    }
}
