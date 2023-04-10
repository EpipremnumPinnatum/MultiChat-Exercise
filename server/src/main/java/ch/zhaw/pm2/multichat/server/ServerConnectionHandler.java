package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Configuration;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerConnectionHandler implements Runnable {
    /**
     * Global counter to generate connection IDs
     */
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * The ID of this connection
     */
    private final int connectionId = connectionCounter.incrementAndGet();

    /**
     * Reference to the registry managing all connections
     */
    private final Map<String, ServerConnectionHandler> connectionRegistry;

    /**
     * The network connection to be used for receiving and sending requests
     */
    private final NetworkHandler.NetworkConnection<String> connection;

    private static final String USER_NONE = "";
    private static final String USER_ALL = "*";

    /**
     * The username associated with this connection
     * Using Anonymous-{@link #connectionId} if not specified by the client
     */
    private String userName = "Anonymous-" + connectionId;

    /**
     * The current state of this connection
     */
    private Configuration.ProtocolState protocolState = Configuration.ProtocolState.NEW;

    @Override
    public void run() {
        startReceiving();
    }

    public ServerConnectionHandler(NetworkHandler.NetworkConnection<String> connection,
                                   Map<String, ServerConnectionHandler> registry) {
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
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
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

    public void sendData(String sender, String receiver, String type, String payload) {
        if (connection.isAvailable()) {
            new StringBuilder();
            String data = new StringBuilder()
                .append(sender + "\n")
                .append(receiver + "\n")
                .append(type + "\n")
                .append(payload + "\n")
                .toString();
            try {
                connection.send(data);
            } catch (SocketException e) {
                System.err.println("Connection closed: " + e.getMessage());
            } catch (EOFException e) {
                System.err.println("Connection terminated by remote peer");
            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            }
        }
    }

    private void processData(String data) {
        try {
            // parse data content
            Scanner scanner = new Scanner(data);
            String sender = readField(scanner, "Sender");
            String receiver = readField(scanner, "Receiver");
            String type = readField(scanner, "Type");
            String payload = readField(scanner, "Payload");

            // dispatch operation based on type parameter
            handleRequest(sender, receiver, Configuration.DataType.valueOf(type), payload);
        } catch (ChatProtocolException error) {
            System.err.println("Error while processing data: " + error.getMessage());
            sendData(USER_NONE, userName, Configuration.DataType.ERROR.toString(), error.getMessage());
        }
    }

    private static String readField(Scanner scanner, String fieldName) throws ChatProtocolException {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        } else {
            throw new ChatProtocolException(fieldName + " not found");
        }
    }

    private void handleRequest(String sender, String receiver, Configuration.DataType dataType, String payload) throws ChatProtocolException {
        switch (dataType) {
            case CONNECT -> handleConnect(sender);
            case CONFIRM -> handleConfirm();
            case DISCONNECT -> handleDisconnect();
            case MESSAGE -> handleMessage(sender, receiver, payload);
            case ERROR -> handleError(sender, payload);
            default -> handleDefault(dataType);
        }
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleConnect(String sender) throws ChatProtocolException {
        if (this.protocolState != Configuration.ProtocolState.NEW) {
            throw new ChatProtocolException("Illegal state for connect request: " + protocolState);
        }
        if (sender == null || sender.isBlank()) {
            sender = this.userName;
        }
        if (connectionRegistry.containsKey(sender)) {
            throw new ChatProtocolException("User name already taken: " + sender);
        }
        this.userName = sender;
        connectionRegistry.put(userName, this);
        sendData(USER_NONE, userName, Configuration.DataType.CONFIRM.toString(), "Registration successfull for " + userName);
        this.protocolState = Configuration.ProtocolState.CONNECTED;
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleConfirm() {
        System.out.println("Not expecting to receive a CONFIRM request from client");
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleDisconnect() throws ChatProtocolException {
        if (protocolState == Configuration.ProtocolState.DISCONNECTED) {
            throw new ChatProtocolException("Illegal state for disconnect request: " + protocolState);
        }
        if (protocolState == Configuration.ProtocolState.CONNECTED) {
            connectionRegistry.remove(this.userName);
        }
        sendData(USER_NONE, userName, Configuration.DataType.CONFIRM.toString(), "Confirm disconnect of " + userName);
        this.protocolState = Configuration.ProtocolState.DISCONNECTED;
        this.stopReceiving();
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleMessage(String sender, String receiver, String payload) throws ChatProtocolException {
        if (protocolState != Configuration.ProtocolState.CONNECTED) {
            throw new ChatProtocolException("Illegal state for message request: " + protocolState);
        }
        if (USER_ALL.equals(receiver)) {
            for (ServerConnectionHandler handler : connectionRegistry.values()) {
                handler.sendData(sender, receiver, Configuration.DataType.MESSAGE.toString(), payload);
            }
        } else {
            ServerConnectionHandler handler = connectionRegistry.get(receiver);
            if (handler != null) {
                handler.sendData(sender, receiver, Configuration.DataType.MESSAGE.toString(), payload);
            } else {
                this.sendData(USER_NONE, userName, Configuration.DataType.ERROR.toString(), "Unknown User: " + receiver);
            }
        }
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleError(String sender, String payload) {
        System.out.println("Received error from client (" + sender + "): " + payload);
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleDefault(Configuration.DataType dataType) {
        System.out.println("Unknown data type received: " + dataType);
    }
}
