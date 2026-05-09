package Factory;

import Model.*;

public class ItemFactory {
    private static int electronicsCounter = 0;
    private static int artCounter = 0;
    private static int vehicleCounter = 0;
    private static int fashionCounter = 0;
    private static int booksCounter = 0;
    private static int sportsCounter = 0;
    private static int jewelryCounter = 0;
    private static int musicCounter = 0;
    private static int furnitureCounter = 0;

    public static Item createElectronics(String name, String description, double startPrice,
                                         String sellerId, String brand, int warrantyMonths,
                                         String model, String condition) {
        String id = "ELC" + String.format("%04d", ++electronicsCounter);
        return new Electronics(id, name, description, startPrice, sellerId, 
                brand, warrantyMonths, model, condition);
    }

    public static Item createArt(String name, String description, double startPrice,
                                 String sellerId, String artist, int yearCreated,
                                 String medium, String style) {
        String id = "ART" + String.format("%04d", ++artCounter);
        return new Art(id, name, description, startPrice, sellerId, 
                artist, yearCreated, medium, style);
    }

    public static Item createVehicle(String name, String description, double startPrice,
                                     String sellerId, String brand, String model, int year,
                                     int mileage, String fuelType, String transmission,
                                     String color, String condition) {
        String id = "VEH" + String.format("%04d", ++vehicleCounter);
        return new Vehicle(id, name, description, startPrice, sellerId, 
                brand, model, year, mileage, fuelType, transmission, color, condition);
    }

    public static Item createFashion(String name, String description, double startPrice,
                                     String sellerId, String brand, String size,
                                     String material, String condition, String gender) {
        String id = "FSH" + String.format("%04d", ++fashionCounter);
        return new Fashion(id, name, description, startPrice, sellerId,
                brand, size, material, condition, gender);
    }

    public static Item createBooks(String name, String description, double startPrice,
                                   String sellerId, String author, String publisher,
                                   String genre, int pageCount, String isbn) {
        String id = "BOK" + String.format("%04d", ++booksCounter);
        return new Books(id, name, description, startPrice, sellerId,
                author, publisher, genre, pageCount, isbn);
    }

    public static Item createSports(String name, String description, double startPrice,
                                    String sellerId, String brand, String sportType,
                                    String condition, String material) {
        String id = "SPT" + String.format("%04d", ++sportsCounter);
        return new Sports(id, name, description, startPrice, sellerId,
                brand, sportType, condition, material);
    }

    public static Item createJewelry(String name, String description, double startPrice,
                                     String sellerId, String material, String gemstone,
                                     double weight, String brand, String condition) {
        String id = "JWL" + String.format("%04d", ++jewelryCounter);
        return new Jewelry(id, name, description, startPrice, sellerId,
                material, gemstone, weight, brand, condition);
    }

    public static Item createMusic(String name, String description, double startPrice,
                                   String sellerId, String artist, String genre,
                                   String format, int releaseYear, String label) {
        String id = "MSC" + String.format("%04d", ++musicCounter);
        return new Music(id, name, description, startPrice, sellerId,
                artist, genre, format, releaseYear, label);
    }

    public static Item createFurniture(String name, String description, double startPrice,
                                       String sellerId, String brand, String material,
                                       String color, String dimensions, String condition) {
        String id = "FNT" + String.format("%04d", ++furnitureCounter);
        return new Furniture(id, name, description, startPrice, sellerId,
                brand, material, color, dimensions, condition);
    }

    public static Item createItem(String category, String name, String description, 
                                  double startPrice, String sellerId) {
        switch (category.toUpperCase()) {
            case "ELECTRONICS":
                return createElectronics(name, description, startPrice, sellerId, 
                        "Unknown", 12, "Unknown", "New");
            case "ART":
                return createArt(name, description, startPrice, sellerId, 
                        "Unknown", 2024, "Unknown", "Unknown");
            case "VEHICLE":
                return createVehicle(name, description, startPrice, sellerId, 
                        "Unknown", "Unknown", 2024, 0, "Petrol", "Automatic", "Unknown", "New");
            case "FASHION":
                return createFashion(name, description, startPrice, sellerId,
                        "Unknown", "M", "Unknown", "New", "Unisex");
            case "BOOKS":
                return createBooks(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", 0, "Unknown");
            case "SPORTS":
                return createSports(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "New", "Unknown");
            case "JEWELRY":
                return createJewelry(name, description, startPrice, sellerId,
                        "Unknown", "None", 0, "Unknown", "New");
            case "MUSIC":
                return createMusic(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", 2024, "Unknown");
            case "FURNITURE":
                return createFurniture(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", "Unknown", "New");
            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
    }
}
