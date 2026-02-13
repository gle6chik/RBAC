import model.User;

public class Main {
    public static void main(String[] args) {
        // Пункт 1.4
        try {
            User normalUser = User.validate("Gleb", "Baranov", "glebbaranov21@gmail.com");
            System.out.println(normalUser.format());

            User userWithoutFullName = User.validate("Ivan", "", "ivan@gmail.com");
            System.out.println(userWithoutFullName.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        }

        try {
            User userWithInvalidEmail = User.validate("Ivan", "Ivanov", "ivangmail.com");
            System.out.println(userWithInvalidEmail.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        }

        try {
            User userWithInvalidUsername = User.validate("Ivan!", "Ivanov", "ivan@gmail.com");
            System.out.println(userWithInvalidUsername.format());
        } catch (IllegalArgumentException e) {
            System.out.println("Validation error: " + e.getMessage());
        }
    }
}
