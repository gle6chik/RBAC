package utils;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {
    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            if (required && input.isEmpty()) {
                System.out.println("Error: This field cannot be empty. Please try again.");
                continue;
            }

            return input;
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);

                if (value < min || value > max) {
                    System.out.println(String.format(
                            "Error: Please enter a number between %d and %d.", min, max));
                    continue;
                }

                return value;

            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (yes/no): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("yes") || input.equals("y")) {
                return true;
            } else if (input.equals("no") || input.equals("n")) {
                return false;
            } else {
                System.out.println("Error: Please enter 'yes' or 'no'.");
            }
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options list cannot be empty");
        }

        System.out.println(message);

        // Вывод опций с номерами
        for (int i = 0; i < options.size(); i++) {
            T option = options.get(i);
            String optionStr = option.toString();

            // Если опция слишком длинная, обрезаем
            if (optionStr.length() > 50) {
                optionStr = optionStr.substring(0, 47) + "...";
            }

            System.out.printf("  %d. %s\n", i + 1, optionStr);
        }

        while (true) {
            System.out.print("Enter your choice (1-" + options.size() + "): ");
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);

                if (choice < 1 || choice > options.size()) {
                    System.out.println(String.format(
                            "Error: Please enter a number between 1 and %d.", options.size()));
                    continue;
                }

                return options.get(choice - 1);

            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number.");
            }
        }
    }

    public static void printSuccess(String message) {
        System.out.println("SUCCESS: " + message);
    }

    public static void printError(String message) {
        System.out.println("ERROR: " + message);
    }

    public static void printWarning(String message) {
        System.out.println("WARNING: " + message);
    }
}
