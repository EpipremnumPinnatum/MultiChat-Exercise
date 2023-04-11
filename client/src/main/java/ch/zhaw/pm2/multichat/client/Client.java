package ch.zhaw.pm2.multichat.client;

import javafx.application.Application;

/**
 * The {@code Client} class is responsible for starting the client application.
 */
public class Client {
    /**
     * Starts the client application by launching the {@code ClientUI} JavaFX application.
     *
     * @param args command-line arguments passed to the application (ignored)
     */
    public static void main(String[] args) {
        System.out.println("Starting Client Application");
        Application.launch(ClientUI.class, args);
        System.out.println("Client Application ended");
    }
}

