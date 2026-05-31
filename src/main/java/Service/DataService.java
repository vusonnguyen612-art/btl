package Service;

import java.io.*;

public class DataService {
    private static final String DATA_FILE = "auction_data.ser";

    public void saveData(AuctionManager manager) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(manager);
        }
    }

    public AuctionManager loadData() throws IOException, ClassNotFoundException {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return AuctionManager.getInstance();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            AuctionManager instance = (AuctionManager) ois.readObject();
            AuctionManager.setInstance(instance);
            return instance;
        }
    }
}
