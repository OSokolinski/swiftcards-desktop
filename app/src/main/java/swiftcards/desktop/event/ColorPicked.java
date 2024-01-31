package swiftcards.desktop.event;

import swiftcards.core.card.CardColor;
import swiftcards.core.util.EventBase;

public class ColorPicked extends EventBase<CardColor> {
    public ColorPicked(CardColor color) {
        super(color);
    }
}
