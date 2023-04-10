package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Configuration;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType.*;
import static ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState.*;

//Todo: write javadoc

/**
 * This class handles the communication with the server
 */

public class ClientConnectionHandler implements Runnable {
    //TODO: (Strukturell) Code Duplikation in ServerConnectionHandler und ClientConnectionHandler, Superklasse ConnectionHandler schreiben
    /**
     * The network connection to be used for receiving and sending requests
     */
    private final NetworkHandler.NetworkConnection<String> connection;

    private final ChatWindowController controller;

    public static final String USER_NONE = "";
    public static final String USER_ALL = "*";

    /**
     * The username associated with this connection.
     */
    private String userName = USER_NONE;

    /**
     * The current state of this connection
     */
    private Configuration.ProtocolState protocolState = NEW;

    //TODO: (Strukturell) Observer Pattern verwenden statt Controller als Parameter
    public ClientConnectionHandler(NetworkHandler.NetworkConnection<String> connection,
                                   String userName,
                                   ChatWindowController controller) {
        this.connection = connection;
        //TODO: (Funktional) Wenn kein Username angegeben, dann Anonymous-<Nr> verwenden
        this.userName = (userName == null || userName.isBlank()) ? USER_NONE : userName;
        this.controller = controller;
    }

    public Configuration.ProtocolState getState() {
        return this.protocolState;
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

    private void startReceiving() {
        try {
            System.out.println("Start receiving data...");
            //TODO: (Funktional) Separater Thread für das Warten auf neue Nachrichten, rest der Applikation blockiert
            while (connection.isAvailable()) {
                String data = connection.receive();
                processData(data);
            }
        } catch (SocketException e) {
            System.out.println("Connection terminated locally");
            this.setState(DISCONNECTED);
            System.out.println("Unregistered because connection terminated" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("Connection terminated by remote peer");
            this.setState(DISCONNECTED);
            System.out.println("Unregistered because connection terminated" + e.getMessage());
        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Received object of unknown type: " + e.getMessage());
        }
    }

    /**
     * Stop receiving packages from the network connection, by closing the connection.
     */
    private void stopReceiving() {
        try {
            System.out.println("Stop receiving data...");
            connection.close();
            System.out.println("Stopped receiving data.");
        } catch (IOException e) {
            System.err.println("Failed to close connection." + e.getMessage());
        }
    }


    //Todo: in kleinere Methoden unterteilen, extrem lang

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
            sendData(USER_NONE, userName, ERROR.toString(), error.getMessage());
        }
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

    private static String readField(Scanner scanner, String fieldName) throws ChatProtocolException {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        } else {
            throw new ChatProtocolException(fieldName + " not found");
        }
    }

    private void handleRequest(String sender, String receiver, Configuration.DataType dataType, String payload) throws ChatProtocolException {
        switch (dataType) {
            case CONNECT -> handleConnect();
            case CONFIRM -> handleConfirm(receiver, payload);
            case DISCONNECT -> handleDisconnect(payload);
            case MESSAGE -> handleMessage(sender, receiver, payload);
            case ERROR -> handleError(payload);
            default -> handleDefault(dataType);
        }
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleConnect() {
        System.err.println("Illegal connect request from server");
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleConfirm(String receiver, String payload) {
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

    // ToDo: Overwrite when implementing issue #28
    private void handleDisconnect(String payload) {
        if (protocolState == DISCONNECTED) {
            System.out.println("DISCONNECT: Already in disconnected: " + payload);
            return;
        }
        controller.addInfo(payload);
        System.out.println("DISCONNECT: " + payload);
        this.setState(DISCONNECTED);
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleMessage(String sender, String receiver, String payload) {
        if (protocolState != CONNECTED) {
            System.out.println("MESSAGE: Illegal state " + protocolState + " for message: " + payload);
            return;
        }
        controller.addMessage(sender, receiver, payload);
        System.out.println("MESSAGE: From " + sender + " to " + receiver + ": " + payload);
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleError(String payload) {
        controller.addError(payload);
        System.out.println("ERROR: " + payload);
    }

    // ToDo: Overwrite when implementing issue #28
    private void handleDefault(Configuration.DataType type) {
        System.out.println("Unknown data type received: " + type);
    }
}
