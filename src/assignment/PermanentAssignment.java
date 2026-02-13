package assignment;

import model.Role;
import model.User;

public class PermanentAssignment extends AbstractRoleAssignment {
    private boolean revoked;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata); // Вызов конструктора родителя
        this.revoked = false;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public void revoke() {
        revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }
}
