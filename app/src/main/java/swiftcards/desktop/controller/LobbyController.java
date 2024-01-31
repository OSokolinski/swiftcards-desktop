package swiftcards.desktop.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import swiftcards.core.game.GameController;
import swiftcards.core.game.lobby.*;
import swiftcards.core.networking.ConnectionChannel;
import swiftcards.core.networking.PlayerCredentials;
import swiftcards.core.networking.event.ChannelDisconnected;
import swiftcards.core.networking.event.ExceptionThrown;
import swiftcards.core.networking.event.lobby.ConnectedSuccessfully;
import swiftcards.core.networking.event.lobby.GameStarted;
import swiftcards.core.networking.event.lobby.LobbyPrepared;
import swiftcards.core.networking.event.lobby.SettingsUpdated;
import swiftcards.core.util.ConfigService;
import swiftcards.core.util.Subscriber;
import swiftcards.desktop.misc.LobbyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class LobbyController {
    @FXML
    public Button playerSlot1Btn;
    @FXML
    public Button playerSlot2Btn;
    @FXML
    public Button playerSlot3Btn;
    @FXML
    public Button playerSlot4Btn;
    @FXML
    public Button playerSlot5Btn;
    @FXML
    public Button playerSlot6Btn;
    @FXML
    public Button playerSlot7Btn;
    @FXML
    public Button playerSlot8Btn;
    @FXML
    public Button playerSlot9Btn;
    @FXML
    public Button playerSlot10Btn;
    @FXML
    public GridPane playerSlotsPane;
    @FXML
    public Text loadingInfoText;
    @FXML
    public CheckBox readinessCheckbox;
    @FXML
    public Button startGameBtn;

    @FXML
    private ResourceBundle resources;

    private LobbyType lobbyType = null;
    private Lobby lobby = null;
    private GameSettings currentSettings = null;

    private final Subscriber<GameSettings> onLobbyPreparedSubscriber = new Subscriber<>(this::drawLobbyManagement);
    private final Subscriber<Void> onSuccessfullyConnectedSubscriber = new Subscriber<>((arg) -> drawLobbyManagement(null));
    private final Subscriber<GameSettings> onSettingsUpdatedSubscriber = new Subscriber<>(this::updateLobbyControls);
    private final Subscriber<ConnectionChannel> onDisconnectedSubscriber = new Subscriber<>(this::showDisconnectionModal);
    private final Subscriber<Void> onGameStartedSubscriber = new Subscriber<>(this::runAsGuest);
    private final Subscriber<Exception> onExceptionThrownSubscriber = new Subscriber<>(this::handleLobbyException);

    private final EventHandler<ActionEvent> onSlotButtonClickedHandler = (event) -> {
        if (currentSettings == null) return;

        EventTarget target = event.getTarget();
        if (!(target instanceof Button)) return;

        Button button = (Button) target;
        String buttonId = button.getId();
        String buttonIteratorStr = buttonId.replace("playerSlot", "").replace("Btn", "");
        int buttonIteratorInt = Integer.parseInt(buttonIteratorStr) - 1;
        PlayerSlot slot = currentSettings.players.get(buttonIteratorInt);

        if (slot.status == PlayerSlot.PlayerSlotStatus.HUMAN) return;

        List<PlayerSlot.PlayerSlotStatus> statusList = lobbyType == LobbyType.SINGLE_PLAYER ?
             new ArrayList<>(List.of(PlayerSlot.PlayerSlotStatus.AI, PlayerSlot.PlayerSlotStatus.CLOSED))
            : new ArrayList<>(List.of(PlayerSlot.PlayerSlotStatus.NETWORK_OPEN, PlayerSlot.PlayerSlotStatus.AI, PlayerSlot.PlayerSlotStatus.CLOSED));

        int indexOfStatus = statusList.indexOf(slot.status);
        if (indexOfStatus >= (statusList.size() - 1)) {
            slot.status = statusList.get(0);
        }
        else {
            slot.status = statusList.get(indexOfStatus + 1);
        }
        slot.playerId = -1;

        ((HostLobby) lobby).updateSettings(currentSettings);
    };

    @FXML
    public void toggleReadiness() {
        ((GuestLobby) lobby).toggleIsReady();
    }

    @FXML
    public void showQuitingConfirmModal() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(resources.getString("lobby.exit_modal_confirm_title"));
            alert.setContentText(resources.getString("lobby.exit_modal_confirm_message"));
            alert.setHeaderText(null);
            Optional<ButtonType> clickedButton = alert.showAndWait();
            if (clickedButton.isPresent() && clickedButton.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    redirectToMenu();
                }
                catch (Exception e) {
                    ConfigService.getInstance().logError("Error while redirecting to gameplay: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void startGame() throws Exception {
        redirectToGameplay();
    }

    public void setLobbyMode(LobbyType lobbyType) {
        this.lobbyType = lobbyType;
    }

    public void startLobby(Integer port, String ipAddress) {
        if (lobbyType == null) return;

        if (lobbyType == LobbyType.SINGLE_PLAYER) {
            lobby = new HostLobby(Lobby.LobbyType.OFFLINE, null);
        }
        else if (lobbyType == LobbyType.LAN_HOST) {
            lobby = new HostLobby(Lobby.LobbyType.LAN, port);
        }
        else if (lobbyType == LobbyType.GUEST) {
            PlayerCredentials cred = new PlayerCredentials(ConfigService.getInstance().getPlayerName());

            lobby = new GuestLobby(cred, ipAddress, port);
        }

        lobby.getEventBus().on(LobbyPrepared.class, onLobbyPreparedSubscriber);
        lobby.getEventBus().on(ConnectedSuccessfully.class, onSuccessfullyConnectedSubscriber);
        lobby.getEventBus().on(SettingsUpdated.class, onSettingsUpdatedSubscriber);
        lobby.getEventBus().on(ChannelDisconnected.class, onDisconnectedSubscriber);
        lobby.getEventBus().on(GameStarted.class, onGameStartedSubscriber);
        lobby.getEventBus().on(ExceptionThrown.class, onExceptionThrownSubscriber);
        lobby.prepare();
    }

    private void drawLobbyManagement(GameSettings gameSettings) {
        loadingInfoText.setVisible(false);
        playerSlotsPane.setVisible(true);
        if (lobbyType == LobbyType.GUEST) {
            readinessCheckbox.setSelected(false);
            readinessCheckbox.setVisible(true);
        }
        else {
            startGameBtn.setVisible(true);
        }
        if (gameSettings != null) {
            updateLobbyControls(gameSettings);
        }
    }

    private void showDisconnectionModal(ConnectionChannel arg) {
        Platform.runLater(() -> {
            ButtonType button = new ButtonType(resources.getString("button.ok"), ButtonBar.ButtonData.OK_DONE);

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle(resources.getString("lobby.modal_closed_title"));
            dialog.setContentText(resources.getString("lobby.modal_closed_message"));
            dialog.getDialogPane()
                .getButtonTypes()
                .add(button);
            dialog.show();
            dialog.setOnHidden((a) -> {
                try {
                    redirectToMenu();
                }
                catch (Exception e) {
                    System.out.println(e);
                }
            });
        });
    }

    private void runAsGuest(Void arg) {
        if (lobby instanceof HostLobby) {
            return;
        }
        Platform.runLater(() -> {
            try {
                redirectToGameplay();
            } catch (Exception e) {
                ConfigService.getInstance().logError("Error while redirecting to gameplay: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updateLobbyControls(GameSettings settings) {
        currentSettings = settings;

        int slotId = 0;
        boolean arePlayersReady = true;
        for (PlayerSlot slot : settings.players) {
            Button lobbyButton = getSlotButton(slotId);

            if (lobby instanceof HostLobby && lobbyButton.getOnAction() != onSlotButtonClickedHandler) {
                lobbyButton.setOnAction(onSlotButtonClickedHandler);
                lobbyButton.setDisable(false);
            }

            String slotDescription = slot.getDescription() + (slot.isReady() ? String.format(" (%s)", resources.getString("lobby.readiness_hint")) : "");

            if (
                slot.status == PlayerSlot.PlayerSlotStatus.NETWORK_OPEN
                ||  (slot.status != PlayerSlot.PlayerSlotStatus.CLOSED && !slot.isReady())
            ) {
                arePlayersReady = false;
            }
            Platform.runLater(() -> lobbyButton.setText(slotDescription));
            slotId++;
        }

        startGameBtn.setDisable(!arePlayersReady);
    }

    private Button getSlotButton(int iterator) {
        return switch (iterator) {
            case 0:
                yield playerSlot1Btn;
            case 1:
                yield playerSlot2Btn;
            case 2:
                yield playerSlot3Btn;
            case 3:
                yield playerSlot4Btn;
            case 4:
                yield playerSlot5Btn;
            case 5:
                yield playerSlot6Btn;
            case 6:
                yield playerSlot7Btn;
            case 7:
                yield playerSlot8Btn;
            case 8:
                yield playerSlot9Btn;
            case 9:
                yield playerSlot10Btn;
            default:
                yield null;
        };
    }

    private void redirectToMenu() throws Exception {
        cleanUp(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"), resources);
        Parent root = loader.load();
        Stage stage = (Stage) loadingInfoText.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private void redirectToGameplay() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gameplay.fxml"), resources);
        Parent root = loader.load();

        Stage stage = (Stage) loadingInfoText.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        GameplayController gameplayController = loader.getController();
        if (lobby instanceof HostLobby) {
            HostLobby hostLobby = (HostLobby) lobby;
            GameController gameController = hostLobby.prepareForStart();
            gameplayController.setSettings(hostLobby, gameController, currentSettings);
            gameplayController.drawControls();
            Thread thread = new Thread(() -> {
                try {
                    gameplayController.startGame();
                }
                catch (Exception e) {
                    ConfigService.getInstance().logError("Error while starting the game: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            thread.start();
        }
        else {
            GuestLobby guestLobby = (GuestLobby) lobby;
            gameplayController.setSettings(guestLobby, null, currentSettings);
            gameplayController.drawControls();
        }

        cleanUp(false);
    }

    private void cleanUp(boolean disconnect) {
        lobby.getEventBus().unsubscribe(LobbyPrepared.class, onLobbyPreparedSubscriber);
        lobby.getEventBus().unsubscribe(ConnectedSuccessfully.class, onSuccessfullyConnectedSubscriber);
        lobby.getEventBus().unsubscribe(SettingsUpdated.class, onSettingsUpdatedSubscriber);
        lobby.getEventBus().unsubscribe(GameStarted.class, onGameStartedSubscriber);
        lobby.getEventBus().unsubscribe(ExceptionThrown.class, onExceptionThrownSubscriber);

        if (disconnect) {
            try {
                lobby.disconnect();
            }
            catch (Exception e) {
                ConfigService.getInstance().logError("Error while disconnecting: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (currentSettings != null) {
            int slotId = 0;
            for (PlayerSlot slot : currentSettings.players) {
                Button lobbyButton = getSlotButton(slotId);
                lobbyButton.removeEventHandler(ActionEvent.ACTION, onSlotButtonClickedHandler);
                slotId++;
            }
        }
    }

    private void handleLobbyException(Exception e) {
        ConfigService.getInstance().logError("Caught exception: " + e.getMessage());
        e.printStackTrace();

        Platform.runLater(() -> {
            try {
                redirectToMenu();
            } catch (Exception e1) {
                ConfigService.getInstance().logError("Error while redirecting to menu: " + e1.getMessage());
                e1.printStackTrace();
            }
        });
    }
}
