import func.BaseLayer;
import func.SecondLayer;
import func.RaceDetection;
import func.Helpers;
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

                            String gender = RaceDetection.detectGender(skinImage);
                            String race = RaceDetection.detectRace(skinImage);
                            System.out.println("Detected gender: " + gender + ", race: " + race);

                            SecondLayer.extractEyes(skinImage, gender, race);
                            SecondLayer.extractHair(skinImage, gender, race);
                            SecondLayer.extractMouth(skinImage, gender, race);
                            SecondLayer.extractBeard(skinImage, gender, race);
                            BaseLayer.extractBaseLayer(skinImage, gender, race);
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

                        String gender = RaceDetection.detectGender(skinImage);
                        String race = RaceDetection.detectRace(skinImage);
                        System.out.println("Detected gender: " + gender + ", race: " + race);

                        SecondLayer.extractEyes(skinImage, gender, race);
                        SecondLayer.extractHair(skinImage, gender, race);
                        SecondLayer.extractMouth(skinImage, gender, race);
                        SecondLayer.extractBeard(skinImage, gender, race);
                        BaseLayer.extractBaseLayer(skinImage, gender, race);

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
                        String gender = RaceDetection.detectGender(skinImage);
                        String race = RaceDetection.detectRace(skinImage);
                        System.out.println("Detected gender: " + gender + ", race: " + race);

                        SecondLayer.extractEyes(skinImage, gender, race);
                        SecondLayer.extractHair(skinImage, gender, race);
                        SecondLayer.extractMouth(skinImage, gender, race);
                        SecondLayer.extractBeard(skinImage, gender, race);
                        BaseLayer.extractBaseLayer(skinImage, gender, race);
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
    
        try {
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
            } else {
                properties.remove("last"); // Clear if no 'next' is available
            }
    
            try (FileOutputStream output = new FileOutputStream("config.properties")) {
                properties.store(output, null);
            }
    
            return response.getJSONArray("skins");
    
        } catch (IOException e) {
            System.out.println("Failed to fetch skins list, resetting 'last' parameter.");
            properties.remove("last");
            try (FileOutputStream output = new FileOutputStream("config.properties")) {
                properties.store(output, null);
            }
            throw e;
        }
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

}
