package managers;

import model.Role;
import model.Permission;
import filters.RoleFilters;
import sorters.RoleSorters;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

class RoleManagerTest {

    private RoleManager roleManager;
    private Permission readPerm;
    private Permission writePerm;
    private Permission deletePerm;
    private Role adminRole;
    private Role viewerRole;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager();

        // Создание права
        readPerm = new Permission("READ", "users", "Can read users");
        writePerm = new Permission("WRITE", "users", "Can write users");
        deletePerm = new Permission("DELETE", "users", "Can delete users");

        // Создание роли
        adminRole = new Role("Administrator", "Admin role");
        adminRole.addPermission(readPerm);
        adminRole.addPermission(writePerm);
        adminRole.addPermission(deletePerm);

        viewerRole = new Role("Viewer", "Viewer role");
        viewerRole.addPermission(readPerm);

        // Добавление ролей
        roleManager.add(adminRole);
        roleManager.add(viewerRole);
    }

    @Test
    @DisplayName("Тест добавления роли")
    void testAddRole() {
        assertEquals(2, roleManager.count());

        Role editorRole = new Role("Editor", "Editor role");
        roleManager.add(editorRole);

        assertEquals(3, roleManager.count());
        assertTrue(roleManager.exists("Editor"));
    }

    @Test
    @DisplayName("Тест добавления дубликата имени роли")
    void testAddDuplicateRole() {
        Role duplicate = new Role("Administrator", "Another admin");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            roleManager.add(duplicate);
        });

        assertTrue(exception.getMessage().contains("already exists"));
        assertEquals(2, roleManager.count());
    }

    @Test
    @DisplayName("Тест удаления роли (без проверки назначений)")
    void testRemoveRoleWithoutAssignments() {
        // Временное отключение проверки назначений (null для assignmentManager)
        assertTrue(roleManager.remove(viewerRole));
        assertEquals(1, roleManager.count());
        assertFalse(roleManager.exists("Viewer"));
    }

    @Test
    @DisplayName("Тест поиска по имени")
    void testFindByName() {
        Optional<Role> found = roleManager.findByName("Administrator");
        assertTrue(found.isPresent());
        assertEquals("Administrator", found.get().getName());

        Optional<Role> notFound = roleManager.findByName("NonExistent");
        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Тест поиска по ID")
    void testFindById() {
        Optional<Role> found = roleManager.findById(adminRole.getId());
        assertTrue(found.isPresent());
        assertEquals(adminRole.getName(), found.get().getName());
    }

    @Test
    @DisplayName("Тест фильтрации ролей")
    void testFindByFilter() {
        // Фильтр по количеству прав
        List<Role> rolesWithManyPerms = roleManager.findByFilter(
                RoleFilters.hasAtLeastNPermissions(3)
        );

        assertEquals(1, rolesWithManyPerms.size());
        assertEquals("Administrator", rolesWithManyPerms.get(0).getName());

        // Фильтр по имени
        List<Role> rolesWithView = roleManager.findByFilter(
                RoleFilters.byNameContains("view")
        );

        assertEquals(1, rolesWithView.size());
        assertEquals("Viewer", rolesWithView.get(0).getName());
    }

    @Test
    @DisplayName("Тест поиска с сортировкой")
    void testFindAllWithFilterAndSorter() {
        List<Role> sorted = roleManager.findAll(
                RoleFilters.hasAtLeastNPermissions(1),
                RoleSorters.byPermissionCount()
        );

        assertEquals(2, sorted.size());
        assertTrue(sorted.get(0).getPermissions().size() <=
                sorted.get(1).getPermissions().size());
    }

    @Test
    @DisplayName("Тест добавления права к роли")
    void testAddPermissionToRole() {
        Permission newPerm = new Permission("EXPORT", "reports", "Export reports");

        roleManager.addPermissionToRole("Viewer", newPerm);

        Optional<Role> updated = roleManager.findByName("Viewer");
        assertTrue(updated.isPresent());
        assertTrue(updated.get().hasPermission("EXPORT", "reports"));
    }

    @Test
    @DisplayName("Тест удаления права из роли")
    void testRemovePermissionFromRole() {
        roleManager.removePermissionFromRole("Administrator", writePerm);

        Optional<Role> updated = roleManager.findByName("Administrator");
        assertTrue(updated.isPresent());
        assertFalse(updated.get().hasPermission("WRITE", "users"));
    }

    @Test
    @DisplayName("Тест поиска ролей с определенным правом")
    void testFindRolesWithPermission() {
        List<Role> rolesWithRead = roleManager.findRolesWithPermission("READ", "users");
        assertEquals(2, rolesWithRead.size());

        List<Role> rolesWithDelete = roleManager.findRolesWithPermission("DELETE", "users");
        assertEquals(1, rolesWithDelete.size());
        assertEquals("Administrator", rolesWithDelete.get(0).getName());
    }
}