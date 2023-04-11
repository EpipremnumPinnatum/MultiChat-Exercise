package ch.zhaw.pm2.multichat.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType.ERROR;
import static ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState.NEW;

/**
 * The ConnectionHandler abstract class provides a base implementation for handling network connections with the server.
 * It provides methods to start and stop receiving data, send data to the server, and handle different types of requests.
 */
public abstract class ConnectionHandler {
    /**
     * The special username to send a message to all users.
     */
    public static final String USER_ALL = "*";

    /**
     * The network connection for this connection handler.
     */
    protected NetworkHandler.NetworkConnection<NetworkMessage> connection;

    /**
     * The current protocol state of this connection handler.
     */
    protected Configuration.ProtocolState protocolState = NEW;

    /**
     * The default username.
     */
    protected static final String USER_NONE = "";

    /**
     * The username for this connection handler.
     */
    protected String userName = USER_NONE;

    /**
     * Returns the username for this connection handler.
     *
     * @return the username for this connection handler
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Returns the current protocol state of this connection handler.
     *
     * @return the current protocol state of this connection handler
     */
    public Configuration.ProtocolState getState() {
        return protocolState;
    }

    /**
     * Constructs a new ConnectionHandler instance with the specified network connection.
     *
     * @param connection the network connection for this connection handler
     */
    protected ConnectionHandler(NetworkHandler.NetworkConnection<NetworkMessage> connection) {
        this.connection = connection;
    }

    /**
     * Start receiving packages from the network connection.
     * It continuously receives packages from the network connection and processes it depending on the package type
     */
    protected void startReceiving() {
        try {
            System.out.println("Start receiving data...");
            //TODO: (Funktional) Separater Thread für das Warten auf neue Nachrichten, rest der Applikation blockiert
            while (connection.isAvailable()) {
                NetworkMessage data = connection.receive();
                processData(data);
            }
        } catch (SocketException e) {
            System.out.println("Connection terminated locally");
            onInterrupted();
            System.out.println("Unregistered because connection terminated" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("Connection terminated by remote peer");
            onInterrupted();
            System.out.println("Unregistered because connection terminated" + e.getMessage());
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
    protected void stopReceiving() {
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

    /**
     * Processes the received network message.
     *
     * @param data the received network message
     */
    protected void processData(NetworkMessage data) {
        try {
            handleRequest(data);
        } catch (ChatProtocolException error) {
            System.err.println("Error while processing data: " + error.getMessage());
            sendData(USER_NONE, userName, ERROR, error.getMessage());
        }
    }

    /**
     * This method sends a NetworkMessage to the connected NetworkConnection if it is available.
     *
     * @param sender   The sender of the message
     * @param receiver The receiver of the message
     * @param type     The DataType of the message
     * @param payload  The message payload
     */
    protected void sendData(String sender, String receiver, Configuration.DataType type, String payload) {
        if (connection.isAvailable()) {
            try {
                connection.send(new NetworkMessage(sender, receiver, type, payload));
            } catch (SocketException e) {
                System.err.println("Connection closed: " + e.getMessage());
            } catch (EOFException e) {
                System.err.println("Connection terminated by remote peer");
            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            }
        }
    }

    /**
     * Handle the CONNECT request received from a client.
     *
     * @param sender The sender of the CONNECT request
     * @throws ChatProtocolException if an error occurs while handling the request
     */
    protected abstract void handleConnect(String sender) throws ChatProtocolException;

    /**
     * Handle the CONFIRM request received from a client.
     *
     * @param receiver The receiver of the CONFIRM request
     * @param payload  The payload of the CONFIRM request
     */
    protected abstract void handleConfirm(String receiver, String payload);

    /**
     * Handle the DISCONNECT request received from a client.
     *
     * @param payload The payload of the DISCONNECT request
     * @throws ChatProtocolException if an error occurs while handling the request
     */
    protected abstract void handleDisconnect(String payload) throws ChatProtocolException;

    /**
     * Handle the MESSAGE request received from a client.
     *
     * @param sender   The sender of the MESSAGE request
     * @param receiver The receiver of the MESSAGE request
     * @param payload  The payload of the MESSAGE request
     * @throws ChatProtocolException if an error occurs while handling the request
     */
    protected abstract void handleMessage(String sender, String receiver, String payload) throws ChatProtocolException;

    /**
     * Handle the ERROR request received from a client.
     *
     * @param sender  The sender of the ERROR request
     * @param payload The payload of the ERROR request
     */
    protected abstract void handleError(String sender, String payload);

    /**
     * Handle the request for a DataType that is not recognized.
     *
     * @param type The unrecognized DataType
     */
    protected abstract void handleDefault(Configuration.DataType type);

    /**
     * Handle the interruption of the connection.
     */
    protected abstract void onInterrupted();

    /**
     * This method handles a request by checking the DataType of the received NetworkMessage and calling the appropriate
     * handle method.
     *
     * @param data The received NetworkMessage
     * @throws ChatProtocolException if an error occurs while handling the request
     */
    private void handleRequest(NetworkMessage data) throws ChatProtocolException {
        switch (data.getType()) {
            case CONNECT -> handleConnect(data.getSender());
            case CONFIRM -> handleConfirm(data.getReceiver(), data.getPayload());
            case DISCONNECT -> handleDisconnect(data.getPayload());
            case MESSAGE -> handleMessage(data.getSender(), data.getReceiver(), data.getPayload());
            case ERROR -> handleError(data.getSender(), data.getPayload());
            default -> handleDefault(data.getType());
        }
    }
}
