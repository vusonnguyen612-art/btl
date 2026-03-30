package Model;

public class Item {
    private String name;
    private String description;
    private int startPrice;

    public Item(String name, String description, int startPrice) {
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
    }

    public String getName() {
        return name;
    }

    public int getStartPrice() {
        return startPrice;
    }
}
