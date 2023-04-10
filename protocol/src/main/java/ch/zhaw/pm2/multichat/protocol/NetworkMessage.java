package ch.zhaw.pm2.multichat.protocol;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private final String sender;
    private final String receiver;
    private final Configuration.DataType type;
    private final String payload;

    public NetworkMessage(String sender, String receiver, Configuration.DataType type, String payload) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
        this.payload = payload;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public Configuration.DataType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString(){
        return String.format("%s %s %s %s", sender, receiver, type, payload);
    }
}
