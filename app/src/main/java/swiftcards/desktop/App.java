/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package swiftcards.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Locale currentLocale = Locale.getDefault();
        Locale locale = new Locale("en_UK");
        ResourceBundle bundle = ResourceBundle.getBundle("labelText", locale);


        primaryStage.setTitle("Swiftcards");
        primaryStage.setResizable(false);

        Parent menuRoot = FXMLLoader.load(getClass().getResource("controller/menu.fxml"), bundle);
        Scene menu = new Scene(menuRoot);

        primaryStage.setScene(menu);
        primaryStage.show();
    }
}
