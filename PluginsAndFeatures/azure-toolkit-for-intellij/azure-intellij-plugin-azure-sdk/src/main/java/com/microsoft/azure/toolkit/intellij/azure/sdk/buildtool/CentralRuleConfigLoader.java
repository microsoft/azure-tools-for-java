package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import com.azure.json.JsonProviders;
import com.google.gson.JsonParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to load the rule configurations from a JSON file.
 * The rule configurations are loaded once and stored in a map.
 * The map contains the key-value pairs where the key is the rule name and the value is the RuleConfig object.
 * The RuleConfig object for a given key can be retrieved using the getRuleConfig method.
 */
public class CentralRuleConfigLoader {

    private static CentralRuleConfigLoader instance;
    private Map<String, RuleConfig> ruleConfigs;

    private static final Logger LOGGER = Logger.getLogger(CentralRuleConfigLoader.class.getName());

    private static final String CONFIG_FILE_PATH = "/META-INF/ruleConfigs.json";

    // Static initializer block to load the configurations once
    static {
        try {
            final String filePath = "/META-INF/ruleConfigs.json";
            instance = new CentralRuleConfigLoader(CONFIG_FILE_PATH);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Configuration file not found at path: " + CONFIG_FILE_PATH
                    + ". Please ensure the file exists and is accessible. Error: " + e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error while reading configuration file from path: " + CONFIG_FILE_PATH
                    + ". Please check file permissions and retry. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Constructor to load the rule configurations from the JSON file
     * @param filePath - the path to the JSON file
     * @throws IOException - if there is an error reading the file
     */
    private CentralRuleConfigLoader(String filePath) throws IOException {
        this.ruleConfigs = loadRuleConfigurations(filePath);
    }

    /**
     * This method returns the instance of the CentralRuleConfigLoader
     * @return CentralRuleConfigLoader instance
     */
    public static CentralRuleConfigLoader getInstance() {
        return instance;
    }

    /**
     * This method returns the RuleConfig object for the given key
     * @param key - the key to get the RuleConfig object
     * @return RuleConfig object
     */
    public RuleConfig getRuleConfig(String key) {
        return ruleConfigs.get(key);
    }

    /**
     * This method loads the rule configurations from the JSON file
     * @param filePath - the path to the JSON file
     * @return Map of RuleConfig objects
     * @throws IOException - if there is an error reading the file
     */
    private Map<String, RuleConfig> loadRuleConfigurations(String filePath) throws IOException {

        Map<String, RuleConfig> ruleConfigs = new HashMap<>();

        // Open the input stream to the JSON file
        try(InputStream is = CentralRuleConfigLoader.class.getResourceAsStream(filePath);

            // Create a JsonReader to read the JSON file
            JsonReader reader = JsonProviders.createReader(is)) {

            // Check if the JSON file starts with an object
            // If not, throw an exception
            // This is to ensure that the JSON file is in the correct format
            if (reader.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Expected start of object");
            }

            // Read the JSON file and parse the RuleConfig objects
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String key = reader.getFieldName();
                RuleConfig ruleConfig = parseRuleConfig(reader);

                // Add the RuleConfig object to the map
                ruleConfigs.put(key, ruleConfig);
            }
        }
        catch (JsonParseException e) {
            LOGGER.log(Level.SEVERE, "Configuration file is not a valid JSON at path: " + CONFIG_FILE_PATH
                    + ". Please check the file format. Error: " + e.getMessage(), e);
        }
        return ruleConfigs;
    }

    /**
     * This method parses the RuleConfig object from the JSON file
     * @param reader - the JsonReader object to read the JSON file
     * @return RuleConfig object parsed from the JSON file
     * @throws IOException - if there is an error reading the file
     */
    private RuleConfig parseRuleConfig(JsonReader reader) throws IOException {

        // Create a new RuleConfig object
        RuleConfig ruleConfig = new RuleConfig();

        // Check if the JSON file starts with an object
        if(reader.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected start of object");
        }

        // Read the JSON file and parse the RuleConfig object
        while (reader.nextToken() != JsonToken.END_OBJECT) {

            // Get the field name
            String fieldName = reader.getFieldName();
            // Check the field name and set the corresponding field in the RuleConfig object
            switch (fieldName) {
                case "methods_to_check":
                    ruleConfig.setMethodsToCheck(getListFromJsonArray(reader));
                    break;
                case "client_name":
                    ruleConfig.setClientName(reader.getString());
                    break;
                case "antipattern_message":
                    ruleConfig.setAntipatternMessage(reader.getString());
                    break;
                default:
                    reader.skipChildren();
            }
        }
        // Return the RuleConfig object
        return ruleConfig;
    }


    /**
     * This method parses the list of strings from the JSON array
     * @param reader - the JsonReader object to read the JSON file
     * @return List of strings parsed from the JSON array
     * @throws IOException - if there is an error reading the file
     */
    private List<String> getListFromJsonArray(JsonReader reader) throws IOException {
        List<String> list = new ArrayList<>();

        // Check if the JSON file starts with an array
        if(reader.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Expected start of array");
        }

        // Read the JSON file and parse the list of strings
        while (reader.nextToken() != JsonToken.END_ARRAY) {
            list.add(reader.getString());
        }
        return list;
    }
}

/**
 * This class represents the RuleConfig object
 * It contains the methods to check, the client name and the antipattern message
 */
class RuleConfig {
    private List<String> methodsToCheck;
    private String clientName;
    private String antipatternMessage;

    // Getters
    /**
     * This method returns the list of methods to check
     * @return List of methods to check
     */
    public List<String> getMethodsToCheck() {
        return methodsToCheck;
    }

    /**
     * This method returns the client name
     * @return Client name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * This method returns the antipattern message
     * @return Antipattern message
     */
    public String getAntipatternMessage() {
        return antipatternMessage;
    }

    // Setters
    /**
     * This method sets the list of methods to check
     * @param methodsToCheck - List of methods to check
     */
    public void setMethodsToCheck(List<String> methodsToCheck) {
        this.methodsToCheck = methodsToCheck;
    }

    /**
     * This method sets the client name
     * @param clientName - Client name
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * This method sets the antipattern message
     * @param antipatternMessage - Antipattern message
     */
    public void setAntipatternMessage(String antipatternMessage) {
        this.antipatternMessage = antipatternMessage;
    }
}