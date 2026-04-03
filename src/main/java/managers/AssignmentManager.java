package managers;

import assignment.*;
import model.User;
import model.Role;
import model.Permission;
import filters.AssignmentFilter;
import repositories.Repository;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class AssignmentManager implements Repository<RoleAssignment> {
    private Map<String, RoleAssignment> assignmentsById = new ConcurrentHashMap<>();

    private UserManager userManager;
    private RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    // Repository methods
    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null.");
        }

        // Проверка существования пользователя и роли
        if (!userManager.exists(assignment.user().username())) {
            throw new IllegalArgumentException("User '" + assignment.user().username() + "' does not exist.");
        }

        Optional<Role> roleOpt = roleManager.findByName(assignment.role().getName());
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role '" + assignment.role().getName() + "' does not exist.");
        }

        // Проверка дублирования активного назначения (синхронизировано)
        synchronized (this) {
            boolean alreadyAssigned = assignmentsById.values().stream()
                    .anyMatch(a -> a.user().equals(assignment.user()) &&
                            a.role().equals(assignment.role()) &&
                            a.isActive());

            if (alreadyAssigned) {
                throw new IllegalArgumentException("User already has active assignment for this role.");
            }

            assignmentsById.put(assignment.assignmentId(), assignment);
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) return false;
        return assignmentsById.remove(assignment.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignmentsById.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignmentsById.values());
    }

    @Override
    public int count() {
        return assignmentsById.size();
    }

    @Override
    public void clear() {
        assignmentsById.clear();
    }

    // Additional methods
    public List<RoleAssignment> findByUser(User user) {
        return assignmentsById.values().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        return assignmentsById.values().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return assignmentsById.values().stream()
                .filter(assignment -> filter.test(assignment))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        return assignmentsById.values().stream()
                .filter(assignment -> filter.test(assignment) )
                .sorted((a1, a2) -> sorter.compare(a1, a2))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignmentsById.values().stream()
                .filter(assignment -> assignment.isActive())
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignmentsById.values().stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        return assignmentsById.values().stream()
                .anyMatch(a -> a.user().equals(user) &&
                        a.role().equals(role) &&
                        a.isActive());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        return getUserPermissions(user).stream()
                .anyMatch(p -> p.name().equalsIgnoreCase(permissionName) &&
                        p.resource().equalsIgnoreCase(resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        return assignmentsById.values().stream()
                .filter(a -> a.user().equals(user) && a.isActive())
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = assignmentsById.get(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment not found.");
        }

        if (assignment instanceof PermanentAssignment) {
            ((PermanentAssignment) assignment).revoke();
        } else {
            throw new IllegalArgumentException("Cannot revoke non-permanent assignment.");
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = assignmentsById.get(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment not found.");
        }

        if (assignment instanceof TemporaryAssignment) {
            ((TemporaryAssignment) assignment).extend(newExpirationDate);
        } else {
            throw new IllegalArgumentException("Cannot extend non-temporary assignment.");
        }
    }

    //
    public boolean isRoleAssigned(Role role) {
        return assignmentsById.values().stream()
                .anyMatch(assignment -> assignment.role().equals(role));
    }
}
