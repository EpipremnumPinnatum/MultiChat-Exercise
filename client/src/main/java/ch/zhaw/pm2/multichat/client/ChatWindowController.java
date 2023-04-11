package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Configuration;
import ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState;
import ch.zhaw.pm2.multichat.protocol.ConnectionHandler;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.zhaw.pm2.multichat.protocol.Configuration.ProtocolState.*;

/**
 * The ChatWindowController class is a controller for the MultiChat client user interface.
 * It handles the connection to the server and the sending and receiving of messages between clients.
 */
public class ChatWindowController {
    /**
     * A pattern to extract the recipient and message from a string. If the string starts with "@recipient", it is
     * assumed that the message is directed to the recipient.
     */
    private final Pattern messagePattern = Pattern.compile("^(?:@(\\w*))?\\s*(.*)$");//Todo: What does it? needs a comment

    /**
     * The connection handler for this client.
     */
    private ClientConnectionHandler connectionHandler;

    /**
     * The list of messages for this client.
     */
    private ClientMessageList messages;

    /**
     * The handler for the window close event.
     */
    private final WindowCloseHandler windowCloseHandler = new WindowCloseHandler();

    @FXML
    private Pane rootPane;
    @FXML
    private TextField serverAddressField;
    @FXML
    private TextField serverPortField;
    @FXML
    private TextField userNameField;
    @FXML
    private TextField messageField;
    @FXML
    private TextArea messageArea;
    @FXML
    private Button connectButton;
    @FXML
    private Button sendButton;
    @FXML
    private TextField filterValue;

    /**
     * Sets up interface to make it functional for the user.
     */
    @FXML
    public void initialize() {
        blockUserInterface(true);
        serverAddressField.setText(NetworkHandler.DEFAULT_ADDRESS.getCanonicalHostName());
        serverPortField.setText(String.valueOf(NetworkHandler.DEFAULT_PORT));
        stateChanged(NEW);
        messages = new ClientMessageList(this);
    }

