package org.example.wtg.views;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
        clientsTable.setItems(FXCollections.observableArrayList(clients));

        // ── PieChart ──
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Actifs (" + actifs + ")", Math.max(actifs, 0)),
                new PieChart.Data("Inactifs (" + inactifs + ")", Math.max(inactifs, 0))
        );
        pieClients.setData(pieData);
        pieClients.setTitle("Clients");
    }

    @FXML
    public void onRetour() {
        SceneManager.switchTo("/fxml/dashboard.fxml", "WorkTogether — Tableau de bord", 1000, 700, true);
    }
}
