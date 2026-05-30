package org.example.wtg.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Boîte de dialogue pour changer son mot de passe.
 * Retourne [ancienMdp, nouveauMdp] ou null si annulé.
 */
public final class PasswordDialog {

    private PasswordDialog() {}

    public static String[] show() {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Changer le mot de passe");

        final String[][] resultat = { null };

        Label titleLabel = new Label("Changer le mot de passe");
        titleLabel.getStyleClass().add("dialog-title");

        PasswordField ancienField = new PasswordField();
        ancienField.setPromptText("Mot de passe actuel");
        ancienField.getStyleClass().add("text-field");

        PasswordField nouveauField = new PasswordField();
        nouveauField.setPromptText("Nouveau mot de passe (min. 8 caractères)");
        nouveauField.getStyleClass().add("text-field");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer le nouveau mot de passe");
        confirmField.getStyleClass().add("text-field");

        Label errLabel = new Label();
        errLabel.getStyleClass().add("dialog-message");
        errLabel.setStyle("-fx-text-fill: #fca5a5;");
        errLabel.setWrapText(true);
        errLabel.setVisible(false);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn-logout");
        cancelBtn.setOnAction(e -> dialog.close());

        Button okBtn = new Button("Changer");
        okBtn.getStyleClass().add("btn-login");
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> {
            String ancien  = ancienField.getText();
            String nouveau = nouveauField.getText();
            String confirm = confirmField.getText();
            if (ancien.isBlank() || nouveau.isBlank()) {
                errLabel.setText("Remplissez tous les champs.");
                errLabel.setVisible(true); return;
            }
            if (nouveau.length() < 8) {
                errLabel.setText("Le nouveau mot de passe doit faire au moins 8 caractères.");
                errLabel.setVisible(true); return;
            }
            if (!nouveau.equals(confirm)) {
                errLabel.setText("Les deux mots de passe ne correspondent pas.");
                errLabel.setVisible(true); return;
            }
            resultat[0] = new String[]{ ancien, nouveau };
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox boutons = new HBox(10, spacer, cancelBtn, okBtn);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14, titleLabel, ancienField, nouveauField, confirmField, errLabel, boutons);
        card.getStyleClass().add("dialog-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(420);

        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("dialog-overlay");
        overlay.setPadding(new Insets(20));

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                PasswordDialog.class.getResource("/fxml/style.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) dialog.close();
        });

        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();

        return resultat[0];
    }
}
