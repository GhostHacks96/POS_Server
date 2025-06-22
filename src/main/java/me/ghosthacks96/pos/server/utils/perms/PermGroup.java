package me.ghosthacks96.pos.server.utils.perms;

import java.util.*;

public class PermGroup {
    private String groupName;
    private String description;
    private final Set<Permission> permissions;
    private final Set<String> parents;
    private boolean isDefault;

    public PermGroup(String groupName, String description, Permission[] permissions, boolean isDefault) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }

        this.groupName = groupName.trim();
        this.description = description != null ? description : "";
        this.permissions = permissions != null ?
                new HashSet<>(Arrays.asList(permissions)) :
                new HashSet<>();
        this.parents = new HashSet<>();
        this.isDefault = isDefault;
    }

    public PermGroup(String groupName, String description, boolean isDefault) {
        this(groupName, description, null, isDefault);
    }

    // Getters
    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public Set<String> getParents() {
        return Collections.unmodifiableSet(parents);
    }

    public boolean isDefault() {
        return isDefault;
    }

    // Setters
    public void setGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        this.groupName = groupName.trim();
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    // Permission management
    public boolean addPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        return permissions.add(permission);
    }

    public boolean removePermission(String permissionName) {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            return false;
        }

        String normalized = permissionName.toLowerCase().trim();
        return permissions.removeIf(perm -> perm.getPermission().equals(normalized));
    }

    public boolean removePermission(Permission permission) {
        return permissions.remove(permission);
    }

    // Parent management
    public boolean addParent(String parent) {
        if (parent == null || parent.trim().isEmpty()) {
            throw new IllegalArgumentException("Parent name cannot be null or empty");
        }

        String normalized = parent.trim();
        if (normalized.equals(this.groupName)) {
            throw new IllegalArgumentException("Group cannot be its own parent");
        }

        return parents.add(normalized);
    }

    public boolean removeParent(String parent) {
        if (parent == null || parent.trim().isEmpty()) {
            return false;
        }
        return parents.remove(parent.trim());
    }

    // Permission checking
    public boolean hasPermission(String permissionToCheck) {
        if (permissionToCheck == null || permissionToCheck.trim().isEmpty()) {
            return false;
        }

        return permissions.stream()
                .anyMatch(perm -> perm.matches(permissionToCheck));
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    // Utility methods
    public boolean isEmpty() {
        return permissions.isEmpty();
    }

    public int getPermissionCount() {
        return permissions.size();
    }

    public int getParentCount() {
        return parents.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PermGroup permGroup = (PermGroup) obj;
        return Objects.equals(groupName, permGroup.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName);
    }

    @Override
    public String toString() {
        return "PermGroup{" +
                "groupName='" + groupName + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions.size() +
                ", parents=" + parents.size() +
                ", isDefault=" + isDefault +
                '}';
    }
}