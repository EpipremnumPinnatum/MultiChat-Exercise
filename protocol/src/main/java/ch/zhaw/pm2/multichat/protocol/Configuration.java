package ch.zhaw.pm2.multichat.protocol;

/**
 * This class manages the constants that have to be accessed by multiple classes.
 */

public class Configuration {

    /**
     * Enumerator, which describes the type of the data package that is
     * supposed to be sent.
     */
    public enum DataType {

        CONNECT("CONNECT"),
        CONFIRM("CONFIRM"),
        DISCONNECT("DISCONNECT"),
        MESSAGE("MESSAGE"),
        ERROR("ERROR");

        private final String chatProtocolText;

        DataType(String chatProtocolText) {
            this.chatProtocolText = chatProtocolText;
        }

        /**
         * returns a String representation of the DataType enumerator.
         */
        @Override
        public String toString() {
            return chatProtocolText;
        }
    }

    /**
     * This enum describes the stage of the protocol procedure.
     */
    public enum ProtocolState {
        NEW, CONFIRM_CONNECT, CONNECTED, CONFIRM_DISCONNECT, DISCONNECTED
    }
}
