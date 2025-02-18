# Minecraft Skin Downloader With Extraction

## Overview
This Java program downloads Minecraft skins from the Mineskin API and extracts the eyes and hair regions from each skin. The extracted parts are processed into grayscale templates and saved in organized folders for later use in procedural skin generation.

## Features
- Fetches a list of skins from the Mineskin API.
- Downloads each skin's texture.
- Extracts eyes and hair regions from each skin.
- Dynamically detects eyes based on contrast with skin tone.
- Dynamically detects hair based on color consistency, handling long and short hair.
- Converts extracted parts to grayscale.
- Saves unique templates based on image content hash to avoid duplicates.
- Keeps track of already processed skins to prevent redundant downloads.
- Organizes templates into structured folders.

## Requirements
- Java Development Kit (JDK) 11 or later.
- Internet connection.
- Mineskin API key (Bearer token).

## Setup
1. Create a `config.properties` file in the program directory with the following content:
    ```
    apiKey=YOUR_MINESKIN_API_KEY
    ```
   Replace `YOUR_MINESKIN_API_KEY` with your actual Mineskin API key.

2. Ensure you have the following libraries (all standard in Java):
    - `java.awt.*`
    - `java.awt.image.*`
    - `java.io.*`
    - `java.net.*`
    - `java.security.*`
    - `java.util.*`
    - `javax.imageio.*`
    - `org.json.*` (download from https://stleary.github.io/JSON-java/ if not already available)

## Compiling
Open a terminal in the directory containing `MinecraftSkinDownloader.java` and run:
```bash
javac -cp .:json-20231013.jar MinecraftSkinDownloader.java
```
Ensure the `json-20231013.jar` (or the appropriate version of the JSON library) is in the same directory.

## Running
After successful compilation, run the program with the number of skins you want to fetch:
```bash
java -cp .:json-20231013.jar MinecraftSkinDownloader 5
```
This will download 5 skins, extract eye and hair templates, and save them in the `templates/` folder structure.

## Output
- Extracted grayscale eye and hair templates will be saved in:
  - `templates/second_layers/humans/eyes/`
  - `templates/second_layers/humans/hair/`

## Folder Structure
```
templates/
  base_layers/
    orcs/
    pillagers/
    humans/
      male/
      female/
  second_layers/
    humans/
      eyes/
      hair/
      other/
    orcs/
  professions/
    smith/
    farmer/
```

## Notes
- The eyes and hair extraction currently uses dynamic detection techniques to handle different styles and colors.
- Duplicate templates are skipped based on image content hashing.
- The program tracks processed skins in `processed_skins.txt` to prevent re-processing the same skins.

## Troubleshooting
- If you get `IOException` errors, ensure your `config.properties` file is correctly set up and your API key is valid.
- If JSON library errors occur, ensure you have included `json-*.jar` in the classpath.

## License
This project is provided as-is. Feel free to modify and adapt it to your needs.
