package ch.zhaw.pm2.multichat.protocol;

/**
 * An exception that represents a problem with the chat protocol.
 */
public class ChatProtocolException extends Exception {

    /**
     * Constructs a new `ChatProtocolException` with the specified detail message.
     *
     * @param message The detail message for the exception.
     */
    public ChatProtocolException(String message) {
        super(message);
    }
}
