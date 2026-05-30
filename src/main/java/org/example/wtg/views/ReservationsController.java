package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.wtg.SceneManager;
import org.example.wtg.entities.Order;
import org.example.wtg.services.ReservationsService;
import org.example.wtg.ui.ConfirmDialog;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Component
public class ReservationsController {

    private final ReservationsService service;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReservationsController(ReservationsService service) {
        this.service = service;
    }

    @FXML private Label statTotal, statPayees, statAttente, statRevenu;
    @FXML private Label feedbackLabel, selectionHint;
    @FXML private javafx.scene.control.TextField rechercheField;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> clientCol, totalCol, statusCol, dateCol;

    @FXML private Button validerBtn, annulerBtn;

    private final ObservableList<Order> toutesCommandes = FXCollections.observableArrayList();

    @FXML private BarChart<String, Number> barStatut;

    @FXML
    public void initialize() {
        // ── Colonnes ──
        clientCol.setCellValueFactory(c -> {
            String email = c.getValue().getUser() != null
                    ? c.getValue().getUser().getEmail() : "?";
            return new SimpleStringProperty(email);
        });
        totalCol.setCellValueFactory(c -> {
            Double t = c.getValue().getTotal();
            // Stocké en centimes dans la DB (ex: 180000 = 1 800,00 €)
            return new SimpleStringProperty(t == null ? "—" : String.format(Locale.FRANCE, "%.2f €", t / 100.0));
        });
        statusCol.setCellValueFactory(c -> {
            String s = c.getValue().getStatus();
            return new SimpleStringProperty(s == null ? "—" : traduireStatut(s));
        });
        dateCol.setCellValueFactory(c -> {
            var d = c.getValue().getCreatedAt();
            return new SimpleStringProperty(d == null ? "—" : d.format(FMT));
        });

        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ── Recherche ──
        FilteredList<Order> filtered = new FilteredList<>(toutesCommandes, o -> true);
        ordersTable.setItems(filtered);
        rechercheField.textProperty().addListener((obs, old, val) -> {
            String t = val == null ? "" : val.toLowerCase().trim();
            filtered.setPredicate(o -> {
                if (t.isEmpty()) return true;
                String email  = o.getUser() != null ? o.getUser().getEmail().toLowerCase() : "";
                String statut = o.getStatus() != null ? traduireStatut(o.getStatus()).toLowerCase() : "";
                return email.contains(t) || statut.contains(t);
            });
        });

        // ── Listener de sélection ──
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                validerBtn.setDisable(true);
                annulerBtn.setDisable(true);
                selectionHint.setText("Aucune sélection");
            } else {
                validerBtn.setDisable(!"pending".equals(sel.getStatus()));
                annulerBtn.setDisable("cancel".equals(sel.getStatus()));
                selectionHint.setText("Sélection : commande #" + sel.getId());
            }
        });

        rafraichir();
    }

    @FXML public void onValider() {
        Order sel = ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherFeedback("Sélectionnez une commande.", false); return; }
        boolean ok = ConfirmDialog.confirm("Valider le paiement ?",
                "La commande #" + sel.getId() + " passera au statut Payée.", "Valider", false);
        if (!ok) return;
        try {
            service.validerPaiement(sel.getId());
            afficherFeedback("Paiement validé — commande #" + sel.getId(), true);
            rafraichir();
            ordersTable.getSelectionModel().clearSelection();
        } catch (IllegalStateException e) { afficherFeedback(e.getMessage(), false); }
    }

    @FXML public void onAnnuler() {
        Order sel = ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherFeedback("Sélectionnez une commande.", false); return; }
        boolean ok = ConfirmDialog.confirm("Annuler cette commande ?",
                "La commande #" + sel.getId() + " sera marquée comme annulée.", "Oui, annuler", true);
        if (!ok) return;
        try {
            service.annulerCommande(sel.getId());
            afficherFeedback("Commande #" + sel.getId() + " annulée.", true);
            rafraichir();
            ordersTable.getSelectionModel().clearSelection();
        } catch (IllegalStateException e) { afficherFeedback(e.getMessage(), false); }
    }

    @FXML public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml", "WorkTogether — Tableau de bord", 1000, 700, true);
    }

    // ────────────────────────────────────────────
    private void rafraichir() {
        long   total   = service.totalCommandes();
        long   payees  = service.commandesPayees();
        long   attente = service.commandesEnAttente();
        double revenu  = service.revenuTotal();

        statTotal.setText(String.valueOf(total));
        statPayees.setText(String.valueOf(payees));
        statAttente.setText(String.valueOf(attente));
        // revenu est en centimes → diviser par 100 pour afficher en euros
        statRevenu.setText(String.format(Locale.FRANCE, "%.2f €", revenu / 100.0));

        toutesCommandes.setAll(service.listerCommandes());
        ordersTable.refresh();

        barStatut.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Commandes");
        for (Map.Entry<String, Long> e : service.commandesParStatut().entrySet()) {
            serie.getData().add(new XYChart.Data<>(traduireStatut(e.getKey()), e.getValue()));
        }
        barStatut.getData().add(serie);
    }

    private void afficherFeedback(String msg, boolean ok) {
        feedbackLabel.setText(msg);
        feedbackLabel.getStyleClass().removeAll("feedback-ok", "feedback-err");
        feedbackLabel.getStyleClass().add(ok ? "feedback-ok" : "feedback-err");
        feedbackLabel.setVisible(true); feedbackLabel.setManaged(true);
    }

    private static String traduireStatut(String s) {
        return switch (s.toLowerCase()) {
            case "paid"    -> "Payée";
            case "pending" -> "En attente";
            case "cancel"  -> "Annulée";
            default        -> s;
        };
    }
}
