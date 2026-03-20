package model;

import utils.ValidationUtils;

public record Permission(String name, String resource, String description) {
    public Permission {
        // Валидация
        ValidationUtils.requireNonEmpty(name, "Permission name");
        ValidationUtils.requireNonEmpty(resource, "Resource");
        ValidationUtils.requireNonEmpty(description, "Description");

        if (name.contains(" "))
            throw new IllegalArgumentException("Permission name cannot contain spaces.");

        // Нормализация
        name = name.toUpperCase().trim();
        resource = resource.toLowerCase().trim();
        description = description.trim();
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches =
                namePattern == null
                || namePattern.isEmpty()
                || name.contains(namePattern)
                || name.matches(namePattern);

        boolean resourceMatches =
                resourcePattern == null
                || resourcePattern.isEmpty()
                || resource.contains(resourcePattern)
                || resource.matches(resourcePattern);

        return nameMatches && resourceMatches;
    }
}
