package swiftcards.desktop.implementation;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import swiftcards.core.card.Card;
import swiftcards.core.card.CardColor;
import swiftcards.core.player.PlayerPrompter;
import swiftcards.core.util.Freezable;
import swiftcards.core.util.Subscriber;
import swiftcards.desktop.controller.GameplayController;
import swiftcards.desktop.event.CardPicked;
import swiftcards.desktop.event.ColorPicked;
import swiftcards.desktop.util.GuiTools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class GuiPlayerPrompter extends Freezable implements PlayerPrompter {

    private final GuiTools guiTools = new GuiTools();
    private final GameplayController controller;
    private final AtomicBoolean noCardAvailable = new AtomicBoolean(false);

    private List<Card> playerCards = new ArrayList<>();

    public GuiPlayerPrompter(GameplayController controller) {
        this.controller = controller;
    }

    @Override
    public void showCardOnTable(Card cardOnTable) {
        Platform.runLater(() -> {
            controller.currentCardPane.getChildren().clear();
            controller.currentCardPane.getChildren().add(guiTools.drawCard(cardOnTable));
        });
    }

    @Override
    public void showPlayerCards(List<Integer> eligibleCardIds) {
        noCardAvailable.set(eligibleCardIds.size() < 1);

        Platform.runLater(() -> {
            controller.cardGridPane.getChildren().clear();

            int columnCount = controller.cardGridPane.getColumnCount();
            int column = 0, row = 0;
            for (Card card : playerCards) {
                boolean isAvailable = eligibleCardIds.contains(card.getId());
                ImageView imageView = guiTools.drawCard(card, !isAvailable);
                if (isAvailable) {
                    imageView.setCursor(Cursor.HAND);

                    // Setting FXML on click event and triggering CardPicked event
                    imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> {
                        controller.getGameplayEventBus().emit(new CardPicked(card));
                    });
                }
                controller.cardGridPane.add(imageView, column, row);

                if (column >= (columnCount - 1)) {
                    row++;
                    column = 0;
                }
                else {
                    column++;
                }
            }
        });
    }

    @Override
    public Card selectCard() {

        if (noCardAvailable.get()) {
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(this::resume);
            freeze();
            return null;
        }
        else {
            AtomicReference<Card> cardRef = new AtomicReference<>();
            Subscriber<Card> cardPickedSubscriber = new Subscriber<>((card) -> {
                cardRef.set(card);
                resume();
            });
            controller.getGameplayEventBus().on(CardPicked.class, cardPickedSubscriber);
            freeze();
            controller.getGameplayEventBus().unsubscribe(CardPicked.class, cardPickedSubscriber);
            return cardRef.get();
        }
    }

    @Override
    public CardColor selectCardColor() {
        controller.colorSelectionPane.setVisible(true);
        AtomicReference<CardColor> colorRef = new AtomicReference<>();
        Subscriber<CardColor> cardPickedSubscriber = new Subscriber<>((card) -> {
            colorRef.set(card);
            resume();
        });
        controller.getGameplayEventBus().on(ColorPicked.class, cardPickedSubscriber);
        freeze();
        controller.getGameplayEventBus().unsubscribe(ColorPicked.class, cardPickedSubscriber);
        controller.colorSelectionPane.setVisible(false);
        return colorRef.get();
    }

    @Override
    public void refreshCards(List<Card> cards) {
        showPlayerCards(new ArrayList<>());
        playerCards = cards;
    }
}
