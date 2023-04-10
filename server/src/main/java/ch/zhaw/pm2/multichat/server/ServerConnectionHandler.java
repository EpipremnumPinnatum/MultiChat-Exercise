package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Configuration;
import ch.zhaw.pm2.multichat.protocol.ConnectionHandler;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerConnectionHandler extends ConnectionHandler implements Runnable {
    /**
     * Global counter to generate connection IDs
     */
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * Reference to the registry managing all connections
     */
    private final Map<String, ServerConnectionHandler> connectionRegistry;

    @Override
    public void run() {
        startReceiving();
    }

    public ServerConnectionHandler(NetworkHandler.NetworkConnection<String> connection,
                                   Map<String, ServerConnectionHandler> registry) {
        super(connection);
        Objects.requireNonNull(connection, "Connection must not be null");
        Objects.requireNonNull(registry, "Registry must not be null");
        this.connectionRegistry = registry;
        userName = "Anonymous-" + connectionCounter.incrementAndGet();
    }

    @Override
    protected void handleConnect(String sender) throws ChatProtocolException {
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
        sendData(USER_NONE, userName, Configuration.DataType.CONFIRM.toString(), "Registration successful for " + userName);
        this.protocolState = Configuration.ProtocolState.CONNECTED;
    }

    @Override
    protected void handleConfirm(String receiver, String payload) {
        System.out.println("Not expecting to receive a CONFIRM request from client");
    }

    @Override
    protected void handleDisconnect(String payload) throws ChatProtocolException {
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

    @Override
    protected void handleMessage(String sender, String receiver, String payload) throws ChatProtocolException {
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

    @Override
    protected void handleError(String sender, String payload) {
        System.out.println("Received error from client (" + sender + "): " + payload);
    }

    @Override
    protected void handleDefault(Configuration.DataType dataType) {
        System.out.println("Unknown data type received: " + dataType);
    }

    @Override
    protected void onInterrupted() {
        connectionRegistry.remove(userName);
    }
}
