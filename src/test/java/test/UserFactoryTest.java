package test;

import Factory.UserFactory;
import Model.User;
import Model.Admin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử đơn vị cho {@link UserFactory}.
 * <p>
 * Kiểm thử:
 * <ul>
 *   <li>Phương thức {@link UserFactory#isValidPassword} — null, quá ngắn, chứa khoảng trắng, hợp lệ</li>
 *   <li>Phương thức {@link UserFactory#getPasswordError} — thông báo lỗi tương ứng từng trường hợp</li>
 *   <li>Tạo {@link User} qua {@link UserFactory#createUser} — ID đúng định dạng "USRxxxx", role mặc định, số dư ban đầu</li>
 *   <li>Tạo {@link Admin} qua {@link UserFactory#createAdmin} — ID đúng định dạng "ADMxxxx", role "ADMIN", isAdmin() = true</li>
 * </ul>
 */
class UserFactoryTest {

    // ── Kiểm thử isValidPassword ────────────────────────────

    /**
     * Kiểm thử {@code isValidPassword(null)} — phải trả về {@code false}.
     */
    @Test
    void testIsValidPassword_Null_ReturnsFalse() {
        assertFalse(UserFactory.isValidPassword(null));
    }

    /**
     * Kiểm thử {@code isValidPassword} với mật khẩu quá ngắn (dưới 6 ký tự) — phải trả về {@code false}.
     */
    @Test
    void testIsValidPassword_TooShort_ReturnsFalse() {
        assertFalse(UserFactory.isValidPassword("abc"));
        assertFalse(UserFactory.isValidPassword("ab"));
        assertFalse(UserFactory.isValidPassword(""));
    }

    /**
     * Kiểm thử {@code isValidPassword} với mật khẩu chứa khoảng trắng — phải trả về {@code false}.
     */
    @Test
    void testIsValidPassword_ContainsWhitespace_ReturnsFalse() {
        assertFalse(UserFactory.isValidPassword("pass word"));
        assertFalse(UserFactory.isValidPassword(" password"));
        assertFalse(UserFactory.isValidPassword("password "));
    }

    /**
     * Kiểm thử {@code isValidPassword} với mật khẩu hợp lệ (>= 6 ký tự, không khoảng trắng) — phải trả về {@code true}.
     */
    @Test
    void testIsValidPassword_Valid_ReturnsTrue() {
        assertTrue(UserFactory.isValidPassword("password123"));
        assertTrue(UserFactory.isValidPassword("abcdef"));
    }

    // ── Kiểm thử getPasswordError ──────────────────────────

    /**
     * Kiểm thử {@code getPasswordError("")} trả về thông báo tiếng Việt tương ứng.
     */
    @Test
    void testGetPasswordError_Empty() {
        assertEquals("Mật khẩu không được để trống.", UserFactory.getPasswordError(""));
    }

    /**
     * Kiểm thử {@code getPasswordError(null)} trả về thông báo tiếng Việt tương ứng.
     */
    @Test
    void testGetPasswordError_Null() {
        assertEquals("Mật khẩu không được để trống.", UserFactory.getPasswordError(null));
    }

    /**
     * Kiểm thử {@code getPasswordError} với mật khẩu quá ngắn — trả về thông báo tiếng Việt tương ứng.
     */
    @Test
    void testGetPasswordError_TooShort() {
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.",
                UserFactory.getPasswordError("abc"));
    }

    /**
     * Kiểm thử {@code getPasswordError} với mật khẩu chứa khoảng trắng — trả về thông báo tiếng Việt tương ứng.
     */
    @Test
    void testGetPasswordError_HasWhitespace() {
        assertEquals("Mật khẩu không được chứa khoảng trắng.",
                UserFactory.getPasswordError("pass word"));
    }

    /**
     * Kiểm thử {@code getPasswordError} với mật khẩu hợp lệ — phải trả về {@code null}.
     */
    @Test
    void testGetPasswordError_Valid_ReturnsNull() {
        assertNull(UserFactory.getPasswordError("validPass123"));
    }

    // ── Kiểm thử createUser ─────────────────────────────────

    /**
     * Kiểm thử {@link UserFactory#createUser}: tạo User thành công,
     * ID bắt đầu bằng "USR" và có đúng 4 ký tự số.
     */
    @Test
    void testCreateUser_GeneratesCorrectId() {
        User user = UserFactory.createUser("testuser", "password123");
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertTrue(user.getId().startsWith("USR"));
        assertEquals(4, user.getId().length() - 3);
    }

    /**
     * Kiểm thử role mặc định của User: isSeller=true, isBidder=true, isAdmin=false, role="BIDDER_SELLER".
     */
    @Test
    void testCreateUser_SetsRoleDefaults() {
        User user = UserFactory.createUser("testuser", "password123");
        assertTrue(user.isSeller());
        assertTrue(user.isBidder());
        assertFalse(user.isAdmin());
        assertEquals("BIDDER_SELLER", user.getRole());
    }

    /**
     * Kiểm thử số dư ban đầu của User: 0.
     */
    @Test
    void testCreateUser_HasInitialBalance() {
        User user = UserFactory.createUser("testuser", "password123");
        assertEquals(0, user.getBalance().doubleValue(), 0.001);
    }

    // ── Kiểm thử createAdmin ────────────────────────────────

    /**
     * Kiểm thử {@link UserFactory#createAdmin}: tạo Admin thành công,
     * ID bắt đầu bằng "ADM", role="ADMIN", isAdmin()=true.
     */
    @Test
    void testCreateAdmin_GeneratesCorrectId() {
        Admin admin = UserFactory.createAdmin("testadmin", "password123");
        assertNotNull(admin);
        assertEquals("testadmin", admin.getUsername());
        assertTrue(admin.getId().startsWith("ADM"));
        assertEquals("ADMIN", admin.getRole());
        assertTrue(admin.isAdmin());
    }

    // ═══════════════════════════════════════════════════════════════
    // 🟡 NEGATIVE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Kiểm thử {@code createUser} với username null — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testCreateUser_NullUsername_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> UserFactory.createUser(null, "password123"));
    }

    /**
     * Kiểm thử {@code createUser} với username rỗng — phải ném {@link IllegalArgumentException}.
     */
    @Test
    void testCreateUser_EmptyUsername_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> UserFactory.createUser("", "password123"));
    }
}
