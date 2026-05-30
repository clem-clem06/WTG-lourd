package org.example.wtg.views;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.wtg.SceneManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    // ── Éléments FXML ──
    @FXML private Label emailLabel;     // affiche l'email de l'utilisateur connecté
    @FXML private Label roleBadge;      // badge "Administrateur" / "Comptable"
    @FXML private VBox adminSection;    // section cachée si pas admin

    /**
     * initialize() est appelée automatiquement par JavaFX après le chargement du FXML,
     * une fois que tous les @FXML sont remplis.
     * C'est l'équivalent d'un constructeur pour JavaFX.
     */
    @FXML
    public void initialize() {
        // Récupère l'utilisateur connecté depuis Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String email = auth.getName();
        emailLabel.setText(email);

        // Vérifie les rôles
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTechnicien = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIEN"));

        if (isAdmin) {
            roleBadge.setText("Administrateur");
            roleBadge.getStyleClass().add("badge-admin");
            adminSection.setVisible(true);
            adminSection.setManaged(true);
        } else if (isTechnicien) {
            roleBadge.setText("Technicien");
            roleBadge.getStyleClass().add("badge-technicien");
            // La section admin reste réservée aux administrateurs
            adminSection.setVisible(false);
            adminSection.setManaged(false);
        } else {
            roleBadge.setText("Comptable");
            roleBadge.getStyleClass().add("badge-comptable");
            // Cache complètement la section admin pour un comptable
            adminSection.setVisible(false);
            adminSection.setManaged(false); // ne prend plus de place dans le layout
        }
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIONS DE LA NAVBAR
    // ─────────────────────────────────────────────────────────

    @FXML
    public void onLogout() {
        // Vide le contexte Spring Security (utilisateur plus connecté)
        SecurityContextHolder.clearContext();

        // Retour à la page de connexion
        SceneManager.switchTo("/fxml/login.fxml", "WorkTogether — Connexion", 450, 520, false);
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIONS DES CARTES (placeholders pour l'instant)
    //  → on créera les vraies vues plus tard
    // ─────────────────────────────────────────────────────────

    @FXML
    public void onUsers() {
        SceneManager.switchTo("/fxml/users.fxml",
                "WorkTogether — Utilisateurs", 1000, 700, true);
    }

    @FXML public void onBaies() {
        SceneManager.switchTo("/fxml/baies.fxml", "WorkTogether — Baies", 1100, 750, true);
    }

    @FXML public void onOffres() {
        SceneManager.switchTo("/fxml/offres.fxml", "WorkTogether — Offres commerciales", 900, 650, true);
    }

    @FXML public void onClients() {
        SceneManager.switchTo("/fxml/clients.fxml", "WorkTogether — Clients", 1100, 750, true);
    }

    @FXML public void onReservations() {
        SceneManager.switchTo("/fxml/reservations.fxml", "WorkTogether — Réservations", 1200, 750, true);
    }
}
