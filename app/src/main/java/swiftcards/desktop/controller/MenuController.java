package swiftcards.desktop.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import swiftcards.core.util.ConfigService;
import swiftcards.core.util.Freezable;
import swiftcards.desktop.misc.LobbyType;

import java.util.Optional;
import java.util.ResourceBundle;

public class MenuController extends Freezable {

    @FXML
    public GridPane menuMainOptions;

    @FXML
    public GridPane menuMultiplayerOptions;

    @FXML
    public TextField playerName;

    @FXML
    private ResourceBundle resources;

    @FXML
    public void toggleMultiplayerMenu() {
        if (playerName.getText() == null || playerName.getText().isEmpty()) {
            playerName.requestFocus();
            return;
        }
        ConfigService.getInstance().setPlayerName(playerName.getText());

        menuMainOptions.setVisible(!menuMainOptions.isVisible());
        menuMultiplayerOptions.setVisible(!menuMultiplayerOptions.isVisible());
        playerName.setDisable(!playerName.isDisabled());
    }

    @FXML
    public void redirectToLobby(ActionEvent event) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"), resources);
        Parent root = loader.load();

        String buttonId = ((Node) event.getSource()).getId();
        LobbyType lobbyType = switch (buttonId) {
            case "singlePlayerButton":
                yield LobbyType.SINGLE_PLAYER;
            case "hostLanGameButton":
                yield LobbyType.LAN_HOST;
            case "hostOnlineGameButton":
                yield LobbyType.ONLINE_HOST;
            case "joinLanGameButton":
            case "joinOnlineGameButton":
            default:
                yield LobbyType.GUEST;
        };

        Integer port = null;
        String ip = null;

        if (lobbyType == LobbyType.SINGLE_PLAYER) {
            if (playerName.getText() == null || playerName.getText().isEmpty()) {
                playerName.requestFocus();
                return;
            }
            ConfigService.getInstance().setPlayerName(playerName.getText());
        }
        else {
            Optional<Pair<Integer, String>> portAndIp = promptPortAndIp(lobbyType == LobbyType.GUEST);
            if (portAndIp.isEmpty()) {
                return;
            }

            port = portAndIp.get().getKey();
            ip = portAndIp.get().getValue();
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        LobbyController lobby = loader.getController();
        lobby.setLobbyMode(lobbyType);
        lobby.startLobby(port, ip);
    }

    public void exit() {
        System.exit(0);
    }

    private Optional<Pair<Integer, String>> promptPortAndIp(boolean promptIp) {
        Dialog<Pair<Integer, String>> dialog = new Dialog<>();
        dialog.setTitle("Insert connection details");

        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        TextField ipField = new TextField();
        ipField.setPromptText("IP");
        TextField portField = new TextField();
        portField.setPromptText("Port");
        portField.setText(Integer.toString(ConfigService.getInstance().getDefaultPort()));

        if (promptIp) {
            gridPane.add(new Label("IP Address:"), 0, 0);
            gridPane.add(ipField, 1, 0);
        }

        gridPane.add(new Label("Port:"), 2, 0);
        gridPane.add(portField, 3, 0);

        dialog.getDialogPane().setContent(gridPane);

        Platform.runLater(ipField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {

                // Validating port
                int port;
                try {
                    port = Integer.parseInt(portField.getText());
                }
                catch (Exception e) {
                    return null;
                }

                // Validating IP
                String ip = ipField.getText();
                String pattern = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
                if (promptIp && !ip.matches(pattern)) {
                    return null;
                }

                return new Pair<>(port, ip);
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
