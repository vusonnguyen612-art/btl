package Model;

public class Item {
    private String name;
    private String description;
    private int startPrice;
    private String sellerID;
    private boolean isEnded;

    public Item(String name, String description, int startPrice, String sellerID) {
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.sellerID=sellerID;
        this.isEnded=false;
    }

    public String getName() {
        return name;
    }

    public int getStartPrice() {
        return startPrice;
    }

    public String getSellerID() {
        return sellerID;
    }

    public void endAuction(){
        this.isEnded=true;
    }
}
