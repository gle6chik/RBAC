package utils;

import managers.*;
import model.*;
import assignment.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {
    public static String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(FormatUtils.formatHeader("USER REPORT"));

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system.\n");
            return sb.toString();
        }

        sb.append(String.format("Total users: %d\n\n", users.size()));

        // parallelStream - для параллельной обработки пользователей
        String userSections = users.parallelStream()
                .map(user -> formatUserSection(user, assignmentManager)) // Каждый пользователь в своей рамке
                .collect(Collectors.joining("\n"));

        sb.append(userSections);

        // Последовательная обработка пользователей
//        for (User user : users) {
//            // Каждый пользователь в своей рамке
//            sb.append(FormatUtils.formatBox(formatUserSection(user, assignmentManager)));
//        }

        sb.append(FormatUtils.formatHeader("END OF USER REPORT"));

        return sb.toString();
    }

    private static String formatUserSection(User user, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        // Информация о пользователе
        sb.append(String.format("USER: %s\n", user.username()));
        sb.append(String.format("Full Name: %s\n", user.fullName()));
        sb.append(String.format("Email: %s\n", user.email()));

        // Назначения пользователя
        List<RoleAssignment> assignments = assignmentManager.findByFilter(
                filters.AssignmentFilters.byUsername(user.username())
        );

        List<RoleAssignment> activeAssignments = assignments.stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());

        List<RoleAssignment> inactiveAssignments = assignments.stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());

        sb.append(String.format("\nRole Assignments:\n"));
        sb.append(String.format("  Active: %d\n", activeAssignments.size()));
        sb.append(String.format("  Inactive: %d\n", inactiveAssignments.size()));

        if (!activeAssignments.isEmpty()) {
            sb.append("\n  Active Roles:\n");
            for (RoleAssignment ra : activeAssignments) {
                String type = ra.assignmentType();
                String expires = "";
                if (ra instanceof TemporaryAssignment) {
                    expires = String.format(", Expires: %s",
                            ((TemporaryAssignment) ra).getExpiresAt());
                }
                sb.append(String.format("    - %s (%s)%s\n",
                        ra.role().getName(), type, expires));
            }
        }

        // Права пользователя
        Set<Permission> permissions = assignmentManager.getUserPermissions(user);
        if (!permissions.isEmpty()) {
            sb.append("\n  Effective Permissions:\n");
            permissions.stream()
                    .collect(Collectors.groupingBy(Permission::resource))
                    .forEach((resource, perms) -> {
                        sb.append(String.format("    Resource: %s\n", resource));
                        perms.forEach(p ->
                                sb.append(String.format("      - %s: %s\n",
                                        p.name(), p.description()))
                        );
                    });
        }

        return sb.toString();
    }

    public static String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(FormatUtils.formatHeader("ROLE REPORT"));

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            sb.append("No roles found in the system.\n");
            return sb.toString();
        }

        sb.append(String.format("Total roles: %d\n\n", roles.size()));

        // Статистика по ролям
        Map<Role, Long> userCountByRole = new HashMap<>();
        Map<Role, Long> activeUserCountByRole = new HashMap<>();

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByFilter(
                    filters.AssignmentFilters.byRole(role)
            );

            long totalUsers = assignments.stream()
                    .map(a -> a.user().username())
                    .distinct()
                    .count();

            long activeUsers = assignments.stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.user().username())
                    .distinct()
                    .count();

            userCountByRole.put(role, totalUsers);
            activeUserCountByRole.put(role, activeUsers);
        }

        List<Role> sortedRoles = roles.stream()
                .sorted((r1, r2) -> Long.compare(
                        userCountByRole.get(r2), userCountByRole.get(r1)))
                .collect(Collectors.toList());

        for (Role role : sortedRoles) {
            // Каждая роль в своей рамке
            sb.append(FormatUtils.formatBox(formatRoleSection(role,
                    userCountByRole.get(role),
                    activeUserCountByRole.get(role),
                    assignmentManager)));
        }

        sb.append(FormatUtils.formatHeader("END OF ROLE REPORT"));

        return sb.toString();
    }

    private static String formatRoleSection(Role role, long totalUsers, long activeUsers,
                                            AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("ROLE: %s\n", role.getName()));
        sb.append(String.format("ID: %s\n", role.getId()));
        sb.append(String.format("Description: %s\n", role.getDescription()));
        sb.append(String.format("Users assigned: %d (active: %d)\n", totalUsers, activeUsers));

        // Права роли
        Set<Permission> permissions = role.getPermissions();
        sb.append(String.format("Permissions: %d\n", permissions.size()));

        if (!permissions.isEmpty()) {
            permissions.stream()
                    .collect(Collectors.groupingBy(Permission::resource))
                    .forEach((resource, perms) -> {
                        sb.append(String.format("  Resource: %s\n", resource));
                        perms.forEach(p ->
                                sb.append(String.format("    - %s: %s\n",
                                        p.name(), p.description()))
                        );
                    });
        }

        // Пользователи с этой ролью
        List<RoleAssignment> assignments = assignmentManager.findByFilter(
                filters.AssignmentFilters.byRole(role)
        );

        if (!assignments.isEmpty()) {
            sb.append("\nUsers with this role:\n");

            // Группировка по статусу
            Map<Boolean, List<RoleAssignment>> byStatus = assignments.stream()
                    .collect(Collectors.groupingBy(RoleAssignment::isActive));

            List<RoleAssignment> active = byStatus.getOrDefault(true, new ArrayList<>());
            List<RoleAssignment> inactive = byStatus.getOrDefault(false, new ArrayList<>());

            if (!active.isEmpty()) {
                sb.append("  Active:\n");
                active.forEach(a -> {
                    String type = a.assignmentType();
                    String info = String.format("    - %s (%s)",
                            a.user().username(), type);
                    if (a instanceof TemporaryAssignment) {
                        info += String.format(", expires: %s",
                                ((TemporaryAssignment) a).getExpiresAt());
                    }
                    sb.append(info).append("\n");
                });
            }

            if (!inactive.isEmpty()) {
                sb.append("  Inactive/Expired:\n");
                inactive.forEach(a ->
                        sb.append(String.format("    - %s\n", a.user().username()))
                );
            }
        }

        return sb.toString();
    }

    public static String generatePermissionMatrix(UserManager userManager,
                                                  AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(FormatUtils.formatHeader("PERMISSION MATRIX"));

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system.\n");
            return sb.toString();
        }

        Set<String> allResources = new TreeSet<>();
        for (User user : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(user);
            perms.stream()
                    .map(Permission::resource)
                    .forEach(allResources::add);
        }

        if (allResources.isEmpty()) {
            sb.append("No permissions assigned to any user.\n");
            return sb.toString();
        }

        List<String> resources = new ArrayList<>(allResources);

        // Заголовки
        String[] headers = new String[resources.size() + 1];
        headers[0] = "Username";
        for (int i = 0; i < resources.size(); i++) {
            headers[i + 1] = resources.get(i);
        }

        // parallelStream - для параллельного построения строк
        List<String[]> rows = users.parallelStream()
                .map(user -> buildUserRow(user, resources, assignmentManager))
                .collect(Collectors.toList());

        sb.append(FormatUtils.formatTable(headers, rows));

        return sb.toString();
    }

    // Вспомогательный
    private static String[] buildUserRow(User user, List<String> resources, AssignmentManager assignmentManager) {
        String[] row = new String[resources.size() + 1];
        row[0] = user.username();

        Set<Permission> userPerms = assignmentManager.getUserPermissions(user);

        for (int i = 0; i < resources.size(); i++) {
            String resource = resources.get(i);
            Set<String> permsForResource = userPerms.stream()
                    .filter(p -> p.resource().equals(resource))
                    .map(Permission::name)
                    .collect(Collectors.toSet());

            if (permsForResource.isEmpty()) {
                row[i + 1] = "";
            } else {
                String abbr = permsForResource.stream()
                        .map(perm -> getPermissionAbbreviation(perm))
                        .sorted((a, b) -> {
                            int p1 = getPriority(a);
                            int p2 = getPriority(b);
                            if (p1 != p2) return Integer.compare(p1, p2);
                            return a.compareTo(b);
                        })
                        .collect(Collectors.joining());
                row[i + 1] = abbr;
            }
        }

        return row;
    }

