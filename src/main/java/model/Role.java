package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;

public class Role {
    private String id;
    private String name;
    private String description;
    private Set<Permission> permissions;

    // Генератор id
    private static int counter;

    public Role(String name, String description) {
        this.id = "role_" + UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        return permissions.stream()
                .anyMatch(p -> p.name().equalsIgnoreCase(permissionName) &&
                        p.resource().equalsIgnoreCase(resource));
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Если это ровно тот же самый объект (в памяти)
        if (o == null || this.getClass() != o.getClass()) return false; // Если объект null или другого класса
        Role role = (Role)o; // Приведение типа Object к типу Role
        return Objects.equals(id, role.id); // Сравнение по id
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Role %s [ID: %s]", name, id);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Role: %s [ID: %s]\n", name, id));
        sb.append(String.format("Description: %s\n", description));
        sb.append(String.format("Permissions (%d):\n", permissions.size()));

        for (Permission p : permissions)
            sb.append(String.format("    - %s\n", p.format()));

        return sb.toString();
    }
}
