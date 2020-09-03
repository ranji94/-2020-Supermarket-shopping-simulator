package gui;

import federates.appInterface.AppInterfaceFederate;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GUI extends Application {
    private static AppInterfaceFederate fed;

    public static void start(AppInterfaceFederate federate){
        GUI.fed=federate;
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui.fxml"));
        Parent root = loader.load();
        GUIController controller = loader.getController();
        fed.setGuiHandler(controller);

        Scene scene = new Scene(root,1024,600);
        primaryStage.setTitle("Shop simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
