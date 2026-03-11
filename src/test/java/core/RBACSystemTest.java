package core;

import model.Role;
import model.User;
import assignment.AssignmentMetadata;
import assignment.PermanentAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RBACSystemTest {
    private RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
    }

    @Test
    void testConstructorAndGetters() {
        assertNotNull(system.getUserManager());
        assertNotNull(system.getRoleManager());
        assertNotNull(system.getAssignmentManager());

        assertEquals("system", system.getCurrentUser());
    }

    @Test
    void testSetCurrentUser() {
        system.setCurrentUser("admin_user");
        assertEquals("admin_user", system.getCurrentUser());
    }

    @Test
    void testInitializeFullCycle() {
        system.initialize();

        assertTrue(system.getRoleManager().findByName("Admin").isPresent());
        assertTrue(system.getRoleManager().findByName("Manager").isPresent());
        assertTrue(system.getRoleManager().findByName("Viewer").isPresent());

        Role adminRole = system.getRoleManager().findByName("Admin").get();
        assertEquals(6, adminRole.getPermissions().size());

        assertTrue(system.getUserManager().findByUsername("admin").isPresent());
    }

    @Test
    void testGenerateStatisticsWithData() {
        system.initialize();

        User u2 = User.validate("user2", "Name", "u2@test.com");
        system.getUserManager().add(u2);

        Role viewer = system.getRoleManager().findByName("Viewer").get();
        Role manager = system.getRoleManager().findByName("Manager").get();

        AssignmentMetadata meta = AssignmentMetadata.now("system", "test");

        system.getAssignmentManager().add(new PermanentAssignment(u2, viewer, meta));
        system.getAssignmentManager().add(new PermanentAssignment(u2, manager, meta));

        String stats = system.generateStatistics();

        assertTrue(stats.contains("Users: 2"));
        assertTrue(stats.contains("Roles: 3"));
        assertTrue(stats.contains("Assignments: total=3"));
        assertTrue(stats.contains("Average roles per user: 1.50"));

        assertTrue(stats.contains("Top 3 Popular Roles:"));
    }

    @Test
    void testGenerateStatisticsEmpty() {
        String stats = system.generateStatistics();
        assertTrue(stats.contains("Average roles per user: 0.00"));
    }

    @Test
    void testClearScreen() {
        assertDoesNotThrow(() -> system.clearScreen());
    }
}