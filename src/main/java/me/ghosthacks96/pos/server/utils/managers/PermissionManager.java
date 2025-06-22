package me.ghosthacks96.pos.server.utils.managers;

import me.ghosthacks96.pos.server.utils.perms.PermGroup;
import me.ghosthacks96.pos.server.utils.perms.Permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final Map<String, PermGroup> groups;
    private final Map<String, Permission> permissions;

    public PermissionManager() {
        this.groups = new ConcurrentHashMap<>();
        this.permissions = new ConcurrentHashMap<>();
    }

    // Group management
    public boolean addGroup(PermGroup group) {
        if (group == null) return false;
        groups.put(group.getGroupName().toLowerCase(), group);
        return true;
    }

    public PermGroup getGroup(String groupName) {
        if (groupName == null) return null;
        return groups.get(groupName.toLowerCase().trim());
    }

    public boolean removeGroup(String groupName) {
        if (groupName == null) return false;
        return groups.remove(groupName.toLowerCase().trim()) != null;
    }

    public Collection<PermGroup> getAllGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    // Permission management
    public boolean addPermission(Permission permission) {
        if (permission == null) return false;
        permissions.put(permission.getPermission(), permission);
        return true;
    }

    public Permission getPermission(String permissionName) {
        if (permissionName == null) return null;
        return permissions.get(permissionName.toLowerCase().trim());
    }

    public Collection<Permission> getAllPermissions() {
        return Collections.unmodifiableCollection(permissions.values());
    }

    // Utility methods
    public boolean hasPermission(String groupName, String permission) {
        PermGroup group = getGroup(groupName);
        if (group == null) return false;

        // Check direct permissions
        if (group.hasPermission(permission)) {
            return true;
        }

        // Check inherited permissions from parents
        return checkParentPermissions(group, permission, new HashSet<>());
    }

    private boolean checkParentPermissions(PermGroup group, String permission, Set<String> visited) {
        // Prevent infinite loops in case of circular inheritance
        if (visited.contains(group.getGroupName())) {
            return false;
        }
        visited.add(group.getGroupName());

        for (String parentName : group.getParents()) {
            PermGroup parent = getGroup(parentName);
            if (parent != null) {
                if (parent.hasPermission(permission) ||
                        checkParentPermissions(parent, permission, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<Permission> getAllEffectivePermissions(String groupName) {
        PermGroup group = getGroup(groupName);
        if (group == null) return Collections.emptySet();

        Set<Permission> effectivePermissions = new HashSet<>(group.getPermissions());
        collectParentPermissions(group, effectivePermissions, new HashSet<>());

        return effectivePermissions;
    }

    private void collectParentPermissions(PermGroup group, Set<Permission> collector, Set<String> visited) {
        if (visited.contains(group.getGroupName())) {
            return;
        }
        visited.add(group.getGroupName());

        for (String parentName : group.getParents()) {
            PermGroup parent = getGroup(parentName);
            if (parent != null) {
                collector.addAll(parent.getPermissions());
                collectParentPermissions(parent, collector, visited);
            }
        }
    }
}