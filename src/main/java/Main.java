import model.*;
import assignment.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    private static DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ НАЧАТО.");

        testUser();
        testPermission();
        testRole();
        testAssignmentMetadata();
        testAssignments();
        testSearchAndFilter();

        System.out.println("ТЕСТИРОВАНИЕ ЗАВЕРШЕНО.");
    }

    private static void testUser() {
        System.out.println("<Тестирование User>");

        try {
            System.out.println("Корректные данные:");
            User user1 = User.validate("gle6chik", "Gleb Baranov", "glebbaranov21@gmail.com");
            System.out.println(user1.format());

            System.out.println("\nНекорректный username:");
            User user2 = User.validate("!gle6chik", "Gleb Baranov", "glebbaranov21@gmail.com");
            System.out.println(user2.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }

        try {
            System.out.println("\nНекорректный fullName:");
            User user3 = User.validate("glebchik", "", "glebbaranov21gmail.com");
            System.out.println(user3.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }

        try {
            System.out.println("\nНекорректный email:");
            User user4 = User.validate("glebchik", "Gleb Baranov", "glebbaranov21gmail.com");
            System.out.println(user4.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }
    }

    private static void testPermission() {
        System.out.println("\n\n<Тестирование Permission>");

        // Создание прав
        Permission p1 = new Permission("read", "users", "Can read users");
        Permission p2 = new Permission("WRITE", "REPORTS", "Can write reports");
        Permission p3 = new Permission("delete", "Settings", "Can delete settings");

        // Вывод прав
        System.out.println(p1.format());
        System.out.println(p2.format());
        System.out.println(p3.format());

        try {
            System.out.println("\nНекорректный name:");
            Permission p = new Permission("READ USERS", "users", "Test");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }
    }

    private static void testRole() {
        System.out.println("\n\n<Тестирование Role>");

        // Создание прав
        Permission readUsers = new Permission("READ", "users", "Can view users");
        Permission writeUsers = new Permission("WRITE", "users", "Can edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");
        Permission readReports = new Permission("READ", "reports", "Can view reports");

        // Создание ролей
        Role adminRole = new Role("Administrator", "Full system access");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readReports);

        Role viewerRole = new Role("Viewer", "Can only view");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readReports);

        // Вывод ролей
        System.out.println(adminRole.format());
        System.out.println(viewerRole.format());

        // Тестирование hasPermission
        System.out.println("    Проверка прав:");
        System.out.println("    Admin has READ on users? " +
                adminRole.hasPermission("READ", "users"));
        System.out.println("    Viewer has DELETE on users? " +
                viewerRole.hasPermission("DELETE", "users"));

        // Тестирование equals/hashCode
        Role anotherAdmin = new Role("Administrator", "Another admin");
        System.out.println("    adminRole.equals(anotherAdmin)? " +
                adminRole.equals(anotherAdmin));
        System.out.println("    adminRole.equals(adminRole)? " +
                adminRole.equals(adminRole));
    }

    private static void testAssignmentMetadata() {
        System.out.println("\n\n<Тестирование AssignmentMetadata>");

        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Initial setup");
        AssignmentMetadata meta2 = AssignmentMetadata.now("manager", null);

        System.out.println(meta1.format());
        System.out.println(meta2.format());
    }

    private static void testAssignments() {
        System.out.println("\n\n<Тестирование Assignments>");

        User admin = null;
        User user1 = null;
        User user2 = null;
        try {
            admin = User.validate("admin", "Admin", "admin@test.com");
            user1 = User.validate("john", "John", "john@test.com");
            user2 = User.validate("jane", "Jane", "jane@test.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка валидации: " + e.getMessage());
        }

        Role viewerRole = new Role("Viewer", "Can view");
        viewerRole.addPermission(new Permission("READ", "users", "Read users"));

        Role editorRole = new Role("Editor", "Can edit");
        editorRole.addPermission(new Permission("READ", "users", "Read users"));
        editorRole.addPermission(new Permission("WRITE", "users", "Write users"));

        // Постоянное назначение
        System.out.println("\nПостоянное назначение:");
        AssignmentMetadata permMeta = AssignmentMetadata.now(admin.username(), "Permanent access");
        PermanentAssignment permAssign = new PermanentAssignment(user1, viewerRole, permMeta);

        System.out.println(permAssign.summary());
        System.out.println("  Активно? " + permAssign.isActive());

        // Отзыв
        permAssign.revoke();
        System.out.println("  После revoke():");
        System.out.println("  Активно? " + permAssign.isActive());
        System.out.println("  Отозвано? " + permAssign.isRevoked());

        // Временное назначение
        System.out.println("\nВременное назначение:");
        String expiresAt = LocalDateTime.now().plusDays(5).format(FORMATTER);
        AssignmentMetadata tempMeta = AssignmentMetadata.now(admin.username(), "Temporary access");
        TemporaryAssignment tempAssign = new TemporaryAssignment(
                user2, editorRole, tempMeta, expiresAt, true
        );

        System.out.println(tempAssign.summary());
        System.out.println("  Активно? " + tempAssign.isActive());
        System.out.println("  Осталось: " + tempAssign.getTimeRemaining());

        // Продление
        String newExpires = LocalDateTime.now().plusDays(10).format(FORMATTER);
        tempAssign.extend(newExpires);
        System.out.println("  После продления:");
        System.out.println("  Новая дата: " + tempAssign.getExpiresAt());
        System.out.println("  Осталось: " + tempAssign.getTimeRemaining());
    }

    private static void testSearchAndFilter() {
        System.out.println("\n\n<Тестирование поиска и фильтрации>");

        // Набор прав
        Permission[] perms = {
                new Permission("READ", "users", "Read users"),
                new Permission("WRITE", "users", "Write users"),
                new Permission("DELETE", "users", "Delete users"),
                new Permission("READ", "reports", "Read reports"),
                new Permission("WRITE", "reports", "Write reports"),
                new Permission("READ", "settings", "Read settings")
        };

        System.out.println("  Все права:");
        for (Permission p : perms) {
            System.out.println("    " + p.format());
        }

        // Поиск по шаблону
        System.out.println("\n  Поиск 'READ' на любом ресурсе:");
        for (Permission p : perms) {
            if (p.matches("READ", null)) {
                System.out.println("    " + p.format());
            }
        }

        System.out.println("\n  Поиск любых прав на 'users':");
        for (Permission p : perms) {
            if (p.matches(null, "users")) {
                System.out.println("    " + p.format());
            }
        }

        System.out.println("\n  Поиск 'READ' на 'users':");
        for (Permission p : perms) {
            if (p.matches("READ", "users")) {
                System.out.println("    " + p.format());
            }
        }
    }
}
