package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.NetworkHandler;
import ch.zhaw.pm2.multichat.protocol.NetworkMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The server class listens for incoming connections from clients and manages connections.
 */
public class Server {
    /**
     * Network server for incoming connections.
     */
    private final NetworkHandler.NetworkServer<NetworkMessage> networkServer;

    /**
     * Executor service for handling server connection requests in parallel.
     */
    private static ExecutorService executorService;

    /**
     * Registry for open connections.
     */
    private final Map<String, ServerConnectionHandler> connections = new HashMap<>();

    /**
     * Main method for starting the server.
     * It will open a network server on the given port and wait for incoming connections.
     * If no port is specified, the default port is used (@see NetworkHandler#DEFAULT_PORT)
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        Server server = null;
        try {
            int port;
            switch (args.length) {
                case 0 -> port = NetworkHandler.DEFAULT_PORT;
                case 1 -> port = Integer.parseInt(args[0]);
                default -> {
                    System.out.println("Illegal number of arguments:  [<ServerPort>]");
                    return;
                }
            }
            server = new Server(port);
            server.start();
        } catch (IOException e) {
            System.err.println("Error while starting server. " + e.getMessage());
        } finally {
            if (server != null) {
                System.out.println("Shutting initiated...");
                server.terminate();
                executorService.shutdown();
            }
            System.out.println("Shutdown complete.");
        }
    }

    /**
     * Create a new server instance.
     *
     * @param serverPort Port to listen on.
     * @throws IOException If the server could not be created.
     */
    public Server(int serverPort) throws IOException {
        System.out.println("Create server connection");
        executorService = Executors.newCachedThreadPool();
        networkServer = NetworkHandler.createServer(serverPort);
        System.out.printf("Listening on %s:%d%n", networkServer.getHostAddress(), networkServer.getHostPort());
    }

    /**
     * Terminate the server.
     * Closes the network server, which will terminate the server and close all connections.
     */
    public void terminate() {
        try {
            System.out.println("Close server connection.");
            networkServer.close();
        } catch (IOException e) {
            System.err.println("Failed to close server connection: " + e.getMessage());
        }
    }

    /**
     * Start the server.
     * <p>
     * Opens a network server and waits for incoming connections.
     * For each connection a new {@link ServerConnectionHandler} is created and started in a new thread.
     * If the network server is closed, all connections are closed and the server is stopped.
     */
    private void start() {
        System.out.println("Server started.");
        while (networkServer.isAvailable()) {
            try {
                NetworkHandler.NetworkConnection<NetworkMessage> connection = networkServer.waitForConnection();
                ServerConnectionHandler connectionHandler = new ServerConnectionHandler(connection, connections);
                executorService.execute(connectionHandler);
                System.out.printf("Connected new Client %s with IP:Port <%s:%d>%n",
                    connectionHandler.getUserName(),
                    connection.getRemoteHost(),
                    connection.getRemotePort()
                );
            } catch (IOException e) {
                System.out.println("Warning: Connect failed " + e.getMessage());
            }
        }
        System.out.println("Server Stopped.");
    }
}
