import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

public class MinecraftSkinDownloader {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please provide the number of skins to fetch as a parameter.");
            return;
        }

        int numSkins;
        try {
            numSkins = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number of skins.");
            return;
        }

        try {
            createTemplateFolders();
            createDefaultRacesJson();

            String apiKey = loadApiKey();
            Set<String> processedSkins = loadProcessedSkins();

            JSONArray skinsList = fetchSkinsList(apiKey, numSkins);

            for (int i = 0; i < skinsList.length(); i++) {
                String uuid = skinsList.getJSONObject(i).getString("uuid");

                if (processedSkins.contains(uuid)) {
                    System.out.println("Skin " + uuid + " already processed. Skipping...");
                    continue;
                }

                String skinUrl = fetchSkinUrlByUuid(apiKey, uuid);

                BufferedImage skinImage = downloadImage(skinUrl);

                String gender = detectGender(skinImage);
                String race = detectRace(skinImage);
                System.out.println("Detected gender: " + gender + ", race: " + race);

                extractEyes(skinImage, gender, race);
                extractHair(skinImage, gender, race);

                markSkinAsProcessed(uuid);
            }

            System.out.println("All skins downloaded and processed successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createDefaultRacesJson() {
        File file = new File("races.json");
        if (!file.exists()) {
            JSONObject defaultRaces = new JSONObject();

            JSONObject orcs = new JSONObject();
            orcs.put("defaultPoints", 0);
            orcs.put("teethPoints", 3);
            orcs.put("earPoints", 0);
            orcs.put("skinTonePoints", 1);
            orcs.put("smoothDarkSkinPoints", 0);
            orcs.put("warmDarkSkinPoints", 0);
            orcs.put("paleSkinPoints", 0);
            orcs.put("teethAndSkinBonus", 2);

            JSONObject humans = new JSONObject();
            humans.put("defaultPoints", 1);
            humans.put("teethPoints", 0);
            humans.put("earPoints", 0);
            humans.put("skinTonePoints", 0);
            humans.put("smoothDarkSkinPoints", 1);
            humans.put("warmDarkSkinPoints", 2);
            humans.put("paleSkinPoints", 0);

            JSONObject elves = new JSONObject();
            elves.put("defaultPoints", 0);
            elves.put("teethPoints", 0);
            elves.put("earPoints", 3);
            elves.put("skinTonePoints", 0);
            elves.put("smoothDarkSkinPoints", 0);
            elves.put("warmDarkSkinPoints", 0);
            elves.put("paleSkinPoints", 0);

            JSONObject vampires = new JSONObject();
            vampires.put("defaultPoints", 0);
            vampires.put("teethPoints", 0);
            vampires.put("earPoints", 0);
            vampires.put("skinTonePoints", 0);
            vampires.put("smoothDarkSkinPoints", 0);
            vampires.put("warmDarkSkinPoints", 0);
            vampires.put("paleSkinPoints", 3);

            defaultRaces.put("orcs", orcs);
            defaultRaces.put("humans", humans);
            defaultRaces.put("elves", elves);
            defaultRaces.put("vampires", vampires);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(defaultRaces.toString(4));
                System.out.println("Created default races.json");
            } catch (IOException e) {
                System.out.println("Failed to create default races.json");
                e.printStackTrace();
            }
        }
    }

    public static BufferedImage downloadImage(String urlString) throws IOException {
        URL url = new URL(urlString);
        return ImageIO.read(url);
    }

    public static String loadApiKey() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            return properties.getProperty("apiKey");
        }
    }

    public static Set<String> loadProcessedSkins() {
        Set<String> processedSkins = new HashSet<>();
        File file = new File("processed_skins.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Could not create processed_skins.txt file.");
            }
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            processedSkins.addAll(lines);
        } catch (IOException e) {
            System.out.println("Could not load processed skins file. Starting fresh.");
        }
        return processedSkins;
    }

    public static void markSkinAsProcessed(String uuid) {
        try {
            Files.writeString(Path.of("processed_skins.txt"), uuid + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Failed to mark skin as processed: " + uuid);
        }
    }

    public static JSONArray fetchSkinsList(String apiKey, int size) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        }

        String lastParam = properties.getProperty("last", "");
        String urlString = "https://api.mineskin.org/v2/skins?size=" + size;
        if (!lastParam.isEmpty()) {
            urlString += "&after=" + lastParam;
        }

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "MinecraftSkinDownloader");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to fetch skins list: HTTP " + conn.getResponseCode());
        }

        String jsonResponse = new String(conn.getInputStream().readAllBytes());
        JSONObject response = new JSONObject(jsonResponse);

        // Save the new 'after' value if it exists
        if (response.has("pagination") && response.getJSONObject("pagination").has("next")) {
            String newAfter = response.getJSONObject("pagination").getJSONObject("next").getString("after");
            properties.setProperty("last", newAfter);
            try (FileOutputStream output = new FileOutputStream("config.properties")) {
                properties.store(output, null);
            }
        }

        return response.getJSONArray("skins");
    }

    public static String fetchSkinUrlByUuid(String apiKey, String uuid) throws IOException {
        URL url = new URL("https://api.mineskin.org/v2/skins/" + uuid);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "MinecraftSkinDownloader");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to fetch skin by UUID: HTTP " + conn.getResponseCode());
        }

        String jsonResponse = new String(conn.getInputStream().readAllBytes());
        JSONObject response = new JSONObject(jsonResponse);
        return response.getJSONObject("skin").getJSONObject("texture").getJSONObject("url").getString("skin");
    }

    public static void createTemplateFolders() {
        String[] folders = {
            "templates/base_layers/vampires",
            "templates/second_layers/vampires/eyes",
            "templates/second_layers/vampires/hair",
            "templates/base_layers/orcs",
            "templates/base_layers/pillagers",
            "templates/base_layers/humans/male",
            "templates/base_layers/humans/female",
            "templates/second_layers/humans/eyes",
            "templates/second_layers/humans/hair",
            "templates/second_layers/humans/other",
            "templates/second_layers/orcs/eyes",
            "templates/second_layers/orcs/hair",
            "templates/second_layers/elf/eyes",
            "templates/second_layers/elf/hair",
            "templates/professions/smith",
            "templates/professions/farmer"
        };

        for (String folder : folders) {
            File dir = new File(folder);
            if (!dir.exists()) {
                dir.mkdirs();
                System.out.println("Created folder: " + folder);
            }
        }
    }

    public static void extractEyes(BufferedImage skin, String gender, String race) throws IOException {
        BufferedImage eyes = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = eyes.createGraphics();

        int eyeStartX = 24;
        int eyeEndX = 32;
        int eyeStartY = 8;
        int eyeEndY = 16;

        Color skinTone = sampleFaceSkinTone(skin);

        boolean foundEyePixels = false;
        for (int y = eyeStartY; y < eyeEndY; y++) {
            for (int x = eyeStartX; x < eyeEndX; x++) {
                int pixel = skin.getRGB(x, y);
                Color color = new Color(pixel, true);
                if (isLikelyEyePixel(color, skinTone)) {
                    g.drawImage(skin.getSubimage(x, y, 1, 1), x, y, null);
                    foundEyePixels = true;
                }
            }
        }
        g.dispose();

        if (foundEyePixels) {
            convertToGrayscale(eyes);
            String path = String.format("templates/second_layers/%s/eyes", race);
            saveUniqueTemplate(eyes, path);
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

        Color dominantHairColor = getDominantColor(hairColors);
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

    Color dominantHairColor = getDominantColor(hairColors);
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
    Color skinTone = sampleFaceSkinTone(skin);

    boolean hasTeeth = detectOrcTeeth(skin);
    boolean hasPointyEars = detectElfEars(skin);
    boolean isOrcTone = isOrcSkinTone(skinTone);
    boolean isSmoothDarkSkin = isSmoothDarkSkin(skinTone);
    boolean isPaleSkin = isPaleSkinTone(skinTone);
    boolean isWarmDarkSkin = isWarmDarkSkin(skinTone);
    boolean hasWarPaint = hasWarPaint(skin);

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
            if (hasTeeth && isOrcTone) {
                score += raceData.optInt("teethAndSkinBonus", 2);
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

private static boolean isSmoothDarkSkin(Color skinTone) {
    int brightness = (skinTone.getRed() + skinTone.getGreen() + skinTone.getBlue()) / 3;
    return brightness < 100 && skinTone.getRed() > 40 && skinTone.getGreen() > 30 && skinTone.getBlue() > 20;
} 

    public static boolean isPaleSkinTone(Color skinTone) {
    int brightness = (skinTone.getRed() + skinTone.getGreen() + skinTone.getBlue()) / 3;
    return brightness > 200; // Considered pale if very bright overall
}

    public static void convertToGrayscale(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgba = image.getRGB(x, y);
                Color col = new Color(rgba, true);
                int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                Color grayColor = new Color(gray, gray, gray, col.getAlpha());
                image.setRGB(x, y, grayColor.getRGB());
            }
        }
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

    public static boolean isColorClose(Color c1, Color c2, double tolerance) {
        double distance = Math.sqrt(Math.pow(c1.getRed() - c2.getRed(), 2)
                + Math.pow(c1.getGreen() - c2.getGreen(), 2)
                + Math.pow(c1.getBlue() - c2.getBlue(), 2));
        return distance <= tolerance;
    }

    public static Color sampleFaceSkinTone(BufferedImage skin) {
        List<Color> samples = Arrays.asList(
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

            if (isLightColor(leftColor) || isLightColor(rightColor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLightColor(Color color) {
        return color.getRed() > 180 && color.getGreen() > 180 && color.getBlue() > 180 && color.getAlpha() > 0;
    }

    private static int colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();
        return (int) Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
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

}
