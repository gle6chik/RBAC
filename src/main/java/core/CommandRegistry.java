package core;

// СДЕЛАТЬ: использование класса ValidationUtils

import model.*;
import assignment.*;
import filters.*;
import managers.*;
import utils.*;

import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static void registerAll(CommandParser parser) {
        registerUserCommands(parser);
        registerRoleCommands(parser);
        registerAssignmentCommands(parser);
        registerPermissionCommands(parser);
        registerUtilityCommands(parser);
        registerReportCommands(parser);
    }

    private static void registerReportCommands(CommandParser parser) {
        parser.registerCommand("report-users", "Generate user report", (scanner, system) -> {
            System.out.println("\nGenerating user report...");

            String report = ReportGenerator.generateUserReport(
                    system.getUserManager(),
                    system.getAssignmentManager()
            );

            // Показываем отчёт
            System.out.println(report);

            // Предлагаем сохранить
            ReportGenerator.exportToFile(
                    report,
                    "user_report.txt"
            );
        });

        parser.registerCommand("report-roles", "Generate role report", (scanner, system) -> {
            System.out.println("\nGenerating role report...");

            String report = ReportGenerator.generateRoleReport(
                    system.getRoleManager(),
                    system.getAssignmentManager()
            );

            System.out.println(report);

            ReportGenerator.exportToFile(
                    report,
                    "role_report.txt"
            );
        });

        parser.registerCommand("report-matrix", "Generate permission matrix", (scanner, system) -> {
            System.out.println("\nGenerating permission matrix...");

            String report = ReportGenerator.generatePermissionMatrix(
                    system.getUserManager(),
                    system.getAssignmentManager()
            );

            System.out.println(report);

            ReportGenerator.exportToFile(
                    report,
                    "permission_matrix.txt"
            );
        });
    }

    // USER COMMANDS
    private static void registerUserCommands(CommandParser parser) {
        parser.registerCommand("user-list", "List all users with optional filters", (scanner, system) -> {
            UserManager um = system.getUserManager();
            List<User> users;
            System.out.print("Apply filters? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                users = searchUsersLogic(scanner, um);
            } else {
                users = um.findAll();
            }
            printUserTable(users);
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, system) -> {
            try {
                System.out.print("Enter username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Enter full name: ");
                String fullName = scanner.nextLine().trim();
                System.out.print("Enter email: ");
                String email = scanner.nextLine().trim();

                User newUser = User.validate(username, fullName, email);
                system.getUserManager().add(newUser);

                // Логирование
                system.getAuditLog().log(
                        "USER_CREATE",
                        system.getCurrentUser(),
                        username,
                        String.format("Full name: %s, Email: %s", fullName, email)
                );

                System.out.println("User created successfully!");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View detailed user info", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                System.out.println(user.format());

                var assignments = system.getAssignmentManager().findByFilter(
                        filters.AssignmentFilters.byUsername(username)
                );
                var allPermissions = system.getAssignmentManager().getUserPermissions(user);

                if (assignments.isEmpty()) {
                    System.out.println("[No roles assigned to this user]");
                } else {
                    assignments.forEach(a -> {
                        String type = a.assignmentType();
                        String status = a.isActive() ? "ACTIVE" : "INACTIVE/EXPIRED";

                        System.out.printf("\nType: %s | Status: [%s]\n", type, status);

                        if (a instanceof TemporaryAssignment temp) {
                            System.out.println("Validity: " + temp.summary());
                        }

                        System.out.println(a.role().format());
                        System.out.println("=".repeat(30));
                    });
                }

                if (allPermissions.isEmpty()) {
                    System.out.println("  [No active permissions found]");
                } else {
                    System.out.println("All permissions for this user:");
                    allPermissions.forEach(p -> System.out.println("    - " + p.format()));
                }
                System.out.println("=".repeat(60));

            }, () -> System.out.println("Error: User '" + username + "' not found."));
        });

        parser.registerCommand("user-update", "Update user data", (scanner, system) -> {
            System.out.print("Enter username to update: ");
            String username = scanner.nextLine().trim();
            try {
                System.out.print("Enter new full name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Enter new email: ");
                String email = scanner.nextLine().trim();
                system.getUserManager().update(username, name, email);
                System.out.println("User updated successfully.");
            } catch (Exception e) {
                System.out.println("Update failed: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user and their assignments", (scanner, system) -> {
            System.out.print("Enter username to delete: ");
            String username = scanner.nextLine().trim();
            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                System.out.print("Confirm deletion (type 'yes'): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                    var userAssignments = system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username));
                    userAssignments.forEach(a -> system.getAssignmentManager().remove(a));
                    system.getUserManager().remove(user);

                    // Логирование
                    system.getAuditLog().log(
                            "USER_DELETE",
                            system.getCurrentUser(),
                            username,
                            "User and all assignments deleted"
                    );

                    System.out.println("User and all their assignments deleted.");
                }
            }, () -> System.out.println("User not found."));
        });

        parser.registerCommand("user-search", "Search users by filters", (scanner, system) -> {
            printUserTable(searchUsersLogic(scanner, system.getUserManager()));
        });
    }

    // ROLE COMMANDS
    private static void registerRoleCommands(CommandParser parser) {
        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            System.out.printf("%-20s | %-15s | %s%n", "Role Name", "Perms Count", "ID");
            System.out.println("-".repeat(65));
            system.getRoleManager().findAll().forEach(r ->
                    System.out.printf("%-20s | %-15d | %s%n", r.getName(), r.getPermissions().size(), r.getId()));
        });

        parser.registerCommand("role-create", "Create a new role", (scanner, system) -> {
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Description: ");
            String desc = scanner.nextLine().trim();
            Role role = new Role(name, desc);
            system.getRoleManager().add(role);

            // Логирование
            system.getAuditLog().log(
                    "ROLE_CREATE",
                    system.getCurrentUser(),
                    name,
                    "Description: " + desc
            );

            System.out.println("Role created. Now add permissions.");
            addPermissionsLoop(scanner, role);
        });

        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();
            system.getRoleManager().findByName(name).ifPresentOrElse(
                    r -> System.out.println(r.format()),
                    () -> System.out.println("Role not found."));
        });

        parser.registerCommand("role-update", "Update role description", (scanner, system) -> {
            System.out.print("Enter role name to update: ");
            String name = scanner.nextLine().trim();
            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                System.out.print("Enter new description: ");
                role.setDescription(scanner.nextLine().trim());
                System.out.println("Role updated successfully.");
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("role-delete", "Delete a role", (scanner, system) -> {
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                if (system.getAssignmentManager().isRoleAssigned(role)) {
                    System.out.println("WARNING: Role is assigned to users:");
                    system.getAssignmentManager().findByFilter(AssignmentFilters.byRole(role))
                            .forEach(a -> System.out.println(" - " + a.user().username()));
                }
                System.out.print("Confirm deletion (type 'yes'): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                    try {
                        system.getRoleManager().remove(role);

                        // Логирование
                        system.getAuditLog().log(
                                "ROLE_DELETE",
                                system.getCurrentUser(),
                                name,
                                "Role deleted from system"
                        );

                        System.out.println("Role deleted.");
                    } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
                }
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            System.out.print("Role name: ");
            String rName = scanner.nextLine().trim();
            try {
                System.out.print("Permission name: ");
                String pName = scanner.nextLine().trim();
                System.out.print("Resource: ");
                String res = scanner.nextLine().trim();
                System.out.print("Description: ");
                String desc = scanner.nextLine().trim();
                system.getRoleManager().addPermissionToRole(rName, new Permission(pName, res, desc));
                System.out.println("Permission added.");
            } catch (Exception e) { System.out.println(e.getMessage()); }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, system) -> {
            System.out.print("Role name: ");
            String rName = scanner.nextLine().trim();
            system.getRoleManager().findByName(rName).ifPresentOrElse(role -> {
                List<Permission> perms = new ArrayList<>(role.getPermissions());
                for (int i = 0; i < perms.size(); i++) {
                    System.out.println((i + 1) + ". " + perms.get(i).format());
                }
                System.out.print("Select permission number to remove: ");
                try {
                    int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    system.getRoleManager().removePermissionFromRole(rName, perms.get(idx));
                    System.out.println("Permission removed.");
                } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            System.out.println("1. By name (contains)\n2. By permission name\n3. By min permissions count");
            System.out.print("Select filter number: ");
            String choice = scanner.nextLine().trim();

            List<Role> found = switch (choice) {
                case "1" -> {
                    System.out.print("Enter role name: ");
                    String rName = scanner.nextLine().trim();
                    yield system.getRoleManager().findByFilter(RoleFilters.byNameContains(rName));
                }
                case "2" -> {
                    System.out.print("Enter permission name: ");
                    String pName = scanner.nextLine().trim();
                    System.out.print("Enter resource (optional): ");
                    String pRes = scanner.nextLine().trim();
                    yield system.getRoleManager().findByFilter(role ->
                            role.getPermissions().stream().anyMatch(p ->
                                    p.name().equalsIgnoreCase(pName) &&
                                            (pRes.isEmpty() || p.resource().equalsIgnoreCase(pRes))
                            )
                    );
                }
                case "3" -> {
                    System.out.print("Enter minimum count: ");
                    try {
                        int min = Integer.parseInt(scanner.nextLine().trim());
                        yield system.getRoleManager().findByFilter(RoleFilters.hasAtLeastNPermissions(min));
                    } catch (NumberFormatException e) {
                        yield new ArrayList<>();
                    }
                }
                default -> {
                    System.out.println("Unknown filter. Showing all.");
                    yield system.getRoleManager().findAll();
                }
            };

            if (found.isEmpty()) {
                System.out.println("No roles found.");
            } else {
                found.forEach(r -> System.out.println(" - " + r.getName() + " (" + r.getPermissions().size() + " perms)"));
            }
        });
    }

    // ASSIGNMENT COMMANDS
    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            try {
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
                User user = system.getUserManager().findByUsername(username).orElseThrow(() -> new Exception("User not found"));

                System.out.println("Available roles:");
                system.getRoleManager().findAll().forEach(r -> System.out.println(" - " + r.getName()));
                System.out.print("Select role: ");
                String roleName = scanner.nextLine().trim();
                Role role = system.getRoleManager().findByName(roleName).orElseThrow(() -> new Exception("Role not found"));

                System.out.print("Type (permanent/temporary): ");
                String type = scanner.nextLine().trim().toLowerCase();
                System.out.print("Reason: ");
                String reason = scanner.nextLine().trim();
                AssignmentMetadata meta = AssignmentMetadata.now(system.getCurrentUser(), reason);

                String details;
                if (type.equals("temporary")) {
                    System.out.print("Expiration (yyyy-MM-dd HH:mm): ");
                    String date = scanner.nextLine().trim();
                    system.getAssignmentManager().add(new TemporaryAssignment(user, role, meta, date, false));
                    details = String.format("Type: TEMPORARY, Expires: %s, Reason: %s", date, reason);
                } else {
                    system.getAssignmentManager().add(new PermanentAssignment(user, role, meta));
                    details = String.format("Type: PERMANENT, Reason: %s", reason);
                }

                // Логирование
                system.getAuditLog().log(
                        "ASSIGNMENT_CREATE",
                        system.getCurrentUser(),
                        String.format("%s -> %s", username, roleName),
                        details
                );

                System.out.println("Role assigned.");
            } catch (Exception e) { System.out.println("Assignment failed: " + e.getMessage()); }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var assignments = system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username))
                    .stream().filter(RoleAssignment::isActive).toList();

            if (assignments.isEmpty()) {
                System.out.println("No active assignments found.");
                return;
            }

            for (int i = 0; i < assignments.size(); i++) {
                System.out.printf("%d. Role: %s (%s)%n", i + 1, assignments.get(i).role().getName(), assignments.get(i).assignmentType());
            }
            System.out.print("Select assignment number to revoke: ");
            try {
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                RoleAssignment assignment = assignments.get(idx);
                system.getAssignmentManager().revokeAssignment(assignments.get(idx).assignmentId());

                // Логирование
                system.getAuditLog().log(
                        "ASSIGNMENT_REVOKE",
                        system.getCurrentUser(),
                        String.format("%s -> %s", username, assignment.role().getName()),
                        String.format("Type: %s", assignment.assignmentType())
                );

                System.out.println("Role revoked.");
            } catch (Exception e) { System.out.println("Revoke failed: " + e.getMessage()); }
        });

        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) -> {
            printAssignmentTable(system.getAssignmentManager().findAll());
        });

        parser.registerCommand("assignment-list-user", "Assignments of user", (scanner, system) -> {
            System.out.print("Username: ");
            String name = scanner.nextLine().trim();
            printAssignmentTable(system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(name)));
        });

        parser.registerCommand("assignment-list-role", "Users with specific role", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String rName = scanner.nextLine().trim();
            system.getRoleManager().findByName(rName).ifPresentOrElse(role -> {
                printAssignmentTable(system.getAssignmentManager().findByFilter(AssignmentFilters.byRole(role)));
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("assignment-active", "Show active assignments", (scanner, system) -> {
            printAssignmentTable(system.getAssignmentManager().findByFilter(AssignmentFilters.activeOnly()));
        });

        parser.registerCommand("assignment-expired", "Show expired temporary assignments details", (scanner, system) -> {
            var allAssignments = system.getAssignmentManager().findAll();

            var expiredAssignments = allAssignments.stream()
                    .filter(a -> a.assignmentType().equalsIgnoreCase("TEMPORARY"))
                    .filter(a -> !a.isActive())
                    .toList();

            System.out.println("\nEXPIRED TEMPORARY ASSIGNMENTS:");
            System.out.printf("%-15s | %-15s | %-10s%n", "User", "Role", "Status");
            System.out.println("-".repeat(50));

            if (expiredAssignments.isEmpty()) {
                System.out.println("No expired temporary assignments found.");
            } else {
                expiredAssignments.forEach(a -> {
                    String username = a.user().username();
                    String roleName = a.role().getName();
                    String status = "EXPIRED";

                    System.out.printf("%-15s | %-15s | %-10s%n",
                            username,
                            roleName,
                            status);
                });
            }
        });

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Enter role name: ");
            String roleName = scanner.nextLine().trim();

            var userOpt = system.getUserManager().findByUsername(username);
            var roleOpt = system.getRoleManager().findByName(roleName);

            if (userOpt.isPresent() && roleOpt.isPresent()) {
                var assignment = system.getAssignmentManager().findAll().stream()
                        .filter(a -> a.user().equals(userOpt.get()))
                        .filter(a -> a.role().equals(roleOpt.get()))
                        .filter(a -> a.assignmentType().equalsIgnoreCase("TEMPORARY"))
                        .findFirst();

                if (assignment.isPresent()) {
                    System.out.print("Enter new expiration date (yyyy-MM-dd HH:mm): ");
                    String newDateStr = scanner.nextLine().trim();

                    try {
                        system.getAssignmentManager().extendTemporaryAssignment(assignment.get().assignmentId(), newDateStr);

                        System.out.println("Success: Assignment extended. Status is now: " +
                                (assignment.get().isActive() ? "ACTIVE" : "STILL EXPIRED (check date)"));
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                } else {
                    System.out.println("Error: No temporary assignment found for this user/role pair.");
                }
            } else {
                System.out.println("Error: User or Role not found.");
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filter", (scanner, system) -> {
            System.out.println("\n1. By User\n2. By Role\n3. By Type (permanent/temporary)\n4. By Status (active/inactive)\n5. After Date\n6. Expiring before Date");
            System.out.print("Select filter number: ");
            String choice = scanner.nextLine().trim();

            var filter = switch(choice) {
                case "1" -> {
                    System.out.print("Enter username: ");
                    yield AssignmentFilters.byUsername(scanner.nextLine().trim());
                }
                case "2" -> {
                    System.out.print("Enter role name: ");
                    yield AssignmentFilters.byRoleName(scanner.nextLine().trim());
                }
                case "3" -> {
                    System.out.print("Enter type (PERMANENT/TEMPORARY): ");
                    yield AssignmentFilters.byType(scanner.nextLine().trim().toUpperCase());
                }
                case "4" -> {
                    System.out.print("Enter status (active/inactive): ");
                    String status = scanner.nextLine().trim().toLowerCase();
                    if (status.equals("inactive")) yield AssignmentFilters.inactiveOnly();
                    yield AssignmentFilters.activeOnly(); // По умолчанию активные
                }
                case "5" -> {
                    System.out.print("Enter start date (yyyy-MM-dd HH:mm): ");
                    yield AssignmentFilters.assignedAfter(scanner.nextLine().trim());
                }
                case "6" -> {
                    System.out.print("Enter end date (yyyy-MM-dd HH:mm): ");
                    yield AssignmentFilters.expiringBefore(scanner.nextLine().trim());
                }
                default -> {
                    System.out.println("Invalid choice. Showing active assignments.");
                    yield AssignmentFilters.activeOnly();
                }
            };

            printAssignmentTable(system.getAssignmentManager().findByFilter(filter));
        });
    }

    // PERMISSION COMMANDS
    private static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user", "User permissions by resource", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                var perms = system.getAssignmentManager().getUserPermissions(user);
                perms.stream().collect(Collectors.groupingBy(Permission::resource))
                        .forEach((res, pList) -> {
                            System.out.println("Resource: " + res);
                            pList.forEach(p -> System.out.println("  - " + p.name() + " (" + p.description() + ")"));
                        });
            }, () -> System.out.println("User not found."));
        });

        parser.registerCommand("permissions-check", "Check specific permission", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                System.out.print("Permission name (READ): ");
                String pName = scanner.nextLine().trim();
                System.out.print("Resource (users): ");
                String res = scanner.nextLine().trim();

                boolean has = system.getAssignmentManager().userHasPermission(user, pName, res);
                System.out.println("Result: " + (has ? "GRANTED" : "DENIED"));

                if (has) {
                    System.out.println("Source roles:");
                    system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username))
                            .stream().filter(a -> a.isActive() && a.role().hasPermission(pName, res))
                            .forEach(a -> System.out.println(" - " + a.role().getName()));
                }
            }, () -> System.out.println("User not found."));
        });
    }

    private static void registerUtilityCommands(CommandParser parser) {
        parser.registerCommand("help", "Show help", (scanner, system) -> parser.printHelp());
        parser.registerCommand("stats", "System statistics", (scanner, system) -> System.out.println(system.generateStatistics()));
        parser.registerCommand("clear", "Clear screen", (scanner, system) -> system.clearScreen());
        parser.registerCommand("exit", "Exit program", (scanner, system) -> {
            System.out.print("Confirm exit? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) System.exit(0);
        });
        parser.registerCommand("audit-log", "Show ausit log", (scanner, system) -> system.getAuditLog().printLog());
    }

    // HELPERS
    private static List<User> searchUsersLogic(Scanner scanner, UserManager um) {
        System.out.println("1. Username (contains)\n2. Email (contains)\n3. Email Domain\n4. Full Name (contains)");
        String choice = scanner.nextLine().trim();
        System.out.print("Enter search string: ");
        String query = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> um.findByFilter(UserFilters.byUsernameContains(query));
            case "2" -> um.findByFilter(UserFilters.byEmail(query));
            case "3" -> um.findByFilter(UserFilters.byEmailDomain(query));
            case "4" -> um.findByFilter(UserFilters.byFullNameContains(query));
            default -> um.findAll();
        };
    }

    private static void addPermissionsLoop(Scanner scanner, Role role) {
        while (true) {
            System.out.print("Add permission to this role? (yes/no): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) break;
            try {
                System.out.print("Permission name: ");
                String pName = scanner.nextLine().trim();
                System.out.print("Resource: ");
                String res = scanner.nextLine().trim();
                System.out.print("Description: ");
                String pDesc = scanner.nextLine().trim();
                role.addPermission(new Permission(pName, res, pDesc));
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
        }
    }

    private static void printUserTable(List<User> users) {
        System.out.printf("%-15s | %-25s | %-25s%n", "Username", "Full Name", "Email");
        System.out.println("-".repeat(70));
        users.forEach(u -> System.out.printf("%-15s | %-25s | %-25s%n", u.username(), u.fullName(), u.email()));
    }

    private static void printAssignmentTable(List<RoleAssignment> list) {
        System.out.printf("%-15s | %-15s | %-10s | %-10s | %s%n", "User", "Role", "Type", "Status", "Assigned At");
        System.out.println("-".repeat(85));
        list.forEach(a -> System.out.printf("%-15s | %-15s | %-10s | %-10s | %s%n",
                a.user().username(), a.role().getName(), a.assignmentType(), a.isActive() ? "ACTIVE" : "INACTIVE", a.metadata().assignedAt()));
    }
}