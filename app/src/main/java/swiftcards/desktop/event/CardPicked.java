package swiftcards.desktop.event;

import swiftcards.core.card.Card;
import swiftcards.core.util.EventBase;

public class CardPicked extends EventBase<Card> {
    public CardPicked(Card card) {
        super(card);
    }
}
