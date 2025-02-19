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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class MinecraftSkinDownloader {

    public static String getRandomRaceQuery() {
        String[] races = {"orcs", "elves", "dwarves", "vampires", "humans"};
        return races[new Random().nextInt(races.length)];
    }

    public static class NovaSkinScraper {
        public static List<String> fetchSkinsFromNovaSkin(String query, String apiKey, String after) throws IOException {
            String urlString = "https://api.novaskin.me/v2/search?q=" + query;
            if (after != null && !after.isEmpty()) {
                urlString += "&after=" + after;
            }
            if (apiKey != null && !apiKey.isEmpty()) {
                urlString += "&key=" + apiKey;
            }

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "MinecraftSkinDownloader");

            if (conn.getResponseCode() != 200) {
                throw new IOException("Failed to fetch skins from NovaSkin API: HTTP " + conn.getResponseCode());
            }

            String jsonResponse = new String(conn.getInputStream().readAllBytes());
            JSONObject response = new JSONObject(jsonResponse);

            JSONArray posts = response.getJSONArray("posts");
            List<String> skinUrls = new ArrayList<>();

            for (int i = 0; i < posts.length(); i++) {
            String texture = posts.getJSONObject(i).getString("texture");
               // String skinUrl = "https://mc-api.poke.dev/texture/" + texture;
               String skinUrl = "https://t.novaskin.me/" + texture;

               skinUrls.add(skinUrl);
            }

            // Save 'after' value for pagination
            if (response.has("meta") && response.getJSONObject("meta").has("after")) {
                String newAfter = String.valueOf(response.getJSONObject("meta").getInt("after"));
                Properties properties = new Properties();
                try (FileInputStream input = new FileInputStream("config.properties")) {
                    properties.load(input);
                }
                properties.setProperty("last", newAfter);
                try (FileOutputStream output = new FileOutputStream("config.properties")) {
                    properties.store(output, null);
                }
            }

            return skinUrls;
        }
    }

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

            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream("config.properties")) {
                properties.load(input);
            }
            String source = properties.getProperty("source", "mineskin");

            int nameMCCount = 0;
            int skinsMCCount = 0;

            JSONArray skinsList = new JSONArray();
            if (source.equalsIgnoreCase("mineskin")) {
                skinsList = fetchSkinsList(apiKey, numSkins);
            }

            for (int i = 0; i < numSkins; i++) {
                String skinUrl = null;
                String uuid = null;

                if (source.equalsIgnoreCase("mineskin")) {
                    if (i < skinsList.length()) {
                        uuid = skinsList.getJSONObject(i).getString("uuid");
                        if (processedSkins.contains(uuid)) {
                            System.out.println("Skin " + uuid + " already processed. Skipping...");
                            continue;
                        }
                        skinUrl = fetchSkinUrlByUuid(apiKey, uuid);
                    } else {
                        System.out.println("Not enough skins from Mineskin. Switching to NameMC.");
                        skinUrl = NameMCSkinScraper.fetchRandomSkinUrl();
                        nameMCCount++;
                        Thread.sleep(1000);
                    }
                } else if (source.equalsIgnoreCase("namemc")) {
                    skinUrl = NameMCSkinScraper.fetchRandomSkinUrl();
                    nameMCCount++;
                    Thread.sleep(1000);
                } else if (source.equalsIgnoreCase("novaskin")) {
                    String novaSkinApiKey = properties.getProperty("novaSkinApiKey", "");
                    String last = properties.getProperty("last", "");
                    String randomRace = getRandomRaceQuery();

                    List<String> skinUrls = NovaSkinScraper.fetchSkinsFromNovaSkin(randomRace, novaSkinApiKey, last);
                    for (String skinUrlFromList : skinUrls) {
                        try {
                            BufferedImage skinImage = downloadImageWithFullHeaders(skinUrlFromList);
                            if (skinImage == null) {
                                System.out.println("Image not found for texture: " + skinUrlFromList);
                                continue;
                            }

                            String gender = detectGender(skinImage);
                            String race = detectRace(skinImage);
                            System.out.println("Detected gender: " + gender + ", race: " + race);

                            extractEyes(skinImage, gender, race);
                            extractHair(skinImage, gender, race);
                            extractMouth(skinImage, gender, race);
                            extractBeard(skinImage, gender, race);

                            Thread.sleep(500);
                        } catch (IOException e) {
                            System.out.println("Failed to download or process skin: " + skinUrlFromList);
                        }
                    }
                    continue;
                } else if (source.equalsIgnoreCase("skinsmc")) {
                    List<String> skinUrls = SkinsMCScraper.fetchRandomSkinUrls();
                    for (String skinUrlFromList : skinUrls) {
                        BufferedImage skinImage = downloadImage(skinUrlFromList);

                        String gender = detectGender(skinImage);
                        String race = detectRace(skinImage);
                        System.out.println("Detected gender: " + gender + ", race: " + race);

                        extractEyes(skinImage, gender, race);
                        extractHair(skinImage, gender, race);
                        extractMouth(skinImage, gender, race);
                        extractBeard(skinImage, gender, race);

                        skinsMCCount++;
                        Thread.sleep(10);
                    }
                    continue;
                } else {
                    throw new IllegalArgumentException("Invalid source: " + source);
                }

                if (skinUrl != null) {
                    BufferedImage skinImage = downloadImage(skinUrl);

                    if (skinImage != null) {
                        String gender = detectGender(skinImage);
                        String race = detectRace(skinImage);
                        System.out.println("Detected gender: " + gender + ", race: " + race);

                        extractEyes(skinImage, gender, race);
                        extractHair(skinImage, gender, race);

                        if (uuid != null) {
                            markSkinAsProcessed(uuid);
                        }
                    } else {
                        System.out.println("Failed to download or process skin: " + skinUrl);
                    }
                } else {
                    System.out.println("No valid skin URL found. Skipping...");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class NameMCSkinScraper {
        public static String fetchRandomSkinUrl() throws IOException {
            Document doc = Jsoup.connect("https://de.namemc.com/minecraft-skins/random")
            .userAgent(getRandomUserAgent())
            .referrer("https://google.com")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .timeout(8000)
            .get();
    
    
            Element skinElement = doc.selectFirst(".card-img-top");
            if (skinElement == null) {
                throw new IOException("Failed to find skin element.");
            }
    
            String style = skinElement.attr("style");
            String textureHash = style.substring(style.indexOf("/texture/") + 9, style.indexOf("?"));
    
            return "https://textures.minecraft.net/texture/" + textureHash;
        }
    }


    public static class SkinsMCScraper {
        private static final String API_URL = "https://elseivnkvvyxdcgyfpuj.supabase.co/rest/v1/random_skins?select=skin&limit=24";
    
        public static List<String> fetchRandomSkinUrls() throws IOException {
            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream("config.properties")) {
                properties.load(input);
            }
    
            String apiKey = properties.getProperty("skinsmcKey");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IOException("skinsmcKey not found in config.properties");
            }
    
            HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("apikey", apiKey);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept-Profile", "public");
    
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Failed to fetch skins from Supabase API: HTTP " + responseCode);
            }
    
            String response = new String(connection.getInputStream().readAllBytes());
            JSONArray skinsArray = new JSONArray(response);
            if (skinsArray.isEmpty()) {
                throw new IOException("No skins found in Supabase API response.");
            }
    
            List<String> skinUrls = new ArrayList<>();
            for (int i = 0; i < skinsArray.length(); i++) {
                JSONObject skinObject = skinsArray.getJSONObject(i);
                String skinId = skinObject.getString("skin");
                skinUrls.add("https://mc-api.poke.dev/texture/" + skinId);
            }
    
            return skinUrls;
        }
    }
    
    

    private static String getRandomUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Linux; Android 10; SM-A205F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        };
        return userAgents[new Random().nextInt(userAgents.length)];
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
            orcs.put("beardPoints", -30); // Penalty for beards

            JSONObject humans = new JSONObject();
            humans.put("defaultPoints", 1);
            humans.put("teethPoints", 0);
            humans.put("earPoints", 0);
            humans.put("skinTonePoints", 0);
            humans.put("smoothDarkSkinPoints", 1);
            humans.put("warmDarkSkinPoints", 2);
            humans.put("paleSkinPoints", 0);
            humans.put("beardPoints", 3); 


            JSONObject elves = new JSONObject();
            elves.put("defaultPoints", 0);
            elves.put("teethPoints", 0);
            elves.put("earPoints", 3);
            elves.put("skinTonePoints", 0);
            elves.put("smoothDarkSkinPoints", 0);
            elves.put("warmDarkSkinPoints", 0);
            elves.put("paleSkinPoints", 0);
            elves.put("beardPoints", -3); // Penalty for beards

            JSONObject vampires = new JSONObject();
            vampires.put("defaultPoints", 0);
            vampires.put("teethPoints", 0);
            vampires.put("earPoints", 0);
            vampires.put("skinTonePoints", 0);
            vampires.put("smoothDarkSkinPoints", 0);
            vampires.put("warmDarkSkinPoints", 0);
            vampires.put("paleSkinPoints", 3);
            vampires.put("beardPoints", -30); // Penalty for beards

            JSONObject dwarves = new JSONObject();
            dwarves.put("defaultPoints", 0);
            dwarves.put("teethPoints", 0);
            dwarves.put("earPoints", 0);
            dwarves.put("skinTonePoints", 0);
            dwarves.put("smoothDarkSkinPoints", 0);
            dwarves.put("warmDarkSkinPoints", 0);
            dwarves.put("paleSkinPoints", 0);
            dwarves.put("teethAndSkinBonus", 0);
            dwarves.put("earPoints", 0);
            dwarves.put("beardPoints", 5);


            defaultRaces.put("orcs", orcs);
            defaultRaces.put("humans", humans);
            defaultRaces.put("elves", elves);
            defaultRaces.put("vampires", vampires);
            defaultRaces.put("dwarves", dwarves);

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
        try {
            URL url = new URL(urlString);
            return ImageIO.read(url);
        } catch (FileNotFoundException e) {
            System.out.println("Image not found: " + urlString);
            return null;
        } catch (IOException e) {
            System.out.println("Failed to download image: " + urlString);
            throw e;
        }
    }

    public static BufferedImage downloadImageWithHeaders(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", getRandomUserAgent());
        conn.setRequestProperty("Referer", "https://minecraft.novaskin.me/");
        conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        conn.setRequestProperty("DNT", "1");
        conn.setRequestProperty("Sec-Fetch-Dest", "image");
        conn.setRequestProperty("Sec-Fetch-Mode", "no-cors");
        conn.setRequestProperty("Sec-Fetch-Site", "same-site");
    
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.out.println("Failed to download image. HTTP Response: " + responseCode + " URL: " + urlString);
            return null;
        }
    
        return ImageIO.read(conn.getInputStream());
    }

    public static BufferedImage downloadImageWithFullHeaders(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", getRandomUserAgent());
        conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        conn.setRequestProperty("Referer", "https://minecraft.novaskin.me/");
        conn.setRequestProperty("DNT", "1");
        conn.setRequestProperty("Sec-Fetch-Dest", "image");
        conn.setRequestProperty("Sec-Fetch-Mode", "no-cors");
        conn.setRequestProperty("Sec-Fetch-Site", "same-site");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setRequestProperty("Cache-Control", "max-age=0");
        conn.setRequestProperty("Connection", "keep-alive");
    
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.out.println("Failed to download image. HTTP Response: " + responseCode + " URL: " + urlString);
            return null;
        }
    
        return ImageIO.read(conn.getInputStream());
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
            "templates/base_layers",
            "templates/second_layers",
            "templates/professions"
        };

        for (String folder : folders) {
            File dir = new File(folder);
            if (!dir.exists()) {
                dir.mkdirs();
                System.out.println("Created folder: " + folder);
            }
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

        int beardStartX = 24;
        int beardEndX = 32;
        int beardStartY = 20;
        int beardEndY = 28;

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
    boolean hasBeard = detectBeard(skin, getDominantHairColor(skin));

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

    return getDominantColor(hairColors);
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
            if (color.getAlpha() > 0 && isColorClose(color, dominantHairColor, tolerance)) {
                beardPixels++;
            }
        }
    }

    double beardCoverage = (double) beardPixels / totalPixels;

    // Return true if a significant part of the chin area is covered by beard-like pixels
    return beardCoverage > 0.4; // Adjust this coverage threshold as needed
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
