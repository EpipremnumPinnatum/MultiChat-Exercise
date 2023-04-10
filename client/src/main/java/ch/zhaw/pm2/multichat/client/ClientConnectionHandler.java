package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Configuration;
import ch.zhaw.pm2.multichat.protocol.ConnectionHandler;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType.*;
import static ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState.*;

//Todo: write javadoc

/**
 * This class handles the communication with the server
 */

public class ClientConnectionHandler extends ConnectionHandler implements Runnable {
    private final ChatWindowController controller;

    //TODO: (Strukturell) Observer Pattern verwenden statt Controller als Parameter
    public ClientConnectionHandler(NetworkHandler.NetworkConnection<String> connection,
                                   String userName,
                                   ChatWindowController controller) {
        super(connection);
        this.userName = (userName == null || userName.isBlank()) ? USER_NONE : userName;
        this.controller = controller;
    }

    public void setState(Configuration.ProtocolState newProtocolState) {
        this.protocolState = newProtocolState;
        controller.stateChanged(newProtocolState);
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
     * Terminate the Connection Handler by closing the connection to not receive any more messages.
     */
    public void terminate() {
        System.out.println("Closing Connection Handler to Server");
        stopReceiving();
        System.out.println("Closed Connection Handler to Server");
    }

    public void connect() throws ChatProtocolException {
        if (protocolState != NEW) throw new ChatProtocolException("Illegal state for connect: " + protocolState);
        this.sendData(userName, USER_NONE, CONNECT.toString(), null);
        this.setState(CONFIRM_CONNECT);
    }

    public void disconnect() throws ChatProtocolException {
        if (protocolState != NEW && protocolState != CONNECTED)
            throw new ChatProtocolException("Illegal state for disconnect: " + protocolState);
        this.sendData(userName, USER_NONE, DISCONNECT.toString(), null);
        this.setState(CONFIRM_DISCONNECT);
    }

    public void message(String receiver, String message) throws ChatProtocolException {
        if (protocolState != CONNECTED) throw new ChatProtocolException("Illegal state for message: " + protocolState);
        this.sendData(userName, receiver, MESSAGE.toString(), message);
    }

    @Override
    protected void handleConnect(String sender) {
        System.err.println("Illegal connect request from server");
    }

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

    @Override
    protected void handleMessage(String sender, String receiver, String payload) {
        if (protocolState != CONNECTED) {
            System.out.println("MESSAGE: Illegal state " + protocolState + " for message: " + payload);
            return;
        }
        controller.addMessage(sender, receiver, payload);
        System.out.println("MESSAGE: From " + sender + " to " + receiver + ": " + payload);
    }

    @Override
    protected void handleError(String sender, String payload) {
        controller.addError(payload);
        System.out.println("ERROR: " + payload);
    }

    @Override
    protected void handleDefault(Configuration.DataType type) {
        System.out.println("Unknown data type received: " + type);
    }

    @Override
    protected void onInterrupted() {
        controller.addError("Connection to server lost");
        controller.disconnect();
    }
}
