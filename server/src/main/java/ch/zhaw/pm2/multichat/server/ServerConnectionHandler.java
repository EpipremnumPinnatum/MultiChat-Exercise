package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.ConnectionHandler;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;
import ch.zhaw.pm2.multichat.protocol.NetworkMessage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType;
import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType.*;
import static ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState.*;

/**
 * This class represents the server-side connection handler for the chat application.
 * <p>
 * It extends the ConnectionHandler class and implements the Runnable interface.
 * It manages the network connection with the clients and the communication protocol between them.
 * It also keeps track of all connected clients using a registry.
 */
public class ServerConnectionHandler extends ConnectionHandler implements Runnable {
    /**
     * Global counter to generate connection IDs
     */
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * Reference to the registry managing all connections
     */
    private final Map<String, ServerConnectionHandler> connectionRegistry;

    /**
     * Constructor for ServerConnectionHandler.
     * Initializes the ConnectionHandler superclass and sets the registry and username.
     *
     * @param connection the network connection to be managed
     * @param registry   the registry managing all connections
     * @throws NullPointerException if the connection or registry is null
     */
    public ServerConnectionHandler(NetworkHandler.NetworkConnection<NetworkMessage> connection,
                                   Map<String, ServerConnectionHandler> registry) {
        super(connection);
        Objects.requireNonNull(connection, "Connection must not be null");
        Objects.requireNonNull(registry, "Registry must not be null");
        this.connectionRegistry = registry;
        userName = "Anonymous-" + connectionCounter.incrementAndGet();
    }

    /**
     * Starts the thread to receive messages from clients.
     */
    @Override
    public void run() {
        startReceiving();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleConnect(String sender) throws ChatProtocolException {
        if (this.protocolState != NEW) {
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
        sendData(USER_NONE, userName, CONFIRM, "Registration successful for " + userName);
        this.protocolState = CONNECTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleConfirm(String receiver, String payload) {
        System.out.println("Not expecting to receive a CONFIRM request from client");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleDisconnect(String payload) throws ChatProtocolException {
        if (protocolState == DISCONNECTED) {
            throw new ChatProtocolException("Illegal state for disconnect request: " + protocolState);
        }
        if (protocolState == CONNECTED) {
            connectionRegistry.remove(this.userName);
        }
        sendData(USER_NONE, userName, CONFIRM, "Confirm disconnect of " + userName);
        this.protocolState = DISCONNECTED;
        this.stopReceiving();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleMessage(String sender, String receiver, String payload) throws ChatProtocolException {
        if (protocolState != CONNECTED) {
            throw new ChatProtocolException("Illegal state for message request: " + protocolState);
        }
        if (USER_ALL.equals(receiver)) {
            for (ServerConnectionHandler handler : connectionRegistry.values()) {
                handler.sendData(sender, receiver, MESSAGE, payload);
            }
        } else {
            ServerConnectionHandler handler = connectionRegistry.get(receiver);
            if (handler != null) {
                handler.sendData(sender, receiver, MESSAGE, payload);
            } else {
                this.sendData(USER_NONE, userName, ERROR, "Unknown User: " + receiver);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleError(String sender, String payload) {
        System.out.println("Received error from client (" + sender + "): " + payload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleDefault(DataType dataType) {
        System.out.println("Unknown data type received: " + dataType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInterrupted() {
        connectionRegistry.remove(userName);
    }
}
