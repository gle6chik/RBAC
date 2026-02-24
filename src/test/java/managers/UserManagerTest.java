package managers;

import model.User;
import filters.UserFilters;
import sorters.UserSorters;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

class UserManagerTest {

    private UserManager userManager;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();

        // Создание пользователей
        user1 = User.validate("john_doe", "John Doe", "john@example.com");
        user2 = User.validate("jane_smith", "Jane Smith", "jane@company.com");

        // Добавление в менеджер
        userManager.add(user1);
        userManager.add(user2);
    }

    @Test
    @DisplayName("Тест добавления пользователя")
    void testAddUser() {
        assertEquals(2, userManager.count());

        User user3 = User.validate("bob", "Bob Wilson", "bob@test.com");
        userManager.add(user3);

        assertEquals(3, userManager.count());
        assertTrue(userManager.exists("bob"));
    }

    @Test
    @DisplayName("Тест добавления дубликата username")
    void testAddDuplicateUser() {
        User duplicate = User.validate("john_doe", "John Duplicate", "john2@example.com");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userManager.add(duplicate);
        });

        assertTrue(exception.getMessage().contains("already exists"));
        assertEquals(2, userManager.count());
    }

    @Test
    @DisplayName("Тест удаления пользователя")
    void testRemoveUser() {
        assertTrue(userManager.remove(user1));
        assertEquals(1, userManager.count());
        assertFalse(userManager.exists("john_doe"));

        assertFalse(userManager.remove(null));
    }

    @Test
    @DisplayName("Тест поиска по username")
    void testFindByUsername() {
        Optional<User> found = userManager.findByUsername("john_doe");
        assertTrue(found.isPresent());
        assertEquals("john_doe", found.get().username());

        Optional<User> notFound = userManager.findByUsername("nonexistent");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Тест поиска по email")
    void testFindByEmail() {
        Optional<User> found = userManager.findByEmail("jane@company.com");
        assertTrue(found.isPresent());
        assertEquals("jane_smith", found.get().username());

        Optional<User> notFound = userManager.findByEmail("nonexistent@test.com");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Тест фильтрации пользователей")
    void testFindByFilter() {
        // Фильтр по домену email
        List<User> companyUsers = userManager.findByFilter(
                UserFilters.byEmailDomain("@company.com")
        );

        assertEquals(1, companyUsers.size());
        assertEquals("jane_smith", companyUsers.get(0).username());

        // Комбинированный фильтр
        List<User> filtered = userManager.findByFilter(
                UserFilters.byUsernameContains("john")
                        .and(UserFilters.byEmailDomain("@example.com"))
        );

        assertEquals(1, filtered.size());
        assertEquals("john_doe", filtered.get(0).username());
    }

    @Test
    @DisplayName("Тест поиска с сортировкой")
    void testFindAllWithFilterAndSorter() {
        List<User> sorted = userManager.findAll(
                UserFilters.byUsernameContains("j"),
                UserSorters.byUsername()
        );

        assertEquals(2, sorted.size());
        assertTrue(sorted.get(0).username().compareTo(sorted.get(1).username()) <= 0);
    }

    @Test
    @DisplayName("Тест обновления пользователя")
    void testUpdateUser() {
        userManager.update("john_doe", "John Updated", "john_new@example.com");

        Optional<User> updated = userManager.findByUsername("john_doe");
        assertTrue(updated.isPresent());
        assertEquals("John Updated", updated.get().fullName());
        assertEquals("john_new@example.com", updated.get().email());
    }

    @Test
    @DisplayName("Тест обновления несуществующего пользователя")
    void testUpdateNonExistentUser() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userManager.update("nonexistent", "New Name", "new@email.com");
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Тест очистки всех пользователей")
    void testClear() {
        userManager.clear();
        assertEquals(0, userManager.count());
        assertFalse(userManager.exists("john_doe"));
        assertFalse(userManager.exists("jane_smith"));
    }
}