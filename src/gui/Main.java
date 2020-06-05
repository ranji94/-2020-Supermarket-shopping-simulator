package gui;

import federates.klient.KlientFederate;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static KlientFederate federate;

    public static void start(KlientFederate federate) {
        Main.federate = federate;
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
//        Parent root = FXMLLoader.load(getClass().getResource("gui.fxml"));
//        primaryStage.setTitle("Supermarket simulation");
//        primaryStage.setScene(new Scene(root, 1024, 675));
//        primaryStage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
