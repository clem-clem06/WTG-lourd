package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.wtg.SceneManager;
import org.example.wtg.entities.Offre;
import org.example.wtg.services.OffresService;
import org.example.wtg.ui.ConfirmDialog;
import org.springframework.stereotype.Component;

@Component
public class OffresController {

    private final OffresService service;
    public OffresController(OffresService service) { this.service = service; }

    @FXML private Label countLabel, selectionHint, feedbackLabel, formTitle;
    @FXML private TableView<Offre> offresTable;
    @FXML private TableColumn<Offre, String> nomCol, unitesCol, mensuelCol, annuelCol;
    @FXML private TextField nomField, unitesField, mensuelField, annuelField;
    @FXML private Button submitBtn, cancelEditBtn, deleteBtn;

    private Offre offreEnEdition = null;

    @FXML
    public void initialize() {
        offresTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        nomCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        unitesCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNombreUnites())));
        mensuelCol.setCellValueFactory(c -> {
            Double v = c.getValue().getPrixMensuel();
            return new SimpleStringProperty(v == null ? "—" : String.format("%.0f €", v));
        });
        annuelCol.setCellValueFactory(c -> {
            Double v = c.getValue().getPrixAnnuel();
            return new SimpleStringProperty(v == null ? "—" : String.format("%.0f €", v));
        });

        // Auto-calcul prix annuel = mensuel × 10 (2 mois offerts)
        mensuelField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) { annuelField.clear(); return; }
            try {
                double m = Double.parseDouble(val.replace(",", "."));
                annuelField.setText(String.valueOf((int) Math.round(m * 10)));
            } catch (NumberFormatException ignored) { /* laisse l'utilisateur finir de taper */ }
        });

        offresTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) modeCreation(); else modeEdition(sel);
        });

        rafraichir();
        modeCreation();
    }

    @FXML public void onSubmit() {
        try {
            String nom  = lire(nomField);
            int unites  = parseInt(unitesField, "Nombre d'unités invalide.");
            double mensuel = parseDouble(mensuelField, "Prix mensuel invalide.");
            double annuel  = parseDouble(annuelField,  "Prix annuel invalide.");

            if (offreEnEdition == null) {
                service.creerOffre(nom, unites, mensuel, annuel);
                afficherFeedback("Offre créée : " + nom, true);
            } else {
                boolean ok = ConfirmDialog.confirm("Modifier l'offre ?",
                        "L'offre « " + offreEnEdition.getNom() + " » sera mise à jour.", "Enregistrer", false);
                if (!ok) return;
                service.modifierOffre(offreEnEdition.getId(), nom, unites, mensuel, annuel);
                afficherFeedback("Offre modifiée : " + nom, true);
            }
            rafraichir();
            offresTable.getSelectionModel().clearSelection();
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherFeedback(e.getMessage(), false);
        }
    }

    @FXML public void onSupprimer() {
        Offre sel = offresTable.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherFeedback("Sélectionnez une offre.", false); return; }
        boolean ok = ConfirmDialog.confirm("Supprimer cette offre ?",
                "L'offre « " + sel.getNom() + " » sera définitivement supprimée.", "Supprimer", true);
        if (!ok) return;
        try {
            service.supprimerOffre(sel.getId());
            afficherFeedback("Offre supprimée : " + sel.getNom(), true);
            rafraichir();
            offresTable.getSelectionModel().clearSelection();
        } catch (IllegalStateException e) { afficherFeedback(e.getMessage(), false); }
    }

    @FXML public void onAnnuler() { offresTable.getSelectionModel().clearSelection(); }

    @FXML public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml", "WorkTogether — Tableau de bord", 1000, 700, true);
    }

    private void modeCreation() {
        offreEnEdition = null;
        nomField.clear(); unitesField.clear(); mensuelField.clear(); annuelField.clear();
        formTitle.setText("NOUVELLE OFFRE");
        submitBtn.setText("Créer l'offre");
        cancelEditBtn.setVisible(false); cancelEditBtn.setManaged(false);
        deleteBtn.setDisable(true);
        selectionHint.setText("Aucune sélection");
    }

    private void modeEdition(Offre o) {
        offreEnEdition = o;
        nomField.setText(o.getNom());
        unitesField.setText(String.valueOf(o.getNombreUnites()));
        // Afficher les prix sans décimales inutiles
        mensuelField.setText(o.getPrixMensuel() == null ? "" : String.format("%.0f", o.getPrixMensuel()));
        annuelField.setText(o.getPrixAnnuel()   == null ? "" : String.format("%.0f", o.getPrixAnnuel()));
        formTitle.setText("MODIFIER : " + o.getNom());
        submitBtn.setText("Enregistrer");
        cancelEditBtn.setVisible(true); cancelEditBtn.setManaged(true);
        deleteBtn.setDisable(false);
        selectionHint.setText("Sélection : " + o.getNom());
    }

    private void rafraichir() {
        long total = service.totalOffres();
        countLabel.setText(total + (total > 1 ? " offres" : " offre"));
        offresTable.setItems(FXCollections.observableArrayList(service.listerOffres()));
    }

    private void afficherFeedback(String msg, boolean ok) {
        feedbackLabel.setText(msg);
        feedbackLabel.getStyleClass().removeAll("feedback-ok", "feedback-err");
        feedbackLabel.getStyleClass().add(ok ? "feedback-ok" : "feedback-err");
        feedbackLabel.setVisible(true); feedbackLabel.setManaged(true);
    }

    private static String lire(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private static int parseInt(TextField tf, String msgErreur) {
        try { return Integer.parseInt(lire(tf)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(msgErreur); }
    }

    private static double parseDouble(TextField tf, String msgErreur) {
        try { return Double.parseDouble(lire(tf).replace(",", ".")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(msgErreur); }
    }
}
