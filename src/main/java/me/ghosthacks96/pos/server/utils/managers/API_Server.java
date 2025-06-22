package me.ghosthacks96.pos.server.utils.managers;


import org.eclipse.jetty.server.Server;

public class API_Server {


    public static Server server;
    public static int port = 8080;
    public static String host = "localhost";
    public static String contextPath = "/api";
    public static String serverName = "POS API Server";


    public static void start() {
        try {
            server = new Server(port);
            server.start();
            System.out.println("API Server started on " + host + ":" + port + contextPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
