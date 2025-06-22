package me.ghosthacks96.pos.server.utils.perms;

import java.util.*;

public class Permission {
    private final String permission;
    private final String description;
    private final Set<String> aliases;
    private final boolean isDefault;

    public Permission(String permission, String description, String[] aliases, boolean isDefault) {
        if (permission == null || permission.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name cannot be null or empty");
        }

        this.permission = permission.toLowerCase().trim();
        this.description = description != null ? description : "";
        this.aliases = aliases != null ?
                new HashSet<>(Arrays.asList(aliases)) :
                new HashSet<>();
        this.isDefault = isDefault;
    }

    public Permission(String permission, String description, boolean isDefault) {
        this(permission, description, null, isDefault);
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAlias(String alias) {
        if (alias == null) return false;
        return aliases.contains(alias.toLowerCase().trim());
    }

    public Set<String> getAliases() {
        return Collections.unmodifiableSet(aliases);
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean matches(String permissionToCheck) {
        if (permissionToCheck == null) return false;
        String normalized = permissionToCheck.toLowerCase().trim();
        return permission.equals(normalized) || aliases.contains(normalized);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Permission that = (Permission) obj;
        return Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "permission='" + permission + '\'' +
                ", description='" + description + '\'' +
                ", aliases=" + aliases +
                ", isDefault=" + isDefault +
                '}';
    }
}
