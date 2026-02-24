package sorters;

import model.User;
import java.util.Comparator;

public class UserSorters {
    public static Comparator<User> byUsername() {
        return (user1, user2) -> user1.username().compareTo(user2.username());
    }

    public static Comparator<User> byFullName() {
        return (user1, user2) -> user1.fullName().compareTo(user2.fullName());
    }

    public static Comparator<User> byEmail() {
        return (user1, user2) -> user1.email().compareTo(user2.email());
    }
}
