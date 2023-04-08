package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.client.ClientConnectionHandler.State;
import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
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

import static ch.zhaw.pm2.multichat.client.ClientConnectionHandler.State.*;

public class ChatWindowController {
    private final Pattern messagePattern = Pattern.compile( "^(?:@(\\w*))?\\s*(.*)$" );//Todo: What does it? needs a comment
    private ClientConnectionHandler connectionHandler;//Todo: (Funktionaler Fehler)  connectionHandler is null
    private ClientMessageList messages;

    private final WindowCloseHandler windowCloseHandler = new WindowCloseHandler();

    @FXML private Pane rootPane;
    @FXML private TextField serverAddressField;
    @FXML private TextField serverPortField;
    @FXML private TextField userNameField;
    @FXML private TextField messageField;
    @FXML private TextArea messageArea;
    @FXML private Button connectButton;
    @FXML private Button sendButton; //Todo: (Funktionaler Fehler) send button does nothing
    @FXML private TextField filterValue;

    //Todo:Write javadoc
    //Todo: public Methoden zuerst, erst dann private
    @FXML
    public void initialize() {
        blockUserInterface(true);
        serverAddressField.setText(NetworkHandler.DEFAULT_ADDRESS.getCanonicalHostName());
        serverPortField.setText(String.valueOf(NetworkHandler.DEFAULT_PORT));
        stateChanged(NEW);
        messages = new ClientMessageList(this);
    }

    private void applicationClose() {
        connectionHandler.setState(DISCONNECTED);
    }

    @FXML
    private void toggleConnection () {
        if (connectionHandler == null || connectionHandler.getState() != CONNECTED) {
            connect();
        } else {
            disconnect();
        }
    }

    /**
     * Activates/Deactivates the UI elements.
     * @param isBlocked true to block the UI, false to unblock it.
     */
    private void blockUserInterface(boolean isBlocked) {
        messageField.setDisable(isBlocked);
        sendButton.setDisable(isBlocked);
        filterValue.setDisable(isBlocked);
    }

    private void connect() {
        try {
            messages = new ClientMessageList(this); // clear message list
            startConnectionHandler();
            connectionHandler.connect();
            blockUserInterface(false);
        } catch(ChatProtocolException | IOException e) {
            writeError(e.getMessage());
        }
    }

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
            if (receiver == null || receiver.isBlank()) receiver = ClientConnectionHandler.USER_ALL;
            try {
                connectionHandler.message(receiver, message);
            } catch (ChatProtocolException e) {
                writeError(e.getMessage());
            }
        } else {
            writeError("Not a valid message format.");
        }
    }

    @FXML
    private void applyFilter( ) {
    	this.redrawMessageList();
    }
    //Todo: (funktionaler Fehler) should be called to create the connection handler
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
    //Todo;javadoc
    private void terminateConnectionHandler() {
        // unregister window close handler
        rootPane.getScene().getWindow().removeEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseHandler);
        if (connectionHandler != null) {
            connectionHandler.terminate();
            connectionHandler = null;
        }
    }
    //Todo;javadoc
    public void stateChanged(State newState) {
        // update UI (need to be run in UI thread: see Platform.runLater())
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                connectButton.setText((newState == CONNECTED || newState == CONFIRM_DISCONNECT) ? "Disconnect" : "Connect");
            }
        });
        if (newState == DISCONNECTED) {
            terminateConnectionHandler();
        }
    }
    //Todo;javadoc
    public void setUserName(String userName) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userNameField.setText(userName);
            }
        });
    }
    //Todo;javadoc
    public void setServerAddress(String serverAddress) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverAddressField.setText(serverAddress);
            }
        });
    }
    //Todo;javadoc
    public void setServerPort(int serverPort) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverPortField.setText(Integer.toString(serverPort));
            }
        });
    }
    //Todo;javadoc
    public void addMessage(String sender, String receiver, String message) {
        messages.addMessage(ClientMessageList.MessageType.MESSAGE, sender, receiver, message);
        this.redrawMessageList();
    }
//Todo;javadoc
    public void addInfo(String message) {
        messages.addMessage(ClientMessageList.MessageType.INFO, null, null, message);
        this.redrawMessageList();
    }
    //Todo;javadoc
    public void addError(String message) {
        messages.addMessage(ClientMessageList.MessageType.ERROR, null, null, message);
        this.redrawMessageList();
    }
    //Todo;javadoc
    public void clearMessageArea() {
        this.messageArea.clear();
    }


    private void redrawMessageList() {
        Platform.runLater(() -> messages.writeFilteredMessages(filterValue.getText().strip()));
    }

    public void writeError(String message) {
        this.messageArea.appendText(String.format("[ERROR] %s\n", message));
    }

    public void writeInfo(String message) {
        this.messageArea.appendText(String.format("[INFO] %s\n", message));
    }

    public void writeMessage(String sender, String reciever, String message) {
        this.messageArea.appendText(String.format("[%s -> %s] %s\n", sender, reciever, message));
    }


    class WindowCloseHandler implements EventHandler<WindowEvent> {
        public void handle(WindowEvent event) {
            applicationClose();
        }

    }




}
