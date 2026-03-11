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
    private static String SEPARATOR = "=".repeat(80);
    private static String LINE = "-".repeat(80);

    public static String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(SEPARATOR).append("\n");
        sb.append("USER REPORT\n");
        sb.append(SEPARATOR).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system.\n");
            return sb.toString();
        }

        sb.append(String.format("Total users: %d\n\n", users.size()));

        for (User user : users) {
            sb.append(formatUserSection(user, assignmentManager));
            sb.append(LINE).append("\n");
        }

        sb.append(SEPARATOR).append("\n");
        sb.append("END OF USER REPORT\n");
        sb.append(SEPARATOR).append("\n");

        return sb.toString();
    }

    private static String formatUserSection(User user, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        // Информация о пользователе
        sb.append(String.format("USER: %s\n", user.username()));
        sb.append(String.format("Full Name: %s\n", user.fullName()));
        sb.append(String.format("Email: %s\n", user.email()));

        // Получаем назначения пользователя
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

        sb.append("\n");
        return sb.toString();
    }

    public static String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(SEPARATOR).append("\n");
        sb.append("ROLE REPORT\n");
        sb.append(SEPARATOR).append("\n\n");

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

        // Сортируем роли по количеству пользователей
        List<Role> sortedRoles = roles.stream()
                .sorted((r1, r2) -> Long.compare(
                        userCountByRole.get(r2), userCountByRole.get(r1)))
                .collect(Collectors.toList());

        for (Role role : sortedRoles) {
            sb.append(formatRoleSection(role,
                    userCountByRole.get(role),
                    activeUserCountByRole.get(role),
                    assignmentManager));
            sb.append(LINE).append("\n");
        }

        sb.append(SEPARATOR).append("\n");
        sb.append("END OF ROLE REPORT\n");
        sb.append(SEPARATOR).append("\n");

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

            // Группируем по статусу
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

        sb.append("\n");
        return sb.toString();
    }

    public static String generatePermissionMatrix(UserManager userManager,
                                                  AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(SEPARATOR).append("\n");
        sb.append("PERMISSION MATRIX\n");
        sb.append(SEPARATOR).append("\n\n");

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users found in the system.\n");
            return sb.toString();
        }

        // Собираем все уникальные ресурсы
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

        // Заголовок таблицы
        sb.append(formatMatrixHeader(resources));

        // Строки для каждого пользователя
        for (User user : users) {
            sb.append(formatMatrixRow(user, resources, assignmentManager));
        }

        sb.append(formatMatrixFooter(resources));

        // Легенда
        sb.append("\nLEGEND:\n");
        sb.append("  R - READ\n");
        sb.append("  W - WRITE\n");
        sb.append("  D - DELETE\n");
        sb.append("  * - multiple permissions\n");

        sb.append(SEPARATOR).append("\n");

        return sb.toString();
    }

    private static String formatMatrixHeader(List<String> resources) {
        StringBuilder sb = new StringBuilder();

        // Верхняя граница
        sb.append("+");
        sb.append("-".repeat(20)); // для колонки пользователя
        sb.append("+");

        for (String resource : resources) {
            sb.append("-".repeat(15));
            sb.append("+");
        }
        sb.append("\n");

        // Заголовки
        sb.append(String.format("| %-18s |", "Username"));
        for (String resource : resources) {
            String shortResource = resource.length() > 13
                    ? resource.substring(0, 10) + "..."
                    : resource;
            sb.append(String.format(" %-13s |", shortResource));
        }
        sb.append("\n");

        // Разделитель
        sb.append("+");
        sb.append("-".repeat(20));
        sb.append("+");
        for (String resource : resources) {
            sb.append("-".repeat(15));
            sb.append("+");
        }
        sb.append("\n");

        return sb.toString();
    }

    private static String formatMatrixRow(User user, List<String> resources,
                                          AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("| %-18s |", user.username()));

        Set<Permission> userPerms = assignmentManager.getUserPermissions(user);

        for (String resource : resources) {
            // Получаем все права пользователя на этот ресурс
            Set<String> permsForResource = userPerms.stream()
                    .filter(p -> p.resource().equals(resource))
                    .map(Permission::name)
                    .collect(Collectors.toSet());

            String cell;
            if (permsForResource.isEmpty()) {
                cell = " ".repeat(13);
            } else if (permsForResource.size() == 1) {
                String perm = permsForResource.iterator().next();
                cell = String.format(" %-12s ", perm.substring(0, Math.min(perm.length(), 12)));
            } else {
                cell = String.format(" %-12s ", "*" + permsForResource.size());
            }

            sb.append(cell).append("|");
        }

        sb.append("\n");
        return sb.toString();
    }

    private static String formatMatrixFooter(List<String> resources) {
        StringBuilder sb = new StringBuilder();

        sb.append("+");
        sb.append("-".repeat(20));
        sb.append("+");

        for (String resource : resources) {
            sb.append("-".repeat(15));
            sb.append("+");
        }
        sb.append("\n");

        return sb.toString();
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
