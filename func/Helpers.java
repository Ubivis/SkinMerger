package func;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;

import javax.imageio.ImageIO;
import java.net.URL;
import java.io.IOException;

public class Helpers {

    public static BufferedImage downloadImage(String urlString) throws IOException {
        URL url = new URL(urlString);
        return ImageIO.read(url);
    }

    public static Color sampleFaceSkinTone(BufferedImage skin) {
        java.util.List<Color> samples = Arrays.asList(
                new Color(skin.getRGB(32, 12), true),
                new Color(skin.getRGB(33, 12), true),
                new Color(skin.getRGB(31, 13), true)
        );

        int r = 0, g = 0, b = 0;
        for (Color c : samples) {
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        int size = samples.size();
        return new Color(r / size, g / size, b / size);
    }


    public static void saveUniqueTemplate(BufferedImage image, String folderPath) throws IOException {
        String imageHash = getImageHash(image);
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] existingFiles = folder.listFiles((dir, name) -> name.endsWith(".png"));

        if (existingFiles != null) {
            for (File existingFile : existingFiles) {
                if (existingFile.getName().startsWith(imageHash)) {
                    System.out.println("Duplicate template detected by hash, skipping save.");
                    return;
                }
            }
        }

        String uniqueFileName = folderPath + "/" + imageHash + ".png";
        ImageIO.write(image, "png", new File(uniqueFileName));
    }

    public static Color getDominantColor(Set<Color> colors) {
        int r = 0, g = 0, b = 0;
        for (Color color : colors) {
            r += color.getRed();
            g += color.getGreen();
            b += color.getBlue();
        }
        int size = colors.size();
        return size > 0 ? new Color(r / size, g / size, b / size) : new Color(0, 0, 0);
    }

