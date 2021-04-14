package trashsoftware.solitaire;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import trashsoftware.solitaire.fxml.controls.GameView;

import java.io.IOException;
import java.util.ResourceBundle;

public class Main extends Application {

    private static ResourceBundle bundle;

    public static void main(String[] args) {
        launch(args);
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        bundle = ResourceBundle.getBundle("trashsoftware.solitaire.bundles.language");
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("fxml/controls/gameView.fxml"),
                bundle
        );
        Parent root = loader.load();

        primaryStage.setTitle(bundle.getString("appName"));

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        GameView gameView = loader.getController();
        gameView.setStage(primaryStage);

        primaryStage.show();
    }
}
