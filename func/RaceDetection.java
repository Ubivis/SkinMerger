package func;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static func.Helpers.*;

public class RaceDetection {
    public static String detectGender(BufferedImage skin) {
        int hairStartY = 0;
        int hairEndY = 16;
        int hairStartX = 0;
        int hairEndX = 32;

        Set<Color> hairColors = new HashSet<>();
        int hairPixelsBelowThreshold = 0;
        int hairLengthThreshold = 12;
        int longHairYLimit = 20; // How far down hair can go to consider it "long"

        // Collect hair colors and check for hair length below threshold
        for (int y = hairStartY; y < hairEndY; y++) {
            for (int x = hairStartX; x < hairEndX; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (color.getAlpha() > 0) {
                    hairColors.add(color);
                    if (y > hairLengthThreshold) {
                        hairPixelsBelowThreshold++;
                        if (y > longHairYLimit) {
                            // Early exit if we are certain it's long hair
                            return "female";
                        }
                    }
                }
            }
        }

        Color dominantHairColor = Helpers.getDominantColor(hairColors);
        double tolerance = 30.0;
        int confirmedHairPixelsBelowThreshold = 0;

        for (int y = hairLengthThreshold; y < longHairYLimit; y++) {
            for (int x = hairStartX; x < hairEndX; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (color.getAlpha() > 0 && isColorClose(color, dominantHairColor, tolerance)) {
                    confirmedHairPixelsBelowThreshold++;
                }
            }
        }

        boolean hasLongHair = confirmedHairPixelsBelowThreshold > 20; // Threshold for "long hair"

        // Face "makeup" hint detection (reduced)
        boolean hasFaceColorHints = false;
        for (int y = 12; y <= 13; y++) {
            for (int x = 30; x <= 32; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (color.getRed() > 170 && color.getGreen() < 140 && color.getBlue() < 140) {
                    hasFaceColorHints = true;
                    break;
                }
            }
        }

        if (hasLongHair || hasFaceColorHints) {
            return "female";
        } else {
            return "male";
        }
    }

    public static String detectRace(BufferedImage skin) {
        Color skinTone = Helpers.sampleFaceSkinTone(skin);

        boolean hasTeeth = detectOrcTeeth(skin);
        boolean hasPointyEars = detectElfEars(skin);
        boolean isOrcTone = Helpers.isOrcSkinTone(skinTone);
        boolean isSmoothDarkSkin = Helpers.isSmoothDarkSkin(skinTone);
        boolean isPaleSkin = Helpers.isPaleSkinTone(skinTone);
        boolean isWarmDarkSkin = Helpers.isWarmDarkSkin(skinTone);
        boolean hasWarPaint = Helpers.hasWarPaint(skin);
        boolean hasBeard = RaceDetection.detectBeard(skin, Helpers.getDominantHairColor(skin));

        Map<String, Integer> raceScores = new HashMap<>();

        try {
            String jsonContent = new String(Files.readAllBytes(Path.of("races.json")));
            JSONObject raceConfig = new JSONObject(jsonContent);

            for (String race : raceConfig.keySet()) {
                JSONObject raceData = raceConfig.getJSONObject(race);

                int score = raceData.optInt("defaultPoints", 0);

                if (hasTeeth) {
                    score += raceData.optInt("teethPoints", 0);
                }
                if (hasPointyEars) {
                    score += raceData.optInt("earPoints", 0);
                }
                if (isOrcTone) {
                    score += raceData.optInt("skinTonePoints", 0);
                }
                if (isSmoothDarkSkin) {
                    score += raceData.optInt("smoothDarkSkinPoints", 0);
                }
                if (isPaleSkin) {
                    score += raceData.optInt("paleSkinPoints", 0);
                }
                if (isWarmDarkSkin) {
                    score += raceData.optInt("warmDarkSkinPoints", 0);
                }
                if (hasTeeth && isOrcTone) {
                    score += raceData.optInt("teethAndSkinBonus", 2);
                }
                if (hasBeard) {
                    score += raceData.optInt("beardPoints", 3); // Add a strong bonus for dwarves if beard is detected.
                }
                raceScores.put(race, score);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "humans"; // Fallback if JSON fails
        }

        return raceScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("humans");
    }

    public static boolean detectBeard(BufferedImage skin, Color dominantHairColor) {
        int beardStartX = 28; // Roughly under the mouth
        int beardEndX = 35;
        int beardStartY = 20; // Chin area
        int beardEndY = 30;
    
        int beardPixels = 0;
        int totalPixels = (beardEndX - beardStartX) * (beardEndY - beardStartY);
        double tolerance = 35.0;
    
        for (int y = beardStartY; y < beardEndY; y++) {
            for (int x = beardStartX; x < beardEndX; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (color.getAlpha() > 0 && Helpers.isColorClose(color, dominantHairColor, tolerance)) {
                    beardPixels++;
                }
            }
        }
    
        double beardCoverage = (double) beardPixels / totalPixels;
    
        // Return true if a significant part of the chin area is covered by beard-like pixels
        return beardCoverage > 0.4; // Adjust this coverage threshold as needed
    }

    public static boolean detectOrcTeeth(BufferedImage skin) {
        int teethPixels = 0;
        for (int y = 20; y <= 24; y++) {
            for (int x = 26; x <= 34; x++) {
                Color color = new Color(skin.getRGB(x, y), true);
                if (color.getRed() > 220 && color.getGreen() > 220 && color.getBlue() > 220) {
                    teethPixels++;
                }
            }
        }
        return teethPixels > 2; // Require at least 3 white pixels for better confidence
    }
    

    public static boolean detectElfEars(BufferedImage skin) {
        for (int y = 8; y <= 12; y++) {
            Color leftColor = new Color(skin.getRGB(0, y), true);
            Color rightColor = new Color(skin.getRGB(39, y), true);

            if (Helpers.isLightColor(leftColor) || Helpers.isLightColor(rightColor)) {
                return true;
            }
        }
        return false;
    }
}
