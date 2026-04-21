package Factory;

import Model.*;

public class ItemFactory {
    private static int electronicsCounter = 0;
    private static int artCounter = 0;
    private static int vehicleCounter = 0;

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
            default:
                throw new IllegalArgumentException("Unknown category: " + category);
        }
    }
}
