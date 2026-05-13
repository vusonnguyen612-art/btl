package test;

import Factory.UserFactory;
import Model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    @Test
    void testIsValidPassword_Null_ReturnsFalse() {
        assertFalse(UserFactory.isValidPassword(null));
    }

    @Test
    void testIsValidPassword_TooShort_ReturnsFalse() {
        assertFalse(UserFactory.isValidPassword("abc"));
        assertFalse(UserFactory.isValidPassword("ab"));
        assertFalse(UserFactory.isValidPassword(""));
    }

    @Test
    void testIsValidPassword_ContainsWhitespace_ReturnsFalse() {
        assertFalse(UserFactory.isValidPassword("pass word"));
        assertFalse(UserFactory.isValidPassword(" password"));
        assertFalse(UserFactory.isValidPassword("password "));
    }

    @Test
    void testIsValidPassword_Valid_ReturnsTrue() {
        assertTrue(UserFactory.isValidPassword("password123"));
        assertTrue(UserFactory.isValidPassword("abcdef"));
    }

    @Test
    void testGetPasswordError_Empty() {
        assertEquals("Password cannot be empty", UserFactory.getPasswordError(""));
    }

    @Test
    void testGetPasswordError_Null() {
        assertEquals("Password cannot be empty", UserFactory.getPasswordError(null));
    }

    @Test
    void testGetPasswordError_TooShort() {
        assertEquals("Password must be at least 6 characters",
                UserFactory.getPasswordError("abc"));
    }

    @Test
    void testGetPasswordError_HasWhitespace() {
        assertEquals("Password cannot contain whitespace",
                UserFactory.getPasswordError("pass word"));
    }

    @Test
    void testGetPasswordError_Valid_ReturnsNull() {
        assertNull(UserFactory.getPasswordError("validPass123"));
    }

    @Test
    void testCreateUser_GeneratesCorrectId() {
        User user = UserFactory.createUser("testuser", "password123");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertTrue(user.getId().startsWith("USR"));
        assertEquals(4, user.getId().length() - 3);
    }

    @Test
    void testCreateUser_SetsRoleDefaults() {
        User user = UserFactory.createUser("testuser", "password123");
        assertTrue(user.isSeller());
        assertTrue(user.isBidder());
        assertFalse(user.isAdmin());
        assertEquals("BIDDER_SELLER", user.getRole());
    }

    @Test
    void testCreateUser_HasInitialBalance() {
        User user = UserFactory.createUser("testuser", "password123");
        assertEquals(300000, user.getBalance().doubleValue(), 0.001);
    }
}