//    public static String generatePermissionMatrix(UserManager userManager,
//                                                  AssignmentManager assignmentManager) {
//        StringBuilder sb = new StringBuilder();
//
//        sb.append(FormatUtils.formatHeader("PERMISSION MATRIX"));
//
//        List<User> users = userManager.findAll();
//
//        if (users.isEmpty()) {
//            sb.append("No users found in the system.\n");
//            return sb.toString();
//        }
//
//        // Все уникальные ресурсы
//        Set<String> allResources = new TreeSet<>();
//        for (User user : users) {
//            Set<Permission> perms = assignmentManager.getUserPermissions(user);
//            perms.stream()
//                    .map(Permission::resource)
//                    .forEach(allResources::add);
//        }
//
//        if (allResources.isEmpty()) {
//            sb.append("No permissions assigned to any user.\n");
//            return sb.toString();
//        }
//
//        List<String> resources = new ArrayList<>(allResources);
//
//        // Заголовки
//        String[] headers = new String[resources.size() + 1];
//        headers[0] = "Username";
//        for (int i = 0; i < resources.size(); i++) {
//            headers[i + 1] = resources.get(i);
//        }
//
//        // Строки данных
//        List<String[]> rows = new ArrayList<>();
//        for (User user : users) {
//            String[] row = new String[resources.size() + 1];
//            row[0] = user.username();
//
//            Set<Permission> userPerms = assignmentManager.getUserPermissions(user);
//
//            for (int i = 0; i < resources.size(); i++) {
//                String resource = resources.get(i);
//                Set<String> permsForResource = userPerms.stream()
//                        .filter(p -> p.resource().equals(resource))
//                        .map(Permission::name)
//                        .collect(Collectors.toSet());
//
//                if (permsForResource.isEmpty()) {
//                    row[i + 1] = "";
//                } else {
//                    String abbr = permsForResource.stream()
//                            .map(perm -> getPermissionAbbreviation(perm))
//                            .sorted((a, b) -> {
//                                int p1 = getPriority(a);
//                                int p2 = getPriority(b);
//                                if (p1 != p2) return Integer.compare(p1, p2);
//                                return a.compareTo(b);
//                            })
//                            .collect(Collectors.joining());
//                    row[i + 1] = abbr;
//                }
//            }
//            rows.add(row);
//        }
//
//        sb.append(FormatUtils.formatTable(headers, rows));
//
//        return sb.toString();
//    }

    private static String getPermissionAbbreviation(String permName) {
        switch (permName.toUpperCase()) {
            case "READ": return "R";
            case "WRITE": return "W";
            case "DELETE": return "D";
            default: return permName.substring(0, 1);
        }
    }

    private static int getPriority(String abbr) {
        switch (abbr) {
            case "R": return 1;
            case "W": return 2;
            case "D": return 3;
            default: return 4;
        }
    }

    public static void exportToFile(String report, String filename) {
        ValidationUtils.requireNonEmpty(filename, "Filename");

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(report);
            System.out.println("Report saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving report: " + e.getMessage());
        }
    }
}
