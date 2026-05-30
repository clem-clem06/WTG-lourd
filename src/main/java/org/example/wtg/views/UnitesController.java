package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.wtg.SceneManager;
import org.example.wtg.entities.Unite;
import org.example.wtg.services.UnitesService;
import org.example.wtg.ui.ConfirmDialog;
import org.springframework.stereotype.Component;

@Component
public class UnitesController {

    private final UnitesService service;

    public UnitesController(UnitesService service) { this.service = service; }

    @FXML private Label statTotal, statLibres, statOccupees, statMaintenance, statIncident;
    @FXML private Label countLabel, selectionHint, feedbackLabel;
    @FXML private TextField rechercheField;

    @FXML private TableView<Unite> unitesTable;
    @FXML private TableColumn<Unite, String> baieCol, numeroCol, etatCol, locataireCol;

    @FXML private ComboBox<String> etatCombo;
    @FXML private Button appliquerBtn;

    private final ObservableList<Unite> toutesUnites = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        unitesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        baieCol.setCellValueFactory(c -> {
            var baie = c.getValue().getBaie();
            return new SimpleStringProperty(baie == null ? "—" : baie.getReference());
        });
        numeroCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        // Cellule État avec badge coloré
        etatCol.setCellValueFactory(c -> {
            String e = c.getValue().getEtat();
            return new SimpleStringProperty(e == null ? "—" : e);
        });
        etatCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String etat, boolean empty) {
                super.updateItem(etat, empty);
                if (empty || etat == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(etat);
                badge.getStyleClass().add(switch (etat.toLowerCase()) {
                    case "ok"          -> "etat-ok";
                    case "maintenance" -> "etat-maintenance";
                    case "incident"    -> "etat-incident";
                    default            -> "etat-autre";
                });
                setGraphic(badge); setText(null);
            }
        });
        locataireCol.setCellValueFactory(c -> {
            var loc = c.getValue().getLocataire();
            return new SimpleStringProperty(loc == null ? "Libre" : loc.getEmail());
        });

        etatCombo.getItems().addAll("OK", "Maintenance", "Incident");
        etatCombo.getSelectionModel().selectFirst();

        // Recherche en temps réel
        FilteredList<Unite> filteredUnites = new FilteredList<>(toutesUnites, u -> true);
        unitesTable.setItems(filteredUnites);
        rechercheField.textProperty().addListener((obs, old, val) -> {
            String t = val == null ? "" : val.toLowerCase().trim();
            filteredUnites.setPredicate(u -> {
                if (t.isEmpty()) return true;
                String baie = u.getBaie() != null ? u.getBaie().getReference().toLowerCase() : "";
                String num  = u.getNumero() != null ? u.getNumero().toLowerCase() : "";
                String etat = u.getEtat()   != null ? u.getEtat().toLowerCase() : "";
                String loc  = u.getLocataire() != null ? u.getLocataire().getEmail().toLowerCase() : "libre";
                return baie.contains(t) || num.contains(t) || etat.contains(t) || loc.contains(t);
            });
        });

        unitesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                appliquerBtn.setDisable(true);
                selectionHint.setText("Aucune sélection");
                // Pré-sélectionne l'état actuel dans le ComboBox
            } else {
                appliquerBtn.setDisable(false);
                selectionHint.setText("Sélection : " + sel.getNumero()
                        + (sel.getBaie() != null ? " (" + sel.getBaie().getReference() + ")" : ""));
                // Pré-sélectionne l'état actuel de l'unité
                if (sel.getEtat() != null && etatCombo.getItems().contains(sel.getEtat())) {
                    etatCombo.getSelectionModel().select(sel.getEtat());
                }
            }
        });

        rafraichir();
    }

    @FXML public void onAppliquer() {
        Unite sel = unitesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherFeedback("Sélectionnez une unité.", false); return; }
        String nouvelEtat = etatCombo.getSelectionModel().getSelectedItem();
        if (nouvelEtat == null) { afficherFeedback("Choisissez un état.", false); return; }

        if (nouvelEtat.equals(sel.getEtat())) {
            afficherFeedback("L'unité a déjà l'état « " + nouvelEtat + " ».", false);
            return;
        }

        boolean ok = ConfirmDialog.confirm(
                "Modifier l'état ?",
                "L'unité " + sel.getNumero() + " passera de « " + sel.getEtat() + " » à « " + nouvelEtat + " ».",
                "Appliquer", false);
        if (!ok) return;

        try {
            service.changerEtat(sel.getId(), nouvelEtat);
            afficherFeedback("État mis à jour : " + sel.getNumero() + " → " + nouvelEtat, true);
            rafraichir();
            unitesTable.getSelectionModel().clearSelection();
        } catch (IllegalStateException e) {
            afficherFeedback(e.getMessage(), false);
        }
    }

    @FXML public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml", "WorkTogether — Tableau de bord", 1000, 700, true);
    }

    private void rafraichir() {
        long total      = service.totalUnites();
        long libres     = service.unitesLibres();
        long occupees   = service.unitesOccupees();
        long mainten    = service.unitesEnMaintenance();
        long incident   = service.unitesEnIncident();

        statTotal.setText(String.valueOf(total));
        statLibres.setText(String.valueOf(libres));
        statOccupees.setText(String.valueOf(occupees));
        statMaintenance.setText(String.valueOf(mainten));
        statIncident.setText(String.valueOf(incident));
        countLabel.setText(total + " unité" + (total > 1 ? "s" : ""));

        toutesUnites.setAll(service.listerUnites());
    }

    private void afficherFeedback(String msg, boolean ok) {
        feedbackLabel.setText(msg);
        feedbackLabel.getStyleClass().removeAll("feedback-ok", "feedback-err");
        feedbackLabel.getStyleClass().add(ok ? "feedback-ok" : "feedback-err");
        feedbackLabel.setVisible(true); feedbackLabel.setManaged(true);
    }
}
