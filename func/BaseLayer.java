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

            // Emphasize chest area with slight variations
            Helpers.adjustPixelBrightness(baseLayer, 23, 21, skinTone, 1.2 + random.nextDouble() * 0.1);
            Helpers.adjustPixelBrightness(baseLayer, 24, 21, skinTone, 1.2 + random.nextDouble() * 0.1);

            Helpers.adjustPixelBrightness(baseLayer, 23, 22, skinTone, 1.1 + random.nextDouble() * 0.1);
            Helpers.adjustPixelBrightness(baseLayer, 24, 22, skinTone, 1.1 + random.nextDouble() * 0.1);

            // Slightly darken waist area with variation
            Helpers.adjustPixelBrightness(baseLayer, 21, 24, skinTone, 0.75 + random.nextDouble() * 0.1);
            Helpers.adjustPixelBrightness(baseLayer, 26, 24, skinTone, 0.75 + random.nextDouble() * 0.1);

            // Slightly widen hips with highlights and variation
            Helpers.adjustPixelBrightness(baseLayer, 23, 27, skinTone, 1.1 + random.nextDouble() * 0.1);
            Helpers.adjustPixelBrightness(baseLayer, 24, 27, skinTone, 1.1 + random.nextDouble() * 0.1);
        } else {
            Helpers.fillRect(baseLayer, g, 20, 20, 8, 12, skinTone); // Wider torso

            // Indicate musculature with slight random variations
            Helpers.adjustPixelBrightness(baseLayer, 22, 22, skinTone, 1.1 + random.nextDouble() * 0.1);
            Helpers.adjustPixelBrightness(baseLayer, 25, 22, skinTone, 1.1 + random.nextDouble() * 0.1);

            // Slightly shade the abdominal area with variation
            Helpers.adjustPixelBrightness(baseLayer, 23, 24, skinTone, 0.9 - random.nextDouble() * 0.1);
            Helpers.adjustPixelBrightness(baseLayer, 24, 24, skinTone, 0.9 - random.nextDouble() * 0.1);
        }

        // Fill arms and legs uniformly
        Helpers.fillRect(baseLayer, g, 44, 20, 4, 12, skinTone);
        Helpers.fillRect(baseLayer, g, 36, 20, 4, 12, skinTone);
        Helpers.fillRect(baseLayer, g, 4, 20, 4, 12, skinTone);
        Helpers.fillRect(baseLayer, g, 20, 52, 4, 12, skinTone);

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
