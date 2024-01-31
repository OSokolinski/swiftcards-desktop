package swiftcards.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import swiftcards.core.game.Summary;
import swiftcards.core.game.lobby.PlayerSlot;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SummaryController {

    @FXML
    public GridPane playerGridPane;

    @FXML
    public Text turnCountText;

    @FXML
    private ResourceBundle resources;

    @FXML
    public void redirectToMenu() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"), resources);
        Parent root = loader.load();
        Stage stage = (Stage) playerGridPane.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void printSummary(Summary summary, List<PlayerSlot> playerSlotList) {

        Platform.runLater(() -> {

            int i = 1;
            for (int playerId : summary.getWinnerList()) {
                Summary.PlayerSummary playerSummary = summary.getPlayerSummary().get(playerId);
                PlayerSlot playerSlot = playerSlotList.stream()
                        .filter(p -> p.playerId == playerId)
                        .collect(Collectors.toList())
                        .get(0);

                playerGridPane.add(new Text(i + ". " + playerSlot.playerName), 0, i);
                playerGridPane.add(new Text(Integer.toString(playerSummary.getTurnCount())), 1, i);
                playerGridPane.add(new Text(Integer.toString(playerSummary.getMaxCardAmount())), 2, i);
                i++;
            }

            turnCountText.setText(turnCountText.getText() + ": " + summary.getTurnCount());
        });
    }
}
