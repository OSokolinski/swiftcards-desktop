package swiftcards.desktop.util;

import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import swiftcards.core.card.*;

import java.util.LinkedHashMap;

public class GuiTools {

    private LinkedHashMap<CardColor, Color> colorMap;

    public GuiTools() {
        colorMap = new LinkedHashMap<>();
        colorMap.put(CardColor.RED, Color.rgb(255, 79, 79));
        colorMap.put(CardColor.GREEN, Color.rgb(79, 221, 93));
        colorMap.put(CardColor.BLUE, Color.rgb(122, 155, 255));
        colorMap.put(CardColor.YELLOW, Color.rgb(250, 255, 103));
    }

    public ImageView drawCard(Card card) {
        return drawCard(card, false);
    }

    public ImageView drawCard(Card card, boolean markUnavailable) {

        String filePath = "swiftcards/desktop/cards/";
        boolean skipColoring = false;

        if (card instanceof StandardCard) {
            filePath += "card_" + ((StandardCard) card).getCardPresentation();
        }
        else if (card instanceof FunctionalSwitchColorCard) {
            filePath += "card_change_color";
            skipColoring = true;
        }
        else if (card instanceof FunctionalPlusFourCard) {
            filePath += "card_plus_four";
            skipColoring = true;
        }
        else if (card instanceof FunctionalStopCard) {
            filePath += "card_stop";
        }
        else if (card instanceof FunctionalReturnCard) {
            filePath += "card_return";
        }
        else if (card instanceof FunctionalPlusTwoCard) {
            filePath += "card_plus_two";
        }
        filePath += ".png";

        Image loadedImage = new Image(filePath);
        PixelReader pixelReader = loadedImage.getPixelReader();

        WritableImage writableImage = new WritableImage(80, 80);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < writableImage.getHeight(); y++) {
            for (int x = 0; x < writableImage.getWidth(); x++) {
                Color loadedColor = pixelReader.getColor(x, y);

                int borderMin = 2;
                int borderMax = 77;
                if (x <= borderMin || x >= borderMax || y <= borderMin || y >= borderMax) {
                    Color borderColor = markUnavailable ? Color.rgb(150, 150, 150) : Color.rgb(0, 0, 0);
                    pixelWriter.setColor(x, y, borderColor);
                }
                else if (loadedColor.getOpacity() == 0 && !skipColoring) {
                    Color newColor = colorMap.get(card.getCardColor());
                    pixelWriter.setColor(x, y, newColor);
                }
                else {
                    pixelWriter.setColor(x, y, loadedColor);
                }
            }
        }

        ImageView imageView = new ImageView(writableImage);
        if (markUnavailable) {
            imageView.setEffect(new GaussianBlur());
        }

        return imageView;
    }

    public ImageView drawColor(CardColor cardColor) {
        Color color = colorMap.get(cardColor);
        WritableImage writableImage = new WritableImage(50, 50);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < writableImage.getHeight(); y++) {
            for (int x = 0; x < writableImage.getWidth(); x++) {
                int borderMin = 2;
                int borderMax = 47;
                if (x <= borderMin || x >= borderMax || y <= borderMin || y >= borderMax) {
                    pixelWriter.setColor(x, y, Color.rgb(0, 0, 0));
                }
                else {
                    pixelWriter.setColor(x, y, color);
                }
            }
        }

        return new ImageView(writableImage);
    }

    public ImageView drawPlayerIcon(boolean isActive) {
        Image loadedImage = new Image("swiftcards/desktop/cards/user_icon.png");
        ImageView imageView = new ImageView(loadedImage);
        if (!isActive) {
            imageView.setEffect(new GaussianBlur());
        }

        return imageView;
    }


}
