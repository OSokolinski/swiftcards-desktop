package swiftcards.desktop.implementation;

import javafx.application.Platform;
import swiftcards.core.card.Card;
import swiftcards.core.card.CardColor;
import swiftcards.core.game.Summary;
import swiftcards.core.networking.ActivityPresenter;
import swiftcards.core.player.activities.PlayerStatusUpdated.PlayerStatusData;
import swiftcards.desktop.controller.GameplayController;
import swiftcards.desktop.util.GuiTools;

import java.util.stream.Collectors;

public class GuiNetworkActivityPresenter implements ActivityPresenter {

    private final GuiTools guiTools = new GuiTools();
    private final GameplayController controller;

    public GuiNetworkActivityPresenter(GameplayController controller) {
        this.controller = controller;
    }

    @Override
    public void cardLiedOnTableByPlayer(Card card) {
        if (card == null) {
            return;
        }
        Platform.runLater(() -> {
            controller.currentCardPane.getChildren().clear();
            controller.currentCardPane.getChildren().add(guiTools.drawCard(card));
            if (card.getCardColor() != null) {
                cardColorChosen(card.getCardColor());
            }
        });
    }

    @Override
    public void cardColorChosen(CardColor cardColor) {
        Platform.runLater(() -> {
            controller.currentColorPane.getChildren().clear();
            controller.currentColorPane.getChildren().add(guiTools.drawColor(cardColor));
        });
    }

    @Override
    public void cardsPulledByPlayer(int cardAmount, int playerId) {
        controller.setActionText("cards_pulled", getPlayerName(playerId), cardAmount);
    }

    @Override
    public void playerStopped(int playerId) {
        controller.setActionText("player_stopped", getPlayerName(playerId));
    }

    @Override
    public void playerTurnStarted(int playerId) {
        controller.setActionText("player_turn", getPlayerName(playerId));
        Platform.runLater(() -> {
            controller.drawPlayerIcons(playerId);
        });
    }

    @Override
    public void initialCardSetOnTable(Card card) {
        Platform.runLater(() -> {
            if (card.getCardColor() != null) {
                cardColorChosen(card.getCardColor());
            }
        });
    }

    @Override
    public void cardsToTakeIncreased(int cardsToTakeAmount) {
        controller.setActionText("cards_to_take", cardsToTakeAmount);
    }

    @Override
    public void gameQueueSequenceReverted() {
        controller.setActionText("sequence_reverted");
    }

    @Override
    public void playerStatusUpdated(PlayerStatusData playerStatusData) {
        controller.getPlayerStatusData().put(playerStatusData.getPlayerId(), playerStatusData);
    }

    @Override
    public void gameFinished(Summary summary) {
        controller.redirectToSummary(summary, controller.getGameSettings().players);
    }

    private String getPlayerName(int playerId) {
        return controller.getGameSettings()
            .players
            .stream()
            .filter(player -> player.playerId == playerId)
            .collect(Collectors.toList())
            .get(0)
            .playerName;
    }
}
