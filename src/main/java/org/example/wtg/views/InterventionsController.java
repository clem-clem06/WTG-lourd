package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.wtg.SceneManager;
import org.example.wtg.entities.Intervention;
import org.example.wtg.entities.Unite;
import org.example.wtg.services.InterventionsService;
import org.example.wtg.ui.ConfirmDialog;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InterventionsController {

    private final InterventionsService service;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public InterventionsController(InterventionsService service) { this.service = service; }

    @FXML private Label statTotal, statPlanifiees, statEnCours, statTerminees;
    @FXML private Label countLabel, selectionHint, feedbackLabel, formTitle;

    @FXML private TableView<Intervention> interventionsTable;
    @FXML private TableColumn<Intervention, String> typeCol, etatCol, descriptionCol, dateDebutCol;

    @FXML private ComboBox<String> typeCombo, etatCombo;
    @FXML private TextField descriptionField;
    @FXML private ListView<Unite> unitesListView;
    @FXML private Button submitBtn, cancelEditBtn, deleteBtn;

    private Intervention interventionEnEdition = null;

    private static final java.util.List<String> TYPES =
            java.util.List.of("Maintenance", "Réparation", "Mise à jour", "Inspection", "Autre");
    private static final java.util.List<String> ETATS =
            java.util.List.of("Planifiée", "En cours", "Terminée");

    @FXML
    public void initialize() {
        interventionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        etatCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEtat()));
        descriptionCol.setCellValueFactory(c -> {
            String d = c.getValue().getDescription();
            return new SimpleStringProperty(d == null ? "—" : d);
        });
        dateDebutCol.setCellValueFactory(c -> {
            LocalDateTime d = c.getValue().getDateDebut();
            return new SimpleStringProperty(d == null ? "—" : d.format(FMT));
        });

        typeCombo.getItems().addAll(TYPES);
        typeCombo.getSelectionModel().selectFirst();
        etatCombo.getItems().addAll(ETATS);
        etatCombo.getSelectionModel().selectFirst();

        // ListView multi-sélection des unités
        unitesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unitesListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Unite u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); return; }
                String baie = u.getBaie() != null ? u.getBaie().getReference() : "?";
                String etat = u.getEtat() != null ? u.getEtat() : "?";
                setText(baie + " — " + u.getNumero() + "  (" + etat + ")");
            }
        });
        unitesListView.setItems(FXCollections.observableArrayList(
                service.listerUnitesDisponibles()));

        interventionsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) modeCreation(); else modeEdition(sel);
        });

        rafraichir();
        modeCreation();
    }

    @FXML public void onSubmit() {
        try {
            String type        = typeCombo.getSelectionModel().getSelectedItem();
            String etat        = etatCombo.getSelectionModel().getSelectedItem();
            String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();

            List<Integer> uniteIds = unitesListView.getSelectionModel()
                    .getSelectedItems().stream()
                    .map(Unite::getId)
                    .collect(Collectors.toList());

            if (interventionEnEdition == null) {
                service.creerIntervention(type, description, etat, LocalDateTime.now(), uniteIds);
                String nbUnites = uniteIds.isEmpty() ? "" : " (" + uniteIds.size() + " unité(s))";
                afficherFeedback("Intervention créée : " + type + nbUnites, true);
            } else {
                boolean ok = ConfirmDialog.confirm("Modifier l'intervention ?",
                        "L'état et la description seront mis à jour.", "Enregistrer", false);
                if (!ok) return;
                service.changerEtat(interventionEnEdition.getId(), etat);
                afficherFeedback("Intervention mise à jour.", true);
            }
            rafraichir();
            interventionsTable.getSelectionModel().clearSelection();
        } catch (IllegalArgumentException | IllegalStateException e) {
            afficherFeedback(e.getMessage(), false);
        }
    }

    @FXML public void onSupprimer() {
        Intervention sel = interventionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherFeedback("Sélectionnez une intervention.", false); return; }
        boolean ok = ConfirmDialog.confirm("Supprimer cette intervention ?",
                "L'intervention « " + sel.getType() + " » sera définitivement supprimée.", "Supprimer", true);
        if (!ok) return;
        try {
            service.supprimerIntervention(sel.getId());
            afficherFeedback("Intervention supprimée.", true);
            rafraichir();
            interventionsTable.getSelectionModel().clearSelection();
        } catch (IllegalStateException e) { afficherFeedback(e.getMessage(), false); }
    }

    @FXML public void onAnnuler() { interventionsTable.getSelectionModel().clearSelection(); }

    @FXML public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml", "WorkTogether — Tableau de bord", 1000, 700, true);
    }

    private void modeCreation() {
        interventionEnEdition = null;
        typeCombo.getSelectionModel().selectFirst();
        etatCombo.getSelectionModel().selectFirst();
        descriptionField.clear();
        unitesListView.getSelectionModel().clearSelection();
        formTitle.setText("NOUVELLE INTERVENTION");
        submitBtn.setText("Créer l'intervention");
        cancelEditBtn.setVisible(false); cancelEditBtn.setManaged(false);
        deleteBtn.setDisable(true);
        selectionHint.setText("Aucune sélection");
    }

    private void modeEdition(Intervention i) {
        interventionEnEdition = i;
        if (i.getType() != null && typeCombo.getItems().contains(i.getType()))
            typeCombo.getSelectionModel().select(i.getType());
        if (i.getEtat() != null && etatCombo.getItems().contains(i.getEtat()))
            etatCombo.getSelectionModel().select(i.getEtat());
        descriptionField.setText(i.getDescription() == null ? "" : i.getDescription());
        // Pré-sélectionner les unités déjà liées
        unitesListView.getSelectionModel().clearSelection();
        if (i.getUnites() != null) {
            unitesListView.getItems().forEach(u -> {
                if (i.getUnites().stream().anyMatch(lu -> lu.getId().equals(u.getId())))
                    unitesListView.getSelectionModel().select(u);
            });
        }
        formTitle.setText("MODIFIER : " + i.getType());
        submitBtn.setText("Enregistrer");
        cancelEditBtn.setVisible(true); cancelEditBtn.setManaged(true);
        deleteBtn.setDisable(false);
        selectionHint.setText("Sélection : " + i.getType() + " — " + i.getEtat());
    }

    private void rafraichir() {
        long total      = service.totalInterventions();
        long planifiees = service.interventionsPlanifiees();
        long enCours    = service.interventionsEnCours();
        long terminees  = service.interventionsTerminees();

        statTotal.setText(String.valueOf(total));
        statPlanifiees.setText(String.valueOf(planifiees));
        statEnCours.setText(String.valueOf(enCours));
        statTerminees.setText(String.valueOf(terminees));
        countLabel.setText(total + " intervention" + (total > 1 ? "s" : ""));

        interventionsTable.setItems(FXCollections.observableArrayList(service.listerInterventions()));
    }

    private void afficherFeedback(String msg, boolean ok) {
        feedbackLabel.setText(msg);
        feedbackLabel.getStyleClass().removeAll("feedback-ok", "feedback-err");
        feedbackLabel.getStyleClass().add(ok ? "feedback-ok" : "feedback-err");
        feedbackLabel.setVisible(true); feedbackLabel.setManaged(true);
    }
}
