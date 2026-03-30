import Model.*;
import Service.*;

public class Main {
    public static void main(String[] args) {

        Item item = new Item("Laptop", "Gaming", 100);
        AuctionService service = new AuctionService(null);

        service.createAuction(item);

        service.placeBid("A", 120);
        service.placeBid("B", 110);
        service.placeBid("C", 150);

        service.placeBid("D", 140); // fail
    }
}
