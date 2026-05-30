package test;

import Factory.ItemFactory;
import Model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử đơn vị cho {@link ItemFactory}.
 * <p>
 * Kiểm thử các phương thức factory tạo từng loại vật phẩm con (subclass) và
 * phương thức tổng quát {@link ItemFactory#createItem(String, String, String, double, String)}:
 * <ul>
 *   <li>9 phương thức factory chuyên biệt — mỗi loại trả về đúng subclass và prefix ID</li>
 *   <li>Phương thức tổng quát ({@code createItem}) — phân loại theo category (không phân biệt hoa thường)</li>
 *   <li>Category không hợp lệ — ném {@link IllegalArgumentException}</li>
 *   <li>Thiết lập các trường cơ bản (tên, mô tả, giá, người bán)</li>
 *   <li>Thông tin đặc thù của từng loại (specific info) không null</li>
 * </ul>
 */
class ItemFactoryTest {

    // ── Factory methods chuyên biệt ─────────────────────────

    /**
     * Kiểm thử {@link ItemFactory#createElectronics}: trả về {@link Electronics},
     * ID bắt đầu bằng "ELC", category là "ELECTRONICS".
     */
    @Test
    void testCreateElectronics_ReturnsCorrectType() {
        Item item = ItemFactory.createElectronics("Phone", "Smartphone", 999.0,
                "seller1", "Apple", 12, "iPhone 15", "New");
        assertInstanceOf(Electronics.class, item);
        assertTrue(item.getId().startsWith("ELC"));
        assertEquals("ELECTRONICS", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createArt}: trả về {@link Art},
     * ID bắt đầu bằng "ART", category là "ART".
     */
    @Test
    void testCreateArt_ReturnsCorrectType() {
        Item item = ItemFactory.createArt("Painting", "Oil painting", 2000.0,
                "seller1", "Picasso", 2024, "Oil", "Modern");
        assertInstanceOf(Art.class, item);
        assertTrue(item.getId().startsWith("ART"));
        assertEquals("ART", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createVehicle}: trả về {@link Vehicle},
     * ID bắt đầu bằng "VEH", category là "VEHICLE".
     */
    @Test
    void testCreateVehicle_ReturnsCorrectType() {
        Item item = ItemFactory.createVehicle("Car", "Sedan", 25000.0,
                "seller1", "Toyota", "Camry", 2024, 0, "Petrol", "Automatic", "Red", "New");
        assertInstanceOf(Vehicle.class, item);
        assertTrue(item.getId().startsWith("VEH"));
        assertEquals("VEHICLE", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createFashion}: trả về {@link Fashion},
     * ID bắt đầu bằng "FSH", category là "FASHION".
     */
    @Test
    void testCreateFashion_ReturnsCorrectType() {
        Item item = ItemFactory.createFashion("Shirt", "Cotton shirt", 29.99,
                "seller1", "Nike", "L", "Cotton", "New", "Male");
        assertInstanceOf(Fashion.class, item);
        assertTrue(item.getId().startsWith("FSH"));
        assertEquals("FASHION", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createBooks}: trả về {@link Books},
     * ID bắt đầu bằng "BOK", category là "BOOKS".
     */
    @Test
    void testCreateBooks_ReturnsCorrectType() {
        Item item = ItemFactory.createBooks("Java Book", "Learn Java", 39.99,
                "seller1", "Author", "Publisher", "Education", 500, "123456");
        assertInstanceOf(Books.class, item);
        assertTrue(item.getId().startsWith("BOK"));
        assertEquals("BOOKS", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createSports}: trả về {@link Sports},
     * ID bắt đầu bằng "SPT", category là "SPORTS".
     */
    @Test
    void testCreateSports_ReturnsCorrectType() {
        Item item = ItemFactory.createSports("Football", "Leather ball", 49.99,
                "seller1", "Adidas", "Soccer", "New", "Leather");
        assertInstanceOf(Sports.class, item);
        assertTrue(item.getId().startsWith("SPT"));
        assertEquals("SPORTS", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createJewelry}: trả về {@link Jewelry},
     * ID bắt đầu bằng "JWL", category là "JEWELRY".
     */
    @Test
    void testCreateJewelry_ReturnsCorrectType() {
        Item item = ItemFactory.createJewelry("Ring", "Gold ring", 999.99,
                "seller1", "Gold", "Diamond", 5.0, "Tiffany", "New");
        assertInstanceOf(Jewelry.class, item);
        assertTrue(item.getId().startsWith("JWL"));
        assertEquals("JEWELRY", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createMusic}: trả về {@link Music},
     * ID bắt đầu bằng "MSC", category là "MUSIC".
     */
    @Test
    void testCreateMusic_ReturnsCorrectType() {
        Item item = ItemFactory.createMusic("Album", "Rock album", 19.99,
                "seller1", "Artist", "Rock", "CD", 2024, "Label");
        assertInstanceOf(Music.class, item);
        assertTrue(item.getId().startsWith("MSC"));
        assertEquals("MUSIC", item.getCategory());
    }

    /**
     * Kiểm thử {@link ItemFactory#createFurniture}: trả về {@link Furniture},
     * ID bắt đầu bằng "FNT", category là "FURNITURE".
     */
    @Test
    void testCreateFurniture_ReturnsCorrectType() {
        Item item = ItemFactory.createFurniture("Table", "Wooden table", 199.99,
                "seller1", "IKEA", "Wood", "Brown", "120x60", "New");
        assertInstanceOf(Furniture.class, item);
        assertTrue(item.getId().startsWith("FNT"));
        assertEquals("FURNITURE", item.getCategory());
    }

    // ── Phương thức tổng quát createItem ────────────────────

    /**
     * Kiểm thử {@link ItemFactory#createItem} với category "ELECTRONICS"
     * — trả về đúng {@link Electronics}.
     */
    @Test
    void testCreateItem_ByCategory_Electronics() {
        Item item = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertInstanceOf(Electronics.class, item);
    }

    /**
     * Kiểm thử {@code createItem} không phân biệt hoa thường (case-insensitive).
     * Cả "electronics" và "ELECTRONICS" đều trả về {@link Electronics}.
     */
    @Test
    void testCreateItem_ByCategory_CaseInsensitive() {
        Item item1 = ItemFactory.createItem("electronics", "Phone", "Smartphone", 999.0, "seller1");
        Item item2 = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertInstanceOf(Electronics.class, item1);
        assertInstanceOf(Electronics.class, item2);
    }

    /**
     * Kiểm thử cả 9 category hợp lệ đều trả về đúng subclass tương ứng.
     */
    @Test
    void testCreateItem_AllCategories() {
        assertInstanceOf(Electronics.class, ItemFactory.createItem("ELECTRONICS", "a", "b", 1, "s"));
        assertInstanceOf(Art.class, ItemFactory.createItem("ART", "a", "b", 1, "s"));
        assertInstanceOf(Vehicle.class, ItemFactory.createItem("VEHICLE", "a", "b", 1, "s"));
        assertInstanceOf(Fashion.class, ItemFactory.createItem("FASHION", "a", "b", 1, "s"));
        assertInstanceOf(Books.class, ItemFactory.createItem("BOOKS", "a", "b", 1, "s"));
        assertInstanceOf(Sports.class, ItemFactory.createItem("SPORTS", "a", "b", 1, "s"));
        assertInstanceOf(Jewelry.class, ItemFactory.createItem("JEWELRY", "a", "b", 1, "s"));
        assertInstanceOf(Music.class, ItemFactory.createItem("MUSIC", "a", "b", 1, "s"));
        assertInstanceOf(Furniture.class, ItemFactory.createItem("FURNITURE", "a", "b", 1, "s"));
    }

    /**
     * Kiểm thử category không hợp lệ — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testCreateItem_UnknownCategory_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("INVALID", "a", "b", 1, "s"));
    }

    /**
     * Kiểm thử {@code createItem} thiết lập đúng các trường cơ bản:
     * tên, mô tả, giá khởi điểm, người bán.
     */
    @Test
    void testCreateItem_SetsBasicFields() {
        Item item = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertEquals("Phone", item.getName());
        assertEquals("Smartphone", item.getDescription());
        assertEquals(999.0, item.getStartPrice(), 0.001);
        assertEquals("seller1", item.getSellerId());
    }

    /**
     * Kiểm thử {@code createItem} gán đúng category và thông tin đặc thù (specific info) không null.
     */
    @Test
    void testCreateItem_DefaultCategoryDetails() {
        Item electronics = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertEquals("ELECTRONICS", electronics.getCategory());
        assertNotNull(electronics.getSpecificInfo());
    }

    // ═══════════════════════════════════════════════════════════════
    // 🟡 NEGATIVE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử {@code createItem} với tên null — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testCreateItem_NullName_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", null, "desc", 100.0, "seller"));
    }

    /**
     * Kiểm thử {@code createItem} với mô tả null — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testCreateItem_NullDescription_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", "name", null, 100.0, "seller"));
    }

    /**
     * Kiểm thử {@code createItem} với giá âm — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testCreateItem_NegativePrice_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", "name", "desc", -1.0, "seller"));
    }

    /**
     * Kiểm thử {@code createItem} với giá bằng 0 — phải ném {@link IllegalArgumentException}
     * vì giá khởi điểm phải dương.
     */
    @Test
    void testCreateItem_ZeroPrice_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("ELECTRONICS", "name", "desc", 0.0, "seller"));
    }
}
