package me.ghosthacks96.pos.server.utils.managers;

import me.ghosthacks96.pos.server.utils.models.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final Map<String, User> users;
    private final Map<String, User> usersByUsername;
    private final PermissionManager permissionManager;

    public UserManager(PermissionManager permissionManager) {
        this.users = new ConcurrentHashMap<>();
        this.usersByUsername = new ConcurrentHashMap<>();
        this.permissionManager = permissionManager;
    }

    public boolean addUser(User user) {
        if (user == null) return false;

        // Set permission manager for the user
        user.setPermissionManager(permissionManager);

        // Check for duplicate username
        if (usersByUsername.containsKey(user.getUsername())) {
            return false; // Username already exists
        }

        users.put(user.getUserId(), user);
        usersByUsername.put(user.getUsername(), user);
        return true;
    }

    public User getUserById(String userId) {
        if (userId == null) return null;
        return users.get(userId.trim());
    }

    public User getUserByUsername(String username) {
        if (username == null) return null;
        return usersByUsername.get(username.toLowerCase().trim());
    }

    public boolean removeUser(String userId) {
        User user = users.remove(userId);
        if (user != null) {
            usersByUsername.remove(user.getUsername());
            return true;
        }
        return false;
    }

    public Collection<User> getAllUsers() {
        return Collections.unmodifiableCollection(users.values());
    }

    public Collection<User> getActiveUsers() {
        return users.values().stream()
                .filter(User::isActive)
                .toList();
    }

    public Collection<User> getUsersInGroup(String groupName) {
        if (groupName == null) return Collections.emptyList();

        return users.values().stream()
                .filter(user -> user.isInGroup(groupName))
                .toList();
    }

    public Collection<User> getUsersWithPermission(String permission) {
        if (permission == null) return Collections.emptyList();

        return users.values().stream()
                .filter(user -> user.hasPermission(permission))
                .toList();
    }

    // Authentication helper methods
    public User authenticate(String username, String passwordHash) {
        User user = getUserByUsername(username);
        if (user == null || !user.canLogin()) {
            return null;
        }

        if (user.getPasswordHash() != null && user.getPasswordHash().equals(passwordHash)) {
            user.recordSuccessfulLogin();
            return user;
        } else {
            user.recordFailedLogin();
            return null;
        }
    }

    public boolean changePassword(String userId, String oldPasswordHash, String newPasswordHash) {
        User user = getUserById(userId);
        if (user == null) return false;

        if (user.getPasswordHash() != null && user.getPasswordHash().equals(oldPasswordHash)) {
            user.setPasswordHash(newPasswordHash);
            return true;
        }
        return false;
    }

    public void unlockUser(String userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setLocked(false);
        }
    }

    public int getTotalUserCount() {
        return users.size();
    }

    public int getActiveUserCount() {
        return (int) users.values().stream().filter(User::isActive).count();
    }

    public int getLockedUserCount() {
        return (int) users.values().stream().filter(User::isLocked).count();
    }
}