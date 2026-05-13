package test;

import Factory.ItemFactory;
import Model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void testCreateElectronics_ReturnsCorrectType() {
        Item item = ItemFactory.createElectronics("Phone", "Smartphone", 999.0,
                "seller1", "Apple", 12, "iPhone 15", "New");
        assertInstanceOf(Electronics.class, item);
        assertTrue(item.getId().startsWith("ELC"));
        assertEquals("ELECTRONICS", item.getCategory());
    }

    @Test
    void testCreateArt_ReturnsCorrectType() {
        Item item = ItemFactory.createArt("Painting", "Oil painting", 2000.0,
                "seller1", "Picasso", 2024, "Oil", "Modern");
        assertInstanceOf(Art.class, item);
        assertTrue(item.getId().startsWith("ART"));
        assertEquals("ART", item.getCategory());
    }

    @Test
    void testCreateVehicle_ReturnsCorrectType() {
        Item item = ItemFactory.createVehicle("Car", "Sedan", 25000.0,
                "seller1", "Toyota", "Camry", 2024, 0, "Petrol", "Automatic", "Red", "New");
        assertInstanceOf(Vehicle.class, item);
        assertTrue(item.getId().startsWith("VEH"));
        assertEquals("VEHICLE", item.getCategory());
    }

    @Test
    void testCreateFashion_ReturnsCorrectType() {
        Item item = ItemFactory.createFashion("Shirt", "Cotton shirt", 29.99,
                "seller1", "Nike", "L", "Cotton", "New", "Male");
        assertInstanceOf(Fashion.class, item);
        assertTrue(item.getId().startsWith("FSH"));
        assertEquals("FASHION", item.getCategory());
    }

    @Test
    void testCreateBooks_ReturnsCorrectType() {
        Item item = ItemFactory.createBooks("Java Book", "Learn Java", 39.99,
                "seller1", "Author", "Publisher", "Education", 500, "123456");
        assertInstanceOf(Books.class, item);
        assertTrue(item.getId().startsWith("BOK"));
        assertEquals("BOOKS", item.getCategory());
    }

    @Test
    void testCreateSports_ReturnsCorrectType() {
        Item item = ItemFactory.createSports("Football", "Leather ball", 49.99,
                "seller1", "Adidas", "Soccer", "New", "Leather");
        assertInstanceOf(Sports.class, item);
        assertTrue(item.getId().startsWith("SPT"));
        assertEquals("SPORTS", item.getCategory());
    }

    @Test
    void testCreateJewelry_ReturnsCorrectType() {
        Item item = ItemFactory.createJewelry("Ring", "Gold ring", 999.99,
                "seller1", "Gold", "Diamond", 5.0, "Tiffany", "New");
        assertInstanceOf(Jewelry.class, item);
        assertTrue(item.getId().startsWith("JWL"));
        assertEquals("JEWELRY", item.getCategory());
    }

    @Test
    void testCreateMusic_ReturnsCorrectType() {
        Item item = ItemFactory.createMusic("Album", "Rock album", 19.99,
                "seller1", "Artist", "Rock", "CD", 2024, "Label");
        assertInstanceOf(Music.class, item);
        assertTrue(item.getId().startsWith("MSC"));
        assertEquals("MUSIC", item.getCategory());
    }

    @Test
    void testCreateFurniture_ReturnsCorrectType() {
        Item item = ItemFactory.createFurniture("Table", "Wooden table", 199.99,
                "seller1", "IKEA", "Wood", "Brown", "120x60", "New");
        assertInstanceOf(Furniture.class, item);
        assertTrue(item.getId().startsWith("FNT"));
        assertEquals("FURNITURE", item.getCategory());
    }

    @Test
    void testCreateItem_ByCategory_Electronics() {
        Item item = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertInstanceOf(Electronics.class, item);
    }

    @Test
    void testCreateItem_ByCategory_CaseInsensitive() {
        Item item1 = ItemFactory.createItem("electronics", "Phone", "Smartphone", 999.0, "seller1");
        Item item2 = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertInstanceOf(Electronics.class, item1);
        assertInstanceOf(Electronics.class, item2);
    }

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

    @Test
    void testCreateItem_UnknownCategory_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.createItem("INVALID", "a", "b", 1, "s"));
    }

    @Test
    void testCreateItem_SetsBasicFields() {
        Item item = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertEquals("Phone", item.getName());
        assertEquals("Smartphone", item.getDescription());
        assertEquals(999.0, item.getStartPrice(), 0.001);
        assertEquals("seller1", item.getSellerId());
    }

    @Test
    void testCreateItem_DefaultCategoryDetails() {
        Item electronics = ItemFactory.createItem("ELECTRONICS", "Phone", "Smartphone", 999.0, "seller1");
        assertEquals("ELECTRONICS", electronics.getCategory());
        assertNotNull(electronics.getSpecificInfo());
    }
}
