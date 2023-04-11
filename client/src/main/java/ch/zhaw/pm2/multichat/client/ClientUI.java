package ch.zhaw.pm2.multichat.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * The main class for starting up the Multichat client user interface. Extends JavaFX's {@link Application} class.
 */
public class ClientUI extends Application {

    /**
     * Overrides the {@link Application#start(Stage)} method to launch the chat window.
     *
     * @param primaryStage the primary stage for this application, onto which the application scene can be set.
     */
    @Override
    public void start(Stage primaryStage) {
        chatWindow(primaryStage);
    }

    /**
     * Loads the chat window FXML file and sets up the scene and stage for the GUI.
     *
     * @param primaryStage the primary stage for this application, onto which the application scene can be set.
     */
    private void chatWindow(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatWindow.fxml"));
            Pane rootPane = loader.load();
            Scene scene = new Scene(rootPane);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(420);
            primaryStage.setMinHeight(250);
            primaryStage.setTitle("Multichat Client");
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error starting up UI. " + e.getMessage());
        }
    }
}
