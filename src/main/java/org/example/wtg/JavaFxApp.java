package org.example.wtg;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApp extends Application {

    // Le contexte Spring (contient tous les beans : repositories, services, etc.)
    private ConfigurableApplicationContext springContext;

    // init() est appelé AVANT start() — on démarre Spring ici
    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(WtgApplication.class)
                .web(WebApplicationType.NONE) // PAS de serveur HTTP, juste JPA + Security
                .run();
    }

    // start() est la méthode principale de JavaFX — elle ouvre la première fenêtre
    @Override
    public void start(Stage primaryStage) throws Exception {

        // FXMLLoader lit le fichier .fxml et construit la fenêtre
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml")
        );

        // Ligne CLEF : dit à JavaFX d'utiliser Spring pour créer les controllers
        // → les controllers JavaFX pourront avoir @Autowired et @Component
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();

        primaryStage.setTitle("WorkTogether — Connexion");
        primaryStage.setScene(new Scene(root, 450, 520));
        primaryStage.setResizable(false);
        primaryStage.show();

        // Rend le Stage accessible partout via SceneManager
        SceneManager.init(primaryStage, springContext);
    }

    // stop() est appelé quand on ferme la fenêtre
    @Override
    public void stop() {
        springContext.close(); // arrête Spring proprement
        Platform.exit();       // arrête JavaFX
    }
}
