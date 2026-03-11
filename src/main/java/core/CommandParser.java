package core;

import java.awt.*;
import java.util.*;

public class CommandParser {
    private Map<String, Command> commands = new LinkedHashMap<>();
    private Map<String, String> commandDescriptions = new LinkedHashMap<>();

    public void registerCommand(String name, String description, Command command) {
        String key = name.toLowerCase();
        commands.put(key, command);
        commandDescriptions.put(key, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command cmd = commands.get(commandName.toLowerCase());
        if (cmd != null) {
            try {
                cmd.execute(scanner, system);
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
        } else {
            System.out.println("Unknown command: '" + commandName + "'. Type 'help' fro available commands.");
        }
    }

    public void printHelp() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("AVAILABLE COMMANDS");
        System.out.println("=".repeat(50));

        commandDescriptions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %-20s - %s\n", e.getKey(), e.getValue()));

        System.out.println("=".repeat(50) + "\n");
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.trim().isEmpty()) return;

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();

        executeCommand(commandName, scanner, system);
    }
}
