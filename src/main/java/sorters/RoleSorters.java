package sorters;

import model.Role;
import java.util.Comparator;

public class RoleSorters {
    public static Comparator<Role> byName() {
        return (role1, role2) -> role1.getName().compareTo(role2.getName());
    }

    public static Comparator<Role> byPermissionCount() {
        return (role1, role2) -> Integer.compare(
                role1.getPermissions().size(),
                role2.getPermissions().size()
        );
    }
}
