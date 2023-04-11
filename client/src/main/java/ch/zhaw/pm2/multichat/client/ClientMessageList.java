package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ch.zhaw.pm2.multichat.protocol.Configuration.MessageType;

/**
 * This class represents a list of messages to be displayed in the chat window of the client application. It
 * contains methods to add messages to the list and to filter the list based on a user-defined string.
 */
public class ClientMessageList {
    /**
     * A list of message types.
     */
    private final List<MessageType> typeList = new ArrayList<>();

    /**
     * A list of message senders.
     */
    private final List<String> senderList = new ArrayList<>();

    /**
     * A list of message receivers.
     */
    private final List<String> receiverList = new ArrayList<>();

    /**
     * A list of messages.
     */
    private final List<String> messageList = new ArrayList<>();

    /**
     * The GUI used to display messages.
     */
    private final ChatWindowController gui;

    /**
     * Constructor for the ClientMessageList class.
     *
     * @param gui an instance of the ChatWindowController class used to display messages.
     */
    public ClientMessageList(ChatWindowController gui) {
        this.gui = gui;
    }

    /**
     * Adds a message to the message list.
     *
     * @param type     the type of message to be added.
     * @param sender   the sender of the message.
     * @param receiver the intended recipient of the message.
     * @param message  the contents of the message.
     */
    public void addMessage(Configuration.MessageType type, String sender, String receiver, String message) {
        typeList.add(type);
        senderList.add(sender);
        receiverList.add(receiver);
        messageList.add(message);
    }

    /**
     * Writes the filtered messages to the GUI's message area based on the user-defined filter.
     *
     * @param filter the user-defined filter string.
     */
    public void writeFilteredMessages(String filter) {
        boolean showAll = filter == null || filter.isBlank();
        gui.clearMessageArea();
        for (int i = 0; i < senderList.size(); i++) {
            String sender = Objects.requireNonNullElse(senderList.get(i), "");
            String receiver = Objects.requireNonNullElse(receiverList.get(i), "");
            String message = Objects.requireNonNull(messageList.get(i), "");
            if (showAll ||
                sender.contains(filter) ||
                receiver.contains(filter) ||
                message.contains(filter)) {
                switch (typeList.get(i)) {
                    case MESSAGE -> gui.writeMessage(senderList.get(i), receiverList.get(i), messageList.get(i));
                    case ERROR -> gui.writeError(messageList.get(i));
                    case INFO -> gui.writeInfo(messageList.get(i));
                    default -> gui.writeError("Unexpected message type: " + typeList.get(i));
                }
            }
        }
    }
}
