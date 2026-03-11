package filters;

import model.User;
import model.Role;
import utils.DateUtils;
import assignment.TemporaryAssignment;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class AssignmentFilters {
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
        return a -> DateUtils.isAfter(a.metadata().assignedAt(), date);
    }

    public static AssignmentFilter expiringBefore(String date) {
        return a -> {
            if (!(a instanceof TemporaryAssignment)) return false;
            TemporaryAssignment temp = (TemporaryAssignment) a;
            return DateUtils.isBefore(temp.getExpiresAt(), date);
        };
    }
}
