package org.example.wtg;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication reste ici pour que Spring scanne tous les packages
@SpringBootApplication
public class WtgApplication {

    public static void main(String[] args) {
        // On ne lance plus SpringApplication.run() directement
        // On lance JavaFX → JavaFX démarrera Spring lui-même dans JavaFxApp
        Application.launch(JavaFxApp.class, args);
    }
}
