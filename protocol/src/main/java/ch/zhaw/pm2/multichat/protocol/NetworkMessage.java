package ch.zhaw.pm2.multichat.protocol;

import java.io.Serializable;

/**
 * A class representing a message sent over the network in the multichat protocol.
 * A NetworkMessage contains information about its sender, receiver, data type and payload.
 */
public class NetworkMessage implements Serializable {
    /**
     * The sender of the message.
     */
    private final String sender;

    /**
     * The intended recipient of the message.
     */
    private final String receiver;

    /**
     * The type of data contained in the message.
     */
    private final Configuration.DataType type;

    /**
     * The payload of the message.
     */
    private final String payload;

    /**
     * Constructs a new NetworkMessage with the given parameters.
     *
     * @param sender   the sender of the message.
     * @param receiver the receiver of the message.
     * @param type     the type of data contained in the message.
     * @param payload  the data contained in the message.
     */
    public NetworkMessage(String sender, String receiver, Configuration.DataType type, String payload) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
        this.payload = payload;
    }

    /**
     * Gets the sender of the message.
     *
     * @return the sender of the message.
     */
    public String getSender() {
        return sender;
    }

    /**
     * Gets the receiver of the message.
     *
     * @return the receiver of the message.
     */
    public String getReceiver() {
        return receiver;
    }

    /**
     * Gets the type of data contained in the message.
     *
     * @return the type of data contained in the message.
     */
    public Configuration.DataType getType() {
        return type;
    }

    /**
     * Gets the data contained in the message.
     *
     * @return the data contained in the message.
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Returns a string representation of this message.
     *
     * @return a string representation of this message.
     */
    @Override
    public String toString() {
        return String.format("%s %s %s %s", sender, receiver, type, payload);
    }
}
