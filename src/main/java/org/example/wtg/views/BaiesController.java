package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.wtg.SceneManager;
import org.example.wtg.entities.Baie;
import org.example.wtg.services.BaiesService;
import org.example.wtg.ui.ConfirmDialog;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BaiesController {

    private final BaiesService service;

    public BaiesController(BaiesService service) {
        this.service = service;
    }

    // Stats
    @FXML private Label statTotalBaies, statTotalUnites, statLibres, statOccupees;

    // Table
    @FXML private TableView<Baie> baiesTable;
    @FXML private TableColumn<Baie, String> refCol, totalCol, occCol, tauxCol;
    @FXML private Label countLabel, selectionHint;
    @FXML private Button deleteBtn;

    // Formulaire
    @FXML private Label formTitle, nbLabel;
    @FXML private TextField refField, nbField;
    @FXML private Button submitBtn, cancelEditBtn;

    // Chart
    @FXML private PieChart pieOccupation;

    // Feedback
    @FXML private Label feedbackLabel;

    private Baie baieEnEdition = null;

    @FXML
    public void initialize() {
        baiesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        refCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReference()));
        totalCol.setCellValueFactory(c -> {
            int nb = service.unitesParBaie(c.getValue()).size();
            return new SimpleStringProperty(String.valueOf(nb));
        });
        occCol.setCellValueFactory(c -> {
            long occ = service.unitesParBaie(c.getValue()).stream()
                    .filter(u -> u.getLocataire() != null).count();
            return new SimpleStringProperty(String.valueOf(occ));
        });
        tauxCol.setCellValueFactory(c -> {
            List<org.example.wtg.entities.Unite> all = service.unitesParBaie(c.getValue());
            if (all.isEmpty()) return new SimpleStringProperty("0 %");
            long occ = all.stream().filter(u -> u.getLocataire() != null).count();
            return new SimpleStringProperty((int) Math.round(occ * 100.0 / all.size()) + " %");
        });

        baiesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) modeCreation(); else modeEdition(sel);
        });

        rafraichir();
        modeCreation();
    }

    @FXML public void onSubmit() {
        try {
            String ref = refField.getText() == null ? "" : refField.getText().trim();
            String nbTxt = nbField.getText() == null ? "" : nbField.getText().trim();
            int nb;
            try { nb = Integer.parseInt(nbTxt); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Nombre d'unités invalide."); }

            if (baieEnEdition == null) {
                service.creerBaie(ref, nb);
                afficherFeedback("Baie créée : " + ref + " (" + nb + " unités)", true);
            } else {
                boolean ok = ConfirmDialog.confirm("Modifier cette baie ?",
                        "La baie « " + baieEnEdition.getReference() + " » sera mise à jour.", "Enregistrer", false);
                if (!ok) return;
                service.modifierBaie(baieEnEdition.getId(), ref, nb);
                afficherFeedback("Baie modifiée : " + ref + " (" + nb + " unités)", true);
            }
            rafraichir();
            baiesTable.getSelectionModel().clearSelection();
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherFeedback(e.getMessage(), false);
        }
    }

    @FXML public void onSupprimer() {
        Baie sel = baiesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherFeedback("Sélectionnez une baie.", false); return; }
        boolean ok = ConfirmDialog.confirm("Supprimer cette baie ?",
                "La baie « " + sel.getReference() + " » et toutes ses unités libres seront supprimées.", "Supprimer", true);
        if (!ok) return;
        try {
            service.supprimerBaie(sel.getId());
            afficherFeedback("Baie supprimée : " + sel.getReference(), true);
            rafraichir();
            baiesTable.getSelectionModel().clearSelection();
        } catch (IllegalStateException e) {
            afficherFeedback(e.getMessage(), false);
        }
    }

    @FXML public void onAnnuler() { baiesTable.getSelectionModel().clearSelection(); }

    @FXML public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml", "WorkTogether — Tableau de bord", 1000, 700, true);
    }

    // ────────────────────────────────────────────
    private void modeCreation() {
        baieEnEdition = null;
        refField.clear(); nbField.clear();
        nbLabel.setVisible(true); nbLabel.setManaged(true);
        nbField.setVisible(true); nbField.setManaged(true);
        nbLabel.setText("Nb unités (max 42)");
        formTitle.setText("NOUVELLE BAIE");
        submitBtn.setText("Créer la baie");
        cancelEditBtn.setVisible(false); cancelEditBtn.setManaged(false);
        deleteBtn.setDisable(true);
        selectionHint.setText("Aucune sélection");
    }

    private void modeEdition(Baie b) {
        baieEnEdition = b;
        refField.setText(b.getReference());
        // Pré-remplir le nb d'unités actuel, modifiable
        int nbActuel = service.unitesParBaie(b).size();
        nbField.setText(String.valueOf(nbActuel));
        nbLabel.setVisible(true); nbLabel.setManaged(true);
        nbField.setVisible(true); nbField.setManaged(true);
        formTitle.setText("MODIFIER : " + b.getReference());
        submitBtn.setText("Enregistrer");
        cancelEditBtn.setVisible(true); cancelEditBtn.setManaged(true);
        deleteBtn.setDisable(false);
        selectionHint.setText("Sélection : " + b.getReference());
    }

    private void rafraichir() {
        long total    = service.totalBaies();
        long unites   = service.totalUnites();
        long libres   = service.unitesLibres();
        long occupees = service.unitesOccupees();
        statTotalBaies.setText(String.valueOf(total));
        statTotalUnites.setText(String.valueOf(unites));
        statLibres.setText(String.valueOf(libres));
        statOccupees.setText(String.valueOf(occupees));
        countLabel.setText(total + (total > 1 ? " baies" : " baie"));

        baiesTable.setItems(FXCollections.observableArrayList(service.listerBaies()));

        ObservableList<PieChart.Data> pie = FXCollections.observableArrayList(
                new PieChart.Data("Occupées (" + occupees + ")", Math.max(occupees, 0)),
                new PieChart.Data("Libres (" + libres + ")", Math.max(libres, 0)));
        pieOccupation.setData(pie);
        pieOccupation.setTitle("Unités");
    }

    private void afficherFeedback(String msg, boolean ok) {
        feedbackLabel.setText(msg);
        feedbackLabel.getStyleClass().removeAll("feedback-ok", "feedback-err");
        feedbackLabel.getStyleClass().add(ok ? "feedback-ok" : "feedback-err");
        feedbackLabel.setVisible(true); feedbackLabel.setManaged(true);
    }
}
