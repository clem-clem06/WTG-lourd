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

    @FXML private Label emailLabel;
    @FXML private Label roleBadge;
    @FXML private VBox adminSection;        // visible uniquement ROLE_ADMIN
    @FXML private VBox comptableSection;    // visible ROLE_ADMIN + ROLE_COMPTABLE
    @FXML private VBox technicienSection;   // visible uniquement ROLE_TECHNICIEN

    @FXML
    public void initialize() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        emailLabel.setText(auth.getName());

        boolean isAdmin      = hasRole(auth, "ROLE_ADMIN");
        boolean isTechnicien = hasRole(auth, "ROLE_TECHNICIEN");

        if (isAdmin) {
            roleBadge.setText("Administrateur");
            roleBadge.getStyleClass().add("badge-admin");
            show(adminSection);
            show(comptableSection);
            hide(technicienSection);
        } else if (isTechnicien) {
            roleBadge.setText("Technicien");
            roleBadge.getStyleClass().add("badge-technicien");
            hide(adminSection);
            hide(comptableSection);
            show(technicienSection);
        } else {
            // ROLE_COMPTABLE
            roleBadge.setText("Comptable");
            roleBadge.getStyleClass().add("badge-comptable");
            hide(adminSection);
            show(comptableSection);
            hide(technicienSection);
        }
    }

    // ── Navbar ──────────────────────────────────────────────────
    @FXML public void onLogout() {
        SecurityContextHolder.clearContext();
        SceneManager.switchTo("/fxml/login.fxml", "WorkTogether — Connexion", 450, 520, false);
    }

    // ── Cartes Admin ─────────────────────────────────────────────
    @FXML public void onUsers() {
        SceneManager.switchTo("/fxml/users.fxml", "WorkTogether — Utilisateurs", 1000, 700, true);
    }
    @FXML public void onBaies() {
        SceneManager.switchTo("/fxml/baies.fxml", "WorkTogether — Baies", 1100, 750, true);
    }
    @FXML public void onOffres() {
        SceneManager.switchTo("/fxml/offres.fxml", "WorkTogether — Offres commerciales", 900, 650, true);
    }

    // ── Cartes Comptable ──────────────────────────────────────────
    @FXML public void onClients() {
        SceneManager.switchTo("/fxml/clients.fxml", "WorkTogether — Clients", 1100, 750, true);
    }
    @FXML public void onReservations() {
        SceneManager.switchTo("/fxml/reservations.fxml", "WorkTogether — Réservations", 1200, 750, true);
    }

    // ── Cartes Technicien ─────────────────────────────────────────
    @FXML public void onUnites() {
        SceneManager.switchTo("/fxml/unites.fxml", "WorkTogether — Unités", 1100, 750, true);
    }
    @FXML public void onInterventions() {
        SceneManager.switchTo("/fxml/interventions.fxml", "WorkTogether — Interventions", 1100, 750, true);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
    private static void show(VBox section) { section.setVisible(true);  section.setManaged(true);  }
    private static void hide(VBox section) { section.setVisible(false); section.setManaged(false); }
}
