package Model;

import java.io.Serializable;

public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String description;
    private double startPrice;
    private String sellerId;
    protected String category;
    private String imagePath;

    public Item(String id, String name, String description, double startPrice, String sellerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.sellerId = sellerId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getCategory() {
        return category;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public abstract String getSpecificInfo();

    @Override
    public String toString() {
        return String.format("[%s] %s - %s - Start: $%.2f", id, name, description, startPrice);
    }
}
