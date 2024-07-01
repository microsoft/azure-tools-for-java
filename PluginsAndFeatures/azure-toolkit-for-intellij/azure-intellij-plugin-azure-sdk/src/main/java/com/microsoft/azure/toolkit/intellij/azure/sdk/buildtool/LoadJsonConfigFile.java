package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * This class is used to load a JSON configuration file and provide access to the loaded JSON object.
 * The constructor is private, so it can't be called from outside the class.
 * The getInstance method returns the single instance of the class, creating it if it doesn't exist.
 * The loadJsonObject method is called in the constructor, so it only runs once when the instance is created.
 * The getJsonObject method returns the loaded JSON object.
 * To use this class, you would call LoadJsonConfigFile.getInstance().getJsonObject().
 * This ensures that the JSON object is only loaded once, no matter how many times or from where getJsonObject is called.
 * */
public class LoadJsonConfigFile {

    // The instance variable holds the single instance of the class
    private static LoadJsonConfigFile instance;
    private JSONObject jsonObject;

    final String CONFIG_FILE_PATH = "META-INF/ruleConfigs.json";

    // The constructor is private, so it can't be called from outside the class
    private LoadJsonConfigFile() throws IOException {
        loadJsonObject();
    }

    // The getInstance method returns the single instance of the class, creating it if it doesn't exist
    public static synchronized LoadJsonConfigFile getInstance() throws IOException {
        if (instance == null) {
            instance = new LoadJsonConfigFile();
        }
        return instance;
    }

    // Load the JSON object from the configuration file
    private void loadJsonObject() throws IOException {

        System.out.println("Start loading JSON configuration");

        try (InputStream inputStream = StorageUploadWithoutLengthCheck.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Configuration file not found");
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                jsonObject = new JSONObject(bufferedReader.lines().collect(Collectors.joining()));
            }
        } catch (Exception e) {
            // Log the error message
            System.err.println("Error loading JSON configuration: " + e.getMessage());
            // Rethrow the exception
            throw e;
        }
    }

    // The getJsonObject method returns the loaded JSON object
    public JSONObject getJsonObject() {
        return jsonObject;
    }
}