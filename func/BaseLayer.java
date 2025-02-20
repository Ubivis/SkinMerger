package func;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static func.Helpers.*;


public class BaseLayer {

    public static void extractBaseLayer(BufferedImage skin, String gender, String race) throws IOException {
        BufferedImage baseLayer = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = baseLayer.createGraphics();
    
        Color skinTone = sampleFaceSkinTone(skin);
    
        // Fill main body parts with skin tone (front parts)
        Helpers.fillRect(baseLayer, g, 20, 20, 8, 12, skinTone);  // Torso front
        Helpers.fillRect(baseLayer, g, 44, 20, 4, 12, skinTone);  // Right arm front
        Helpers.fillRect(baseLayer, g, 36, 20, 4, 12, skinTone);  // Left arm front
        Helpers.fillRect(baseLayer, g, 4, 20, 4, 12, skinTone);   // Right leg front
        Helpers.fillRect(baseLayer, g, 20, 52, 4, 12, skinTone);  // Left leg front
    
        // Apply shading for anatomy and curvature (while keeping the skin tone color base)
        applyTorsoShading(baseLayer, skinTone, gender);
        applyLimbShading(baseLayer, skinTone, 44, 20, 4, 12); // Right arm
        applyLimbShading(baseLayer, skinTone, 36, 20, 4, 12); // Left arm
        applyLimbShading(baseLayer, skinTone, 4, 20, 4, 12);  // Right leg
        applyLimbShading(baseLayer, skinTone, 20, 52, 4, 12); // Left leg
    
        g.dispose();
    
        String path = String.format("templates/base_layers/%s/%s", race, gender);
        saveUniqueTemplate(baseLayer, path);
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
