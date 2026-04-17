package org.example.wtg.views;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.wtg.SceneManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// @Component : Spring sait créer ce controller → permet l'injection de AuthenticationManager
// Attention : pour JavaFX, on doit créer les controllers via Spring
// (voir setControllerFactory dans JavaFxApp)
@Component
public class LoginController {

    // ── Services Spring injectés par le constructeur ──
    private final AuthenticationManager authenticationManager;

    public LoginController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // ── Éléments FXML (le fx:id du .fxml correspond au nom du champ ici) ──
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    /**
     * Appelé quand on clique le bouton "Se connecter".
     * Le .fxml contient onAction="#onLogin" → JavaFX cherche cette méthode ici.
     */
    @FXML
    public void onLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Validation basique : champs non vides
        if (email.isBlank() || password.isBlank()) {
            afficherErreur("Merci de remplir les deux champs.");
            return;
        }

        try {
            // Construit un "jeton" qui contient l'email et le mot de passe
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(email, password);

            // Spring Security vérifie : user existe ? mot de passe correct ? rôle autorisé ?
            // → si KO, ça lance une AuthenticationException (ou DisabledException pour les clients)
            Authentication authentication = authenticationManager.authenticate(token);

            // Stocke l'utilisateur connecté dans le contexte Spring Security global
            // → le DashboardController pourra récupérer l'email et les rôles
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Authentification OK → on ouvre le dashboard (900×650 redimensionnable)
            SceneManager.switchTo(
                    "/fxml/dashboard.fxml",
                    "WorkTogether — Tableau de bord",
                    1000, 700, true
            );

        } catch (AuthenticationException e) {
            // Affiche le message d'erreur adapté selon le type d'erreur
            String message = switch (e.getClass().getSimpleName()) {
                case "DisabledException"   -> "Accès refusé : cette application est réservée au personnel.";
                case "BadCredentialsException" -> "Email ou mot de passe incorrect.";
                default -> "Erreur de connexion : " + e.getMessage();
            };
            afficherErreur(message);
        }
    }

    private void afficherErreur(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true); // prend de la place dans le layout (sinon caché)
    }
}
