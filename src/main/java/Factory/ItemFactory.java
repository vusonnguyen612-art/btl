package Factory;

import Model.*;
import java.util.EnumMap;
import java.util.Map;

/** Factory tạo các đối tượng Item (theo danh mục) với ID tự sinh. */
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

    private static String nextId(ItemCategory category) {
        int nextValue = counters.getOrDefault(category, 0) + 1;
        counters.put(category, nextValue);
        return category.idPrefix + String.format("%04d", nextValue);
    }

    /** Tạo item danh mục Electronics với ID "ELCxxxx". */
    public static Item createElectronics(String name, String description, double startPrice,
                                         String sellerId, String brand, int warrantyMonths,
                                         String model, String condition) {
        String id = nextId(ItemCategory.ELECTRONICS);
        return new Electronics(id, name, description, startPrice, sellerId, 
                brand, warrantyMonths, model, condition);
    }

    /** Tạo item danh mục Art với ID "ARTxxxx". */
    public static Item createArt(String name, String description, double startPrice,
                                 String sellerId, String artist, int yearCreated,
                                 String medium, String style) {
        String id = nextId(ItemCategory.ART);
        return new Art(id, name, description, startPrice, sellerId, 
                artist, yearCreated, medium, style);
    }

    /** Tạo item danh mục Vehicle với ID "VEHxxxx". */
    public static Item createVehicle(String name, String description, double startPrice,
                                     String sellerId, String brand, String model, int year,
                                     int mileage, String fuelType, String transmission,
                                     String color, String condition) {
        String id = nextId(ItemCategory.VEHICLE);
        return new Vehicle(id, name, description, startPrice, sellerId, 
                brand, model, year, mileage, fuelType, transmission, color, condition);
    }

    /** Tạo item danh mục Fashion với ID "FSHxxxx". */
    public static Item createFashion(String name, String description, double startPrice,
                                     String sellerId, String brand, String size,
                                     String material, String condition, String gender) {
        String id = nextId(ItemCategory.FASHION);
        return new Fashion(id, name, description, startPrice, sellerId,
                brand, size, material, condition, gender);
    }

    /** Tạo item danh mục Books với ID "BOKxxxx". */
    public static Item createBooks(String name, String description, double startPrice,
                                   String sellerId, String author, String publisher,
                                   String genre, int pageCount, String isbn) {
        String id = nextId(ItemCategory.BOOKS);
        return new Books(id, name, description, startPrice, sellerId,
                author, publisher, genre, pageCount, isbn);
    }

    /** Tạo item danh mục Sports với ID "SPTxxxx". */
    public static Item createSports(String name, String description, double startPrice,
                                    String sellerId, String brand, String sportType,
                                    String condition, String material) {
        String id = nextId(ItemCategory.SPORTS);
        return new Sports(id, name, description, startPrice, sellerId,
                brand, sportType, condition, material);
    }

    /** Tạo item danh mục Jewelry với ID "JWLxxxx". */
    public static Item createJewelry(String name, String description, double startPrice,
                                     String sellerId, String material, String gemstone,
                                     double weight, String brand, String condition) {
        String id = nextId(ItemCategory.JEWELRY);
        return new Jewelry(id, name, description, startPrice, sellerId,
                material, gemstone, weight, brand, condition);
    }

    /** Tạo item danh mục Music với ID "MSCxxxx". */
    public static Item createMusic(String name, String description, double startPrice,
                                   String sellerId, String artist, String genre,
                                   String format, int releaseYear, String label) {
        String id = nextId(ItemCategory.MUSIC);
        return new Music(id, name, description, startPrice, sellerId,
                artist, genre, format, releaseYear, label);
    }

    /** Tạo item danh mục Furniture với ID "FNTxxxx". */
    public static Item createFurniture(String name, String description, double startPrice,
                                       String sellerId, String brand, String material,
                                       String color, String dimensions, String condition) {
        String id = nextId(ItemCategory.FURNITURE);
        return new Furniture(id, name, description, startPrice, sellerId,
                brand, material, color, dimensions, condition);
    }

    /**
     * Factory method tổng quát: tạo Item dựa trên tên danh mục (không phân biệt hoa thường).
     * Các thông số chi tiết mặc định (brand="Unknown", condition="New"...).
     *
     * @param category   danh mục (ELECTRONICS, ART, VEHICLE, FASHION, BOOKS, SPORTS, JEWELRY, MUSIC, FURNITURE)
     * @param name       tên sản phẩm
     * @param description mô tả
     * @param startPrice giá khởi điểm
     * @param sellerId   ID người bán
     * @return Item tương ứng
     */
    public static Item createItem(String category, String name, String description, 
                                   double startPrice, String sellerId) {
        return switch (category.toUpperCase()) {
            case "ELECTRONICS" -> createElectronics(name, description, startPrice, sellerId,
                        "Unknown", 12, "Unknown", "New");
            case "ART" -> createArt(name, description, startPrice, sellerId,
                        "Unknown", 2024, "Unknown", "Unknown");
            case "VEHICLE" -> createVehicle(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", 2024, 0, "Petrol", "Automatic", "Unknown", "New");
            case "FASHION" -> createFashion(name, description, startPrice, sellerId,
                        "Unknown", "M", "Unknown", "New", "Unisex");
            case "BOOKS" -> createBooks(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", 0, "Unknown");
            case "SPORTS" -> createSports(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "New", "Unknown");
            case "JEWELRY" -> createJewelry(name, description, startPrice, sellerId,
                        "Unknown", "None", 0, "Unknown", "New");
            case "MUSIC" -> createMusic(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", 2024, "Unknown");
            case "FURNITURE" -> createFurniture(name, description, startPrice, sellerId,
                        "Unknown", "Unknown", "Unknown", "Unknown", "New");
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };
    }
}
