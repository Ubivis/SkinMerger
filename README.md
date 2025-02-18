# Minecraft Skin Downloader and Feature Extractor

This Java-based application automatically downloads Minecraft skins, identifies the gender and race of each skin, and extracts specific features—such as eyes and hair—for storage in pre-defined template folders.

## Features

- **Automatic Skin Downloading:**
  - Supports multiple sources for skin downloads: MineSkin, SkinsMC ("Number of Skins" Parameter will be a multiplicated with 24, as SkinMC always returns 24 skin per call), and NameMC (though NameMC is not working reliably at the moment).
  - The preferred source can be selected in `config.properties`.
- **Gender and Race Detection:**
  - Applies image analysis to determine whether a skin is male or female and categorizes it into one of several races (e.g., humans, orcs, elves, vampires).
- **Feature Extraction:**
  - Isolates and processes eyes and hair from skins, saving them as grayscale images in organized directories for further use.
- **Duplicate Handling:**
  - Skins are checked against a local record to avoid re-downloading and re-processing.
- **Dynamic Race Configuration:**
  - Includes a `races.json` file that can be customized to alter detection criteria without modifying the source code.

## Requirements

- Java Development Kit (JDK) 8 or higher
- An API key from MineSkin or SkinsMC (if using those sources)
- Internet connection for API access

## Installation

1. Clone the repository:

   ```
   git clone https://github.com/your-username/MinecraftSkinDownloader.git
   cd MinecraftSkinDownloader
   ```

2. Ensure you have the required `config.properties` file with the following content:

   ```
   apiKey=your_mineskin_api_key_here
   skinsmcKey=your_skinsmc_api_key_here
   source=mineskin   # Options: mineskin, skinsmc, namemc
   ```

3. (Optional) Customize `races.json` to modify how skins are categorized. If `races.json` is not present, the application will generate a default configuration.

## Building and Running

1. Compile the application:

   ```
   javac -cp .;json-20210307.jar;jsoup-1.18.3.jar MinecraftSkinDownloader.java
   ```

2. Run the application, providing the number of skins to download as an argument:

   ```
   java -cp .;json-20210307.jar;jsoup-1.18.3.jar MinecraftSkinDownloader 10
   ```

   In this example, the application will attempt to download and process 10 skins (multiplied by 24 if skinsmc is used).

## Output

- **Downloaded Skins:**
  Processed skins will be saved to the configured template directories. For instance:
  ```
  templates/second_layers/humans/eyes
  templates/second_layers/humans/hair
  templates/second_layers/orcs
  templates/base_layers/humans/male
  templates/base_layers/humans/female
  ```
- **Processed Records:**
  Each successfully processed skin is recorded in `processed_skins.txt` to prevent redundant downloads and processing.

## Customizing Race Detection

Edit the `races.json` file to adjust how gender and race are determined. For example, you can change the scoring values for certain features:

```json
{
  "orcs": {
    "defaultPoints": 0,
    "teethPoints": 3,
    "earPoints": 0,
    "skinTonePoints": 1,
    "smoothDarkSkinPoints": 0,
    "warmDarkSkinPoints": 0,
    "paleSkinPoints": 0,
    "teethAndSkinBonus": 2
  },
  "humans": {
    "defaultPoints": 1,
    "teethPoints": 0,
    "earPoints": 0,
    "skinTonePoints": 0,
    "smoothDarkSkinPoints": 1,
    "warmDarkSkinPoints": 2,
    "paleSkinPoints": 0
  },
  "elves": {
    "defaultPoints": 0,
    "teethPoints": 0,
    "earPoints": 3,
    "skinTonePoints": 0,
    "smoothDarkSkinPoints": 0,
    "warmDarkSkinPoints": 0,
    "paleSkinPoints": 0
  },
  "vampires": {
    "defaultPoints": 0,
    "teethPoints": 0,
    "earPoints": 0,
    "skinTonePoints": 0,
    "smoothDarkSkinPoints": 0,
    "warmDarkSkinPoints": 0,
    "paleSkinPoints": 3
  }
}
```

This structure allows you to fine-tune detection logic without altering the code.

## Future Enhancements

- **Additional Races:**
  - New races can be added simply by extending `races.json` and updating the appropriate templates.
- **Improved Detection Algorithms:**
  - Ongoing refinements to detection methods will enhance accuracy, particularly in distinguishing similar races.
- **Integration with Other APIs:**
  - Additional APIs could be integrated for a broader range of skin sources or to add more metadata.

## Troubleshooting

- **HTTP Errors:**
  - Ensure the API key in `config.properties` is valid and that you have internet connectivity.
- **NameMC Issues:**
  - NameMC scraping does not currently work well and may fail frequently.
- **Missing Folders:**
  - If the template directories are not created, check that the application has permission to write to the project directory.
- **Detection Issues:**
  - Adjust `races.json` to refine detection criteria. For example, increasing or decreasing points assigned to certain features can help improve categorization.

