package managers;

import model.*;
import assignment.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

class AssignmentManagerTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    private User admin;
    private User user1;
    private User user2;

    private Role adminRole;
    private Role viewerRole;
    private Role editorRole;

    private Permission readPerm;
    private Permission writePerm;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        // Связывание RoleManager с AssignmentManager для проверок удаления
        roleManager.setAssignmentManager(assignmentManager);

        // Создание права
        readPerm = new Permission("READ", "users", "Read users");
        writePerm = new Permission("WRITE", "users", "Write users");

        // Создание пользователей
        admin = User.validate("admin", "Admin User", "admin@system.com");
        user1 = User.validate("john", "John Doe", "john@example.com");
        user2 = User.validate("jane", "Jane Smith", "jane@example.com");

        userManager.add(admin);
        userManager.add(user1);
        userManager.add(user2);

        // Создание ролей
        adminRole = new Role("Administrator", "Admin role");
        adminRole.addPermission(readPerm);
        adminRole.addPermission(writePerm);

        viewerRole = new Role("Viewer", "Viewer role");
        viewerRole.addPermission(readPerm);

        editorRole = new Role("Editor", "Editor role");
        editorRole.addPermission(readPerm);
        editorRole.addPermission(writePerm);

        roleManager.add(adminRole);
        roleManager.add(viewerRole);
        roleManager.add(editorRole);
    }

    @Test
    @DisplayName("Тест добавления постоянного назначения")
    void testAddPermanentAssignment() {
        AssignmentMetadata meta = AssignmentMetadata.now(admin.username(), "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, viewerRole, meta);

        assignmentManager.add(assignment);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
    }

    @Test
    @DisplayName("Тест добавления временного назначения")
    void testAddTemporaryAssignment() {
        String expiresAt = LocalDateTime.now().plusDays(5).format(FORMATTER);
        AssignmentMetadata meta = AssignmentMetadata.now(admin.username(), "Temp");
        TemporaryAssignment assignment = new TemporaryAssignment(
                user2, editorRole, meta, expiresAt, true
        );

        assignmentManager.add(assignment);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(assignment.assignmentId()).isPresent());
    }

    @Test
    @DisplayName("Тест добавления назначения для несуществующего пользователя")
    void testAddAssignmentForNonExistentUser() {
        User nonExistent = User.validate("ghost", "Ghost", "ghost@test.com");
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(nonExistent, viewerRole, meta);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            assignmentManager.add(assignment);
        });

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("Тест добавления назначения для несуществующей роли")
    void testAddAssignmentForNonExistentRole() {
        Role nonExistent = new Role("GhostRole", "Non existent");
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(user1, nonExistent, meta);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            assignmentManager.add(assignment);
        });

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("Тест запрета дублирования активного назначения")
    void testPreventDuplicateActiveAssignment() {
        AssignmentMetadata meta1 = AssignmentMetadata.now(admin.username(), "First");
        PermanentAssignment assignment1 = new PermanentAssignment(user1, viewerRole, meta1);
        assignmentManager.add(assignment1);

        AssignmentMetadata meta2 = AssignmentMetadata.now(admin.username(), "Duplicate");
        PermanentAssignment assignment2 = new PermanentAssignment(user1, viewerRole, meta2);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            assignmentManager.add(assignment2);
        });

        assertTrue(exception.getMessage().contains("already has active assignment"));
    }

    @Test
    @DisplayName("Тест поиска назначений по пользователю")
    void testFindByUser() {
        addTestAssignments();

        List<RoleAssignment> userAssignments = assignmentManager.findByUser(user1);
        assertEquals(3, userAssignments.size());

        List<RoleAssignment> adminAssignments = assignmentManager.findByUser(admin);
        assertEquals(0, adminAssignments.size());
    }

    @Test
    @DisplayName("Тест поиска активных назначений")
    void testGetActiveAssignments() {
        addTestAssignments();

        List<RoleAssignment> active = assignmentManager.getActiveAssignments();
        assertEquals(3, active.size()); // Все активны, так как новые

        // Отзыв одного
        Optional<RoleAssignment> toRevoke = assignmentManager.findByUser(user1).stream()
                .filter(a -> a instanceof PermanentAssignment)
                .findFirst();

        if (toRevoke.isPresent()) {
            assignmentManager.revokeAssignment(toRevoke.get().assignmentId());
        }

        active = assignmentManager.getActiveAssignments();
        assertEquals(2, active.size());
    }

    @Test
    @DisplayName("Тест проверки наличия роли у пользователя")
    void testUserHasRole() {
        addTestAssignments();

        assertTrue(assignmentManager.userHasRole(user1, viewerRole));
        assertTrue(assignmentManager.userHasRole(user1, editorRole));
        assertTrue(assignmentManager.userHasRole(user1, adminRole));
        assertFalse(assignmentManager.userHasRole(user2, viewerRole));
    }

    @Test
    @DisplayName("Тест получения прав пользователя из всех ролей")
    void testGetUserPermissions() {
        addTestAssignments();

        Set<Permission> permissions = assignmentManager.getUserPermissions(user1);

        assertEquals(2, permissions.size()); // READ и WRITE
        assertTrue(permissions.stream().anyMatch(p -> p.name().equals("READ")));
        assertTrue(permissions.stream().anyMatch(p -> p.name().equals("WRITE")));

        Set<Permission> user2Permissions = assignmentManager.getUserPermissions(user2);
        assertEquals(0, user2Permissions.size()); // нет назначений
    }

    @Test
    @DisplayName("Тест проверки наличия права у пользователя")
    void testUserHasPermission() {
        addTestAssignments();

        assertTrue(assignmentManager.userHasPermission(user1, "READ", "users"));
        assertTrue(assignmentManager.userHasPermission(user1, "WRITE", "users"));
        assertFalse(assignmentManager.userHasPermission(user1, "DELETE", "users"));

        assertFalse(assignmentManager.userHasPermission(user2, "READ", "users"));
    }

    @Test
    @DisplayName("Тест отзыва назначения")
    void testRevokeAssignment() {
        addTestAssignments();

        Optional<RoleAssignment> permAssign = assignmentManager.findByUser(user1).stream()
                .filter(a -> a instanceof PermanentAssignment)
                .findFirst();

        assertTrue(permAssign.isPresent());
        assertTrue(permAssign.get().isActive());

        assignmentManager.revokeAssignment(permAssign.get().assignmentId());

        Optional<RoleAssignment> revoked = assignmentManager.findById(
                permAssign.get().assignmentId()
        );
        assertTrue(revoked.isPresent());
        assertFalse(revoked.get().isActive());
    }

    @Test
    @DisplayName("Тест продления временного назначения")
    void testExtendTemporaryAssignment() {
        String expiresAt = LocalDateTime.now().plusDays(1).format(FORMATTER);
        AssignmentMetadata meta = AssignmentMetadata.now(admin.username(), "Temp");
        TemporaryAssignment tempAssign = new TemporaryAssignment(
                user1, editorRole, meta, expiresAt, false
        );

        assignmentManager.add(tempAssign);

        String newExpires = LocalDateTime.now().plusDays(10).format(FORMATTER);
        assignmentManager.extendTemporaryAssignment(tempAssign.assignmentId(), newExpires);

        assertEquals(newExpires, tempAssign.getExpiresAt());
    }

    @Test
    @DisplayName("Тест удаления роли, назначенной пользователям")
    void testDeleteRoleThatIsAssigned() {
        addTestAssignments();

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            roleManager.remove(viewerRole);
        });

        assertTrue(exception.getMessage().contains("assigned to users"));
    }

    private void addTestAssignments() {
        // Назначения для user1
        AssignmentMetadata meta1 = AssignmentMetadata.now(admin.username(), "Permanent");
        assignmentManager.add(new PermanentAssignment(user1, viewerRole, meta1));

        AssignmentMetadata meta2 = AssignmentMetadata.now(admin.username(), "Temporary");
        String expiresAt = LocalDateTime.now().plusDays(5).format(FORMATTER);
        assignmentManager.add(new TemporaryAssignment(
                user1, editorRole, meta2, expiresAt, false
        ));

        // Еще одно для user1 (другая роль)
        AssignmentMetadata meta3 = AssignmentMetadata.now(admin.username(), "Another");
        assignmentManager.add(new PermanentAssignment(user1, adminRole, meta3));
    }
}