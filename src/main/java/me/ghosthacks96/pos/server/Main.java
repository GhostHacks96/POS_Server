package me.ghosthacks96.pos.server;

import me.ghosthacks96.pos.server.utils.managers.ConfigManager;
import me.ghosthacks96.pos.server.utils.managers.DatabaseManager;
import me.ghosthacks96.pos.server.utils.managers.PermissionManager;
import me.ghosthacks96.pos.server.utils.managers.UserManager;
import me.ghosthacks96.pos.server.utils.models.User;
import me.ghosthacks96.pos.server.utils.perms.PermGroup;
import me.ghosthacks96.pos.server.utils.perms.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Main {



    public static final String SERVER_VERSION = "1.0.0";
    public static final String SERVER_NAME = "POS Server";
    public static final String SERVER_AUTHOR = "GhostHacks96";
    public static final String SERVER_DESCRIPTION = "A Point of Sale server application for managing sales and inventory.";

    public static PermissionManager permissionManager;
    public static UserManager userManager;
    public static DatabaseManager databaseManager;
    public static ConfigManager configManager;

    public static void main(String[] args) {

        //initalize the managers
        configManager = ConfigManager.getInstance();
        userManager = new UserManager(permissionManager);
        permissionManager = new PermissionManager();

        loadConfigs();
        loadDatabase();
        loadPerms();
        loadUsers();
        loadServer();

    }

    private static void loadConfigs(){
        ConfigManager.Builder configs = ConfigManager.builder();
        for(File f : Objects.requireNonNull(new File("config").listFiles())) {
            if(f.isFile() && f.getName().endsWith(".yml")) {
                configs.addFileConfig(f.getName(), f.getAbsolutePath()).enableAutoReload(30);
            }
        }
        configManager = configs.build();
    }

    private static void loadDatabase(){
        String user = configManager.getConfig("database.yml").getString("database.user");
        String password = configManager.getConfig("database.yml").getString("database.password");
        String url = configManager.getConfig("database.yml").getString("database.url");
        int port = configManager.getConfig("database.yml").getInt("database.port");
        String dbName = configManager.getConfig("database.yml").getString("database.name");
        databaseManager = new DatabaseManager(url, port,dbName, user, password);
    }

    private static void loadPerms(){
        CompletableFuture<List<PermGroup>> permGroups = databaseManager.loadAllPermGroupsAsync();

        for(PermGroup group : permGroups.join()){
            permissionManager.addGroup(group);
        }

        CompletableFuture<List<Permission>> permissions = databaseManager.loadAllPermissionsAsync();
        for(Permission permission : permissions.join()){
            permissionManager.addPermission(permission);
        }

    }
    private static void loadUsers(){
        CompletableFuture<List<User>> users = databaseManager.loadAllUsersAsync();
        List<User> userList = users.join();
        for(User user : userList){
            userManager.addUser(user);
        }
    }

    private static void loadServer() {
        System.out.println("Starting " + SERVER_NAME + " v" + SERVER_VERSION + " by " + SERVER_AUTHOR);
        System.out.println(SERVER_DESCRIPTION);
    }
}
