package model;

import utils.ValidationUtils;
import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {

    public static User validate(String username, String fullName, String email) {
        ValidationUtils.requireNonEmpty(username, "Username");
        ValidationUtils.requireNonEmpty(fullName, "Full name");
        ValidationUtils.requireNonEmpty(email, "Email");

        if (!ValidationUtils.isValidUsername(username)) {
            throw new IllegalArgumentException(
                    "Username must be 3-20 chars and contain only letters, numbers, underscore."
            );
        }

        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        String normalizedUsername = ValidationUtils.normalizeUsername(username);
        String normalizedFullName = ValidationUtils.normalizeString(fullName);
        String normalizedEmail = ValidationUtils.normalizeEmail(email);

        return new User(normalizedUsername, normalizedFullName, normalizedEmail);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}