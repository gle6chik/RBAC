package core;

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
        registerAsyncCommands(parser);
    }

    private static void registerAsyncCommands(CommandParser parser) {
        parser.registerCommand("report-users-async", "Generate user report in background", (scanner, system) -> {
            System.out.println("Generating user report in background...");

            BackgroundExecutor.submit(() -> {
                String report = ReportGenerator.generateUserReport(
                        system.getUserManager(),
                        system.getAssignmentManager()
                );

                System.out.println("\n" + "=".repeat(50));
                System.out.println("ASYNC REPORT READY");
                System.out.println("=".repeat(50));
                System.out.println(report);
            });

            System.out.println("Report generation started. It will appear when ready.");
        });

        parser.registerCommand("save-async", "Save all users to CSV in background", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Enter filename (e.g., users.csv): ", true);
            if (!filename.endsWith(".csv")) {
                filename += ".csv";
            }

            system.getUserManager().exportToCsvAsync(filename);
            System.out.println("Saving users to " + filename + " in background...");
        });
    }

    private static void registerReportCommands(CommandParser parser) {
        parser.registerCommand("report-users", "Generate user report", (scanner, system) -> {
            System.out.println("\nGenerating user report...");

            String report = ReportGenerator.generateUserReport(
                    system.getUserManager(),
                    system.getAssignmentManager()
            );

            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Save report to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Enter filename [user_report.txt]: ", false);
                if (filename.isEmpty()) {
                    filename = "user_report.txt";
                }
                ReportGenerator.exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-roles", "Generate role report", (scanner, system) -> {
            System.out.println("\nGenerating role report...");

            String report = ReportGenerator.generateRoleReport(
                    system.getRoleManager(),
                    system.getAssignmentManager()
            );

            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Save report to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Enter filename [role_report.txt]: ", false);
                if (filename.isEmpty()) {
                    filename = "role_report.txt";
                }
                ReportGenerator.exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-matrix", "Generate permission matrix", (scanner, system) -> {
            System.out.println("\nGenerating permission matrix...");

            String report = ReportGenerator.generatePermissionMatrix(
                    system.getUserManager(),
                    system.getAssignmentManager()
            );

            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Save matrix to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Enter filename [permission_matrix.txt]: ", false);
                if (filename.isEmpty()) {
                    filename = "permission_matrix.txt";
                }
                ReportGenerator.exportToFile(report, filename);
            }
        });
    }

    // USER COMMANDS
    private static void registerUserCommands(CommandParser parser) {
        parser.registerCommand("user-list", "List all users with optional filters", (scanner, system) -> {
            UserManager um = system.getUserManager();
            List<User> users;

            if (ConsoleUtils.promptYesNo(scanner, "Apply filters?")) {
                users = searchUsersLogic(scanner, um);
            } else {
                users = um.findAll();
            }
            printUserTable(users);
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, system) -> {
            try {
                String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);
                String fullName = ConsoleUtils.promptString(scanner, "Enter full name: ", true);
                String email = ConsoleUtils.promptString(scanner, "Enter email: ", true);

                User newUser = User.validate(username, fullName, email);
                system.getUserManager().add(newUser);

                system.getAuditLog().log(
                        "USER_CREATE",
                        system.getCurrentUser(),
                        username,
                        String.format("Full name: %s, Email: %s", fullName, email)
                );

                ConsoleUtils.printSuccess("User created successfully!");
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View detailed user info", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);

            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                // Информация о пользователе в рамке
                System.out.println(FormatUtils.formatBox(user.format()));

                var assignments = system.getAssignmentManager().findByFilter(
                        filters.AssignmentFilters.byUsername(username)
                );
                var allPermissions = system.getAssignmentManager().getUserPermissions(user);

                if (assignments.isEmpty()) {
                    System.out.println("[No roles assigned to this user]");
                } else {
                    System.out.println("\nRoles assigned to " + username + ":");

                    assignments.forEach(a -> {
                        StringBuilder roleInfo = new StringBuilder();

                        // Заголовок роли
                        String type = a.assignmentType();
                        String status = a.isActive() ? "ACTIVE" : "INACTIVE/EXPIRED";
                        roleInfo.append(String.format("Type: %s | Status: [%s]\n", type, status));

                        // Информация о временном назначении
                        if (a instanceof TemporaryAssignment temp) {
                            roleInfo.append("Validity: ").append(temp.summary()).append("\n");
                        }

                        // Детальная информация о роли
                        roleInfo.append(a.role().format());

                        // Каждая роль в своей рамке
                        System.out.println(FormatUtils.formatBox(roleInfo.toString()));
                    });
                }

                // Права пользователя в рамке
                if (allPermissions.isEmpty()) {
                    System.out.println(FormatUtils.formatBox("No active permissions found"));
                } else {
                    StringBuilder permsInfo = new StringBuilder("All permissions for this user:\n\n");
                    allPermissions.forEach(p -> permsInfo.append("  - ").append(p.format()).append("\n"));
                    System.out.println(FormatUtils.formatBox(permsInfo.toString()));
                }

            }, () -> ConsoleUtils.printError("User '" + username + "' not found."));
        });

        parser.registerCommand("user-update", "Update user data", (scanner, system) -> {
            // Заменяем прямые вызовы scanner на ConsoleUtils
            String username = ConsoleUtils.promptString(scanner, "Enter username to update: ", true);

            try {
                String name = ConsoleUtils.promptString(scanner, "Enter new full name: ", true);
                String email = ConsoleUtils.promptString(scanner, "Enter new email: ", true);

                system.getUserManager().update(username, name, email);
                ConsoleUtils.printSuccess("User updated successfully.");
            } catch (Exception e) {
                ConsoleUtils.printError("Update failed: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user and their assignments", (scanner, system) -> {
            // Заменяем прямой ввод на ConsoleUtils
            String username = ConsoleUtils.promptString(scanner, "Enter username to delete: ", true);

            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                // Заменяем прямой ввод на ConsoleUtils
                if (ConsoleUtils.promptYesNo(scanner, "Confirm deletion")) {
                    var userAssignments = system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username));
                    userAssignments.forEach(a -> system.getAssignmentManager().remove(a));
                    system.getUserManager().remove(user);

                    system.getAuditLog().log(
                            "USER_DELETE",
                            system.getCurrentUser(),
                            username,
                            "User and all assignments deleted"
                    );

                    ConsoleUtils.printSuccess("User and all their assignments deleted.");
                }
            }, () -> ConsoleUtils.printError("User not found."));
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
            // Заменяем прямые вызовы scanner на ConsoleUtils
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);
            String desc = ConsoleUtils.promptString(scanner, "Description: ", true);

            try {
                Role role = new Role(name, desc);
                system.getRoleManager().add(role);

                system.getAuditLog().log(
                        "ROLE_CREATE",
                        system.getCurrentUser(),
                        name,
                        "Description: " + desc
                );

                ConsoleUtils.printSuccess("Role created. Now add permissions.");
                addPermissionsLoop(scanner, role);
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            system.getRoleManager().findByName(name).ifPresentOrElse(
                    r -> {
                        String roleContent = r.format();
                        System.out.println(FormatUtils.formatBox(roleContent));
                    },
                    () -> ConsoleUtils.printError("Role not found."));
        });

        parser.registerCommand("role-update", "Update role description", (scanner, system) -> {
            // Заменяем прямые вызовы scanner на ConsoleUtils
            String name = ConsoleUtils.promptString(scanner, "Enter role name to update: ", true);

            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                String desc = ConsoleUtils.promptString(scanner, "Enter new description: ", true);
                role.setDescription(desc);
                ConsoleUtils.printSuccess("Role updated successfully.");
            }, () -> ConsoleUtils.printError("Role not found."));
        });

        parser.registerCommand("role-delete", "Delete a role", (scanner, system) -> {
            // Заменяем прямой ввод на ConsoleUtils
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);

            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                if (system.getAssignmentManager().isRoleAssigned(role)) {
                    System.out.println("WARNING: Role is assigned to users:");
                    system.getAssignmentManager().findByFilter(AssignmentFilters.byRole(role))
                            .forEach(a -> System.out.println(" - " + a.user().username()));
                }

                if (ConsoleUtils.promptYesNo(scanner, "Confirm deletion")) {
                    try {
                        system.getRoleManager().remove(role);

                        system.getAuditLog().log(
                                "ROLE_DELETE",
                                system.getCurrentUser(),
                                name,
                                "Role deleted from system"
                        );

                        ConsoleUtils.printSuccess("Role deleted.");
                    } catch (Exception e) {
                        ConsoleUtils.printError(e.getMessage());
                    }
                }
            }, () -> ConsoleUtils.printError("Role not found."));
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            String rName = ConsoleUtils.promptString(scanner, "Role name: ", true);

            try {
                String pName = ConsoleUtils.promptString(scanner, "Permission name: ", true);
                String res = ConsoleUtils.promptString(scanner, "Resource: ", true);
                String desc = ConsoleUtils.promptString(scanner, "Description: ", true);

                system.getRoleManager().addPermissionToRole(rName, new Permission(pName, res, desc));
                ConsoleUtils.printSuccess("Permission added.");
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, system) -> {
            String rName = ConsoleUtils.promptString(scanner, "Role name: ", true);

            system.getRoleManager().findByName(rName).ifPresentOrElse(role -> {
                List<Permission> perms = new ArrayList<>(role.getPermissions());

                if (perms.isEmpty()) {
                    ConsoleUtils.printWarning("Role has no permissions to remove.");
                    return;
                }

                Permission selected = ConsoleUtils.promptChoice(
                        scanner,
                        "Select permission to remove:",
                        perms
                );

                try {
                    system.getRoleManager().removePermissionFromRole(rName, selected);
                    ConsoleUtils.printSuccess("Permission removed.");
                } catch (Exception e) {
                    ConsoleUtils.printError(e.getMessage());
                }

            }, () -> ConsoleUtils.printError("Role not found."));
        });

        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            List<String> searchOptions = Arrays.asList(
                    "By name (contains)",
                    "By permission name",
                    "By min permissions count"
            );

            String choice = ConsoleUtils.promptChoice(scanner, "Select filter:", searchOptions);

            List<Role> found = switch (searchOptions.indexOf(choice)) {
                case 0 -> {
                    String rName = ConsoleUtils.promptString(scanner, "Enter role name: ", true);
                    yield system.getRoleManager().findByFilter(RoleFilters.byNameContains(rName));
                }
                case 1 -> {
                    String pName = ConsoleUtils.promptString(scanner, "Enter permission name: ", true);
                    String pRes = ConsoleUtils.promptString(scanner, "Enter resource (optional): ", false);
                    yield system.getRoleManager().findByFilter(role ->
                            role.getPermissions().stream().anyMatch(p ->
                                    p.name().equalsIgnoreCase(pName) &&
                                            (pRes.isEmpty() || p.resource().equalsIgnoreCase(pRes))
                            )
                    );
                }
                case 2 -> {
                    int min = ConsoleUtils.promptInt(scanner, "Enter minimum count: ", 0, 100);
                    yield system.getRoleManager().findByFilter(RoleFilters.hasAtLeastNPermissions(min));
                }
                default -> system.getRoleManager().findAll();
            };

            if (found.isEmpty()) {
                ConsoleUtils.printWarning("No roles found.");
            } else {
                found.forEach(r -> System.out.println(" - " + r.getName() + " (" + r.getPermissions().size() + " perms)"));
            }
        });
    }

    // ASSIGNMENT COMMANDS
    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            try {
                String username = ConsoleUtils.promptString(scanner, "Username: ", true);
                User user = system.getUserManager().findByUsername(username)
                        .orElseThrow(() -> new Exception("User not found"));

                List<Role> availableRoles = system.getRoleManager().findAll();
                if (availableRoles.isEmpty()) {
                    ConsoleUtils.printWarning("No roles available to assign.");
                    return;
                }

                Role role = ConsoleUtils.promptChoice(
                        scanner,
                        "Available roles:",
                        availableRoles
                );

                String type = ConsoleUtils.promptString(scanner, "Type (permanent/temporary): ", true);
                String reason = ConsoleUtils.promptString(scanner, "Reason: ", true);
                AssignmentMetadata meta = AssignmentMetadata.now(system.getCurrentUser(), reason);

                String details;
                if (type.equalsIgnoreCase("temporary")) {
                    String date = ConsoleUtils.promptString(scanner, "Expiration (yyyy-MM-dd HH:mm): ", true);
                    system.getAssignmentManager().add(new TemporaryAssignment(user, role, meta, date, false));
                    details = String.format("Type: TEMPORARY, Expires: %s, Reason: %s", date, reason);
                } else {
                    system.getAssignmentManager().add(new PermanentAssignment(user, role, meta));
                    details = String.format("Type: PERMANENT, Reason: %s", reason);
                }

                system.getAuditLog().log(
                        "ASSIGNMENT_CREATE",
                        system.getCurrentUser(),
                        String.format("%s -> %s", username, role.getName()),
                        details
                );

                ConsoleUtils.printSuccess("Role assigned.");
            } catch (Exception e) {
                ConsoleUtils.printError("Assignment failed: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            var assignments = system.getAssignmentManager()
                    .findByFilter(AssignmentFilters.byUsername(username))
                    .stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());

            if (assignments.isEmpty()) {
                ConsoleUtils.printWarning("No active assignments found.");
                return;
            }

            RoleAssignment selected = ConsoleUtils.promptChoice(
                    scanner,
                    "Select assignment to revoke:",
                    assignments
            );

            if (ConsoleUtils.promptYesNo(scanner, "Confirm revocation")) {
                try {
                    system.getAssignmentManager().revokeAssignment(selected.assignmentId());

                    system.getAuditLog().log(
                            "ASSIGNMENT_REVOKE",
                            system.getCurrentUser(),
                            String.format("%s -> %s", username, selected.role().getName()),
                            String.format("Type: %s", selected.assignmentType())
                    );

                    ConsoleUtils.printSuccess("Role revoked.");
                } catch (Exception e) {
                    ConsoleUtils.printError("Revoke failed: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) -> {
            printAssignmentTable(system.getAssignmentManager().findAll());
        });

        parser.registerCommand("assignment-list-user", "Assignments of user", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Username: ", true);
            printAssignmentTable(system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(name)));
        });

        parser.registerCommand("assignment-list-role", "Users with specific role", (scanner, system) -> {
            String rName = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            system.getRoleManager().findByName(rName).ifPresentOrElse(role -> {
                printAssignmentTable(system.getAssignmentManager().findByFilter(AssignmentFilters.byRole(role)));
            }, () -> ConsoleUtils.printError("Role not found."));
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

            System.out.println(FormatUtils.formatHeader("EXPIRED TEMPORARY ASSIGNMENTS"));

            if (expiredAssignments.isEmpty()) {
                System.out.println(FormatUtils.formatBox("No expired temporary assignments found."));
            } else {
                String[] headers = {"User", "Role", "Status"};

                List<String[]> rows = expiredAssignments.stream()
                        .map(a -> new String[]{
                                a.user().username(),
                                a.role().getName(),
                                "EXPIRED"
                        })
                        .collect(Collectors.toList());

                System.out.print(FormatUtils.formatTable(headers, rows));
            }
        });

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);
            String roleName = ConsoleUtils.promptString(scanner, "Enter role name: ", true);

            var userOpt = system.getUserManager().findByUsername(username);
            var roleOpt = system.getRoleManager().findByName(roleName);

            if (userOpt.isPresent() && roleOpt.isPresent()) {
                var assignment = system.getAssignmentManager().findAll().stream()
                        .filter(a -> a.user().equals(userOpt.get()))
                        .filter(a -> a.role().equals(roleOpt.get()))
                        .filter(a -> a.assignmentType().equalsIgnoreCase("TEMPORARY"))
                        .findFirst();

                if (assignment.isPresent()) {
                    String newDateStr = ConsoleUtils.promptString(scanner, "Enter new expiration date (yyyy-MM-dd HH:mm): ", true);

                    try {
                        system.getAssignmentManager().extendTemporaryAssignment(assignment.get().assignmentId(), newDateStr);

                        ConsoleUtils.printSuccess("Assignment extended. Status is now: " +
                                (assignment.get().isActive() ? "ACTIVE" : "STILL EXPIRED (check date)"));
                    } catch (Exception e) {
                        ConsoleUtils.printError(e.getMessage());
                    }
                } else {
                    ConsoleUtils.printError("No temporary assignment found for this user/role pair.");
                }
            } else {
                ConsoleUtils.printError("User or Role not found.");
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filter", (scanner, system) -> {
            List<String> filterOptions = Arrays.asList(
                    "By User",
                    "By Role",
                    "By Type (permanent/temporary)",
                    "By Status (active/inactive)",
                    "After Date",
                    "Expiring before Date"
            );

            String choice = ConsoleUtils.promptChoice(scanner, "Select filter:", filterOptions);

            var filter = switch (filterOptions.indexOf(choice)) {
                case 0 -> {
                    String username = ConsoleUtils.promptString(scanner, "Enter username: ", true);
                    yield AssignmentFilters.byUsername(username);
                }
                case 1 -> {
                    String roleName = ConsoleUtils.promptString(scanner, "Enter role name: ", true);
                    yield AssignmentFilters.byRoleName(roleName);
                }
                case 2 -> {
                    String type = ConsoleUtils.promptString(scanner, "Enter type (PERMANENT/TEMPORARY): ", true);
                    yield AssignmentFilters.byType(type.toUpperCase());
                }
                case 3 -> {
                    String status = ConsoleUtils.promptString(scanner, "Enter status (active/inactive): ", true);
                    if (status.equalsIgnoreCase("inactive")) yield AssignmentFilters.inactiveOnly();
                    yield AssignmentFilters.activeOnly();
                }
                case 4 -> {
                    String date = ConsoleUtils.promptString(scanner, "Enter start date (yyyy-MM-dd HH:mm): ", true);
                    yield AssignmentFilters.assignedAfter(date);
                }
                case 5 -> {
                    String date = ConsoleUtils.promptString(scanner, "Enter end date (yyyy-MM-dd HH:mm): ", true);
                    yield AssignmentFilters.expiringBefore(date);
                }
                default -> AssignmentFilters.activeOnly();
            };

            printAssignmentTable(system.getAssignmentManager().findByFilter(filter));
        });
    }

    // PERMISSION COMMANDS
    private static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user", "User permissions by resource", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                var perms = system.getAssignmentManager().getUserPermissions(user);

                if (perms.isEmpty()) {
                    System.out.println(FormatUtils.formatBox("No permissions found for user: " + username));
                } else {
                    Map<String, List<Permission>> byResource = perms.stream()
                            .collect(Collectors.groupingBy(Permission::resource));

                    System.out.println(FormatUtils.formatHeader("Permissions for " + username));

                    byResource.forEach((resource, permissions) -> {
                        StringBuilder resourceContent = new StringBuilder();
                        resourceContent.append("Resource: ").append(resource).append("\n");
                        resourceContent.append("-".repeat(resource.length() + 10)).append("\n");

                        permissions.forEach(p ->
                                resourceContent.append("  - ").append(p.name())
                                        .append(": ").append(p.description())
                                        .append("\n")
                        );

                        System.out.println(FormatUtils.formatBox(resourceContent.toString()));
                    });
                }
            }, () -> ConsoleUtils.printError("User not found."));
        });

        parser.registerCommand("permissions-check", "Check specific permission", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                String pName = ConsoleUtils.promptString(scanner, "Permission name (READ): ", true);
                String res = ConsoleUtils.promptString(scanner, "Resource (users): ", true);

                boolean has = system.getAssignmentManager().userHasPermission(user, pName, res);
                System.out.println("Result: " + (has ? "GRANTED" : "DENIED"));

                if (has) {
                    System.out.println("Source roles:");
                    system.getAssignmentManager().findByFilter(AssignmentFilters.byUsername(username))
                            .stream().filter(a -> a.isActive() && a.role().hasPermission(pName, res))
                            .forEach(a -> System.out.println(" - " + a.role().getName()));
                }
            }, () -> ConsoleUtils.printError("User not found."));
        });
    }

    private static void registerUtilityCommands(CommandParser parser) {
        parser.registerCommand("help", "Show help", (scanner, system) -> parser.printHelp());
        parser.registerCommand("stats", "System statistics", (scanner, system) -> System.out.println(system.generateStatistics()));
        parser.registerCommand("clear", "Clear screen", (scanner, system) -> system.clearScreen());
        parser.registerCommand("exit", "Exit program", (scanner, system) -> {
            // Заменяем прямой ввод на ConsoleUtils
            if (ConsoleUtils.promptYesNo(scanner, "Confirm exit")) {
                System.exit(0);
            }
        });
        parser.registerCommand("audit-log", "Show audit log", (scanner, system) -> system.getAuditLog().printLog());
    }

    // HELPERS
    private static List<User> searchUsersLogic(Scanner scanner, UserManager um) {
        List<String> searchOptions = Arrays.asList(
                "Username (contains)",
                "Email (contains)",
                "Email Domain",
                "Full Name (contains)"
        );

        String choice = ConsoleUtils.promptChoice(scanner, "Select search type:", searchOptions);
        String query = ConsoleUtils.promptString(scanner, "Enter search string: ", true);

        return switch (searchOptions.indexOf(choice)) {
            case 0 -> um.findByFilter(UserFilters.byUsernameContains(query));
            case 1 -> um.findByFilter(UserFilters.byEmail(query));
            case 2 -> um.findByFilter(UserFilters.byEmailDomain(query));
            case 3 -> um.findByFilter(UserFilters.byFullNameContains(query));
            default -> um.findAll();
        };
    }

    private static void addPermissionsLoop(Scanner scanner, Role role) {
        while (ConsoleUtils.promptYesNo(scanner, "Add permission to this role?")) {
            try {
                String pName = ConsoleUtils.promptString(scanner, "Permission name: ", true);
                String res = ConsoleUtils.promptString(scanner, "Resource: ", true);
                String pDesc = ConsoleUtils.promptString(scanner, "Description: ", true);
                role.addPermission(new Permission(pName, res, pDesc));
                ConsoleUtils.printSuccess("Permission added.");
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
        }
    }

    private static void printUserTable(List<User> users) {
        String[] headers = {"Username", "Full Name", "Email"};
        List<String[]> rows = users.stream()
                .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                .collect(Collectors.toList());
        System.out.print(FormatUtils.formatTable(headers, rows));
    }

    private static void printAssignmentTable(List<RoleAssignment> list) {
        String[] headers = {"User", "Role", "Type", "Status", "Assigned At"};
        List<String[]> rows = list.stream()
                .map(a -> new String[]{
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        a.isActive() ? "ACTIVE" : "INACTIVE",
                        a.metadata().assignedAt()
                })
                .collect(Collectors.toList());
        System.out.print(FormatUtils.formatTable(headers, rows));
    }
}