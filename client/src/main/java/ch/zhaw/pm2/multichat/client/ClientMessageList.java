package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ch.zhaw.pm2.multichat.protocol.Configuration.MessageType;

public class ClientMessageList {
    private final List<MessageType> typeList = new ArrayList<>();
    private final List<String> senderList = new ArrayList<>();
    private final List<String> receiverList = new ArrayList<>();
    private final List<String> messageList = new ArrayList<>();
    private final ChatWindowController gui;

    public ClientMessageList(ChatWindowController gui) {
        this.gui = gui;
    }

    public void addMessage(Configuration.MessageType type, String sender, String receiver, String message) {
        typeList.add(type);
        senderList.add(sender);
        receiverList.add(receiver);
        messageList.add(message);
    }

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
                    case MESSAGE:
                        gui.writeMessage(senderList.get(i), receiverList.get(i), messageList.get(i));
                        break;
                    case ERROR:
                        gui.writeError(messageList.get(i));
                        break;
                    case INFO:
                        gui.writeInfo(messageList.get(i));
                        break;
                    default:
                        gui.writeError("Unexpected message type: " + typeList.get(i));
                }
            }
        }
    }
}
