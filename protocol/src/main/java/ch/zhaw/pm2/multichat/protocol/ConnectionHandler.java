package ch.zhaw.pm2.multichat.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import static ch.zhaw.pm2.multichat.protocol.Configuration.DataType.ERROR;
import static ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState.NEW;

public abstract class ConnectionHandler {
    public static final String USER_ALL = "*";
    protected NetworkHandler.NetworkConnection<String> connection;
    protected Configuration.ProtocolState protocolState = NEW;
    protected static final String USER_NONE = "";
    protected String userName = USER_NONE;

    protected ConnectionHandler(NetworkHandler.NetworkConnection<String> connection) {
        this.connection = connection;
    }

    public String getUserName() {
        return this.userName;
    }

    public Configuration.ProtocolState getState() {
        return protocolState;
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
                String data = connection.receive();
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

    protected void processData(String data) {
        try {
            Scanner scanner = new Scanner(data);
            String sender = readField(scanner, "Sender");
            String receiver = readField(scanner, "Receiver");
            String type = readField(scanner, "Type");
            String payload = readField(scanner, "Payload");
            handleRequest(sender, receiver, Configuration.DataType.valueOf(type), payload);
        } catch (ChatProtocolException error) {
            System.err.println("Error while processing data: " + error.getMessage());
            sendData(USER_NONE, userName, ERROR.toString(), error.getMessage());
        }
    }

    protected void sendData(String sender, String receiver, String type, String payload) {
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

    protected abstract void handleConnect(String sender) throws ChatProtocolException;

    protected abstract void handleConfirm(String receiver, String payload);

    protected abstract void handleDisconnect(String payload) throws ChatProtocolException;

    protected abstract void handleMessage(String sender, String receiver, String payload) throws ChatProtocolException;

    protected abstract void handleError(String sender, String payload);

    protected abstract void handleDefault(Configuration.DataType type);

    protected abstract void onInterrupted();

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
            case CONFIRM -> handleConfirm(receiver, payload);
            case DISCONNECT -> handleDisconnect(payload);
            case MESSAGE -> handleMessage(sender, receiver, payload);
            case ERROR -> handleError(sender, payload);
            default -> handleDefault(dataType);
        }
    }
}
