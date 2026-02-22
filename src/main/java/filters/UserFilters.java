package filters;

import model.User;

public class UserFilters {
    public static UserFilter byUsername(String username) {
        return user -> user.username().equals(username);
    }

    public static UserFilter byUsernameContains(String substring) {
        String lower = substring.toLowerCase();
        return user -> user.username().toLowerCase().contains(lower);
    }

    public static UserFilter byEmail(String email) {
        return user -> user.email().equals(email);
    }

    public static UserFilter byEmailDomain(String domain) {
        return user -> user.email().endsWith(domain);
    }

    public static UserFilter byFullNameContains(String substring) {
        String lower = substring.toLowerCase();
        return user -> user.fullName().toLowerCase().contains(lower);
    }
}
