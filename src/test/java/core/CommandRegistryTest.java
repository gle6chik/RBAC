package core;

import assignment.AssignmentMetadata;
import assignment.PermanentAssignment;
import assignment.TemporaryAssignment;
import model.Role;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {
    private RBACSystem system;
    private CommandParser parser;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
        parser = new CommandParser();
        CommandRegistry.registerAll(parser);
        System.setOut(new PrintStream(outContent));
    }

    private Scanner simulateInput(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    @DisplayName("user-list: should display all users when no filters are applied")
    void testUserListWithoutFilters() {
        Scanner scanner = simulateInput("no\n");
        parser.executeCommand("user-list", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Username"));
        assertTrue(output.contains("admin"));
    }

    @Test
    @DisplayName("user-list: should display filtered users when search logic is triggered")
    void testUserListWithFilters() {
        User tester = User.validate("tester", "Test User", "tester@example.com");
        system.getUserManager().add(tester);
        Scanner scanner = simulateInput("yes\n1\ntester\n");
        parser.executeCommand("user-list", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("tester"));
    }

    @Test
    @DisplayName("user-create: should successfully create a user when data is valid")
    void testUserCreateSuccess() {
        Scanner scanner = simulateInput("new_user\nNew User\nnew@example.com\n");
        parser.executeCommand("user-create", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: User created successfully!"));
        var userOpt = system.getUserManager().findByUsername("new_user");
        assertTrue(userOpt.isPresent());
        assertEquals("new@example.com", userOpt.get().email());
    }

    @Test
    @DisplayName("user-create: should display error message when user data is invalid")
    void testUserCreateFailure() {
        Scanner scanner = simulateInput("bad_user\n\n\n");
        parser.executeCommand("user-create", scanner, system);
        assertTrue(outContent.toString().contains("ERROR:"));
        assertTrue(system.getUserManager().findByUsername("bad_user").isEmpty());
    }

    @Test
    @DisplayName("user-view: should display error when user is not found")
    void testUserViewNotFound() {
        Scanner scanner = simulateInput("ghost_user\n");
        parser.executeCommand("user-view", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: User 'ghost_user' not found."));
    }

    @Test
    @DisplayName("user-view: should show detailed info for user with no assignments")
    void testUserViewNoAssignments() {
        User user = User.validate("empty_user", "Empty User", "empty@test.com");
        system.getUserManager().add(user);
        Scanner scanner = simulateInput("empty_user\n");
        parser.executeCommand("user-view", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("empty_user"));
        assertTrue(output.contains("[No roles assigned to this user]"));
        assertTrue(output.contains("No active permissions found"));
    }

    @Test
    @DisplayName("user-view: should show roles and permissions for user with active assignments")
    void testUserViewWithFullInfo() {
        Scanner scanner = simulateInput("admin\n");
        parser.executeCommand("user-view", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("Type:"));
        assertTrue(output.contains("ACTIVE"));
        assertTrue(output.contains("All permissions for this user:"));
    }

    @Test
    @DisplayName("user-update: should successfully update user data when user exists")
    void testUserUpdateSuccess() {
        User user = User.validate("original_user", "Old Name", "old@test.com");
        system.getUserManager().add(user);
        Scanner scanner = simulateInput("original_user\nUpdated Name\nupdated@test.com\n");
        parser.executeCommand("user-update", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: User updated successfully."));
        User updatedUser = system.getUserManager().findByUsername("original_user").get();
        assertEquals("Updated Name", updatedUser.fullName());
        assertEquals("updated@test.com", updatedUser.email());
    }

    @Test
    @DisplayName("user-update: should display error message when update fails")
    void testUserUpdateFailure() {
        Scanner scanner = simulateInput("non_existent\nNew Name\nemail@test.com\n");
        parser.executeCommand("user-update", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: Update failed:"));
    }

    @Test
    @DisplayName("user-delete: should successfully remove user and their assignments when confirmed")
    void testUserDeleteSuccess() {
        String username = "delete_me";
        system.getUserManager().add(model.User.validate(username, "Delete Me", "del@test.com"));
        assertTrue(system.getUserManager().findByUsername(username).isPresent());
        Scanner scanner = simulateInput(username + "\nyes\n");
        parser.executeCommand("user-delete", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: User and all their assignments deleted."));
        assertTrue(system.getUserManager().findByUsername(username).isEmpty());
    }

    @Test
    @DisplayName("user-delete: should not remove user if confirmation is not 'yes'")
    void testUserDeleteCancellation() {
        String username = "keep_me";
        system.getUserManager().add(model.User.validate(username, "Keep Me", "keep@test.com"));
        Scanner scanner = simulateInput(username + "\nno\n");
        parser.executeCommand("user-delete", scanner, system);
        assertTrue(system.getUserManager().findByUsername(username).isPresent());
    }

    @Test
    @DisplayName("user-delete: should show error message when user does not exist")
    void testUserDeleteNotFound() {
        Scanner scanner = simulateInput("non_existent_user\n");
        parser.executeCommand("user-delete", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: User not found."));
    }

    @Test
    @DisplayName("user-search: should display users matching the name filter")
    void testUserSearchByName() {
        String username = "search_target";
        system.getUserManager().add(model.User.validate(username, "Search Target", "target@test.com"));
        Scanner scanner = simulateInput("1\ntarget\n");
        parser.executeCommand("user-search", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Username"));
        assertTrue(output.contains(username));
    }

    @Test
    @DisplayName("user-search: should display an empty table when no users match the filter")
    void testUserSearchNoResults() {
        Scanner scanner = simulateInput("2\nnonexistent@void.com\n");
        parser.executeCommand("user-search", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Username"));
        assertFalse(output.contains("admin"));
    }

    @Test
    @DisplayName("user-search: should handle invalid input and retry until valid choice is entered")
    void testUserSearchInvalidInput() {
        Scanner scanner = simulateInput("99\ntest\n1\nadmin\n");
        parser.executeCommand("user-search", scanner, system);

        String output = outContent.toString();

        assertTrue(output.contains("Error: Please enter a number between 1 and 4.") ||
                output.contains("Error: Please enter a valid number."));

        assertTrue(output.contains("admin"));
    }

    @Test
    @DisplayName("role-list: should display all registered roles in a table format")
    void testRoleListDisplay() {
        parser.executeCommand("role-list", new Scanner(""), system);
        String output = outContent.toString();
        assertTrue(output.contains("Role Name"));
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("Manager"));
        assertTrue(output.contains("Viewer"));
    }

    @Test
    @DisplayName("role-create: should successfully create a role and add permissions through the loop")
    void testRoleCreateWithPermissions() {
        String input = "CustomRole\nCustom Desc\nyes\nREAD\nfiles\nRead files\nno\n";
        Scanner scanner = simulateInput(input);
        parser.executeCommand("role-create", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: Role created."));
        var roleOpt = system.getRoleManager().findByName("CustomRole");
        assertTrue(roleOpt.isPresent());
        model.Role role = roleOpt.get();
        assertEquals("Custom Desc", role.getDescription());
        assertTrue(role.hasPermission("READ", "files"));
    }

    @Test
    @DisplayName("role-create: should create a role even if no permissions are added")
    void testRoleCreateWithoutPermissions() {
        Scanner scanner = simulateInput("EmptyRole\nNo Perms\nno\n");
        parser.executeCommand("role-create", scanner, system);
        var roleOpt = system.getRoleManager().findByName("EmptyRole");
        assertTrue(roleOpt.isPresent());
        assertTrue(roleOpt.get().getPermissions().isEmpty());
    }

    @Test
    @DisplayName("role-view: should display formatted role details when role exists")
    void testRoleViewSuccess() {
        Scanner scanner = simulateInput("Admin\n");
        parser.executeCommand("role-view", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("READ"));
    }

    @Test
    @DisplayName("role-view: should display error message when role is not found")
    void testRoleViewNotFound() {
        Scanner scanner = simulateInput("GhostRole\n");
        parser.executeCommand("role-view", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: Role not found."));
    }

    @Test
    @DisplayName("role-update: should successfully update role description when role exists")
    void testRoleUpdateSuccess() {
        Scanner scanner = simulateInput("Viewer\nNew Updated Description\n");
        parser.executeCommand("role-update", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: Role updated successfully."));
        var role = system.getRoleManager().findByName("Viewer").get();
        assertEquals("New Updated Description", role.getDescription());
    }

    @Test
    @DisplayName("role-update: should display error message when role to update is not found")
    void testRoleUpdateNotFound() {
        Scanner scanner = simulateInput("NonExistentRole\nSome Description\n");
        parser.executeCommand("role-update", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: Role not found."));
    }

    @Test
    @DisplayName("role-delete: should successfully delete role when confirmed and not assigned")
    void testRoleDeleteSuccess() {
        system.getRoleManager().add(new model.Role("UnusedRole", "No assignments"));
        Scanner scanner = simulateInput("UnusedRole\nyes\n");
        parser.executeCommand("role-delete", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: Role deleted."));
        assertTrue(system.getRoleManager().findByName("UnusedRole").isEmpty());
    }

    @Test
    @DisplayName("role-delete: should show warning when role is assigned to users")
    void testRoleDeleteWithWarning() {
        Scanner scanner = simulateInput("Admin\nno\n");
        parser.executeCommand("role-delete", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("WARNING: Role is assigned to users:"));
        assertTrue(output.contains("- admin"));
    }

    @Test
    @DisplayName("role-delete: should not delete role if confirmation is not 'yes'")
    void testRoleDeleteCancellation() {
        Scanner scanner = simulateInput("Viewer\nno\n");
        parser.executeCommand("role-delete", scanner, system);
        assertTrue(system.getRoleManager().findByName("Viewer").isPresent());
    }

    @Test
    @DisplayName("role-delete: should display error message when role not found")
    void testRoleDeleteNotFound() {
        Scanner scanner = simulateInput("FakeRole\n");
        parser.executeCommand("role-delete", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: Role not found."));
    }

    @Test
    @DisplayName("role-add-permission: should successfully add a permission to an existing role")
    void testRoleAddPermissionSuccess() {
        String input = "Viewer\nEXECUTE\nscripts\nExecute shell scripts\n";
        Scanner scanner = simulateInput(input);
        parser.executeCommand("role-add-permission", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: Permission added."));
        var role = system.getRoleManager().findByName("Viewer").get();
        assertTrue(role.hasPermission("EXECUTE", "scripts"));
    }

    @Test
    @DisplayName("role-add-permission: should display error message when role is not found or data is invalid")
    void testRoleAddPermissionFailure() {
        String input = "NonExistentRole\nREAD\nfiles\nDesc\n";
        Scanner scanner = simulateInput(input);
        parser.executeCommand("role-add-permission", scanner, system);
        assertFalse(outContent.toString().contains("SUCCESS: Permission added."));
        assertTrue(outContent.toString().contains("ERROR:") || outContent.toString().length() > 0);
    }

    @Test
    @DisplayName("role-remove-permission: should successfully remove permission when index is valid")
    void testRoleRemovePermissionSuccess() {
        Scanner scanner = simulateInput("Admin\n1\n");
        parser.executeCommand("role-remove-permission", scanner, system);
        assertTrue(outContent.toString().contains("SUCCESS: Permission removed."));
        var role = system.getRoleManager().findByName("Admin").get();
        assertEquals(5, role.getPermissions().size());
    }

    @Test
    @DisplayName("role-remove-permission: should handle invalid input and retry")
    void testRoleRemovePermissionInvalidFormat() {
        Scanner scanner = simulateInput("Admin\nabc\n1\n");
        parser.executeCommand("role-remove-permission", scanner, system);

        String output = outContent.toString();
        assertTrue(output.contains("Error: Please enter a valid number.") ||
                output.contains("ERROR:"));
        assertTrue(output.contains("SUCCESS: Permission removed."));
    }

    @Test
    @DisplayName("role-remove-permission: should display error when role is not found")
    void testRoleRemovePermissionRoleNotFound() {
        Scanner scanner = simulateInput("UnknownRole\n");
        parser.executeCommand("role-remove-permission", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: Role not found."));
    }

    @Test
    @DisplayName("role-search: should find roles by name containing search string")
    void testRoleSearchByName() {
        Scanner scanner = simulateInput("1\nMan\n");
        parser.executeCommand("role-search", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Manager"));
        assertFalse(output.contains("Viewer"));
    }

    @Test
    @DisplayName("role-search: should find roles by permission name and resource")
    void testRoleSearchByPermission() {
        Scanner scanner = simulateInput("2\nREAD\nusers\n");
        parser.executeCommand("role-search", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Admin"));
    }

    @Test
    @DisplayName("role-search: should find roles by minimum permissions count")
    void testRoleSearchByMinCount() {
        Scanner scanner = simulateInput("3\n5\n");
        parser.executeCommand("role-search", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Admin"));
        assertFalse(output.contains("Viewer"));
    }

    @Test
    @DisplayName("role-search: should handle invalid number format for min count filter")
    void testRoleSearchByMinCountInvalidFormat() {
        Scanner scanner = simulateInput("3\nabc\n");
        parser.executeCommand("role-search", scanner, system);
        String output = outContent.toString();
        assertTrue(output.isEmpty() || !output.contains("Admin"));
    }

    @Test
    @DisplayName("role-search: should display 'No roles found' when filter returns empty list")
    void testRoleSearchNoResults() {
        Scanner scanner = simulateInput("1\nNonExistentRole\n");
        parser.executeCommand("role-search", scanner, system);
        assertTrue(outContent.toString().contains("WARNING: No roles found."));
    }

    @Test
    @DisplayName("assign-role: should successfully create a permanent assignment")
    void testAssignRolePermanent() {
        User target = User.validate("target_user", "Target", "target@test.com");
        system.getUserManager().add(target);

        // Получаем список ролей и находим номер роли "Viewer"
        List<Role> roles = system.getRoleManager().findAll();
        int viewerIndex = -1;
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).getName().equals("Viewer")) {
                viewerIndex = i + 1;
                break;
            }
        }

        assertTrue(viewerIndex > 0);

        Scanner scanner = simulateInput("target_user\n" + viewerIndex + "\npermanent\nTesting perm assignment\n");
        parser.executeCommand("assign-role", scanner, system);

        assertTrue(outContent.toString().contains("SUCCESS: Role assigned."));

        var assignments = system.getAssignmentManager().findByFilter(filters.AssignmentFilters.byUsername("target_user"));
        assertEquals(1, assignments.size());
        assertTrue(assignments.get(0) instanceof PermanentAssignment);
        assertEquals("Viewer", assignments.get(0).role().getName());
    }

    @Test
    @DisplayName("assign-role: should successfully create a temporary assignment with date")
    void testAssignRoleTemporary() {
        User target = User.validate("temp_user", "Temp", "temp@test.com");
        system.getUserManager().add(target);

        // Выбираем роль (3 - Viewer), тип (temporary), дату
        Scanner scanner = simulateInput("temp_user\n3\ntemporary\nTesting temp\n2026-12-31 23:59\n");
        parser.executeCommand("assign-role", scanner, system);

        assertTrue(outContent.toString().contains("SUCCESS: Role assigned."));

        var assignments = system.getAssignmentManager().findByFilter(filters.AssignmentFilters.byUsername("temp_user"));
        assertTrue(assignments.get(0) instanceof TemporaryAssignment);
    }

    @Test
    @DisplayName("assign-role: should fail when user or role does not exist")
    void testAssignRoleFailure() {
        Scanner scanner = simulateInput("ghost_user\n3\npermanent\nReason\n");
        parser.executeCommand("assign-role", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: Assignment failed: User not found"));
    }

    @Test
    @DisplayName("revoke-role: should successfully revoke an active assignment")
    void testRevokeRoleSuccess() {
        String username = "admin";

        // Выбираем назначение (1) и подтверждаем (yes)
        Scanner scanner = simulateInput(username + "\n1\nyes\n");
        parser.executeCommand("revoke-role", scanner, system);

        String output = outContent.toString();
        assertTrue(output.contains("SUCCESS: Role revoked."), "Должно быть сообщение об успешном отзыве");

        var activeAfter = system.getAssignmentManager().findByFilter(filters.AssignmentFilters.byUsername(username))
                .stream().filter(assignment.RoleAssignment::isActive).toList();
        assertTrue(activeAfter.isEmpty(), "У пользователя не должно остаться активных назначений");
    }

    @Test
    @DisplayName("revoke-role: should show message when no active assignments exist")
    void testRevokeRoleNoActive() {
        User user = User.validate("clean_user", "Clean", "clean@test.com");
        system.getUserManager().add(user);

        Scanner scanner = simulateInput("clean_user\n");
        parser.executeCommand("revoke-role", scanner, system);

        assertTrue(outContent.toString().contains("WARNING: No active assignments found."));
    }

    @Test
    @DisplayName("revoke-role: should display error for invalid index or format")
    void testRevokeRoleInvalidInput() {
        String username = "admin";
        Scanner scanner = simulateInput(username + "\nnot_a_number\n");

        parser.executeCommand("revoke-role", scanner, system);

        String output = outContent.toString();
        assertTrue(output.contains("ERROR: Revoke failed:") ||
                        output.contains("Error: Please enter a valid number."),
                "Должно быть выведено сообщение об ошибке");
    }

    @Test
    @DisplayName("assignment-list: should display all assignments in a table format")
    void testAssignmentListDisplay() {
        parser.executeCommand("assignment-list", new Scanner(""), system);
        String output = outContent.toString();
        assertTrue(output.contains("User"));
        assertTrue(output.contains("Role"));
        assertTrue(output.contains("Type"));
        assertTrue(output.contains("admin"));
    }

    @Test
    @DisplayName("assignment-list-user: should display assignments only for the specified user")
    void testAssignmentListUserSuccess() {
        Scanner scanner = simulateInput("admin\n");
        parser.executeCommand("assignment-list-user", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("Role"));
    }

    @Test
    @DisplayName("assignment-list-user: should show empty table or no data for user without assignments")
    void testAssignmentListUserEmpty() {
        User newUser = User.validate("ghost", "Ghost", "ghost@test.com");
        system.getUserManager().add(newUser);
        Scanner scanner = simulateInput("ghost\n");
        parser.executeCommand("assignment-list-user", scanner, system);
        String output = outContent.toString();
        assertFalse(output.contains("admin"));
        assertTrue(output.contains("User") || output.contains("Username"));
    }

    @Test
    @DisplayName("assignment-list-role: should display users assigned to a specific role")
    void testAssignmentListByRoleSuccess() {
        Scanner scanner = simulateInput("Admin\n");
        parser.executeCommand("assignment-list-role", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Role"));
        assertTrue(output.contains("Admin"));
        assertTrue(output.contains("admin"));
    }

    @Test
    @DisplayName("assignment-list-role: should display error message when role does not exist")
    void testAssignmentListByRoleNotFound() {
        Scanner scanner = simulateInput("SuperPowerRole\n");
        parser.executeCommand("assignment-list-role", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: Role not found."));
    }

    @Test
    @DisplayName("assignment-active: should display only active assignments")
    void testAssignmentActiveDisplay() {
        parser.executeCommand("assignment-active", new Scanner(""), system);
        String output = outContent.toString();
        assertTrue(output.contains("User"));
        assertTrue(output.contains("admin"));
    }

    @Test
    @DisplayName("assignment-active: should handle empty active list gracefully")
    void testAssignmentActiveEmpty() {
        system.getAssignmentManager().findAll().forEach(a -> {
            if (a instanceof PermanentAssignment) {
                ((PermanentAssignment) a).revoke();
            }
        });
        parser.executeCommand("assignment-active", new Scanner(""), system);
        String output = outContent.toString();
        assertFalse(output.contains("admin"));
    }

    @Test
    @DisplayName("assignment-expired: should show only temporary assignments that are no longer active")
    void testAssignmentExpiredList() {
        User user = User.validate("expired_user", "Old User", "old@test.com");
        system.getUserManager().add(user);
        Role role = system.getRoleManager().findByName("Viewer").get();

        AssignmentMetadata meta = AssignmentMetadata.now(system.getCurrentUser(), "Expired naturally");
        TemporaryAssignment expired = new TemporaryAssignment(user, role, meta, "2020-01-01 00:00", false);
        system.getAssignmentManager().add(expired);

        TemporaryAssignment active = new TemporaryAssignment(user, role, meta, "2026-12-31 23:59", false);
        system.getAssignmentManager().add(active);

        parser.executeCommand("assignment-expired", new Scanner(""), system);

        String output = outContent.toString();
        assertTrue(output.contains("EXPIRED TEMPORARY ASSIGNMENTS"));
        assertTrue(output.contains("expired_user"));
        assertTrue(output.contains("EXPIRED"));
    }

    @Test
    @DisplayName("assignment-expired: should display message when no expired assignments exist")
    void testAssignmentExpiredEmpty() {
        parser.executeCommand("assignment-expired", new Scanner(""), system);
        String output = outContent.toString();
        assertTrue(output.contains("No expired temporary assignments found."));
        assertFalse(output.contains("admin"));
    }

    @Test
    @DisplayName("assignment-extend: should successfully extend a temporary assignment")
    void testAssignmentExtendSuccess() {
        User admin = system.getUserManager().findByUsername("admin").get();
        Role viewer = system.getRoleManager().findByName("Viewer").get();
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Extension test");

        TemporaryAssignment temp = new TemporaryAssignment(admin, viewer, meta, "2020-01-01 10:00", false);
        system.getAssignmentManager().add(temp);

        Scanner scanner = simulateInput("admin\nViewer\n2026-12-31 23:59\n");
        parser.executeCommand("assignment-extend", scanner, system);

        String output = outContent.toString();
        assertTrue(output.contains("SUCCESS: Assignment extended."));
        assertTrue(output.contains("ACTIVE"));
    }

    @Test
    @DisplayName("assignment-extend: should fail if the assignment is not temporary")
    void testAssignmentExtendWrongType() {
        Scanner scanner = simulateInput("admin\nAdmin\n2027-01-01 12:00\n");
        parser.executeCommand("assignment-extend", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: No temporary assignment found"));
    }

    @Test
    @DisplayName("assignment-extend: should display error when user or role does not exist")
    void testAssignmentExtendNotFound() {
        Scanner scanner = simulateInput("non_existent_user\nViewer\n2027-01-01 12:00\n");
        parser.executeCommand("assignment-extend", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: User or Role not found."));
    }

    @Test
    @DisplayName("assignment-extend: should warn if new date is still in the past")
    void testAssignmentExtendStillExpired() {
        User admin = system.getUserManager().findByUsername("admin").get();
        Role viewer = system.getRoleManager().findByName("Viewer").get();
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Past extension test");

        TemporaryAssignment temp = new TemporaryAssignment(admin, viewer, meta, "2020-01-01 10:00", false);
        system.getAssignmentManager().add(temp);

        Scanner scanner = simulateInput("admin\nViewer\n2021-01-01 10:00\n");
        parser.executeCommand("assignment-extend", scanner, system);

        assertTrue(outContent.toString().contains("STILL EXPIRED"));
    }

    @Test
    @DisplayName("assignment-search: should filter by username (Option 1)")
    void testAssignmentSearchByUsername() {
        Scanner scanner = simulateInput("1\nadmin\n");
        parser.executeCommand("assignment-search", scanner, system);
        assertTrue(outContent.toString().contains("admin"));
    }

    @Test
    @DisplayName("assignment-search: should filter by role name (Option 2)")
    void testAssignmentSearchByRoleName() {
        Scanner scanner = simulateInput("2\nAdmin\n");
        parser.executeCommand("assignment-search", scanner, system);
        assertTrue(outContent.toString().contains("Admin"));
    }

    @Test
    @DisplayName("assignment-search: should filter by type (Option 3)")
    void testAssignmentSearchByType() {
        Scanner scanner = simulateInput("3\nPERMANENT\n");
        parser.executeCommand("assignment-search", scanner, system);
        assertTrue(outContent.toString().contains("PERMANENT"));
    }

    @Test
    @DisplayName("assignment-search: should filter by status (Option 4)")
    void testAssignmentSearchByStatus() {
        Scanner scanner = simulateInput("4\ninactive\n");
        parser.executeCommand("assignment-search", scanner, system);
        assertTrue(outContent.toString().contains("User"));
    }

    @Test
    @DisplayName("assignment-search: should handle date filters (Options 5 & 6)")
    void testAssignmentSearchByDates() {
        Scanner scanner = simulateInput("5\n2020-01-01 00:00\n");
        parser.executeCommand("assignment-search", scanner, system);
        assertTrue(outContent.toString().contains("User"));

        ByteArrayOutputStream newOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(newOut));
        Scanner scanner6 = simulateInput("6\n2030-01-01 00:00\n");
        parser.executeCommand("assignment-search", scanner6, system);
        assertTrue(newOut.toString().contains("User"));
    }

    @Test
    @DisplayName("assignment-search: should fall back to activeOnly on default/invalid choice")
    void testAssignmentSearchDefault() {
        Scanner scanner = simulateInput("99\n");
        parser.executeCommand("assignment-search", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("User") || output.contains("Role") || output.contains("admin"));
    }

    @Test
    @DisplayName("permissions-user: should list and group permissions by resource for a valid user")
    void testPermissionsUserGrouping() {
        Scanner scanner = simulateInput("admin\n");
        parser.executeCommand("permissions-user", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Resource: users"));
        assertTrue(output.contains("- READ"));
        assertTrue(output.contains("- WRITE"));
    }

    @Test
    @DisplayName("permissions-user: should display error message when user is not found")
    void testPermissionsUserNotFound() {
        Scanner scanner = simulateInput("unknown_ghost\n");
        parser.executeCommand("permissions-user", scanner, system);
        assertTrue(outContent.toString().contains("ERROR: User not found."));
    }

    @Test
    @DisplayName("permissions-user: should handle user with zero permissions")
    void testPermissionsUserNoPerms() {
        User emptyUser = User.validate("clean_user", "Clean", "clean@test.com");
        system.getUserManager().add(emptyUser);
        Scanner scanner = simulateInput("clean_user\n");
        parser.executeCommand("permissions-user", scanner, system);
        assertFalse(outContent.toString().contains("Resource:"));
    }

    @Test
    @DisplayName("permissions-check: should return GRANTED and list source roles")
    void testPermissionsCheckGranted() {
        Scanner scanner = simulateInput("admin\nREAD\nusers\n");
        parser.executeCommand("permissions-check", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("GRANTED"));
        assertTrue(output.contains("Source roles:"));
        assertTrue(output.contains("- Admin"));
    }

    @Test
    @DisplayName("permissions-check: should return DENIED when permission is missing")
    void testPermissionsCheckDenied() {
        Scanner scanner = simulateInput("admin\nDELETE\nsystem\n");
        parser.executeCommand("permissions-check", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("DENIED"));
        assertFalse(output.contains("Source roles:"));
    }

    @Test
    @DisplayName("permissions-check: should show DENIED for inactive assignments")
    void testPermissionsCheckInactive() {
        User user = User.validate("temp_user", "Temp", "temp@test.com");
        system.getUserManager().add(user);
        Role role = system.getRoleManager().findByName("Viewer").get();

        AssignmentMetadata meta = AssignmentMetadata.now(system.getCurrentUser(), "Expired check");
        TemporaryAssignment expired = new TemporaryAssignment(user, role, meta, "2020-01-01 00:00", false);
        system.getAssignmentManager().add(expired);

        Scanner scanner = simulateInput("temp_user\nREAD\nusers\n");
        parser.executeCommand("permissions-check", scanner, system);
        assertTrue(outContent.toString().contains("DENIED"));
    }

    @Test
    @DisplayName("help: should call printHelp and display available commands")
    void testHelpCommand() {
        parser.executeCommand("help", new Scanner(""), system);
        String output = outContent.toString();
        assertTrue(output.contains("help"));
        assertTrue(output.contains("assign-role"));
    }

    @Test
    @DisplayName("stats: should display system statistics from the system object")
    void testStatsCommand() {
        parser.executeCommand("stats", new Scanner(""), system);
        String output = outContent.toString();
        assertTrue(output.contains("Users") || output.contains("Roles") || output.contains("Assignments"));
    }

    @Test
    @DisplayName("clear: should invoke system clear screen logic")
    void testClearCommand() {
        assertDoesNotThrow(() -> parser.executeCommand("clear", new Scanner(""), system));
    }

    @Test
    @DisplayName("exit: should not exit if user types 'no'")
    void testExitCommandCancel() {
        Scanner scanner = simulateInput("no\n");
        parser.executeCommand("exit", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Confirm exit") ||
                output.contains("confirm") ||
                output.contains("yes/no"));
    }
}