package core;

import managers.*;
import model.*;
import assignment.*;
import java.util.*;

public class RBACSystem {
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.roleManager.setAssignmentManager(assignmentManager);
        this.currentUser = "system";
    }

    public UserManager getUserManager() { return userManager; }
    public RoleManager getRoleManager() { return roleManager; }
    public AssignmentManager getAssignmentManager() { return assignmentManager; }
    public String getCurrentUser() { return currentUser; }

    public void setCurrentUser(String username) { this.currentUser = username; }

    public void initialize() {
        Permission[] permissions = createDefaultPermissions();
        createDefaultRoles(permissions);
        createAdminUser();
    }

    private Permission[] createDefaultPermissions() {
        Permission[] perms = new Permission[6];
        perms[0] = new Permission("READ", "users", "Can read users");
        perms[1] = new Permission("WRITE", "users", "Can write users");
        perms[2] = new Permission("DELETE", "users", "Can delete users");
        perms[3] = new Permission("READ", "reports", "Can read reports");
        perms[4] = new Permission("WRITE", "reports", "Can write reports");
        perms[5] = new Permission("READ", "settings", "Can read settings");
        return perms;
    }

    private void createDefaultRoles(Permission[] perms) {
        // Admin-роль - все права
        Role adminRole = new Role("Admin", "Full system access");
        for (Permission p : perms) {
            adminRole.addPermission(p);
        }
        roleManager.add(adminRole);

        // Manager-роль
        Role managerRole = new Role("Manager", "Can manage users and reports");
        managerRole.addPermission(perms[0]); // READ users
        managerRole.addPermission(perms[1]); // WRITE users
        managerRole.addPermission(perms[3]); // READ reports
        managerRole.addPermission(perms[4]); // WRITE reports
        roleManager.add(managerRole);

        // Viewer-роль
        Role viewerRole = new Role("Viewer", "Can view only");
        viewerRole.addPermission(perms[0]); // READ users
        viewerRole.addPermission(perms[3]); // READ reports
        viewerRole.addPermission(perms[5]); // READ settings
        roleManager.add(viewerRole);
    }

    private void createAdminUser() {
        try {
            User admin = User.validate("admin", "System Administrator", "admin@system.com");
            userManager.add(admin);

            Role adminRole = roleManager.findByName("Admin")
                    .orElseThrow(() -> new RuntimeException("Admin role not found."));

            AssignmentMetadata meta = AssignmentMetadata.now(currentUser, "System initialization.");
            PermanentAssignment assignment = new PermanentAssignment(admin, adminRole, meta);
            assignmentManager.add(assignment);

            System.out.println("System initialized with admin user.");
        } catch (Exception e) {
            System.out.println("Failed to create admin user: " + e.getMessage());
        }
    }

    public String generateStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n" + "=".repeat(50) + "\n");
        sb.append("SYSTEM STATISTICS\n");
        sb.append("=".repeat(50) + "\n");

        int userCount = userManager.count();
        int roleCount = roleManager.count();
        int totalAssignments = assignmentManager.count();
        long activeAssignments = assignmentManager.getActiveAssignments().size();
        long expiredAssignments = assignmentManager.getExpiredAssignments().size();

        sb.append(String.format("Users: %d\n", userCount));
        sb.append(String.format("Roles: %d\n", roleCount));
        sb.append(String.format("Assignments: total=%d, active=%d, expired=%d\n",
                totalAssignments, activeAssignments, expiredAssignments));

        sb.append("=".repeat(50) + "\n");
        return sb.toString();
    }

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
