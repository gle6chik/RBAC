package assignment;

import model.Role;
import model.User;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private String assignmentId;
    private User user;
    Role role;
    AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        this.assignmentId = UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        AbstractRoleAssignment abstractRoleAssignment = (AbstractRoleAssignment)o;
        return Objects.equals(assignmentId, abstractRoleAssignment.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(assignmentId);
    }

    public String summary() {
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        String type = assignmentType();
        String reason = metadata.reason() != null ? metadata.reason() : "No reason";

        return String.format("[%s] %s assigned to %s by %s at %s\nReason: %s\nStatus: %s",
                type,
                role.getName(),
                user.username(),
                metadata.assignedBy(),
                metadata.assignedAt(),
                reason,
                status
        );
    }
}
