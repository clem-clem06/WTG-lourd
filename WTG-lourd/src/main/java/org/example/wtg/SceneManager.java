package org.example.wtg;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

/**
 * Utilitaire pour changer la scène affichée dans la fenêtre principale.
 * Équivalent d'un "redirect" en Symfony, mais pour les fenêtres JavaFX.
 */
public class SceneManager {

    private static Stage stage;
    private static ApplicationContext springContext;

    // Appelé une seule fois au démarrage dans JavaFxApp.start()
    public static void init(Stage primaryStage, ApplicationContext context) {
        stage = primaryStage;
        springContext = context;
    }

    /**
     * Charge un fichier .fxml et l'affiche dans la fenêtre principale.
     *
     * @param fxmlPath chemin du fichier, ex: "/fxml/dashboard.fxml"
     * @param title    titre de la fenêtre
     * @param width    largeur en pixels
     * @param height   hauteur en pixels
     * @param resizable la fenêtre peut-elle être redimensionnée ?
     */
    public static void switchTo(String fxmlPath, String title, int width, int height, boolean resizable) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(fxmlPath)
            );
            // On réutilise Spring pour créer le controller du nouveau FXML
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.setResizable(resizable);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
