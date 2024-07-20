package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import com.azure.json.JsonProviders;

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
class RuleConfigLoader {

    private static final RuleConfigLoader instance;
    private final Map<String, RuleConfig> ruleConfigs;

    private static final Logger LOGGER = Logger.getLogger(RuleConfigLoader.class.getName());

    private static final String CONFIG_FILE_PATH = "/META-INF/ruleConfigs.json";

    // Static initializer block to load the configurations once
    static {
        RuleConfigLoader instanceTemp;
        try {
            instanceTemp = new RuleConfigLoader(CONFIG_FILE_PATH);
        } catch (FileNotFoundException e) {
            instanceTemp = null;
            LOGGER.log(Level.SEVERE, "Configuration file not found at path: " + CONFIG_FILE_PATH + ". Please ensure the file exists and is accessible. Error: " + e);
        } catch (IOException e) {
            instanceTemp = null;
            LOGGER.log(Level.SEVERE, "IO error while reading configuration file from path: " + CONFIG_FILE_PATH + ". Please check file permissions and retry. Error: " + e);
        }
        instance = instanceTemp;
    }

    /**
     * Constructor to load the rule configurations from the JSON file
     *
     * @param filePath - the path to the JSON file
     * @throws IOException - if there is an error reading the file
     */
    private RuleConfigLoader(String filePath) throws IOException {
        this.ruleConfigs = loadRuleConfigurations(filePath);
    }

    /**
     * This method returns the instance of the CentralRuleConfigLoader
     *
     * @return CentralRuleConfigLoader instance
     */
    static RuleConfigLoader getInstance() {
        return instance;
    }

    /**
     * This method returns the RuleConfig object for the given key
     *
     * @param key - the key to get the RuleConfig object
     * @return RuleConfig object or empty rule if one does not exist, {@link RuleConfig#EMPTY_RULE}
     */
    RuleConfig getRuleConfig(String key) {

        RuleConfig ruleConfig = ruleConfigs.get(key);
        if (ruleConfig == null) {
            return RuleConfig.EMPTY_RULE;
        } else {
            return ruleConfig;
        }
    }

    /**
     * This method loads the rule configurations from the JSON file
     *
     * @param filePath - the path to the JSON file
     * @return Map of RuleConfig objects
     */
    private Map<String, RuleConfig> loadRuleConfigurations(String filePath) {

        // temporary map to store the RuleConfig objects that will then be returned to the final map
        Map<String, RuleConfig> ruleConfigs = new HashMap<>();

        // Open the input stream to the JSON file
        try (InputStream is = RuleConfigLoader.class.getResourceAsStream(filePath);

             // Create a JsonReader to read the JSON file -- need another try to close the json reader
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
                RuleConfig ruleConfig = getRuleConfig(reader);

                // Add the RuleConfig object to the map
                ruleConfigs.put(key, ruleConfig);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error while parsing Json from path: " + CONFIG_FILE_PATH + ". Please check file permissions and retry. Error: " + e);
        }
        return ruleConfigs;
    }

    /**
     * This method parses the RuleConfig object from the JSON file
     * The RuleConfig object either contains the methods to check, clients to check or services to check,
     * and the anti-pattern messages
     *
     * @param reader - the JsonReader object to read the JSON file
     * @return RuleConfig object parsed from the JSON file
     * @throws IOException - if there is an error reading the file
     */
    private RuleConfig getRuleConfig(JsonReader reader) throws IOException {
        List<String> methodsToCheck = new ArrayList<>();
        List<String> clientsToCheck = new ArrayList<>();
        List<String> servicesToCheck = new ArrayList<>();
        Map<String, String> antiPatternMessageMap = new HashMap<>();

        // Check if the JSON file starts with an object
        if (reader.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected start of object");
        }

        // Read the JSON file and parse the RuleConfig object
        while (reader.nextToken() != JsonToken.END_OBJECT) {

            // Get the field name
            String fieldName = reader.getFieldName();
            // Check the field name and set the corresponding field in the RuleConfig object
            switch (fieldName) {
                case "methodsToCheck":
                    methodsToCheck = getListFromJsonArray(reader);
                    break;
                case "antiPatternMessage":
                    antiPatternMessageMap = getMapFromJsonObject(reader, antiPatternMessageMap);
                    break;
                case "clientsToCheck":
                    clientsToCheck = getListFromJsonArray(reader);
                    break;
                case "servicesToCheck":
                    servicesToCheck = getListFromJsonArray(reader);
                    break;
                default:
                    if (fieldName.endsWith("Check")) {
                        // Move to the next token to process the nested object
                        reader.nextToken();
                        antiPatternMessageMap = getMapFromJsonObject(reader, antiPatternMessageMap);
                    } else {
                        reader.skipChildren();
                    }
                    break;
            }
        }
        return new RuleConfig(methodsToCheck, clientsToCheck, servicesToCheck, antiPatternMessageMap);
    }

    /**
     * This method parses the list of strings from the JSON array
     *
     * @param reader - the JsonReader object to read the JSON file
     * @return List of strings parsed from the JSON array
     * @throws IOException - if there is an error reading the file
     */
    private List<String> getListFromJsonArray(JsonReader reader) throws IOException {
        List<String> list = new ArrayList<>();

        // Check if the JSON file starts with an array
        if (reader.nextToken() != JsonToken.START_ARRAY) {

            // check if a string has been passed
            if (reader.currentToken() == JsonToken.STRING) {
                list.add(reader.getString());
                return list;
            } else {
                throw new IOException("Expected start of array");
            }
        }

        // Read the JSON file and parse the list of strings
        while (reader.nextToken() != JsonToken.END_ARRAY) {
            list.add(reader.getString());
        }
        return list;
    }

    /**
     * This method is used to parse the anti-pattern messages from a JSON file
     * The anti-pattern messages can be for a set of methods, clients, or services
     * The anti-pattern messages can also be for specific rules like "no_block" and "with_subscribe"
     *
     * @param reader                - the JsonReader object to read the JSON file
     * @param antiPatternMessageMap - the map to store the anti-pattern messages
     * @return Map of strings parsed from the JSON object
     * @throws IOException - if there is an error reading the file
     */
    private Map<String, String> getMapFromJsonObject(JsonReader reader, Map<String, String> antiPatternMessageMap) throws IOException {

        String identifiersToCheck = null;
        String antiPatternMessage = null;

        // Read the JSON file and parse the RuleConfig object
        while (reader.nextToken() != JsonToken.END_OBJECT) {

            // Get the field name
            String fieldName = reader.getFieldName();
            switch (fieldName) {
                case "methodsToCheck":
                    identifiersToCheck = getListFromJsonArray(reader).get(0);
                    break;
                case "antiPatternMessage":
                    antiPatternMessage = reader.getString();
                    break;
                default:
                    reader.skipChildren();
                    break;
            }
            // Add to map based on conditions
            if (antiPatternMessage != null) {

                // This map is for base classes that have a set of discouraged identifiers and a corresponding set of antipattern messages
                if (identifiersToCheck != null) {
                    antiPatternMessageMap.put(identifiersToCheck, antiPatternMessage);
                    System.out.println(antiPatternMessageMap);
                } else {
                    // This map is for single anti-pattern messages of a particular rule
                    antiPatternMessageMap.put(fieldName, antiPatternMessage);
                    return antiPatternMessageMap;
                }
            }
        }
        // This map is to return mapped anti-pattern messages that have a set of discouraged identifiers and a corresponding set of antipattern messages
        return antiPatternMessageMap;
    }
}
