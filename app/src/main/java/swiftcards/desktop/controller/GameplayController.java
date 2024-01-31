package swiftcards.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import swiftcards.core.card.CardColor;
import swiftcards.core.game.GameController;
import swiftcards.core.game.Summary;
import swiftcards.core.game.lobby.GameSettings;
import swiftcards.core.game.lobby.GuestLobby;
import swiftcards.core.game.lobby.Lobby;
import swiftcards.core.game.lobby.PlayerSlot;
import swiftcards.core.networking.NetworkActivityHandler;
import swiftcards.core.player.NetworkPlayerPrompterHandler;
import swiftcards.core.player.activities.PlayerStatusUpdated.PlayerStatusData;
import swiftcards.core.util.ConfigService;
import swiftcards.core.util.DefaultEventBus;
import swiftcards.core.util.EventBus;
import swiftcards.desktop.event.ColorPicked;
import swiftcards.desktop.implementation.GuiNetworkActivityPresenter;
import swiftcards.desktop.implementation.GuiPlayerPrompter;
import swiftcards.desktop.util.GuiTools;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;


public class GameplayController {

    @FXML
    public GridPane cardGridPane;
    @FXML
    public Pane currentCardPane;
    @FXML
    public Pane currentColorPane;
    @FXML
    public Pane colorSelectionPane;
    @FXML
    public Text actionDescription;

    @FXML
    private ResourceBundle resources;

    private final EventBus gameplayEventBus = new DefaultEventBus();

    private final GuiTools guiTools = new GuiTools();

    private Lobby lobby;
    private GameController gameController = null;
    private GameSettings gameSettings = null;
    private NetworkActivityHandler activityHandler = null;
    private NetworkPlayerPrompterHandler playerPrompterHandler = null;
    private final LinkedHashMap<Integer, PlayerStatusData> playerStatusData = new LinkedHashMap<>();

    public void setSettings(
            Lobby lobby,
            GameController gameController,
            GameSettings gameSettings
    ) {
        this.lobby = lobby;
        this.gameController = gameController;
        this.gameSettings = gameSettings;

        if (lobby instanceof GuestLobby) {
            this.activityHandler = ((GuestLobby) lobby).createNetworkActivityHandler();
            this.playerPrompterHandler = ((GuestLobby) lobby).createNetworkPlayerPrompterHandler();
        }

        ConfigService.getInstance().setNetworkActivityPresenter(new GuiNetworkActivityPresenter(this));
        ConfigService.getInstance().setPlayerPrompter(new GuiPlayerPrompter(this));
    }

    public GameSettings getGameSettings() {
        return gameSettings;
    }

    public void drawControls() {
        drawPlayerIcons(null);

        List<CardColor> colorList = Arrays.asList(CardColor.RED, CardColor.GREEN, CardColor.BLUE, CardColor.YELLOW);
        for (CardColor color : colorList) {
            ImageView imgView = guiTools.drawColor(color);
            imgView.setCursor(Cursor.HAND);
            imgView.addEventHandler(MouseEvent.MOUSE_CLICKED, (evt) -> {
                gameplayEventBus.emit(new ColorPicked(color));
            });
            getColorPane(color.ordinal()).getChildren().add(imgView);
        }
    }

    public void drawPlayerIcons(Integer activePlayerId) {
        for (PlayerSlot playerSlot : gameSettings.players) {
            boolean isActivePlayer = activePlayerId != null && activePlayerId == playerSlot.playerId;
            if (
                playerSlot.status != PlayerSlot.PlayerSlotStatus.NETWORK_USED
                    && playerSlot.status != PlayerSlot.PlayerSlotStatus.AI
                    && playerSlot.status != PlayerSlot.PlayerSlotStatus.HUMAN
            ) {
                continue;
            }

            PlayerStatusData playerData = playerStatusData.get(playerSlot.playerId);

            String playerComment = String.format(resources.getString("gameplay.player_has_cards"), ConfigService.getInstance().getInitialPlayerCardAmount());
            if (playerData != null) {
                if (playerData.hasFinishedGame()) {
                    playerComment = resources.getString("gameplay.player_has_finished");
                }
                else {
                    playerComment = String.format(resources.getString("gameplay.player_has_cards"), playerData.getCardAmount());
                }
            }
            String playerName = String.format("%s (%s)", playerSlot.playerName, playerComment);

            getPlayerText(playerSlot.playerId).setText(playerName);
            if (isActivePlayer) {
                getPlayerText(playerSlot.playerId).setStyle("-fx-font-weight: bold");
            }
            else {
                getPlayerText(playerSlot.playerId).setStyle(null);
            }
            getPlayerPane(playerSlot.playerId).getChildren().clear();
            getPlayerPane(playerSlot.playerId).getChildren().add(guiTools.drawPlayerIcon(isActivePlayer));
        }
    }

    public void startGame() throws Exception {
        gameController.startGame();
    }

    public Text getPlayerText(int playerId) {
        return (Text) currentCardPane.getScene().lookup("#playerName" + (playerId + 1));
    }

    public Pane getPlayerPane(int playerId) {
        return (Pane) currentCardPane.getScene().lookup("#playerPane" + (playerId + 1));
    }

    public Pane getColorPane(int paneId) {
        return (Pane) currentCardPane.getScene().lookup("#colorSelection" + (paneId + 1));
    }

    public void setActionText(String variable, Object ...args) {
        actionDescription.setText(String.format(resources.getString("gameplay.action."+variable), args));
    }

    public EventBus getGameplayEventBus() {
        return gameplayEventBus;
    }

    public LinkedHashMap<Integer, PlayerStatusData> getPlayerStatusData() {
        return playerStatusData;
    }

    public void redirectToSummary(Summary summary, List<PlayerSlot> playerSlotList) {
        Platform.runLater(() -> {
            colorSelectionPane.getChildren().clear();
            cardGridPane.getChildren().clear();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("summary.fxml"), resources);
                Parent root = loader.load();

                SummaryController summaryController = loader.getController();
                summaryController.printSummary(summary, playerSlotList);

                Stage stage = (Stage) cardGridPane.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                System.out.println(e);
            }
        });
        if (activityHandler != null) {
            activityHandler.stop();
        }
        if (playerPrompterHandler != null) {
            playerPrompterHandler.stop();
        }
        lobby.disconnect();
    }
}
