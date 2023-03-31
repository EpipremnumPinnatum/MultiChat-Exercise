package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import static ch.zhaw.pm2.multichat.client.ClientConnectionHandler.State.*;

public class ClientConnectionHandler implements Runnable {
    //TODO: (Strukturell) Code Duplikation in ServerConnectionHandler und ClientConnectionHandler, Superklasse ConnectionHandler schreiben
    /** The network connection to be used for receiving and sending requests */
    private final NetworkHandler.NetworkConnection<String> connection;

    private final ChatWindowController controller;

    //TODO: (Strukturell) DATA_TYPE als ENUM definieren

    // Data types used for the Chat Protocol
    private static final String DATA_TYPE_CONNECT = "CONNECT";
    private static final String DATA_TYPE_CONFIRM = "CONFIRM";
    private static final String DATA_TYPE_DISCONNECT = "DISCONNECT";
    private static final String DATA_TYPE_MESSAGE = "MESSAGE";
    private static final String DATA_TYPE_ERROR = "ERROR";

    public static final String USER_NONE = "";
    public static final String USER_ALL = "*";

    /** The username associated with this connection. */
    private String userName = USER_NONE;

    /** The current state of this connection */
    private State state = NEW;

    enum State {
        NEW, CONFIRM_CONNECT, CONNECTED, CONFIRM_DISCONNECT, DISCONNECTED;
    }

    //TODO: (Strukturell) Observer Pattern verwenden statt Controller als Parameter
    public ClientConnectionHandler(NetworkHandler.NetworkConnection<String> connection,
                                   String userName,
                                   ChatWindowController controller)  {
        this.connection = connection;
        //TODO: (Funktional) Wenn kein Username angegeben, dann Anonymous-<Nr> verwenden
        this.userName = (userName == null || userName.isBlank())? USER_NONE : userName;
        this.controller = controller;
    }

    public State getState() {
        return this.state;
    }

    public void setState (State newState) {
        this.state = newState;
        controller.stateChanged(newState);
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
        } catch(IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch(ClassNotFoundException e) {
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
                System.err.println("Illegal connect request from server");
            } else if (type.equals(DATA_TYPE_CONFIRM)) {
                if (state == CONFIRM_CONNECT) {
                    this.userName = reciever;
                    controller.setUserName(userName);
                    controller.setServerPort(connection.getRemotePort());
                    controller.setServerAddress(connection.getRemoteHost());
                    controller.addInfo(payload);
                    System.out.println("CONFIRM: " + payload);
                    this.setState(CONNECTED);
                } else if (state == CONFIRM_DISCONNECT) {
                    controller.addInfo(payload);
                    System.out.println("CONFIRM: " + payload);
                    this.setState(DISCONNECTED);
                } else {
                    System.err.println("Got unexpected confirm message: " + payload);
                }
            } else if (type.equals(DATA_TYPE_DISCONNECT)) {
                if (state == DISCONNECTED) {
                    System.out.println("DISCONNECT: Already in disconnected: " + payload);
                    return;
                }
                controller.addInfo(payload);
                System.out.println("DISCONNECT: " + payload);
                this.setState(DISCONNECTED);
            } else if (type.equals(DATA_TYPE_MESSAGE)) {
                if (state != CONNECTED) {
                    System.out.println("MESSAGE: Illegal state " + state + " for message: " + payload);
                    return;
                }
                controller.addMessage(sender, reciever, payload);
                System.out.println("MESSAGE: From " + sender + " to " + reciever + ": "+  payload);
            } else if (type.equals(DATA_TYPE_ERROR)) {
                controller.addError(payload);
                System.out.println("ERROR: " + payload);
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

    public void connect() throws ChatProtocolException {
        if (state != NEW) throw new ChatProtocolException("Illegal state for connect: " + state);
        this.sendData(userName, USER_NONE, DATA_TYPE_CONNECT,null);
        this.setState(CONFIRM_CONNECT);
    }

    public void disconnect() throws ChatProtocolException {
        if (state != NEW && state != CONNECTED) throw new ChatProtocolException("Illegal state for disconnect: " + state);
        this.sendData(userName, USER_NONE, DATA_TYPE_DISCONNECT,null);
        this.setState(CONFIRM_DISCONNECT);
    }

    public void message(String receiver, String message) throws ChatProtocolException {
        if (state != CONNECTED) throw new ChatProtocolException("Illegal state for message: " + state);
        this.sendData(userName, receiver, DATA_TYPE_MESSAGE,message);
    }

}
