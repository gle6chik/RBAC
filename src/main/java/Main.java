import core.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // 1. Инициализация системы (Менеджеры создаются внутри RBACSystem)
        RBACSystem system = new RBACSystem();
        system.initialize(); // Создает права, роли Admin/Manager/Viewer и админа

        // 2. Инициализация парсера и регистрация команд
        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        Scanner scanner = new Scanner(System.in);

        // 3. Приветствие
        System.out.println("=== RBAC Management System v1.0 ===");
        System.out.println("Type 'help' to see available commands or 'exit' to quit.");
        System.out.println("Current system user: " + system.getCurrentUser());

        // 4. Главный цикл программы
        while (true) {
            System.out.print("\n> ");
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            // Парсим ввод и выполняем команду
            // Метод parseAndExecute сам найдет команду и передаст ей scanner и system
            parser.parseAndExecute(input, scanner, system);
        }

        scanner.close();
    }
}