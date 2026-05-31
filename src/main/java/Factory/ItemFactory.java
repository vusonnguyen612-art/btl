package Factory;

import Model.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ItemFactory {
    private enum ItemCategory {
        ELECTRONICS("ELC"),
        ART("ART"),
        VEHICLE("VEH"),
        FASHION("FSH"),
        BOOKS("BOK"),
        SPORTS("SPT"),
        JEWELRY("JWL"),
        MUSIC("MSC"),
        FURNITURE("FNT");

        private final String idPrefix;

        ItemCategory(String idPrefix) {
            this.idPrefix = idPrefix;
        }
    }

    private static final Map<ItemCategory, Integer> counters = new EnumMap<>(ItemCategory.class);

    private static final Map<String, ItemCategory> CATEGORY_MAP = Map.ofEntries(
        Map.entry("ELECTRONICS", ItemCategory.ELECTRONICS),
        Map.entry("ART", ItemCategory.ART),
        Map.entry("VEHICLE", ItemCategory.VEHICLE),
        Map.entry("FASHION", ItemCategory.FASHION),
        Map.entry("BOOKS", ItemCategory.BOOKS),
        Map.entry("SPORTS", ItemCategory.SPORTS),
        Map.entry("JEWELRY", ItemCategory.JEWELRY),
        Map.entry("MUSIC", ItemCategory.MUSIC),
        Map.entry("FURNITURE", ItemCategory.FURNITURE)
    );



    private static String nextId(ItemCategory category) {
        int nextValue = counters.getOrDefault(category, 0) + 1;
        counters.put(category, nextValue);
        return category.idPrefix + String.format("%04d", nextValue);
    }

    private static ItemCategory resolveCategory(String category) {
        ItemCategory cat = CATEGORY_MAP.get(category.toUpperCase());
        if (cat == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        return cat;
    }

    public static Item createElectronics(String name, String description, double startPrice,
                                          String sellerId, String brand, int warrantyMonths,
                                          String model, String condition) {
        String id = nextId(ItemCategory.ELECTRONICS);
        return new Electronics(id, name, description, startPrice, sellerId,
                brand, warrantyMonths, model, condition);
    }

    public static Item createArt(String name, String description, double startPrice,
                                  String sellerId, String artist, int yearCreated,
                                  String medium, String style) {
        String id = nextId(ItemCategory.ART);
        return new Art(id, name, description, startPrice, sellerId,
                artist, yearCreated, medium, style);
    }

    public static Item createVehicle(String name, String description, double startPrice,
                                      String sellerId, String brand, String model, int year,
                                      int mileage, String fuelType, String transmission,
                                      String color, String condition) {
        String id = nextId(ItemCategory.VEHICLE);
        return new Vehicle(id, name, description, startPrice, sellerId,
                brand, model, year, mileage, fuelType, transmission, color, condition);
    }

    public static Item createFashion(String name, String description, double startPrice,
                                      String sellerId, String brand, String size,
                                      String material, String condition, String gender) {
        String id = nextId(ItemCategory.FASHION);
        return new Fashion(id, name, description, startPrice, sellerId,
                brand, size, material, condition, gender);
    }

    public static Item createBooks(String name, String description, double startPrice,
                                    String sellerId, String author, String publisher,
                                    String genre, int pageCount, String isbn) {
        String id = nextId(ItemCategory.BOOKS);
        return new Books(id, name, description, startPrice, sellerId,
                author, publisher, genre, pageCount, isbn);
    }

    public static Item createSports(String name, String description, double startPrice,
                                     String sellerId, String brand, String sportType,
                                     String condition, String material) {
        String id = nextId(ItemCategory.SPORTS);
        return new Sports(id, name, description, startPrice, sellerId,
                brand, sportType, condition, material);
    }

    public static Item createJewelry(String name, String description, double startPrice,
                                      String sellerId, String material, String gemstone,
                                      double weight, String brand, String condition) {
        String id = nextId(ItemCategory.JEWELRY);
        return new Jewelry(id, name, description, startPrice, sellerId,
                material, gemstone, weight, brand, condition);
    }

    public static Item createMusic(String name, String description, double startPrice,
                                    String sellerId, String artist, String genre,
                                    String format, int releaseYear, String label) {
        String id = nextId(ItemCategory.MUSIC);
        return new Music(id, name, description, startPrice, sellerId,
                artist, genre, format, releaseYear, label);
    }

    public static Item createFurniture(String name, String description, double startPrice,
                                        String sellerId, String brand, String material,
                                        String color, String dimensions, String condition) {
        String id = nextId(ItemCategory.FURNITURE);
        return new Furniture(id, name, description, startPrice, sellerId,
                brand, material, color, dimensions, condition);
    }

    public static Item createItem(String category, String name, String description,
                                   double startPrice, String sellerId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        if (startPrice <= 0) {
            throw new IllegalArgumentException("Start price must be positive");
        }
        return switch (resolveCategory(category)) {
            case ELECTRONICS -> createElectronics(name, description, startPrice, sellerId,
                        "Unknown", 12, "Unknown", "New");
            case ART -> createArt(name, description, startPrice, sellerId,
                        "Unknown", 2024, "Unknown", "Unknown");
            case VEHICLE -> createVehicle(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", 2024, 0, "Petrol", "Automatic", "Unknown", "New");
            case FASHION -> createFashion(name, description, startPrice, sellerId,
                        "Unknown", "M", "Unknown", "New", "Unisex");
            case BOOKS -> createBooks(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", 0, "Unknown");
            case SPORTS -> createSports(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "New", "Unknown");
            case JEWELRY -> createJewelry(name, description, startPrice, sellerId,
                        "Unknown", "None", 0, "Unknown", "New");
            case MUSIC -> createMusic(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", 2024, "Unknown");
            case FURNITURE -> createFurniture(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", "Unknown", "New");
        };
    }
}
