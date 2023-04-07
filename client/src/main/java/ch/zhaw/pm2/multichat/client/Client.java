package ch.zhaw.pm2.multichat.client;

import javafx.application.Application;

public class Client {

    //Todo: write javadoc remove comment in main

    public static void main(String[] args) {
        // Start UI
        System.out.println("Starting Client Application");
        Application.launch(ClientUI.class, args);
        System.out.println("Client Application ended");
    }
}

