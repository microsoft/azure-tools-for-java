package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.JsonParseException;
import org.json.JSONArray;
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

    static final String CONFIG_FILE_PATH = "META-INF/ruleConfigs.json";
    static final String PACKAGE_NAME = "com.azure";

    private static final Logger LOGGER = Logger.getLogger(LoadJsonConfigFile.class.getName());


    // The constructor is private, so it can't be called from outside the class
    private LoadJsonConfigFile() throws IOException {
        loadJsonObject();
    }

    /** The getInstance method returns the single instance of the class, creating it if it doesn't exist
     * @return LoadJsonConfigFile instance of the class
     * @throws IOException if the configuration file is not found
     */
    public static synchronized LoadJsonConfigFile getInstance() throws IOException {
        if (instance == null) {
            instance = new LoadJsonConfigFile();
        }
        return instance;
    }

    /** Load the JSON object from the configuration file
     * @throws IOException if the configuration file is not found
     */
    private void loadJsonObject() throws IOException {

        try (InputStream inputStream = StorageUploadWithoutLengthCheck.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Configuration file not found");
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                jsonObject = new JSONObject(bufferedReader.lines().collect(Collectors.joining()));
            }
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Configuration file not found at path: " + CONFIG_FILE_PATH
                    + ". Please ensure the file exists and is accessible. Error: " + e.getMessage(), e);
        } catch (JsonParseException e) {
            LOGGER.log(Level.SEVERE, "Configuration file is not a valid JSON at path: " + CONFIG_FILE_PATH
                    + ". Please check the file format. Error: " + e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error while reading configuration file from path: " + CONFIG_FILE_PATH
                    + ". Please check file permissions and retry. Error: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error while loading configuration. " + "Please investigate further. Error: " + e.getMessage(), e);
        }

    }

    /**
     * The getJsonObject method returns the loaded JSON object
     * @return JSONObject loaded JSON object
     */
    public JSONObject getJsonObject() {
        return jsonObject;
    }


    /** Method to get the value for a given rule name and key as an Object
     * If the rule name exists and has the key, return the value
     * @param ruleName String rule name
     * @param key String key
     * @return Object value for the key
     */
    private Object getConfigValue(String ruleName, String key) {
        if (jsonObject.has(ruleName)) {
            JSONObject ruleConfig = jsonObject.getJSONObject(ruleName);
            if (ruleConfig.has(key)) {
                return ruleConfig.get(key);
            }
        }
        return null;
    }

    /**
     * Method to get the methods to check for a given rule name, which is a List of Strings
     * @param ruleName String rule name
     * @return List<String> methods to check
     */
    public List<String> getMethodsToCheck(String ruleName) {
        String key = "methods_to_check";
        Object value = getConfigValue(ruleName, key);
        return processMethodsToCheck(value);
    }

    /** Method to get the method to check for a given rule name, which is a String
     * If the rule name exists and has the key, return the value
     * @param ruleName String rule name
     * @return String method to check
     */
    public String getMethodToCheck(String ruleName) {
        String key = "method_to_check";
        return (String) getConfigValue(ruleName, key);
    }

    /**
     * Method to get the client name for a given rule name
     * @param ruleName String rule name
     * @return String client name
     */
    public String getClientName(String ruleName) {
        String key = "client_name";
        return (String) getConfigValue(ruleName, key);
    }

    /** Method to get the suggestion message for a given rule name
     * @param ruleName String rule name
     * @return String suggestion message
     */
    public String getSuggestionMessage(String ruleName) {
        String key = "antipattern_message";
        return (String) getConfigValue(ruleName, key);
    }

    /**
     * Method to get the solution for a given rule name
     * @param ruleName String rule name
     * @return String solution
     */
    public String getSolution(String ruleName) {
        String key = "solution";
        return (String) getConfigValue(ruleName, key);
    }

    /**
     * Method to process the methods to check based on its type
     * If the methods to check is a JSONArray, return a List of Strings
     * @param methodsToCheck Object methods to check
     * @return List<String> methods to check
     */
    private List<String> processMethodsToCheck(Object methodsToCheck) {
        List<String> methods = new ArrayList<>();
         if (methodsToCheck instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) methodsToCheck;
            for (int i = 0; i < jsonArray.length(); i++) {
                methods.add(jsonArray.getString(i));
            }
        }
        return methods;
    }

    /** Method to get all information for a given rule name
     * @param ruleName String rule name
     * @return JSONObject rule details
     */
    public JSONObject getCheckDetails(String ruleName) {
        return jsonObject.optJSONObject(ruleName);
    }
}