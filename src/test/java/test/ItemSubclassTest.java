package test;

import Model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemSubclassTest {

    @Test
    void testAllCategories_SetCorrectCategory() {
        assertEquals("ELECTRONICS", new Electronics("id", "n", "d", 1, "s").getCategory());
        assertEquals("ART", new Art("id", "n", "d", 1, "s").getCategory());
        assertEquals("VEHICLE", new Vehicle("id", "n", "d", 1, "s").getCategory());
        assertEquals("FASHION", new Fashion("id", "n", "d", 1, "s").getCategory());
        assertEquals("BOOKS", new Books("id", "n", "d", 1, "s").getCategory());
        assertEquals("SPORTS", new Sports("id", "n", "d", 1, "s").getCategory());
        assertEquals("JEWELRY", new Jewelry("id", "n", "d", 1, "s").getCategory());
        assertEquals("MUSIC", new Music("id", "n", "d", 1, "s").getCategory());
        assertEquals("FURNITURE", new Furniture("id", "n", "d", 1, "s").getCategory());
    }

    @Test
    void testItemToString_Format() {
        Item item = new Electronics("ELC001", "Laptop", "Gaming laptop", 999.99, "seller1");
        String str = item.toString();
        assertTrue(str.contains("ELC001"));
        assertTrue(str.contains("Laptop"));
        assertTrue(str.contains("999.99"));
    }

    @Test
    void testGetSpecificInfo_NotEmpty() {
        Item item = new Electronics("ELC001", "Laptop", "Gaming laptop", 999.99, "seller1",
                "Dell", 24, "XPS", "New");
        String info = item.getSpecificInfo();
        assertNotNull(info);
        assertTrue(info.contains("Dell"));
        assertTrue(info.contains("XPS"));
        assertTrue(info.contains("24"));
        assertTrue(info.contains("New"));
    }

    @Test
    void testAllSubclasses_HaveSpecificInfo() {
        assertNotNull(new Electronics("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Art("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Vehicle("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Fashion("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Books("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Sports("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Jewelry("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Music("id", "n", "d", 1, "s").getSpecificInfo());
        assertNotNull(new Furniture("id", "n", "d", 1, "s").getSpecificInfo());
    }
}
