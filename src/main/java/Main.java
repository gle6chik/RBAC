import assignment.AssignmentMetadata;
import assignment.PermanentAssignment;
import core.*;
import filters.AssignmentFilters;
import filters.RoleFilters;
import filters.UserFilters;
import model.Permission;
import model.Role;
import model.User;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
//        RBACSystem system = new RBACSystem();
//        system.initialize();
//
//        CommandParser parser = new CommandParser();
//        CommandRegistry.registerAll(parser);
//
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.println("=== RBAC Management System ===");
//        System.out.println("Type 'help' to see available commands or 'exit' to quit.");
//        System.out.println("Current system user: " + system.getCurrentUser());
//
//        // Главный цикл
//        while (true) {
//            System.out.print("\n> ");
//            if (!scanner.hasNextLine()) break;
//
//            String input = scanner.nextLine().trim();
//
//            if (input.isEmpty()) continue;
//
//            parser.parseAndExecute(input, scanner, system);
//        }
//
//        scanner.close();

        load_test();
    }

    public static void load_test() {
        System.out.println("=== LOAD TEST START ===");

        RBACSystem system = new RBACSystem();
        system.initialize();

        // Очищаем перед тестом
        system.getUserManager().clear();
        system.getRoleManager().clear();
        system.getAssignmentManager().clear();

        ExecutorService testExecutor = Executors.newFixedThreadPool(10);

        for (int t = 0; t < 10; t++) {
            final int threadId = t;
            testExecutor.submit(() -> {
                try {
                    for (int i = 0; i < 20; i++) {
                        String username = "test_user_" + threadId + "_" + i;
                        String email = username + "@test.com";
                        User user = User.validate(username, "Test User " + i, email);
                        system.getUserManager().add(user);

                        Role role = new Role("ROLE_" + threadId + "_" + i, "Test role");
                        system.getRoleManager().add(role);

                        Permission perm = new Permission("READ", "test", "Test permission");
                        role.addPermission(perm);

                        // Назначаем только если ещё не назначена
                        if (!system.getAssignmentManager().userHasRole(user, role)) {
                            AssignmentMetadata meta = AssignmentMetadata.now("loadtest", "Load test assignment");
                            system.getAssignmentManager().add(new PermanentAssignment(user, role, meta));
                        }

                        system.getUserManager().findByFilter(UserFilters.byUsernameContains("test"));
                        system.getRoleManager().findByFilter(RoleFilters.hasAtLeastNPermissions(1));
                        system.getAssignmentManager().findByFilter(AssignmentFilters.activeOnly());
                        system.getAssignmentManager().userHasPermission(user, "READ", "test");

                        if (i % 5 == 0) {
                            System.out.println("Thread " + threadId + " completed " + i + " operations");
                        }
                    }
                    System.out.println("Thread " + threadId + " FINISHED successfully");
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " ERROR: " + e.getMessage());
                }
            });
        }

        // Пробуем создать двух одинаковых пользователей
//        try {
//            system.getUserManager().add(User.validate("duplicate", "Test", "dup@test.com"));
//            system.getUserManager().add(User.validate("duplicate", "Test", "dup@test.com"));
//            System.out.println("DUPLICATE CREATED - BUG!");
//        } catch (IllegalArgumentException e) {
//            System.out.println("Duplicate prevented correctly: " + e.getMessage());
//        }

        testExecutor.shutdown();
        try {
            testExecutor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n=== LOAD TEST RESULTS ===");
        System.out.println("Total users: " + system.getUserManager().count());
        System.out.println("Total roles: " + system.getRoleManager().count());
        System.out.println("Total assignments: " + system.getAssignmentManager().count());

        System.out.println("\n=== LOAD TEST COMPLETED ===");

        system.shutdown();
    }
}