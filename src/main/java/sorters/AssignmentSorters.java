package sorters;

import assignment.RoleAssignment;
import java.util.Comparator;

public class AssignmentSorters {
    public static Comparator<RoleAssignment> byUsername() {
        return (assignment1, assignment2) -> assignment1.user().username().compareTo(assignment2.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return (assignment1, assignment2) -> assignment1.role().getName().compareTo(assignment2.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return (assignment1, assignment2) -> assignment1.metadata().assignedAt().compareTo(assignment2.metadata().assignedAt());
    }
}
