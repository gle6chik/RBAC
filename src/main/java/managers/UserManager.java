package managers;

import model.User;
import filters.UserFilter;
import repositories.Repository;
import java.util.*;
import java.util.stream.Collectors;


public class UserManager implements Repository<User> {
    private Map<String, User> usersByUsername = new HashMap<>();

    // Repository methods
    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (usersByUsername.containsKey(user.username())) {
            throw new IllegalArgumentException("User with username '" + user.username() + "' already exists.");
        }
        usersByUsername.put(user.username(), user);
    }

    @Override
    public boolean remove(User user) {
        if (user == null) return false;
        return usersByUsername.remove(user.username()) != null;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersByUsername.get(id)); // username - идентификатор
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(usersByUsername.values());
    }

    @Override
    public int count() {
        return usersByUsername.size();
    }

    @Override
    public void clear() {
        usersByUsername.clear();
    }

    // Additional methods
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    public Optional<User> findByEmail(String email) {
        return usersByUsername.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        return usersByUsername.values().stream()
                .filter(user -> filter.test(user))
                .collect(Collectors.toList());
    }

    public List<User> fingAll(UserFilter filter, Comparator<User> sorter) {
        return usersByUsername.values().stream()
                .filter(user -> filter.test(user))
                .sorted((u1, u2) -> sorter.compare(u1, u2))
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        return usersByUsername.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        User existing = usersByUsername.get(username);
        if (existing == null) {
            throw new IllegalArgumentException("User '" + username + "' not found.");
        }

        User updated = User.validate(username, newFullName, newEmail);
        usersByUsername.put(username, updated);
    }
}
