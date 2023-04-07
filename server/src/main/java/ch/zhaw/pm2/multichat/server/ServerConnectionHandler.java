package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.zhaw.pm2.multichat.server.ServerConnectionHandler.State.*;

public class ServerConnectionHandler implements Runnable{
    /** Global counter to generate connection IDs */
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    /** The ID of this connection */
    private final int connectionId = connectionCounter.incrementAndGet();

    /** Reference to the registry managing all connections */
    private final Map<String,ServerConnectionHandler> connectionRegistry;

    /** The network connection to be used for receiving and sending requests */
    private final NetworkHandler.NetworkConnection<String> connection;

    // Data types used for the Chat Protocol
    private static final String DATA_TYPE_CONNECT = "CONNECT";
    private static final String DATA_TYPE_CONFIRM = "CONFIRM";
    private static final String DATA_TYPE_DISCONNECT = "DISCONNECT";
    private static final String DATA_TYPE_MESSAGE = "MESSAGE";
    private static final String DATA_TYPE_ERROR = "ERROR";

    private static final String USER_NONE = "";
    private static final String USER_ALL = "*";

    /**
     * The username associated with this connection
     * Using Anonymous-{@link #connectionId} if not specified by the client
     */
    private String userName = "Anonymous-"+connectionId;

    /** The current state of this connection */
    private State state = NEW;

    @Override
    public void run() {
        startReceiving();
    }

    enum State {
        NEW, CONNECTED, DISCONNECTED;
    }

    public ServerConnectionHandler(NetworkHandler.NetworkConnection<String> connection,
                                   Map<String,ServerConnectionHandler> registry) {
        Objects.requireNonNull(connection, "Connection must not be null");
        Objects.requireNonNull(registry, "Registry must not be null");
        this.connection = connection;
        this.connectionRegistry = registry;
    }

    public String getUserName() {
        return this.userName;
    }

    /**
     * Start receiving packages from the network connection.
     * It continuously receives packages from the network connection and processes it depending on the package type
     */
    public void startReceiving() {
        System.out.println("Starting Connection Handler for " + userName);
        try {
            System.out.println("Start receiving data...");
            while (connection.isAvailable()) {
                String data = connection.receive();
                processData(data);
            }
        } catch (SocketException e) {
            System.out.println("Connection terminated locally");
            connectionRegistry.remove(userName);
            System.out.println("Unregistered because connection terminated: " + userName + " " + e.getMessage());
        } catch (EOFException e) {
            System.out.println("Connection terminated by remote peer");
            connectionRegistry.remove(userName);
            System.out.println("Unregistered because connection terminated: " + userName + " " + e.getMessage());
        } catch(IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch(ClassNotFoundException e) {
            System.err.println("Received object of unknown type: " + e.getMessage());
        }
        System.out.println("Ended Connection Handler for " + userName);
    }

    /**
     * Stop receiving packages from the network connection, by closing the connection.
     */
    public void stopReceiving() {
        System.out.println("Closing Connection Handler for " + userName + "...");
        try {
            System.out.println("Stop receiving data...");
            connection.close();
            System.out.println("Stopped receiving data.");
        } catch (IOException e) {
            System.err.println("Failed to close connection." + e.getMessage());
        }
        System.out.println("Closed Connection Handler for " + userName);
    }
//Todo: extrem lange Methode, in verschiedene Methoden unterteilen
    private void processData(String data) {
        try {
            // parse data content
            Scanner scanner = new Scanner(data);
            String sender = null;
            String reciever = null;
            String type = null;
            String payload = null;
            if (scanner.hasNextLine()) {
                sender = scanner.nextLine();
            } else {
                throw new ChatProtocolException("No Sender found");
            }
            if (scanner.hasNextLine()) {
                reciever = scanner.nextLine();
            } else {
                throw new ChatProtocolException("No Reciever found");
            }
            if (scanner.hasNextLine()) {
                type = scanner.nextLine();
            } else {
                throw new ChatProtocolException("No Type found");
            }
            if (scanner.hasNextLine()) {
                payload = scanner.nextLine();
            }

            // dispatch operation based on type parameter
            if (type.equals(DATA_TYPE_CONNECT)) {
                if (this.state != NEW) throw new ChatProtocolException("Illegal state for connect request: " + state);
                if (sender == null || sender.isBlank()) sender = this.userName;
                if (connectionRegistry.containsKey(sender))
                    throw new ChatProtocolException("User name already taken: " + sender);
                this.userName = sender;
                connectionRegistry.put(userName, this);
                sendData(USER_NONE, userName, DATA_TYPE_CONFIRM, "Registration successfull for " + userName);
                this.state = CONNECTED;
            } else if (type.equals(DATA_TYPE_CONFIRM)) {
                System.out.println("Not expecting to receive a CONFIRM request from client");
            } else if (type.equals(DATA_TYPE_DISCONNECT)) {
                if (state == DISCONNECTED) {
                    throw new ChatProtocolException("Illegal state for disconnect request: " + state);
                }
                if (state == CONNECTED) {
                    connectionRegistry.remove(this.userName);
                }
                sendData(USER_NONE, userName, DATA_TYPE_CONFIRM, "Confirm disconnect of " + userName);
                this.state = DISCONNECTED;
                this.stopReceiving();
            } else if (type.equals(DATA_TYPE_MESSAGE)) {
                if (state != CONNECTED) throw new ChatProtocolException("Illegal state for message request: " + state);
                if (USER_ALL.equals(reciever)) {
                    for (ServerConnectionHandler handler : connectionRegistry.values()) {
                        handler.sendData(sender, reciever, type, payload);
                    }
                } else {
                    ServerConnectionHandler handler = connectionRegistry.get(reciever);
                    if (handler != null) {
                        handler.sendData(sender, reciever, type, payload);
                    } else {
                        this.sendData(USER_NONE, userName, DATA_TYPE_ERROR, "Unknown User: " + reciever);
                    }
                }
            } else if (type.equals(DATA_TYPE_ERROR)) {
                System.out.println("Received error from client (" + sender + "): " + payload);
            } else {
                System.out.println("Unknown data type received: " + type);
            }
        } catch(ChatProtocolException error) {
            System.err.println("Error while processing data: " + error.getMessage());
            sendData(USER_NONE, userName, DATA_TYPE_ERROR, error.getMessage());
        }
    }

    public void sendData(String sender, String receiver, String type, String payload) {
        if (connection.isAvailable()) {
            new StringBuilder();
            String data = new StringBuilder()
                .append(sender+"\n")
                .append(receiver+"\n")
                .append(type+"\n")
                .append(payload+"\n")
                .toString();
            try {
                connection.send(data);
            } catch (SocketException e) {
                System.err.println("Connection closed: " + e.getMessage());
            } catch (EOFException e) {
                System.err.println("Connection terminated by remote peer");
            } catch(IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            }
        }
    }
}
