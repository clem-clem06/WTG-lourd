package org.example.wtg.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Boîte de dialogue de confirmation 100% custom (pas d'Alert JavaFX natif).
 *
 * Avantages par rapport à Alert :
 *  - même thème sombre que l'app (utilise style.css),
 *  - bouton de validation peut être coloré (bleu ou rouge selon danger),
 *  - arrondis, ombre portée, fond semi-transparent autour.
 *
 * Usage :
 *    boolean ok = ConfirmDialog.confirm("Titre", "Message...", "Supprimer", true);
 */
public final class ConfirmDialog {

    private ConfirmDialog() {} // classe utilitaire, pas d'instanciation

    /**
     * @param titre       gros titre en haut de la boîte
     * @param message     texte explicatif au-dessous
     * @param labelOk     texte du bouton de confirmation (ex: "Supprimer", "Enregistrer")
     * @param danger      true → bouton rouge (action destructrice), false → bouton bleu
     * @return true si l'utilisateur a cliqué le bouton de confirmation, false sinon
     */
    public static boolean confirm(String titre, String message, String labelOk, boolean danger) {

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(titre);

        // Tableau pour récupérer le choix de l'utilisateur depuis la lambda
        final boolean[] resultat = { false };

        // ── Contenu de la boîte ──
        Label titleLabel = new Label(titre);
        titleLabel.getStyleClass().add("dialog-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn-logout");
        cancelBtn.setOnAction(e -> dialog.close());

        Button okBtn = new Button(labelOk);
        okBtn.getStyleClass().add(danger ? "btn-delete" : "btn-login");
        okBtn.setOnAction(e -> {
            resultat[0] = true;
            dialog.close();
        });
        okBtn.setDefaultButton(true); // Entrée = valide

        // Spacer pousse les boutons à droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox boutons = new HBox(10, spacer, cancelBtn, okBtn);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14, titleLabel, messageLabel, boutons);
        card.getStyleClass().add("dialog-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(440);

        // ── Overlay semi-transparent autour de la carte ──
        // StackPane = couche noire 60% + la carte centrée dessus
        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("dialog-overlay");
        overlay.setPadding(new Insets(20));

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                ConfirmDialog.class.getResource("/fxml/style.css").toExternalForm());

        // ESC ferme la boîte = annuler
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait(); // bloque jusqu'à la fermeture

        return resultat[0];
    }
}
