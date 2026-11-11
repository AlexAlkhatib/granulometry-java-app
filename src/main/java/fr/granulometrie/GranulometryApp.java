package fr.granulometrie;

import fr.granulometrie.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Point d'entree JavaFX de Granulometrie Studio. */
public class GranulometryApp extends Application {

    public static final String APP_NAME = "Granulometrie Studio";
    public static final String VERSION = "3.0.0";

    @Override
    public void start(Stage stage) {
        MainWindow root = new MainWindow(stage);
        Scene scene = new Scene(root, 1340, 860);
        var css = GranulometryApp.class.getResource("/css/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setTitle(APP_NAME + " " + VERSION + " - analyse d'images scientifiques");
        var icon = GranulometryApp.class.getResourceAsStream("/img/icon.jpg");
        if (icon != null) {
            stage.getIcons().add(new Image(icon));
        }
        stage.setMinWidth(1120);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
