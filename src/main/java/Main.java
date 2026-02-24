import model.*;
import assignment.*;
import filters.*;
import sorters.*;
import managers.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Main {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ RBAC");
        System.out.println("=========================================\n");

        // Создание менеджеров
        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager();
        AssignmentManager assignmentManager = new AssignmentManager(userManager, roleManager);

        // Связывание RoleManager с AssignmentManager
        roleManager.setAssignmentManager(assignmentManager);

        // 1. ТЕСТ СОЗДАНИЯ ПОЛЬЗОВАТЕЛЕЙ
        System.out.println("1. ТЕСТ СОЗДАНИЯ ПОЛЬЗОВАТЕЛЕЙ");
        System.out.println("----------------------------------------");
        testCreateUsers(userManager);

        // 2. ТЕСТ СОЗДАНИЯ ПРАВ
        System.out.println("\n2. ТЕСТ СОЗДАНИЯ ПРАВ (PERMISSIONS)");
        System.out.println("----------------------------------------");
        Permission[] permissions = testCreatePermissions();

        // 3. ТЕСТ СОЗДАНИЯ РОЛЕЙ
        System.out.println("\n3. ТЕСТ СОЗДАНИЯ РОЛЕЙ");
        System.out.println("----------------------------------------");
        Role[] roles = testCreateRoles(roleManager, permissions);

        // 4. ТЕСТ НАЗНАЧЕНИЙ
        System.out.println("\n4. ТЕСТ НАЗНАЧЕНИЙ (ASSIGNMENTS)");
        System.out.println("----------------------------------------");
        testAssignments(assignmentManager, userManager, roleManager);

        // 5. ТЕСТ ФИЛЬТРОВ
        System.out.println("\n5. ТЕСТ ФИЛЬТРОВ");
        System.out.println("----------------------------------------");
        testFilters(userManager, roleManager, assignmentManager);

        // 6. ТЕСТ СОРТИРОВКИ
        System.out.println("\n6. ТЕСТ СОРТИРОВКИ");
        System.out.println("----------------------------------------");
        testSorters(userManager, roleManager, assignmentManager);

        // 7. ТЕСТ ПОИСКА ПРАВ ПОЛЬЗОВАТЕЛЯ
        System.out.println("\n7. ТЕСТ ПОИСКА ПРАВ ПОЛЬЗОВАТЕЛЯ");
        System.out.println("----------------------------------------");
        testUserPermissions(assignmentManager, userManager);

        // 8. ТЕСТ УДАЛЕНИЯ РОЛИ
        System.out.println("\n8. ТЕСТ УДАЛЕНИЯ РОЛИ");
        System.out.println("----------------------------------------");
        testDeleteRole(roleManager, assignmentManager);

        // 9. ТЕСТ ОТЗЫВА И ПРОДЛЕНИЯ
        System.out.println("\n9. ТЕСТ ОТЗЫВА И ПРОДЛЕНИЯ");
        System.out.println("----------------------------------------");
        testRevokeAndExtend(assignmentManager);

        System.out.println("\n=========================================");
        System.out.println("ТЕСТИРОВАНИЕ ЗАВЕРШЕНО");
    }

    private static void testCreateUsers(UserManager userManager) {
        try {
            User admin = User.validate("admin", "Admin User", "admin@system.com");
            User john = User.validate("john", "John Doe", "john@gmail.com");
            User jane = User.validate("jane", "Jane Smith", "jane@company.com");
            User bob = User.validate("bob", "Bob Wilson", "bob@yahoo.com");

            userManager.add(admin);
            userManager.add(john);
            userManager.add(jane);
            userManager.add(bob);

            System.out.println("Создано пользователей: " + userManager.count());
            System.out.println("   Все пользователи:");
            userManager.findAll().forEach(u ->
                    System.out.println("   * " + u.format()));
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static Permission[] testCreatePermissions() {
        Permission[] perms = new Permission[6];

        try {
            perms[0] = new Permission("READ", "users", "Can read users");
            perms[1] = new Permission("WRITE", "users", "Can write users");
            perms[2] = new Permission("DELETE", "users", "Can delete users");
            perms[3] = new Permission("READ", "reports", "Can read reports");
            perms[4] = new Permission("WRITE", "reports", "Can write reports");
            perms[5] = new Permission("READ", "settings", "Can read settings");

            System.out.println("Создано прав:");
            for (Permission p : perms) {
                System.out.println("   * " + p.format());
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        return perms;
    }

    private static Role[] testCreateRoles(RoleManager roleManager, Permission[] perms) {
        Role[] roles = new Role[3];

        try {
            Role adminRole = new Role("Administrator", "Full system access");
            adminRole.addPermission(perms[0]); // READ users
            adminRole.addPermission(perms[1]); // WRITE users
            adminRole.addPermission(perms[2]); // DELETE users
            adminRole.addPermission(perms[3]); // READ reports
            roleManager.add(adminRole);
            roles[0] = adminRole;

            Role editorRole = new Role("Editor", "Can edit users");
            editorRole.addPermission(perms[0]); // READ users
            editorRole.addPermission(perms[1]); // WRITE users
            roleManager.add(editorRole);
            roles[1] = editorRole;

            Role viewerRole = new Role("Viewer", "Can view only");
            viewerRole.addPermission(perms[0]); // READ users
            viewerRole.addPermission(perms[3]); // READ reports
            roleManager.add(viewerRole);
            roles[2] = viewerRole;

            System.out.println("Создано ролей: " + roleManager.count());
            System.out.println(adminRole.format());
            System.out.println(editorRole.format());
            System.out.println(viewerRole.format());
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        return roles;
    }

    private static void testAssignments(AssignmentManager am, UserManager um, RoleManager rm) {
        try {
            User admin = um.findByUsername("admin").orElse(null);
            User john = um.findByUsername("john").orElse(null);
            User jane = um.findByUsername("jane").orElse(null);

            Role adminRole = rm.findByName("Administrator").orElse(null);
            Role editorRole = rm.findByName("Editor").orElse(null);
            Role viewerRole = rm.findByName("Viewer").orElse(null);

            // Постоянное назначение
            AssignmentMetadata meta1 = AssignmentMetadata.now(admin.username(), "Permanent assignment");
            PermanentAssignment permAssign = new PermanentAssignment(john, viewerRole, meta1);
            am.add(permAssign);
            System.out.println("Постоянное назначение:");
            System.out.println("   " + permAssign.summary());

            // Временное назначение
            String expiresAt = LocalDateTime.now().plusDays(7).format(FORMATTER);
            AssignmentMetadata meta2 = AssignmentMetadata.now(admin.username(), "Temporary assignment");
            TemporaryAssignment tempAssign = new TemporaryAssignment(jane, editorRole, meta2, expiresAt, true);
            am.add(tempAssign);
            System.out.println("\nВременное назначение:");
            System.out.println("   " + tempAssign.summary());

            System.out.println("\nВсего назначений: " + am.count());

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void testFilters(UserManager um, RoleManager rm, AssignmentManager am) {
        // Фильтр пользователей по домену
        System.out.println("Фильтр пользователей (email домен @company.com):");
        List<User> companyUsers = um.findByFilter(
                UserFilters.byEmailDomain("@company.com")
        );
        companyUsers.forEach(u -> System.out.println("   * " + u.format()));

        // Фильтр ролей с минимум 2 правами
        System.out.println("\nФильтр ролей (минимум 2 права):");
        List<Role> rolesWithPerms = rm.findByFilter(
                RoleFilters.hasAtLeastNPermissions(2)
        );
        rolesWithPerms.forEach(r -> System.out.println("   * " + r.getName() +
                " (" + r.getPermissions().size() + " прав)"));

        // Фильтр назначений (активные)
        System.out.println("\nФильтр назначений (активные):");
        List<RoleAssignment> active = am.findByFilter(
                AssignmentFilters.activeOnly()
        );
        active.forEach(a -> System.out.println("   * " + a.user().username() +
                " -> " + a.role().getName()));
    }

    private static void testSorters(UserManager um, RoleManager rm, AssignmentManager am) {
        // Сортировка пользователей по имени
        System.out.println("Пользователи (сортировка по полному имени):");
        um.findAll().stream()
                .sorted(UserSorters.byFullName())
                .forEach(u -> System.out.println("   * " + u.fullName()));

        // Сортировка ролей по количеству прав
        System.out.println("\nРоли (сортировка по количеству прав):");
        rm.findAll().stream()
                .sorted(RoleSorters.byPermissionCount())
                .forEach(r -> System.out.println("   * " + r.getName() +
                        " (" + r.getPermissions().size() + " прав)"));

        // Сортировка назначений по дате
        System.out.println("\nНазначения (сортировка по дате):");
        am.findAll().stream()
                .sorted(AssignmentSorters.byAssignmentDate())
                .forEach(a -> System.out.println("   * " + a.user().username() +
                        " -> " + a.role().getName() + " (" + a.metadata().assignedAt() + ")"));
    }

    private static void testUserPermissions(AssignmentManager am, UserManager um) {
        User john = um.findByUsername("john").orElse(null);

        if (john != null) {
            System.out.println("Права пользователя " + john.username() + ":");
            am.getUserPermissions(john).forEach(p ->
                    System.out.println("   * " + p.format()));

            System.out.println("\nПроверка конкретных прав:");
            System.out.println("   * Имеет право READ на users? " +
                    am.userHasPermission(john, "READ", "users"));
            System.out.println("   * Имеет право WRITE на users? " +
                    am.userHasPermission(john, "WRITE", "users"));
        }
    }

    private static void testDeleteRole(RoleManager rm, AssignmentManager am) {
        try {
            Role viewer = rm.findByName("Viewer").orElse(null);

            if (viewer != null) {
                System.out.println("Попытка удалить роль '" + viewer.getName() + "'...");
                rm.remove(viewer);
            }
        } catch (IllegalStateException e) {
            System.out.println("   Нельзя удалить: " + e.getMessage());
        }
    }

    private static void testRevokeAndExtend(AssignmentManager am) {
        try {
            // Нахождение постоянное назначение
            List<RoleAssignment> permAssignments = am.findByFilter(
                    a -> a instanceof PermanentAssignment
            );

            if (!permAssignments.isEmpty()) {
                RoleAssignment perm = permAssignments.get(0);
                System.out.println("Постоянное назначение до отзыва:");
                System.out.println("   Активно? " + perm.isActive());

                am.revokeAssignment(perm.assignmentId());
                System.out.println("После отзыва:");
                System.out.println("   Активно? " + perm.isActive());
            }

            // Находим временное назначение
            List<RoleAssignment> tempAssignments = am.findByFilter(
                    a -> a instanceof TemporaryAssignment
            );

            if (!tempAssignments.isEmpty()) {
                TemporaryAssignment temp = (TemporaryAssignment) tempAssignments.get(0);
                System.out.println("\nВременное назначение:");
                System.out.println("   Истекает: " + temp.getExpiresAt());
                System.out.println("   Осталось: " + temp.getTimeRemaining());

                String newExpires = LocalDateTime.now().plusDays(14).format(FORMATTER);
                am.extendTemporaryAssignment(temp.assignmentId(), newExpires);
                System.out.println("После продления:");
                System.out.println("   Новая дата: " + temp.getExpiresAt());
                System.out.println("   Осталось: " + temp.getTimeRemaining());
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}