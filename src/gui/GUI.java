package gui;

import federates.interfejs.InterfejsFederate;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GUI extends Application {
    private static InterfejsFederate fed;

    public static void start(InterfejsFederate federat){
        GUI.fed=federat;
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui.fxml"));
        Parent root = loader.load();
        GUIController controller = loader.getController();
        fed.setGuiHandler(controller);
        controller.setFed(fed);

        Scene scene = new Scene(root,1024,600);
        primaryStage.setTitle("Symulacja sklepu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}