    /**
     * Updates the User Interface. Informs the user, of the current state of the protocol.
     *
     * @param newProtocolState uses the enumerator that describes the protocol state
     */
    public void stateChanged(ProtocolState newProtocolState) {
        // update UI (need to be run in UI thread: see Platform.runLater())
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                connectButton.setText((newProtocolState == CONNECTED || newProtocolState == CONFIRM_DISCONNECT) ? "Disconnect" : "Connect");
            }
        });
        if (newProtocolState == DISCONNECTED) {
            blockUserInterface(true);
            terminateConnectionHandler();
        }
    }

    /**
     * Sets the username in the UI.
     *
     * @param userName the username to set
     */
    public void setUserName(String userName) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userNameField.setText(userName);
            }
        });
    }

    /**
     * Sets the server address in the UI.
     *
     * @param serverAddress the server address to set
     */
    public void setServerAddress(String serverAddress) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverAddressField.setText(serverAddress);
            }
        });
    }

    /**
     * Sets the port number of the server.
     *
     * @param serverPort the port number of the server.
     */
    public void setServerPort(int serverPort) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverPortField.setText(Integer.toString(serverPort));
            }
        });
    }

    /**
     * Adds a message to the message list and redraws the message list.
     *
     * @param sender   the sender of the message.
     * @param receiver the receiver of the message.
     * @param message  the message content.
     */
    public void addMessage(String sender, String receiver, String message) {
        messages.addMessage(Configuration.MessageType.MESSAGE, sender, receiver, message);
        this.redrawMessageList();
    }

    /**
     * Adds an informational message to the message list and redraws the message list.
     *
     * @param message the message content.
     */
    public void addInfo(String message) {
        messages.addMessage(Configuration.MessageType.INFO, null, null, message);
        this.redrawMessageList();
    }

    /**
     * Adds an error message to the message list and redraws the message list.
     *
     * @param message the message content.
     */
    public void addError(String message) {
        messages.addMessage(Configuration.MessageType.ERROR, null, null, message);
        this.redrawMessageList();
    }

    /**
     * Writes an error message to the message area.
     *
     * @param message the message content.
     */
    public void writeError(String message) {
        this.messageArea.appendText(String.format("[ERROR] %s\n", message));
    }

    /**
     * Writes an informational message to the message area.
     *
     * @param message the message content.
     */
    public void writeInfo(String message) {
        this.messageArea.appendText(String.format("[INFO] %s\n", message));
    }

    /**
     * Writes a message to the message area.
     *
     * @param sender   the sender of the message.
     * @param receiver the receiver of the message.
     * @param message  the message content.
     */
    public void writeMessage(String sender, String receiver, String message) {
        this.messageArea.appendText(String.format("[%s -> %s] %s\n", sender, receiver, message));
    }

    /**
     * Clears the message area.
     */
    public void clearMessageArea() {
        this.messageArea.clear();
    }

    /**
     * Handles the window close event.
     */
    private void applicationClose() {
        connectionHandler.setState(DISCONNECTED);
    }

    /**
     * Activates/Deactivates the UI elements.
     *
     * @param isBlocked true to block the UI, false to unblock it.
     */
    private void blockUserInterface(boolean isBlocked) {
        messageField.setDisable(isBlocked);
        sendButton.setDisable(isBlocked);
        filterValue.setDisable(isBlocked);
    }

    /**
     * Connects to the server.
     */
    private void connect() {
        try {
            messages = new ClientMessageList(this); // clear message list
            startConnectionHandler();
            connectionHandler.connect();
            blockUserInterface(false);
            messageField.requestFocus();
        } catch (ChatProtocolException | IOException e) {
            writeError(e.getMessage());
        }
    }

    /**
     * Disconnects from the server.
     */
    private void disconnect() {
        if (connectionHandler == null) {
            writeError("No connection handler");
            return;
        }
        try {
            connectionHandler.disconnect();
            blockUserInterface(true);
        } catch (ChatProtocolException e) {
            writeError(e.getMessage());
        }
    }

    /**
     * Starts a new client connection handler with the specified user name, server address, and server port.
     * Creates a new thread for the connection handler and registers a window close handler.
     *
     * @throws IOException if an I/O error occurs when opening the connection
     */
    private void startConnectionHandler() throws IOException {
        String userName = userNameField.getText();
        String serverAddress = serverAddressField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());
        connectionHandler = new ClientConnectionHandler(
            NetworkHandler.openConnection(serverAddress, serverPort), userName,
            this);
        new Thread(connectionHandler).start();

        // register window close handler
        rootPane.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseHandler);
    }

    /**
     * Unregisters the window close handler and terminates the client connection handler.
     */
    private void terminateConnectionHandler() {
        // unregister window close handler
        rootPane.getScene().getWindow().removeEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseHandler);
        if (connectionHandler != null) {
            connectionHandler.terminate();
            connectionHandler = null;
        }
    }

    /**
     * Redraws the message list on the UI thread.
     */
    private void redrawMessageList() {
        Platform.runLater(() -> messages.writeFilteredMessages(filterValue.getText().strip()));
    }

    /**
     * Toggles the connection status of the client. If the client is not connected, initiates a connection.
     * Otherwise, terminates the connection.
     */
    @FXML
    private void toggleConnection() {
        if (connectionHandler == null || connectionHandler.getState() != CONNECTED) {
            connect();
        } else {
            disconnect();
        }
    }

    /**
     * Sends a message to the server using the current connection handler.
     * Validates the message format and handles any protocol exceptions.
     */
    @FXML
    private void message() {
        if (connectionHandler == null) {
            writeError("No connection handler");
            return;
        }
        String messageString = messageField.getText().strip();
        Matcher matcher = messagePattern.matcher(messageString);
        if (matcher.find()) {
            String receiver = matcher.group(1);
            String message = matcher.group(2);
            if (receiver == null || receiver.isBlank()) receiver = ConnectionHandler.USER_ALL;
            try {
                connectionHandler.message(receiver, message);
                messageField.clear();
                messageField.requestFocus();
            } catch (ChatProtocolException e) {
                writeError(e.getMessage());
            }
        } else {
            writeError("Not a valid message format.");
        }
    }

    /**
     * Applies the current filter value to the message list.
     */
    @FXML
    private void applyFilter() {
        this.redrawMessageList();
    }

    /**
     * Inner class for the event handler that closes the application window. Terminates the connection handler.
     */
    class WindowCloseHandler implements EventHandler<WindowEvent> {
        public void handle(WindowEvent event) {
            applicationClose();
        }
    }
}
