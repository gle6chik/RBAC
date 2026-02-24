package filters;

import model.User;
import model.Role;
import assignment.TemporaryAssignment;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class AssignmentFilters {
    private static DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AssignmentFilter byUser(User user) {
        return a -> a.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return a -> a.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return a -> a.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return a -> a.role().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return a -> a.isActive();
    }

    public static AssignmentFilter inactiveOnly() {
        return a -> !a.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return a -> a.assignmentType().equals(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return a -> a.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        LocalDateTime after = LocalDateTime.parse(date, FORMATTER);
        return a -> {
            LocalDateTime assigned = LocalDateTime.parse(a.metadata().assignedAt(), FORMATTER);
            return assigned.isAfter(after);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        LocalDateTime before = LocalDateTime.parse(date, FORMATTER);
        return a -> {
            if (!(a instanceof TemporaryAssignment)) return false;
            TemporaryAssignment temp = (TemporaryAssignment) a;
            LocalDateTime expires = LocalDateTime.parse(temp.getExpiresAt(), FORMATTER);
            return expires.isBefore(before);
        };
    }
}
