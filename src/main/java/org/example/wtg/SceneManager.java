package org.example.wtg;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Utilitaire pour changer la scène affichée dans la fenêtre principale.
 * Équivalent d'un "redirect" en Symfony, mais pour les fenêtres JavaFX.
 */
public class SceneManager {

    private static final Logger log = LoggerFactory.getLogger(SceneManager.class);
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

            Parent newRoot = loader.load();
            stage.setTitle(title);

            Scene currentScene = stage.getScene();
            if (currentScene != null) {
                // Remplace uniquement le contenu de la scène existante :
                // la fenêtre ne change PAS de taille, pas de flash, pas de resize.
                currentScene.setRoot(newRoot);
            } else {
                // Première création (login) — on crée la Scene une seule fois
                stage.setScene(new Scene(newRoot, width, height));
            }
            stage.show();

        } catch (Exception e) {
            log.error("Erreur lors du chargement de {}", fxmlPath, e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de chargement");
            alert.setHeaderText("Impossible d'ouvrir la page");
            alert.setContentText(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            alert.showAndWait();
        }
    }
}
