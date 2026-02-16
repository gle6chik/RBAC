package model;

import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {
    // Валидация username
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    // Валидация email
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Валидация
    public static User validate(String username, String fullName, String email) {
        // Проверка на null и пустые строки
        if (username == null || username.trim().isEmpty())
            throw new IllegalArgumentException("Username cannot be empty.");
        if (fullName == null || fullName.trim().isEmpty())
            throw new IllegalArgumentException("Full name cannot be empty.");
        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("Email cannot be empty.");

        // Проверка username
        if (!USERNAME_PATTERN.matcher(username).matches())
            throw new IllegalArgumentException("Username must be 3-20 chars contain only letters, numbers, underscore.");

        // Проверка email
        if (!EMAIL_PATTERN.matcher(email).matches())
            throw new IllegalArgumentException("Invalid email format.");

        // Если исключений не произошло
        return new User(username.trim(), fullName.trim(), email.trim());
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}
