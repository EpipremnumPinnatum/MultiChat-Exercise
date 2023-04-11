package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.Configuration;

/**
 * ClientConnectionObserver is an interface that defines the methods to be implemented by classes
 * that need to observe the state and actions of the ClientConnectionHandler.
 * This allows the implementing class to react to state changes, messages, errors and other events
 * that occur during the client-server communication.
 */
public interface ClientConnectionObserver {
    /**
     * Called when the protocol state of the connection changes.
     *
     * @param newProtocolState The new protocol state.
     */
    void stateChanged(Configuration.ProtocolState newProtocolState);

    /**
     * Sets the username of the client.
     *
     * @param userName The username to set.
     */
    void setUserName(String userName);

    /**
     * Sets the server port for the connection.
     *
     * @param port The server port.
     */
    void setServerPort(int port);

    /**
     * Sets the server address for the connection.
     *
     * @param address The server address.
     */
    void setServerAddress(String address);

    /**
     * Adds an informational message to the observer.
     *
     * @param info The informational message.
     */
    void addInfo(String info);

    /**
     * Adds a received message from the server to the observer.
     *
     * @param sender   The sender of the message.
     * @param receiver The receiver of the message.
     * @param message  The content of the message.
     */
    void addMessage(String sender, String receiver, String message);

    /**
     * Adds an error message to the observer.
     *
     * @param error The error message.
     */
    void addError(String error);
}
