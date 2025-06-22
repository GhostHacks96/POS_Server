package me.ghosthacks96.pos.server.utils.models;

import me.ghosthacks96.pos.server.utils.managers.PermissionManager;
import me.ghosthacks96.pos.server.utils.perms.PermGroup;
import me.ghosthacks96.pos.server.utils.perms.Permission;

import java.time.LocalDateTime;
import java.util.*;

public class User {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private boolean isActive;
    private boolean isLocked;
    private LocalDateTime createdDate;
    private LocalDateTime lastLoginDate;
    private LocalDateTime lastPasswordChange;
    private int failedLoginAttempts;

    // Permission-related fields
    private final Set<String> groupNames;
    private final Set<Permission> directPermissions;
    private PermissionManager permissionManager;

    public User(String userId, String username, String firstName, String lastName, String email) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        this.userId = userId.trim();
        this.username = username.toLowerCase().trim();
        this.firstName = firstName != null ? firstName.trim() : "";
        this.lastName = lastName != null ? lastName.trim() : "";
        this.email = email != null ? email.toLowerCase().trim() : "";
        this.isActive = true;
        this.isLocked = false;
        this.createdDate = LocalDateTime.now();
        this.failedLoginAttempts = 0;

        this.groupNames = new HashSet<>();
        this.directPermissions = new HashSet<>();
    }

    // Basic getters and setters
    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        this.username = username.toLowerCase().trim();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName.trim() : "";
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName.trim() : "";
    }

    public String getFullName() {
        if (firstName.isEmpty() && lastName.isEmpty()) {
            return username;
        }
        return (firstName + " " + lastName).trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : "";
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.lastPasswordChange = LocalDateTime.now();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
        if (!locked) {
            this.failedLoginAttempts = 0;
        }
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public LocalDateTime getLastPasswordChange() {
        return lastPasswordChange;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    // Permission Manager
    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    // Group management
    public boolean addToGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }

        // Verify group exists if permission manager is set
        if (permissionManager != null) {
            PermGroup group = permissionManager.getGroup(groupName);
            if (group == null) {
                throw new IllegalArgumentException("Group '" + groupName + "' does not exist");
            }
        }

        return groupNames.add(groupName.trim());
    }

    public boolean removeFromGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return false;
        }
        return groupNames.remove(groupName.trim());
    }

    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groupNames);
    }

    public boolean isInGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return false;
        }
        return groupNames.contains(groupName.trim());
    }

    // Direct permission management
    public boolean addDirectPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        return directPermissions.add(permission);
    }

    public boolean removeDirectPermission(Permission permission) {
        return directPermissions.remove(permission);
    }

    public boolean removeDirectPermission(String permissionName) {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            return false;
        }

        String normalized = permissionName.toLowerCase().trim();
        return directPermissions.removeIf(perm -> perm.getPermission().equals(normalized));
    }

    public Set<Permission> getDirectPermissions() {
        return Collections.unmodifiableSet(directPermissions);
    }

    // Permission checking methods
    public boolean hasPermission(String permissionName) {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            return false;
        }

        // Check if user is active and not locked
        if (!isActive || isLocked) {
            return false;
        }

        // Check direct permissions first
        if (hasDirectPermission(permissionName)) {
            return true;
        }

        // Check group permissions
        if (permissionManager != null) {
            for (String groupName : groupNames) {
                if (permissionManager.hasPermission(groupName, permissionName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasDirectPermission(String permissionName) {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            return false;
        }

        return directPermissions.stream()
                .anyMatch(perm -> perm.matches(permissionName));
    }

    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    // Get all effective permissions (direct + inherited from groups)
    public Set<Permission> getAllEffectivePermissions() {
        Set<Permission> allPermissions = new HashSet<>(directPermissions);

        if (permissionManager != null) {
            for (String groupName : groupNames) {
                allPermissions.addAll(permissionManager.getAllEffectivePermissions(groupName));
            }
        }

        return allPermissions;
    }

    // Utility methods
    public boolean canLogin() {
        return isActive && !isLocked;
    }

    public void recordSuccessfulLogin() {
        this.lastLoginDate = LocalDateTime.now();
        this.failedLoginAttempts = 0;
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        // Auto-lock after 5 failed attempts (configurable)
        if (this.failedLoginAttempts >= 5) {
            this.isLocked = true;
        }
    }

    public boolean isPasswordExpired(int maxDaysValid) {
        if (lastPasswordChange == null) {
            return true; // Never set password
        }
        return lastPasswordChange.plusDays(maxDaysValid).isBefore(LocalDateTime.now());
    }

    public int getGroupCount() {
        return groupNames.size();
    }

    public int getDirectPermissionCount() {
        return directPermissions.size();
    }

    public int getTotalPermissionCount() {
        return getAllEffectivePermissions().size();
    }

    // Permission checking with specific contexts (useful for POS operations)
    public boolean canPerformPOSOperation(String operation) {
        return hasPermission("pos." + operation) || hasPermission("pos.admin");
    }

    public boolean canAccessCashier() {
        return hasPermission("pos.cashier") || hasPermission("pos.admin");
    }

    public boolean canProcessRefunds() {
        return hasPermission("pos.refund") || hasPermission("pos.admin");
    }

    public boolean canManageInventory() {
        return hasPermission("pos.inventory") || hasPermission("pos.admin");
    }

    public boolean canViewReports() {
        return hasPermission("pos.reports") || hasPermission("pos.admin");
    }

    public boolean canManageUsers() {
        return hasPermission("system.users") || hasPermission("system.admin");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                ", isLocked=" + isLocked +
                ", groups=" + groupNames.size() +
                ", directPermissions=" + directPermissions.size() +
                ", createdDate=" + createdDate +
                '}';
    }
}
