import java.awt.*;
import java.awt.image.BufferedImage;
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

                extractEyes(skinImage);
                extractHair(skinImage);

                markSkinAsProcessed(uuid);
            }

            System.out.println("All skins downloaded and processed successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        URL url = new URL("https://api.mineskin.org/v2/skins?size=" + size);
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
            "templates/base_layers/orcs",
            "templates/base_layers/pillagers",
            "templates/base_layers/humans/male",
            "templates/base_layers/humans/female",
            "templates/second_layers/humans/eyes",
            "templates/second_layers/humans/hair",
            "templates/second_layers/humans/other",
            "templates/second_layers/orcs",
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

    public static void extractEyes(BufferedImage skin) throws IOException {
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
            saveUniqueTemplate(eyes, "templates/second_layers/humans/eyes");
        } else {
            System.out.println("No eyes detected on skin.");
        }
    }

    public static void extractHair(BufferedImage skin) throws IOException {
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
            saveUniqueTemplate(hair, "templates/second_layers/humans/hair");
        } else {
            System.out.println("No hair detected on skin.");
        }
    }

    private static Color getDominantColor(Set<Color> colors) {
        int r = 0, g = 0, b = 0;
        for (Color color : colors) {
            r += color.getRed();
            g += color.getGreen();
            b += color.getBlue();
        }
        int size = colors.size();
        return new Color(r / size, g / size, b / size);
    }

    private static boolean isColorClose(Color c1, Color c2, double tolerance) {
        double distance = Math.sqrt(Math.pow(c1.getRed() - c2.getRed(), 2)
                + Math.pow(c1.getGreen() - c2.getGreen(), 2)
                + Math.pow(c1.getBlue() - c2.getBlue(), 2));
        return distance <= tolerance;
    }

    private static boolean isLikelyEyePixel(Color eyePixel, Color skinTone) {
        int eyeDiff = colorDistance(eyePixel, skinTone);
        return eyeDiff > 40;
    }

    private static Color sampleFaceSkinTone(BufferedImage skin) {
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

    private static int colorDistance(Color c1, Color c2) {
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
                int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                Color grayColor = new Color(gray, gray, gray, col.getAlpha());
                image.setRGB(x, y, grayColor.getRGB());
            }
        }
    }

    public static void saveUniqueTemplate(BufferedImage image, String folderPath) throws IOException {
        String imageHash = getImageHash(image);
        File folder = new File(folderPath);
        File[] existingFiles = folder.listFiles((dir, name) -> name.endsWith(".png"));

        for (File existingFile : Objects.requireNonNull(existingFiles)) {
            if (existingFile.getName().startsWith(imageHash)) {
                System.out.println("Duplicate template detected by hash, skipping save.");
                return;
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

    public static BufferedImage downloadImage(String urlString) throws IOException {
        URL url = new URL(urlString);
        return ImageIO.read(url);
    }
}