    public static void adjustPixelBrightness(BufferedImage image, int x, int y, Color baseColor, double factor) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return; // Avoid out-of-bounds
        }
    
        int rgb = image.getRGB(x, y);
        Color original = new Color(rgb, true);
    
        if (original.getAlpha() == 0) return; // Skip transparent pixels
    
        int r = (int) Math.min(255, baseColor.getRed() * factor);
        int g = (int) Math.min(255, baseColor.getGreen() * factor);
        int b = (int) Math.min(255, baseColor.getBlue() * factor);
    
        Color adjusted = new Color(r, g, b, original.getAlpha());
        image.setRGB(x, y, adjusted.getRGB());
    }

    public static void fillRect(BufferedImage image, Graphics2D g, int x, int y, int width, int height, Color color) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                image.setRGB(i, j, color.getRGB());
            }
        }
    }
    
    public static Color darkenColor(Color color, int amount) {
        int r = Math.max(color.getRed() - amount, 0);
        int g = Math.max(color.getGreen() - amount, 0);
        int b = Math.max(color.getBlue() - amount, 0);
        return new Color(r, g, b, color.getAlpha());
    }
    
    public static Color lightenColor(Color color, int amount) {
        int r = Math.min(color.getRed() + amount, 255);
        int g = Math.min(color.getGreen() + amount, 255);
        int b = Math.min(color.getBlue() + amount, 255);
        return new Color(r, g, b, color.getAlpha());
    }

    public static int colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();
        return (int) Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }

    public static void convertToGrayscale(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgba = image.getRGB(x, y);
                Color col = new Color(rgba, true);
                if (col.getAlpha() == 0) {
                    // Skip transparent pixels
                    continue;
                }
                int brightness = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                int shading = (int) (brightness * 0.8); // Slight darkening for shading effect
                shading = Math.max(0, Math.min(255, shading));
                Color grayColor = new Color(shading, shading, shading, col.getAlpha());
                image.setRGB(x, y, grayColor.getRGB());
            }
        }
    }

    public static boolean isColorClose(Color c1, Color c2, double tolerance) {
        double distance = Math.sqrt(Math.pow(c1.getRed() - c2.getRed(), 2)
                + Math.pow(c1.getGreen() - c2.getGreen(), 2)
                + Math.pow(c1.getBlue() - c2.getBlue(), 2));
        return distance <= tolerance;
    }


    public static boolean isLikelyEyePixel(Color eyePixel, Color skinTone) {
        int eyeDiff = colorDistance(eyePixel, skinTone);
        return eyeDiff > 40;
    }

    public static boolean isOrcSkinTone(Color skinTone) {
        // Check for greenish or muddy brown (typical orc tones)
        float[] hsb = Color.RGBtoHSB(skinTone.getRed(), skinTone.getGreen(), skinTone.getBlue(), null);
    
        boolean isGreenish = hsb[0] >= 0.25 && hsb[0] <= 0.45 && hsb[1] >= 0.3;
        boolean isMuddyBrown = hsb[0] >= 0.08 && hsb[0] <= 0.15 && hsb[1] >= 0.4 && hsb[2] < 0.6;
    
        return isGreenish || isMuddyBrown;
    }

    public static boolean isWarmDarkSkin(Color skinTone) {
        float[] hsb = Color.RGBtoHSB(skinTone.getRed(), skinTone.getGreen(), skinTone.getBlue(), null);
        return hsb[0] >= 0.05 && hsb[0] <= 0.15 && hsb[1] >= 0.3 && hsb[2] < 0.5; // Warm brown tones
    }
    
    public static boolean isLightColor(Color color) {
        return color.getRed() > 180 && color.getGreen() > 180 && color.getBlue() > 180 && color.getAlpha() > 0;
    }

    public static String getImageHash(BufferedImage image) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    digest.update((byte) (rgb >> 16));
                    digest.update((byte) (rgb >> 8));
                    digest.update((byte) rgb);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to compute image hash", e);
        }
    }

    public static boolean hasWarPaint(BufferedImage skin) {
        for (int y = 8; y <= 16; y++) {
            for (int x = 24; x <= 32; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (!isSkinTone(color)) { // Implement isSkinTone to detect skin-like colors
                    return true;
                }
            }
        }
        return false;
    }
    
    public static boolean isSkinTone(Color color) {
        // Example logic: consider it a skin tone if the color is not too far from a known skin-tone range
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return (r > 100 && r < 255) && (g > 70 && g < 200) && (b > 50 && b < 180);
    }

    public static boolean isSmoothDarkSkin(Color skinTone) {
        int brightness = (skinTone.getRed() + skinTone.getGreen() + skinTone.getBlue()) / 3;
        return brightness < 100 && skinTone.getRed() > 40 && skinTone.getGreen() > 30 && skinTone.getBlue() > 20;
    } 

        public static boolean isPaleSkinTone(Color skinTone) {
        int brightness = (skinTone.getRed() + skinTone.getGreen() + skinTone.getBlue()) / 3;
        return brightness > 200; // Considered pale if very bright overall
    }

        public static Color getDominantHairColor(BufferedImage skin) {
        int hairStartY = 0;
        int hairEndY = 8;
        int hairStartX = 0;
        int hairEndX = 32;

        Set<Color> hairColors = new HashSet<>();

        for (int y = hairStartY; y < hairEndY; y++) {
            for (int x = hairStartX; x < hairEndX; x++) {
                int pixel = skin.getRGB(x, y);
                Color color = new Color(pixel, true);
                if (color.getAlpha() > 0) {
                    hairColors.add(color);
                }
            }
        }

        return Helpers.getDominantColor(hairColors);
    }

    public static void addLimbShading(BufferedImage baseLayer, Color skinTone, String race, String gender) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load config.properties, using default shading values.");
        }
    
        double shadingFactor;
        String key = "shadingFactor." + race.toLowerCase() + "." + gender.toLowerCase();
        if (properties.containsKey(key)) {
            try {
                shadingFactor = Double.parseDouble(properties.getProperty(key));
            } catch (NumberFormatException e) {
                shadingFactor = 0.85; // Fallback default
            }
        } else {
            // Default fallback values if not specified in properties
            shadingFactor = 0.85;
        }
    
        // Apply shading to limbs
        for (int y = 20; y < 32; y++) {
            Helpers.adjustPixelBrightness(baseLayer, 44, y, skinTone, shadingFactor);
            Helpers.adjustPixelBrightness(baseLayer, 47, y, skinTone, shadingFactor);
            Helpers.adjustPixelBrightness(baseLayer, 36, y, skinTone, shadingFactor);
            Helpers.adjustPixelBrightness(baseLayer, 39, y, skinTone, shadingFactor);
            Helpers.adjustPixelBrightness(baseLayer, 4, y, skinTone, shadingFactor);
            Helpers.adjustPixelBrightness(baseLayer, 7, y, skinTone, shadingFactor);
        }
        for (int y = 52; y < 64; y++) {
            Helpers.adjustPixelBrightness(baseLayer, 20, y, skinTone, shadingFactor);
            Helpers.adjustPixelBrightness(baseLayer, 23, y, skinTone, shadingFactor);
        }
    }
    
}
