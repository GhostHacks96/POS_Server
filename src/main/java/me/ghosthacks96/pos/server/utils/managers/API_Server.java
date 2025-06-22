package me.ghosthacks96.pos.server.utils.managers;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class API_Server {


    public static Server server;
    public static int port = 8080;
    public static String host = "localhost";
    public static String contextPath = "/api";
    public static String serverName = "POS API Server";


    public static void start() {
        try {
            server = new Server(port);
            server.setHandler(new APIHandler()); // Assuming APIHandler is defined elsewhere
            server.start();
            System.out.println("API Server started on " + host + ":" + port + contextPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        try {
            if (server != null && server.isRunning()) {
                server.stop();
                System.out.println("API Server stopped.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class APIHandler extends Handler.Abstract {
        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            String path = request.getHttpURI().getPath();
            String html;
            response.getHeaders().put("Content-Type", "text/html;charset=utf-8");
            response.setStatus(200);

            // Static pages for each model
            if (path.equals(contextPath + "/health")) {
                html = getHealthPage();
            } else if (path.equals(contextPath + "/users")) {
                html = getUsersPage();
            } else if (path.equals(contextPath + "/products")) {
                html = getProductsPage();
            } else if (path.equals(contextPath + "/customers")) {
                html = getCustomersPage();
            } else if (path.equals(contextPath + "/transactions")) {
                html = getTransactionsPage();
            } else if (path.equals(contextPath + "/receipts")) {
                html = getReceiptsPage();
            } else {
                // Default page
                html = "<h1>Hello World!</h1>" +
                        "<p>Request path: " + path + "</p>" +
                        "<p>Method: " + request.getMethod() + "</p>";
            }

            response.write(true, ByteBuffer.wrap(html.getBytes()), callback);
            return true;
        }

        // Static page generators for each model
        private String getHealthPage() {
            StringBuilder sb = new StringBuilder("<h1>Server Health Check</h1><ul>");
            sb.append("<li>Server Name: ").append(me.ghosthacks96.pos.server.Main.SERVER_NAME).append("</li>");
            sb.append("<li>Version: ").append(me.ghosthacks96.pos.server.Main.SERVER_VERSION).append("</li>");
            sb.append("<li>Author: ").append(me.ghosthacks96.pos.server.Main.SERVER_AUTHOR).append("</li>");
            sb.append("<li>Description: ").append(me.ghosthacks96.pos.server.Main.SERVER_DESCRIPTION).append("</li>");
            sb.append("<li>Database Connected: ").append(me.ghosthacks96.pos.server.Main.databaseManager != null && me.ghosthacks96.pos.server.Main.databaseManager.isConnected() ? "Yes" : "No").append("</li>");
            sb.append("<li>Config Healthy: ").append(me.ghosthacks96.pos.server.Main.configManager != null && me.ghosthacks96.pos.server.Main.configManager.isHealthy() ? "Yes" : "No").append("</li>");
            sb.append("<li>User Count: ").append(me.ghosthacks96.pos.server.Main.userManager != null ? me.ghosthacks96.pos.server.Main.userManager.getTotalUserCount() : 0).append("</li>");
            sb.append("</ul>");
            sb.append("<p>Status: <b>");
            boolean healthy = me.ghosthacks96.pos.server.Main.databaseManager != null && me.ghosthacks96.pos.server.Main.databaseManager.isConnected()
                    && me.ghosthacks96.pos.server.Main.configManager != null && me.ghosthacks96.pos.server.Main.configManager.isHealthy();
            sb.append(healthy ? "OK" : "ERROR");
            sb.append("</b></p>");
            return sb.toString();
        }

        private String getUsersPage() {
            StringBuilder sb = new StringBuilder("<h1>Users</h1><ul>");
            for (var user : me.ghosthacks96.pos.server.Main.userManager.getAllUsers()) {
                sb.append("<li>").append(user.getUsername()).append(" (ID: ").append(user.getUserId()).append(")</li>");
            }
            sb.append("</ul>");
            return sb.toString();
        }

        private String getProductsPage() {
            StringBuilder sb = new StringBuilder("<h1>Products</h1><ul>");
            // You may want to cache or load products from DB here
            var products = me.ghosthacks96.pos.server.Main.databaseManager.loadAllProducts().join();
            for (var product : products) {
                sb.append("<li>").append(product.getName()).append(" (ID: ").append(product.getProductId()).append(")</li>");
            }
            sb.append("</ul>");
            return sb.toString();
        }

        private String getCustomersPage() {
            StringBuilder sb = new StringBuilder("<h1>Customers</h1><ul>");
            var customers = me.ghosthacks96.pos.server.Main.databaseManager.loadAllCustomersDirect().join();
            for (var customer : customers) {
                sb.append("<li>").append(customer.getFullName()).append(" (ID: ").append(customer.getCustomerId()).append(")</li>");
            }
            sb.append("</ul>");
            return sb.toString();
        }

        private String getTransactionsPage() {
            StringBuilder sb = new StringBuilder("<h1>Transactions</h1><ul>");
            // You would need to implement a method to load all transactions in your DatabaseManager
            sb.append("<li>Transaction listing not implemented</li>");
            sb.append("</ul>");
            return sb.toString();
        }

        private String getReceiptsPage() {
            StringBuilder sb = new StringBuilder("<h1>Receipts</h1><ul>");
            // You would need to implement a method to load all receipts in your DatabaseManager
            sb.append("<li>Receipt listing not implemented</li>");
            sb.append("</ul>");
            return sb.toString();
        }
    }

}
