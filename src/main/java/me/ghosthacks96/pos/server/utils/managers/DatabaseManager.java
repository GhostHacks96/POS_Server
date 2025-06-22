package me.ghosthacks96.pos.server.utils.managers;

import me.ghosthacks96.pos.server.utils.models.*;
import me.ghosthacks96.pos.server.utils.perms.PermGroup;
import me.ghosthacks96.pos.server.utils.perms.Permission;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private final String url;
    private final String username;
    private final String password;
    private Connection connection;
    private final ExecutorService executorService;

    public DatabaseManager(String host, int port, String database, String username, String password) {
        this.url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                host, port, database);
        this.username = username;
        this.password = password;
        this.executorService = Executors.newFixedThreadPool(5); // Adjust pool size as needed
    }

    // Connection Management - Synchronous version for initial connection
    public CompletableFuture<Boolean> connectAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, username, password);
                LOGGER.info("Connected to MySQL database successfully");
                return true;
            } catch (ClassNotFoundException | SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
                return false;
            }
        }, executorService);
    }

    // Blocking connect method that waits for connection
    public boolean connect() {
        try {
            return connectAsync().get(); // This will block until connection completes
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
            return false;
        }
    }

    public CompletableFuture<Void> disconnectAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    LOGGER.info("Disconnected from database");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error disconnecting from database", e);
            }
        }, executorService);
    }

    public void disconnect() {
        try {
            disconnectAsync().get(); // Block until disconnect completes
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.WARNING, "Error during async disconnect", e);
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // Database Initialization
    public CompletableFuture<Boolean> initializeDatabaseAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                LOGGER.warning("Not connected to database");
                return false;
            }

            try {
                createTables();
                LOGGER.info("Database tables initialized successfully");
                return true;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize database tables", e);
                return false;
            }
        }, executorService);
    }

    public boolean initializeDatabase() {
        try {
            return initializeDatabaseAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        String[] createTableQueries = {
                // Permissions table
                """
            CREATE TABLE IF NOT EXISTS permissions (
                permission_name VARCHAR(255) PRIMARY KEY,
                description TEXT,
                aliases TEXT,
                is_default BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // Permission groups table
                """
            CREATE TABLE IF NOT EXISTS permission_groups (
                group_name VARCHAR(255) PRIMARY KEY,
                description TEXT,
                is_default BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // Group permissions junction table
                """
            CREATE TABLE IF NOT EXISTS group_permissions (
                group_name VARCHAR(255),
                permission_name VARCHAR(255),
                PRIMARY KEY (group_name, permission_name),
                FOREIGN KEY (group_name) REFERENCES permission_groups(group_name) ON DELETE CASCADE,
                FOREIGN KEY (permission_name) REFERENCES permissions(permission_name) ON DELETE CASCADE
            )
            """,

                // Group inheritance table
                """
            CREATE TABLE IF NOT EXISTS group_inheritance (
                child_group VARCHAR(255),
                parent_group VARCHAR(255),
                PRIMARY KEY (child_group, parent_group),
                FOREIGN KEY (child_group) REFERENCES permission_groups(group_name) ON DELETE CASCADE,
                FOREIGN KEY (parent_group) REFERENCES permission_groups(group_name) ON DELETE CASCADE
            )
            """,

                // Users table
                """
            CREATE TABLE IF NOT EXISTS users (
                user_id VARCHAR(255) PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                first_name VARCHAR(255),
                last_name VARCHAR(255),
                email VARCHAR(255),
                password_hash VARCHAR(255),
                is_active BOOLEAN DEFAULT TRUE,
                is_locked BOOLEAN DEFAULT FALSE,
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login_date TIMESTAMP NULL,
                last_password_change TIMESTAMP NULL,
                failed_login_attempts INT DEFAULT 0
            )
            """,

                // User groups junction table
                """
            CREATE TABLE IF NOT EXISTS user_groups (
                user_id VARCHAR(255),
                group_name VARCHAR(255),
                PRIMARY KEY (user_id, group_name),
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (group_name) REFERENCES permission_groups(group_name) ON DELETE CASCADE
            )
            """,

                // User direct permissions table
                """
            CREATE TABLE IF NOT EXISTS user_permissions (
                user_id VARCHAR(255),
                permission_name VARCHAR(255),
                PRIMARY KEY (user_id, permission_name),
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (permission_name) REFERENCES permissions(permission_name) ON DELETE CASCADE
            )
            """,

                // Customers table
                """
            CREATE TABLE IF NOT EXISTS customers (
                customer_id VARCHAR(255) PRIMARY KEY,
                first_name VARCHAR(255),
                last_name VARCHAR(255),
                email VARCHAR(255),
                phone VARCHAR(50),
                address TEXT,
                loyalty_points DECIMAL(10,2) DEFAULT 0.00,
                customer_type ENUM('REGULAR', 'VIP', 'MEMBER') DEFAULT 'REGULAR',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // Products table
                """
            CREATE TABLE IF NOT EXISTS products (
                product_id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                price DECIMAL(10,2) NOT NULL,
                category VARCHAR(255),
                barcode VARCHAR(255) UNIQUE,
                stock_quantity INT DEFAULT 0,
                is_active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """,

                // Transactions table
                """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id VARCHAR(255) PRIMARY KEY,
                customer_id VARCHAR(255),
                subtotal DECIMAL(10,2) DEFAULT 0.00,
                tax DECIMAL(10,2) DEFAULT 0.00,
                discount DECIMAL(10,2) DEFAULT 0.00,
                total_amount DECIMAL(10,2) DEFAULT 0.00,
                payment_method ENUM('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'MOBILE_PAY', 'GIFT_CARD'),
                status ENUM('PENDING', 'COMPLETED', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
                transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                cashier_id VARCHAR(255),
                FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                FOREIGN KEY (cashier_id) REFERENCES users(user_id)
            )
            """,

                // Transaction items table
                """
            CREATE TABLE IF NOT EXISTS transaction_items (
                item_id VARCHAR(255) PRIMARY KEY,
                transaction_id VARCHAR(255),
                product_id VARCHAR(255),
                quantity INT NOT NULL,
                unit_price DECIMAL(10,2) NOT NULL,
                discount DECIMAL(10,2) DEFAULT 0.00,
                total_price DECIMAL(10,2) NOT NULL,
                FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE,
                FOREIGN KEY (product_id) REFERENCES products(product_id)
            )
            """,

                // Receipts table
                """
            CREATE TABLE IF NOT EXISTS receipts (
                receipt_id VARCHAR(255) PRIMARY KEY,
                transaction_id VARCHAR(255) UNIQUE,
                store_name VARCHAR(255),
                store_address TEXT,
                store_phone VARCHAR(50),
                print_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
            )
            """
        };

        for (String query : createTableQueries) {
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.executeUpdate();
            }
        }
    }

    // Permission Operations - Async versions
    public CompletableFuture<Boolean> savePermissionAsync(Permission permission) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO permissions (permission_name, description, aliases, is_default)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                description = VALUES(description),
                aliases = VALUES(aliases),
                is_default = VALUES(is_default)
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, permission.getPermission());
                stmt.setString(2, permission.getDescription());
                stmt.setString(3, String.join(",", permission.getAliases()));
                stmt.setBoolean(4, permission.isDefault());

                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to save permission: " + permission.getPermission(), e);
                return false;
            }
        }, executorService);
    }

    public boolean savePermission(Permission permission) {
        try {
            return savePermissionAsync(permission).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to save permission", e);
            return false;
        }
    }

    public CompletableFuture<Permission> loadPermissionAsync(String permissionName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM permissions WHERE permission_name = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, permissionName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String aliases = rs.getString("aliases");
                    String[] aliasArray = aliases != null && !aliases.isEmpty() ?
                            aliases.split(",") : new String[0];

                    return new Permission(
                            rs.getString("permission_name"),
                            rs.getString("description"),
                            aliasArray,
                            rs.getBoolean("is_default")
                    );
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load permission: " + permissionName, e);
            }

            return null;
        }, executorService);
    }

    public Permission loadPermission(String permissionName) {
        try {
            return loadPermissionAsync(permissionName).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to load permission", e);
            return null;
        }
    }

    public CompletableFuture<List<Permission>> loadAllPermissionsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Permission> permissions = new ArrayList<>();
            String sql = "SELECT * FROM permissions ORDER BY permission_name";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String aliases = rs.getString("aliases");
                    String[] aliasArray = aliases != null && !aliases.isEmpty() ?
                            aliases.split(",") : new String[0];

                    permissions.add(new Permission(
                            rs.getString("permission_name"),
                            rs.getString("description"),
                            aliasArray,
                            rs.getBoolean("is_default")
                    ));
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load all permissions", e);
            }

            return permissions;
        }, executorService);
    }

    public List<Permission> loadAllPermissions() {
        try {
            return loadAllPermissionsAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to load all permissions", e);
            return new ArrayList<>();
        }
    }

    // Permission Group Operations - Async versions
    public CompletableFuture<Boolean> savePermGroupAsync(PermGroup group) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                connection.setAutoCommit(false);

                // Save group basic info
                String groupSql = """
                    INSERT INTO permission_groups (group_name, description, is_default)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE 
                    description = VALUES(description),
                    is_default = VALUES(is_default)
                    """;

                try (PreparedStatement stmt = connection.prepareStatement(groupSql)) {
                    stmt.setString(1, group.getGroupName());
                    stmt.setString(2, group.getDescription());
                    stmt.setBoolean(3, group.isDefault());
                    stmt.executeUpdate();
                }

                // Clear existing permissions and parents
                clearGroupPermissions(group.getGroupName());
                clearGroupParents(group.getGroupName());

                // Save group permissions
                if (!group.getPermissions().isEmpty()) {
                    String permSql = "INSERT INTO group_permissions (group_name, permission_name) VALUES (?, ?)";
                    try (PreparedStatement stmt = connection.prepareStatement(permSql)) {
                        for (Permission perm : group.getPermissions()) {
                            stmt.setString(1, group.getGroupName());
                            stmt.setString(2, perm.getPermission());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // Save group parents
                if (!group.getParents().isEmpty()) {
                    String parentSql = "INSERT INTO group_inheritance (child_group, parent_group) VALUES (?, ?)";
                    try (PreparedStatement stmt = connection.prepareStatement(parentSql)) {
                        for (String parent : group.getParents()) {
                            stmt.setString(1, group.getGroupName());
                            stmt.setString(2, parent);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                connection.commit();
                return true;

            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
                LOGGER.log(Level.SEVERE, "Failed to save permission group: " + group.getGroupName(), e);
                return false;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to reset auto-commit", e);
                }
            }
        }, executorService);
    }

    public boolean savePermGroup(PermGroup group) {
        try {
            return savePermGroupAsync(group).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to save permission group", e);
            return false;
        }
    }

    private void clearGroupPermissions(String groupName) throws SQLException {
        String sql = "DELETE FROM group_permissions WHERE group_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupName);
            stmt.executeUpdate();
        }
    }

    private void clearGroupParents(String groupName) throws SQLException {
        String sql = "DELETE FROM group_inheritance WHERE child_group = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupName);
            stmt.executeUpdate();
        }
    }

    public CompletableFuture<PermGroup> loadPermGroupAsync(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM permission_groups WHERE group_name = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, groupName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    PermGroup group = new PermGroup(
                            rs.getString("group_name"),
                            rs.getString("description"),
                            rs.getBoolean("is_default")
                    );

                    // Load permissions
                    loadGroupPermissions(group);

                    // Load parents
                    loadGroupParents(group);

                    return group;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load permission group: " + groupName, e);
            }

            return null;
        }, executorService);
    }

    public PermGroup loadPermGroup(String groupName) {
        try {
            return loadPermGroupAsync(groupName).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to load permission group", e);
            return null;
        }
    }

    private void loadGroupPermissions(PermGroup group) throws SQLException {
        String sql = """
            SELECT p.* FROM permissions p
            JOIN group_permissions gp ON p.permission_name = gp.permission_name
            WHERE gp.group_name = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group.getGroupName());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String aliases = rs.getString("aliases");
                String[] aliasArray = aliases != null && !aliases.isEmpty() ?
                        aliases.split(",") : new String[0];

                Permission permission = new Permission(
                        rs.getString("permission_name"),
                        rs.getString("description"),
                        aliasArray,
                        rs.getBoolean("is_default")
                );

                group.addPermission(permission);
            }
        }
    }

    private void loadGroupParents(PermGroup group) throws SQLException {
        String sql = "SELECT parent_group FROM group_inheritance WHERE child_group = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group.getGroupName());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                group.addParent(rs.getString("parent_group"));
            }
        }
    }

    public CompletableFuture<List<PermGroup>> loadAllPermGroupsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<PermGroup> groups = new ArrayList<>();
            String sql = "SELECT group_name FROM permission_groups ORDER BY group_name";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    PermGroup group = loadPermGroup(rs.getString("group_name"));
                    if (group != null) {
                        groups.add(group);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load all permission groups", e);
            }

            return groups;
        }, executorService);
    }

    public List<PermGroup> loadAllPermGroups() {
        try {
            return loadAllPermGroupsAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to load all permission groups", e);
            return new ArrayList<>();
        }
    }

    // User Operations - Async versions
    public CompletableFuture<Boolean> saveUserAsync(User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                connection.setAutoCommit(false);

                // Save user basic info
                String userSql = """
                    INSERT INTO users (user_id, username, first_name, last_name, email, 
                                     password_hash, is_active, is_locked, created_date, 
                                     last_login_date, last_password_change, failed_login_attempts)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE 
                    username = VALUES(username),
                    first_name = VALUES(first_name),
                    last_name = VALUES(last_name),
                    email = VALUES(email),
                    password_hash = VALUES(password_hash),
                    is_active = VALUES(is_active),
                    is_locked = VALUES(is_locked),
                    last_login_date = VALUES(last_login_date),
                    last_password_change = VALUES(last_password_change),
                    failed_login_attempts = VALUES(failed_login_attempts)
                    """;

                try (PreparedStatement stmt = connection.prepareStatement(userSql)) {
                    stmt.setString(1, user.getUserId());
                    stmt.setString(2, user.getUsername());
                    stmt.setString(3, user.getFirstName());
                    stmt.setString(4, user.getLastName());
                    stmt.setString(5, user.getEmail());
                    stmt.setString(6, user.getPasswordHash());
                    stmt.setBoolean(7, user.isActive());
                    stmt.setBoolean(8, user.isLocked());
                    stmt.setTimestamp(9, Timestamp.valueOf(user.getCreatedDate()));
                    stmt.setTimestamp(10, user.getLastLoginDate() != null ?
                            Timestamp.valueOf(user.getLastLoginDate()) : null);
                    stmt.setTimestamp(11, user.getLastPasswordChange() != null ?
                            Timestamp.valueOf(user.getLastPasswordChange()) : null);
                    stmt.setInt(12, user.getFailedLoginAttempts());
                    stmt.executeUpdate();
                }

                // Clear existing groups and permissions
                clearUserGroups(user.getUserId());
                clearUserPermissions(user.getUserId());

                // Save user groups
                if (!user.getGroups().isEmpty()) {
                    String groupSql = "INSERT INTO user_groups (user_id, group_name) VALUES (?, ?)";
                    try (PreparedStatement stmt = connection.prepareStatement(groupSql)) {
                        for (String groupName : user.getGroups()) {
                            stmt.setString(1, user.getUserId());
                            stmt.setString(2, groupName);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // Save user direct permissions
                if (!user.getDirectPermissions().isEmpty()) {
                    String permSql = "INSERT INTO user_permissions (user_id, permission_name) VALUES (?, ?)";
                    try (PreparedStatement stmt = connection.prepareStatement(permSql)) {
                        for (Permission perm : user.getDirectPermissions()) {
                            stmt.setString(1, user.getUserId());
                            stmt.setString(2, perm.getPermission());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                connection.commit();
                return true;

            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
                LOGGER.log(Level.SEVERE, "Failed to save user: " + user.getUserId(), e);
                return false;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to reset auto-commit", e);
                }
            }
        }, executorService);
    }

    public boolean saveUser(User user) {
        try {
            return saveUserAsync(user).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to save user", e);
            return false;
        }
    }

    private void clearUserGroups(String userId) throws SQLException {
        String sql = "DELETE FROM user_groups WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
    }

    private void clearUserPermissions(String userId) throws SQLException {
        String sql = "DELETE FROM user_permissions WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
    }

    public CompletableFuture<User> loadUserAsync(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM users WHERE user_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    User user = new User(
                            rs.getString("user_id"),
                            rs.getString("username"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email")
                    );

                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setActive(rs.getBoolean("is_active"));
                    user.setLocked(rs.getBoolean("is_locked"));

                    Timestamp lastLogin = rs.getTimestamp("last_login_date");
                    if (lastLogin != null) {
                        user.setLastLoginDate(lastLogin.toLocalDateTime());
                    }

                    // Load user groups
                    loadUserGroups(user);

                    // Load user direct permissions
                    loadUserDirectPermissions(user);

                    return user;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load user: " + userId, e);
            }

            return null;
        }, executorService);
    }

    public User loadUser(String userId) {
        try {
            return loadUserAsync(userId).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to load user", e);
            return null;
        }
    }

    private void loadUserGroups(User user) throws SQLException {
        String sql = "SELECT group_name FROM user_groups WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUserId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                user.addToGroup(rs.getString("group_name"));
            }
        }
    }

    private void loadUserDirectPermissions(User user) throws SQLException {
        String sql = """
            SELECT p.* FROM permissions p
            JOIN user_permissions up ON p.permission_name = up.permission_name
            WHERE up.user_id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUserId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String aliases = rs.getString("aliases");
                String[] aliasArray = aliases != null && !aliases.isEmpty() ?
                        aliases.split(",") : new String[0];

                Permission permission = new Permission(
                        rs.getString("permission_name"),
                        rs.getString("description"),
                        aliasArray,
                        rs.getBoolean("is_default")
                );

                user.addDirectPermission(permission);
            }
        }
    }

    public CompletableFuture<List<User>> loadAllUsersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<User> users = new ArrayList<>();
            String sql = "SELECT user_id FROM users ORDER BY username";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    User user = loadUser(rs.getString("user_id"));
                    if (user != null) {
                        users.add(user);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load all users", e);
            }

            return users;
        }, executorService);
    }

    public List<User> loadAllUsers() {
        try {
            return loadAllUsersAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Failed to load all users", e);
            return new ArrayList<>();
        }
    }

    // Customer Operations - Async versions
    public CompletableFuture<Boolean> saveCustomerAsync(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO customers (customer_id, first_name, last_name, email, phone, 
                                     address, loyalty_points, customer_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                first_name = VALUES(first_name),
                last_name = VALUES(last_name),
                email = VALUES(email),
                phone = VALUES(phone),
                address = VALUES(address),
                loyalty_points = VALUES(loyalty_points),
                customer_type = VALUES(customer_type)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, customer.getCustomerId());
            stmt.setString(2, customer.getFirstName());
            stmt.setString(3, customer.getLastName());
            stmt.setString(4, customer.getEmail());
            stmt.setString(5, customer.getPhone());
            stmt.setString(6, customer.getAddress());
            stmt.setDouble(7, customer.getLoyaltyPoints());
            stmt.setString(8, customer.getCustomerType().name());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save customer: " + customer.getCustomerId(), e);
            return false;
        }
    });
    }

    public CompletableFuture<Customer> loadCustomer(String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM customers WHERE customer_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, customerId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Customer customer = new Customer(
                            rs.getString("customer_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name")
                    );

                    customer.setEmail(rs.getString("email"));
                    customer.setPhone(rs.getString("phone"));
                    customer.setAddress(rs.getString("address"));
                    customer.setLoyaltyPoints(rs.getDouble("loyalty_points"));
                    customer.setCustomerType(Customer.CustomerType.valueOf(rs.getString("customer_type")));

                    return customer;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load customer: " + customerId, e);
                throw new RuntimeException("Failed to load customer: " + customerId, e);
            }

            return null;
        }, executorService);
    }

    public CompletableFuture<List<Customer>> loadAllCustomers() {
        return CompletableFuture.supplyAsync(() -> {
                    List<String> customerIds = new ArrayList<>();
                    String sql = "SELECT customer_id FROM customers ORDER BY first_name, last_name";

                    try (PreparedStatement stmt = connection.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {
                            customerIds.add(rs.getString("customer_id"));
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Failed to load customer IDs", e);
                        throw new RuntimeException("Failed to load customer IDs", e);
                    }

                    return customerIds;
                }, executorService)
                .thenCompose(customerIds -> {
                    // Load all customers concurrently
                    List<CompletableFuture<Customer>> customerFutures = customerIds.stream()
                            .map(this::loadCustomer)
                            .toList();

                    return CompletableFuture.allOf(customerFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> customerFutures.stream()
                                    .map(CompletableFuture::join)
                                    .filter(customer -> customer != null)
                                    .toList());
                });
    }

    // Alternative version that loads customers directly without individual calls
    public CompletableFuture<List<Customer>> loadAllCustomersDirect() {
        return CompletableFuture.supplyAsync(() -> {
            List<Customer> customers = new ArrayList<>();
            String sql = """
                SELECT customer_id, first_name, last_name, email, phone, 
                       address, loyalty_points, customer_type 
                FROM customers 
                ORDER BY first_name, last_name
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Customer customer = new Customer(
                            rs.getString("customer_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name")
                    );

                    customer.setEmail(rs.getString("email"));
                    customer.setPhone(rs.getString("phone"));
                    customer.setAddress(rs.getString("address"));
                    customer.setLoyaltyPoints(rs.getDouble("loyalty_points"));
                    customer.setCustomerType(Customer.CustomerType.valueOf(rs.getString("customer_type")));

                    customers.add(customer);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load all customers", e);
                throw new RuntimeException("Failed to load all customers", e);
            }

            return customers;
        }, executorService);
    }

    // Product Operations
    public CompletableFuture<Boolean> saveProduct(Product product) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO products (product_id, name, description, price, category, 
                                    barcode, stock_quantity, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                name = VALUES(name),
                description = VALUES(description),
                price = VALUES(price),
                category = VALUES(category),
                barcode = VALUES(barcode),
                stock_quantity = VALUES(stock_quantity),
                is_active = VALUES(is_active)
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, product.getProductId());
                stmt.setString(2, product.getName());
                stmt.setString(3, product.getDescription());
                stmt.setDouble(4, product.getPrice());
                stmt.setString(5, product.getCategory());
                stmt.setString(6, product.getBarcode());
                stmt.setInt(7, product.getStockQuantity());
                stmt.setBoolean(8, product.isActive());

                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to save product: " + product.getProductId(), e);
                throw new RuntimeException("Failed to save product: " + product.getProductId(), e);
            }
        }, executorService);
    }

    public CompletableFuture<Product> loadProduct(String productId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM products WHERE product_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, productId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Product product = new Product(
                            rs.getString("product_id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("barcode")
                    );

                    product.setDescription(rs.getString("description"));
                    product.setCategory(rs.getString("category"));
                    product.setStockQuantity(rs.getInt("stock_quantity"));
                    product.setActive(rs.getBoolean("is_active"));

                    return product;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load product: " + productId, e);
                throw new RuntimeException("Failed to load product: " + productId, e);
            }

            return null;
        }, executorService);
    }

    public CompletableFuture<List<Product>> loadAllProducts() {
        return CompletableFuture.supplyAsync(() -> {
            List<Product> products = new ArrayList<>();
            String sql = """
                SELECT product_id, name, description, price, category, 
                       barcode, stock_quantity, is_active 
                FROM products 
                ORDER BY name
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Product product = new Product(
                            rs.getString("product_id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("barcode")
                    );

                    product.setDescription(rs.getString("description"));
                    product.setCategory(rs.getString("category"));
                    product.setStockQuantity(rs.getInt("stock_quantity"));
                    product.setActive(rs.getBoolean("is_active"));

                    products.add(product);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load all products", e);
                throw new RuntimeException("Failed to load all products", e);
            }

            return products;
        }, executorService);
    }

    // Utility method to save multiple products concurrently
    public CompletableFuture<List<Boolean>> saveProductsBatch(List<Product> products) {
        List<CompletableFuture<Boolean>> saveFutures = products.stream()
                .map(this::saveProduct)
                .toList();

        return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> saveFutures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    // Example usage methods showing how to compose operations
    public CompletableFuture<Void> processCustomerOrder(String customerId, List<String> productIds) {
        CompletableFuture<Customer> customerFuture = loadCustomer(customerId);

        List<CompletableFuture<Product>> productFutures = productIds.stream()
                .map(this::loadProduct)
                .toList();

        CompletableFuture<List<Product>> allProductsFuture =
                CompletableFuture.allOf(productFutures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> productFutures.stream()
                                .map(CompletableFuture::join)
                                .filter(product -> product != null)
                                .toList());

        return customerFuture.thenCombine(allProductsFuture, (customer, products) -> {
            if (customer == null) {
                throw new RuntimeException("Customer not found: " + customerId);
            }

            // Process the order logic here
            LOGGER.info("Processing order for customer: " + customer.getFirstName() +
                    " with " + products.size() + " products");

            return null; // Void return
        });
    }

    // Cleanup method
    public void shutdown() {
        if (executorService instanceof ExecutorService) {
            ((ExecutorService) executorService).shutdown();
        }
    }
}
