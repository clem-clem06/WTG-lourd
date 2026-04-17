package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.wtg.SceneManager;
import org.example.wtg.entities.User;
import org.example.wtg.services.UserManagementService;
import org.example.wtg.ui.ConfirmDialog;
import org.springframework.stereotype.Component;

/**
 * Controller de la vue /fxml/users.fxml.
 * Deux modes :
 *  - CRÉATION : aucune ligne sélectionnée, le formulaire crée un nouveau user.
 *  - ÉDITION  : une ligne est sélectionnée, le formulaire modifie ce user
 *               (mot de passe optionnel = vide pour le garder).
 */
@Component
public class UsersController {

    private final UserManagementService service;

    public UsersController(UserManagementService service) {
        this.service = service;
    }

    // ── Éléments FXML : tableau ──
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private Label countLabel;
    @FXML private Label selectionHint;
    @FXML private Button deleteBtn;

    // ── Éléments FXML : formulaire ──
    @FXML private Label formTitle;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Button submitBtn;
    @FXML private Button cancelEditBtn;

    // ── Feedback ──
    @FXML private Label feedbackLabel;

    // Libellés humains (ComboBox)
    private static final String LIB_ADMIN = "Administrateur";
    private static final String LIB_COMPTABLE = "Comptable";

    // Null = mode création. Non-null = mode édition (ce user est en cours de modif).
    private User userEnEdition = null;

    // ─────────────────────────────────────────────────────────
    //  INITIALISATION
    // ─────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // --- Colonnes du tableau ---
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(cell -> {
            String json = cell.getValue().getRoles();
            return new SimpleStringProperty(libelleRole(json));
        });

        // --- ComboBox rôle ---
        roleCombo.getItems().addAll(LIB_ADMIN, LIB_COMPTABLE);
        roleCombo.getSelectionModel().selectFirst();

        // --- Quand on sélectionne/désélectionne une ligne, on bascule de mode ---
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel == null) {
                        passerEnModeCreation();
                    } else {
                        passerEnModeEdition(newSel);
                    }
                });

        // --- Charge la liste ---
        rafraichirTable();
        passerEnModeCreation();
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIONS (boutons)
    // ─────────────────────────────────────────────────────────

    /** Dispatche vers créer ou modifier selon l'état. */
    @FXML
    public void onSubmit() {
        String email = lireChamp(emailField);
        String password = passwordField.getText(); // peut être vide en mode édition
        String role = LIB_ADMIN.equals(roleCombo.getValue()) ? "ROLE_ADMIN" : "ROLE_COMPTABLE";

        try {
            if (userEnEdition == null) {
                // ── Création ──
                User cree = service.creerPersonnel(email, password, role);
                afficherFeedback("Compte créé : " + cree.getEmail()
                        + " (" + roleCombo.getValue() + ")", true);
                rafraichirTable();
                passerEnModeCreation();

            } else {
                // ── Modification : on demande confirmation ──
                boolean ok = ConfirmDialog.confirm(
                        "Enregistrer les modifications ?",
                        "Le compte « " + userEnEdition.getEmail() + " » sera mis à jour"
                                + (password != null && !password.isEmpty()
                                        ? " (y compris son mot de passe)." : "."),
                        "Enregistrer",
                        false); // bleu (action non destructrice)
                if (!ok) return;

                User modif = service.modifierPersonnel(
                        userEnEdition.getId(), email, password, role);
                afficherFeedback("Compte modifié : " + modif.getEmail(), true);
                rafraichirTable();
                passerEnModeCreation();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherFeedback(e.getMessage(), false);
        }
    }

    @FXML
    public void onSupprimer() {
        User selectionne = usersTable.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            afficherFeedback("Sélectionnez une ligne dans le tableau avant de supprimer.", false);
            return;
        }

        boolean ok = ConfirmDialog.confirm(
                "Supprimer ce compte ?",
                "Le compte « " + selectionne.getEmail()
                        + " » sera définitivement supprimé. Cette action est irréversible.",
                "Supprimer",
                true); // rouge (destructeur)
        if (!ok) return;

        try {
            service.supprimerPersonnel(selectionne.getId());
            afficherFeedback("Compte supprimé : " + selectionne.getEmail(), true);
            rafraichirTable();
            passerEnModeCreation();
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherFeedback(e.getMessage(), false);
        }
    }

    /** Bouton "Annuler" en mode édition → retour en mode création. */
    @FXML
    public void onAnnulerEdition() {
        usersTable.getSelectionModel().clearSelection();
        // Le listener de sélection appellera passerEnModeCreation() automatiquement
    }

    @FXML
    public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml",
                "WorkTogether — Tableau de bord", 1000, 700, true);
    }

    // ─────────────────────────────────────────────────────────
    //  GESTION DES MODES
    // ─────────────────────────────────────────────────────────

    private void passerEnModeCreation() {
        userEnEdition = null;
        emailField.clear();
        passwordField.clear();
        passwordField.setPromptText("Minimum 8 caractères");
        roleCombo.getSelectionModel().selectFirst();

        formTitle.setText("NOUVEAU COMPTE");
        submitBtn.setText("Créer le compte");
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);

        deleteBtn.setDisable(true);
        selectionHint.setText("Aucune ligne sélectionnée");
    }

    private void passerEnModeEdition(User user) {
        userEnEdition = user;
        emailField.setText(user.getEmail());
        passwordField.clear();
        passwordField.setPromptText("Laisser vide pour conserver");

        String libelle = libelleRole(user.getRoles());
        roleCombo.getSelectionModel().select(libelle);

        formTitle.setText("MODIFIER : " + user.getEmail());
        submitBtn.setText("Enregistrer les modifications");
        cancelEditBtn.setVisible(true);
        cancelEditBtn.setManaged(true);

        deleteBtn.setDisable(false);
        selectionHint.setText("Sélection : " + user.getEmail());
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    private void rafraichirTable() {
        var personnel = service.listerPersonnel();
        usersTable.setItems(FXCollections.observableArrayList(personnel));
        countLabel.setText(personnel.size() + (personnel.size() > 1 ? " comptes" : " compte"));
    }

    /** Transforme le JSON brut ["ROLE_X"] en libellé français. */
    private static String libelleRole(String json) {
        if (json == null) return "?";
        if (json.contains("ROLE_ADMIN")) return LIB_ADMIN;
        if (json.contains("ROLE_COMPTABLE")) return LIB_COMPTABLE;
        return "?";
    }

    private static String lireChamp(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private void afficherFeedback(String message, boolean succes) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-ok", "feedback-err");
        feedbackLabel.getStyleClass().add(succes ? "feedback-ok" : "feedback-err");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }
}
