package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.wtg.SceneManager;
import org.example.wtg.entities.User;
import org.example.wtg.services.ClientsService;
import org.springframework.stereotype.Component;

@Component
public class ClientsController {

    private final ClientsService service;

    public ClientsController(ClientsService service) {
        this.service = service;
    }

    @FXML private Label statTotal;
    @FXML private Label statActifs;
    @FXML private Label statInactifs;
    @FXML private Label statUnites;

    @FXML private javafx.scene.control.TextField rechercheField;
    @FXML private TableView<User> clientsTable;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> cmdCol;
    @FXML private TableColumn<User, String> unitesCol;

    @FXML private PieChart pieClients;

    @FXML
    public void initialize() {
        // Un seul appel DB — listerClients() est @Transactional et initialise
        // toutes les collections lazy (orders, unites) dans la même transaction.
        // On calcule ensuite tous les stats en mémoire pour éviter tout problème
        // de LazyInitializationException hors transaction.
        java.util.List<User> clients = service.listerClients();

        long total    = clients.size();
        long actifs   = clients.stream().filter(u -> !u.getOrders().isEmpty()).count();
        long inactifs = total - actifs;
        long unites   = clients.stream().mapToLong(u -> u.getUnites().size()).sum();

        // ── Stats ──
        statTotal.setText(String.valueOf(total));
        statActifs.setText(String.valueOf(actifs));
        statInactifs.setText(String.valueOf(inactifs));
        statUnites.setText(String.valueOf(unites));

        // ── Table ──
        clientsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        emailCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));
        cmdCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getOrders().size())));
        unitesCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getUnites().size())));

        ObservableList<User> obs = FXCollections.observableArrayList(clients);
        FilteredList<User> filtered = new FilteredList<>(obs, u -> true);
        clientsTable.setItems(filtered);
        rechercheField.textProperty().addListener((o, old, val) -> {
            String t = val == null ? "" : val.toLowerCase().trim();
            filtered.setPredicate(u -> t.isEmpty() || u.getEmail().toLowerCase().contains(t));
        });

        // ── PieChart : répartition des unités louées par client ──
        // Chaque client occupant au moins une unité = une part proportionnelle.
        // Plus parlant pour un datacenter que « actif / inactif ».
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        clients.stream()
                .filter(u -> !u.getUnites().isEmpty())
                .sorted((a, b) -> Integer.compare(b.getUnites().size(), a.getUnites().size()))
                .forEach(u -> pieData.add(new PieChart.Data(
                        u.getEmail() + " (" + u.getUnites().size() + ")",
                        u.getUnites().size())));
        pieClients.setData(pieData);
        pieClients.setTitle(pieData.isEmpty() ? "Aucune unité louée" : "Unités louées par client");
    }

    @FXML
    public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml", "WorkTogether — Tableau de bord", 1000, 700, true);
    }
}
