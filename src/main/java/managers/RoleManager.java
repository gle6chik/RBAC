package managers;

import model.Role;
import model.Permission;
import filters.RoleFilter;
import repositories.Repository;
import java.util.*;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {
    private Map<String, Role> rolesById = new HashMap<>();
    private Map<String, Role> rolesByName = new HashMap<>();

    // Repository methods
    @Override
    public void add(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null.");
        }
        if (rolesById.containsKey(role.getId())) {
            throw new IllegalArgumentException("Role with ID '" + role.getId() + "' already exists.");
        }
        if (rolesByName.containsKey(role.getName())) {
            throw new IllegalArgumentException("Role with name '" + role.getName() + "' already exists.");
        }

        rolesById.put(role.getId(), role);
        rolesByName.put(role.getName(), role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) return false;

        // TODO: Проверка, не назначена ли роль пользователям

        Role removed = rolesById.remove(role.getId());
        if (removed != null) {
            rolesByName.remove(role.getName());
            return true;
        }
        return false;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    // Additional methods
    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return rolesById.values().stream()
                .filter(role -> filter.test(role))
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return rolesById.values().stream()
                .filter(role -> filter.test(role))
                .sorted((r1, r2) -> sorter.compare(r1, r2))
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role '" + roleName + "' not found.");
        }
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role '" + roleName + "' not found.");
        }
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream()
                .filter(r -> r.hasPermission(permissionName, resource))
                .collect(Collectors.toList());
    }
}
