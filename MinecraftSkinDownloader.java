import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
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
            JSONArray skinsList = fetchSkinsList(apiKey, numSkins);

            for (int i = 0; i < skinsList.length(); i++) {
                String uuid = skinsList.getJSONObject(i).getString("uuid");
                String skinUrl = fetchSkinUrlByUuid(apiKey, uuid);

                BufferedImage skinImage = downloadImage(skinUrl);
                ImageIO.write(skinImage, "png", new File("skin_" + i + ".png"));

                extractEyes(skinImage);
                extractHair(skinImage);
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
        g.drawImage(skin.getSubimage(26, 12, 8, 4), 26, 12, null);
        g.dispose();
        convertToGrayscale(eyes);
        saveUniqueTemplate(eyes, "templates/second_layers/humans/eyes");
    }

    public static void extractHair(BufferedImage skin) throws IOException {
        BufferedImage hair = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = hair.createGraphics();
        g.drawImage(skin.getSubimage(0, 0, 32, 8), 0, 0, null);
        g.dispose();
        convertToGrayscale(hair);
        saveUniqueTemplate(hair, "templates/second_layers/humans/hair");
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
