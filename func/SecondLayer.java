package func;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import static func.Helpers.*;


public class SecondLayer {
    public static void extractEyes(BufferedImage skin, String gender, String race) throws IOException {
        BufferedImage eyesTemplate = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = eyesTemplate.createGraphics();
    
        int leftEyeStartX = 26, leftEyeStartY = 9;
        int rightEyeStartX = 30, rightEyeStartY = 9;
        int eyeWidth = 2, eyeHeight = 2;
    
        Color skinTone = sampleFaceSkinTone(skin);
        boolean foundEyePixels = false;
    
        // Extract left eye
        for (int y = leftEyeStartY; y < leftEyeStartY + eyeHeight; y++) {
            for (int x = leftEyeStartX; x < leftEyeStartX + eyeWidth; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (isLikelyEyePixel(color, skinTone)) {
                    eyesTemplate.setRGB(x, y, color.getRGB());
                    foundEyePixels = true;
                }
            }
        }
    
        // Extract right eye
        for (int y = rightEyeStartY; y < rightEyeStartY + eyeHeight; y++) {
            for (int x = rightEyeStartX; x < rightEyeStartX + eyeWidth; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (isLikelyEyePixel(color, skinTone)) {
                    eyesTemplate.setRGB(x, y, color.getRGB());
                    foundEyePixels = true;
                }
            }
        }
    
        g.dispose();
    
        if (foundEyePixels) {
            convertToGrayscale(eyesTemplate);
            String path = String.format("templates/second_layers/%s/eyes", race);
            saveUniqueTemplate(eyesTemplate, path);
        }
    }
    
    

    public static void extractHair(BufferedImage skin, String gender, String race) throws IOException {
        BufferedImage hair = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = hair.createGraphics();

        int hairStartY = 0;
        int hairEndY = 8;
        int hairStartX = 0;
        int hairEndX = 32;

        Set<Color> hairColors = new HashSet<>();
        boolean foundHair = false;

        for (int y = hairStartY; y < hairEndY; y++) {
            for (int x = hairStartX; x < hairEndX; x++) {
                int pixel = skin.getRGB(x, y);
                Color color = new Color(pixel, true);
                if (color.getAlpha() > 0) {
                    hairColors.add(color);
                }
            }
        }

        Color dominantHairColor = Helpers.getDominantColor(hairColors);
        double tolerance = 30.0;

        for (int y = hairStartY; y < 16; y++) {
            for (int x = hairStartX; x < hairEndX; x++) {
                int pixel = skin.getRGB(x, y);
                Color color = new Color(pixel, true);
                if (color.getAlpha() > 0 && isColorClose(color, dominantHairColor, tolerance)) {
                    g.drawImage(skin.getSubimage(x, y, 1, 1), x, y, null);
                    foundHair = true;
                }
            }
        }
        g.dispose();

        if (foundHair) {
            convertToGrayscale(hair);
            String path = String.format("templates/second_layers/%s/hair", race);
            saveUniqueTemplate(hair, path);
        }
    }

    public static void extractMouth(BufferedImage skin, String gender, String race) throws IOException {
        BufferedImage mouth = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mouth.createGraphics();

        int mouthStartX = 24;
        int mouthEndX = 32;
        int mouthStartY = 20;
        int mouthEndY = 24;

        boolean foundMouthPixels = false;
        for (int y = mouthStartY; y < mouthEndY; y++) {
            for (int x = mouthStartX; x < mouthEndX; x++) {
                int pixel = skin.getRGB(x, y);
                Color color = new Color(pixel, true);
                if (color.getAlpha() > 0) {
                    g.drawImage(skin.getSubimage(x, y, 1, 1), x, y, null);
                    foundMouthPixels = true;
                }
            }
        }
        g.dispose();

        if (foundMouthPixels) {
            convertToGrayscale(mouth);
            String path = String.format("templates/second_layers/%s/mouth", race);
            saveUniqueTemplate(mouth, path);
        }
    }

    public static void extractBeard(BufferedImage skin, String gender, String race) throws IOException {
        BufferedImage beard = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = beard.createGraphics();

        int beardStartX = 16;
        int beardEndX = 48;
        int beardStartY = 20;
        int beardEndY = 48; // Extended further down for long beards

        boolean foundBeardPixels = false;

        for (int y = beardStartY; y < beardEndY; y++) {
            for (int x = beardStartX; x < beardEndX; x++) {
                int pixel = skin.getRGB(x, y);
                Color color = new Color(pixel, true);
                if (color.getAlpha() > 0) {
                    g.drawImage(skin.getSubimage(x, y, 1, 1), x, y, null);
                    foundBeardPixels = true;
                }
            }
        }
        g.dispose();

        if (foundBeardPixels) {
            convertToGrayscale(beard);
            String path = String.format("templates/second_layers/%s/beard", race);
            saveUniqueTemplate(beard, path);
        }
    }
}
