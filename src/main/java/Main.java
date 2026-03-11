import core.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== RBAC Management System ===");
        System.out.println("Type 'help' to see available commands or 'exit' to quit.");
        System.out.println("Current system user: " + system.getCurrentUser());

        // Главный цикл
        while (true) {
            System.out.print("\n> ");
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            parser.parseAndExecute(input, scanner, system);
        }

        scanner.close();
    }
}