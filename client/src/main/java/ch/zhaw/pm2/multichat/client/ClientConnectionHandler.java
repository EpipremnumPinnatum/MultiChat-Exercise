package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.*;

import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType;
import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType.*;
import static ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState.*;

/**
 * This class handles the communication with the server
 */
public class ClientConnectionHandler extends ConnectionHandler implements Runnable {
    private final ChatWindowController controller;

    //TODO: (Strukturell) Observer Pattern verwenden statt Controller als Parameter

    /**
     * Constructs a new ClientConnectionHandler.
     *
     * @param connection the network connection
     * @param userName   the username
     * @param controller the ChatWindowController instance
     */
    public ClientConnectionHandler(NetworkHandler.NetworkConnection<NetworkMessage> connection,
                                   String userName,
                                   ChatWindowController controller) {
        super(connection);
        this.userName = (userName == null || userName.isBlank()) ? USER_NONE : userName;
        this.controller = controller;
    }

    /**
     * Sets the current protocol state.
     *
     * @param newProtocolState the new protocol state
     */
    public void setState(Configuration.ProtocolState newProtocolState) {
        this.protocolState = newProtocolState;
        controller.stateChanged(newProtocolState);
    }

    /**
     * Terminates the Connection Handler by closing the connection to not receive any more messages.
     */
    public void terminate() {
        System.out.println("Closing Connection Handler to Server");
        stopReceiving();
        System.out.println("Closed Connection Handler to Server");
    }

    /**
     * Connects to the server.
     *
     * @throws ChatProtocolException if the current protocol state is not NEW
     */
    public void connect() throws ChatProtocolException {
        if (protocolState != NEW) throw new ChatProtocolException("Illegal state for connect: " + protocolState);
        this.sendData(userName, USER_NONE, CONNECT, null);
        this.setState(CONFIRM_CONNECT);
    }

    /**
     * Disconnects from the server.
     *
     * @throws ChatProtocolException if the current protocol state is not NEW or CONNECTED
     */
    public void disconnect() throws ChatProtocolException {
        if (protocolState != NEW && protocolState != CONNECTED)
            throw new ChatProtocolException("Illegal state for disconnect: " + protocolState);
        this.sendData(userName, USER_NONE, DISCONNECT, null);
        this.setState(CONFIRM_DISCONNECT);
    }

    /**
     * Sends a message to the specified receiver.
     *
     * @param receiver the receiver of the message
     * @param message  the message to send
     * @throws ChatProtocolException if the current protocol state is not CONNECTED
     */
    public void message(String receiver, String message) throws ChatProtocolException {
        if (protocolState != CONNECTED) throw new ChatProtocolException("Illegal state for message: " + protocolState);
        this.sendData(userName, receiver, MESSAGE, message);
    }

    /**
     * Start the connection handler.
     * It will start listening for incoming messages from the server and process them.
     */
    @Override
    public void run() {
        System.out.println("Starting Connection Handler");
        startReceiving();
        System.out.println("Ended Connection Handler");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleConnect(String sender) {
        System.err.println("Illegal connect request from server");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleConfirm(String receiver, String payload) {
        if (protocolState == CONFIRM_CONNECT) {
            this.userName = receiver;
            controller.setUserName(userName);
            controller.setServerPort(connection.getRemotePort());
            controller.setServerAddress(connection.getRemoteHost());
            controller.addInfo(payload);
            System.out.println("CONFIRM: " + payload);
            this.setState(CONNECTED);
        } else if (protocolState == CONFIRM_DISCONNECT) {
            controller.addInfo(payload);
            System.out.println("CONFIRM: " + payload);
            this.setState(DISCONNECTED);
        } else {
            System.err.println("Got unexpected confirm message: " + payload);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleDisconnect(String payload) {
        if (protocolState == DISCONNECTED) {
            System.out.println("DISCONNECT: Already in disconnected: " + payload);
            return;
        }
        controller.addInfo(payload);
        System.out.println("DISCONNECT: " + payload);
        this.setState(DISCONNECTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleMessage(String sender, String receiver, String payload) {
        if (protocolState != CONNECTED) {
            System.out.println("MESSAGE: Illegal state " + protocolState + " for message: " + payload);
            return;
        }
        controller.addMessage(sender, receiver, payload);
        System.out.println("MESSAGE: From " + sender + " to " + receiver + ": " + payload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleError(String sender, String payload) {
        controller.addError(payload);
        System.out.println("ERROR: " + payload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleDefault(DataType type) {
        System.out.println("Unknown data type received: " + type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInterrupted() {
        controller.addError("Connection to server lost");
        controller.stateChanged(DISCONNECTED);
    }
}
