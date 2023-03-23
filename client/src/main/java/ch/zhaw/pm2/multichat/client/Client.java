package ch.zhaw.pm2.multichat.client;

import javafx.application.Application;

public class Client {

    public static void main(String[] args) {
        // Start UI
        System.out.println("Starting Client Application");
        Application.launch(ClientUI.class, args);
        System.out.println("Client Application ended");
    }
}

