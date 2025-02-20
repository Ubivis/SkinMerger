package func;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

import static func.Helpers.*;


public class BaseLayer {

    public static void extractBaseLayer(BufferedImage skin, String gender, String race) throws IOException {
        BufferedImage baseLayer = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = baseLayer.createGraphics();
    
        Color skinTone = Helpers.sampleFaceSkinTone(skin);
    
        Random random = new Random();
    
        if (gender.equalsIgnoreCase("female")) {
            Helpers.fillRect(baseLayer, g, 21, 20, 6, 12, skinTone); // Narrower torso
    
            // Randomly determine breast size: A, B, C, D, DD
            String[] sizes = {"A", "B", "C", "D", "DD"};
            String breastSize = sizes[random.nextInt(sizes.length)];
    
            // Apply shading based on breast size
            switch (breastSize) {
                case "A":
                    Helpers.adjustPixelBrightness(baseLayer, 23, 21, skinTone, 1.1);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 21, skinTone, 1.1);
                    break;
                case "B":
                    Helpers.adjustPixelBrightness(baseLayer, 22, 21, skinTone, 1.15);
                    Helpers.adjustPixelBrightness(baseLayer, 23, 21, skinTone, 1.2);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 21, skinTone, 1.2);
                    Helpers.adjustPixelBrightness(baseLayer, 25, 21, skinTone, 1.15);
                    break;
                case "C":
                    Helpers.adjustPixelBrightness(baseLayer, 22, 21, skinTone, 1.15);
                    Helpers.adjustPixelBrightness(baseLayer, 23, 21, skinTone, 1.25);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 21, skinTone, 1.25);
                    Helpers.adjustPixelBrightness(baseLayer, 25, 21, skinTone, 1.15);
                    Helpers.adjustPixelBrightness(baseLayer, 23, 22, skinTone, 1.1);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 22, skinTone, 1.1);
                    break;
                case "D":
                    Helpers.adjustPixelBrightness(baseLayer, 22, 21, skinTone, 1.2);
                    Helpers.adjustPixelBrightness(baseLayer, 23, 21, skinTone, 1.3);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 21, skinTone, 1.3);
                    Helpers.adjustPixelBrightness(baseLayer, 25, 21, skinTone, 1.2);
                    Helpers.adjustPixelBrightness(baseLayer, 23, 22, skinTone, 1.15);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 22, skinTone, 1.15);
                    break;
                case "DD":
                    Helpers.adjustPixelBrightness(baseLayer, 22, 21, skinTone, 1.25);
                    Helpers.adjustPixelBrightness(baseLayer, 23, 21, skinTone, 1.35);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 21, skinTone, 1.35);
                    Helpers.adjustPixelBrightness(baseLayer, 25, 21, skinTone, 1.25);
                    Helpers.adjustPixelBrightness(baseLayer, 22, 22, skinTone, 1.1);
                    Helpers.adjustPixelBrightness(baseLayer, 23, 22, skinTone, 1.2);
                    Helpers.adjustPixelBrightness(baseLayer, 24, 22, skinTone, 1.2);
                    Helpers.adjustPixelBrightness(baseLayer, 25, 22, skinTone, 1.1);
                    break;
            }
    
            // Subtle shading below chest
            Helpers.adjustPixelBrightness(baseLayer, 23, 23, skinTone, 0.9);
            Helpers.adjustPixelBrightness(baseLayer, 24, 23, skinTone, 0.9);
    
            // Slightly darken waist sides
            Helpers.adjustPixelBrightness(baseLayer, 21, 24, skinTone, 0.8);
            Helpers.adjustPixelBrightness(baseLayer, 26, 24, skinTone, 0.8);
    
            // Highlight hips
            Helpers.adjustPixelBrightness(baseLayer, 22, 26, skinTone, 1.05);
            Helpers.adjustPixelBrightness(baseLayer, 23, 27, skinTone, 1.1);
            Helpers.adjustPixelBrightness(baseLayer, 24, 27, skinTone, 1.1);
            Helpers.adjustPixelBrightness(baseLayer, 25, 26, skinTone, 1.05);
    
            // Random soft shading variations for smoothness
            for (int y = 20; y < 32; y++) {
                for (int x = 21; x < 27; x++) {
                    if (random.nextDouble() < 0.05) { // 5% chance
                        double factor = 0.95 + (random.nextDouble() * 0.1); // Subtle variation
                        Helpers.adjustPixelBrightness(baseLayer, x, y, skinTone, factor);
                    }
                }
            }
        } else {
            Helpers.fillRect(baseLayer, g, 20, 20, 8, 12, skinTone); // Wider torso
    
            // Indicate chest muscles
            Helpers.adjustPixelBrightness(baseLayer, 22, 21, skinTone, 1.1);
            Helpers.adjustPixelBrightness(baseLayer, 25, 21, skinTone, 1.1);
    
            // Indicate abdominal muscles
            Helpers.adjustPixelBrightness(baseLayer, 23, 23, skinTone, 0.95);
            Helpers.adjustPixelBrightness(baseLayer, 24, 23, skinTone, 0.95);
            Helpers.adjustPixelBrightness(baseLayer, 23, 24, skinTone, 0.9);
            Helpers.adjustPixelBrightness(baseLayer, 24, 24, skinTone, 0.9);
            Helpers.adjustPixelBrightness(baseLayer, 23, 25, skinTone, 0.95);
            Helpers.adjustPixelBrightness(baseLayer, 24, 25, skinTone, 0.95);
    
            // Random slight variations in shading
            for (int y = 20; y < 32; y++) {
                for (int x = 20; x < 28; x++) {
                    if (random.nextDouble() < 0.1) { // 10% chance
                        double factor = 0.9 + (random.nextDouble() * 0.2); // Random brightness variation
                        Helpers.adjustPixelBrightness(baseLayer, x, y, skinTone, factor);
                    }
                }
            }
        }
    
        // Fill and shade arms and legs
        Helpers.fillRect(baseLayer, g, 44, 20, 4, 12, skinTone);
        Helpers.fillRect(baseLayer, g, 36, 20, 4, 12, skinTone);
        Helpers.fillRect(baseLayer, g, 4, 20, 4, 12, skinTone);
        Helpers.fillRect(baseLayer, g, 20, 52, 4, 12, skinTone);
    
        Helpers.addLimbShading(baseLayer, skinTone, gender, race);
    
        g.dispose();
    
        String path = String.format("templates/base_layers/%s/%s", race, gender);
        Helpers.saveUniqueTemplate(baseLayer, path);
    }

    public static void applyTorsoShading(BufferedImage baseLayer, Color skinTone, String gender) {
        Graphics2D g = baseLayer.createGraphics();
    
        // Example subtle shading (adjust these values later based on taste)
        int chestHighlightY = 22;
        int chestShadowY = 26;
    
        for (int x = 22; x <= 25; x++) { // Chest Highlight
            Helpers.adjustPixelBrightness(baseLayer, x, chestHighlightY, skinTone, 1.2);
        }
        for (int x = 22; x <= 25; x++) { // Lower Torso Shadow
            Helpers.adjustPixelBrightness(baseLayer, x, chestShadowY, skinTone, 0.8);
        }
    
        g.dispose();
    }
    
    public static void applyLimbShading(BufferedImage baseLayer, Color skinTone, int startX, int startY, int width, int height) {
        Graphics2D g = baseLayer.createGraphics();
    
        // Example limb shading: darker towards edges for cylindrical appearance
        for (int y = startY; y < startY + height; y++) {
            Helpers.adjustPixelBrightness(baseLayer, startX, y, skinTone, 0.85); // Left side darker
            Helpers.adjustPixelBrightness(baseLayer, startX + width - 1, y, skinTone, 0.85); // Right side darker
        }
    
        g.dispose();
    }
}
