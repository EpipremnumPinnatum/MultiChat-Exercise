package ch.zhaw.pm2.multichat.protocol;

/**
 * This class manages the information that has to be accessed by the ServerConnectionHandler.java
 * as well as the ClientConnectionHandler.
 */

public class Configuration {


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
         *returns a String representation of the DataType enumerator.
         */
        @Override
        public String toString(){
            return chatProtocolText;
        }
    }
}
