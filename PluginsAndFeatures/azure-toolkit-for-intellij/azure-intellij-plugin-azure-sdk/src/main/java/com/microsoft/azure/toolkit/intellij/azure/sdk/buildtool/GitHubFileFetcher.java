package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Class to fetch files from GitHub.
 * The class fetches .txt and .json files from GitHub and parses them to get the data.
 * The data is used to check the version of the libraries in the pom.xml file against the recommended version.
 */
class GitHubFileFetcher {

    private static final Logger LOGGER = Logger.getLogger(GitHubFileFetcher.class.getName());

    /**
     * The fetchTxtFileFromGitHub method fetches a .txt file from GitHub and parses it to get the recommended version for each library.
     * This method is used to fetch the recommended version for the libraries in the pom.xml file. It is used by the UpgradeLibraryVersionCheck inspection.
     *
     * @param fileUrl The URL of the .txt file to fetch
     * @return A map of the recommended version for each library
     * @throws IOException If an error occurs while reading the file
     */
    static Map<String, String> fetchTxtFileFromGitHub(String fileUrl) throws IOException {
        StringBuilder content = new StringBuilder();

        URL url = new URL(fileUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Read the file from the URL
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("#")) {
                    continue; // Ignore comment lines
                }
                content.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            LOGGER.severe("Error reading file from GitHub: " + e);
        } catch (SecurityException e) {
            LOGGER.severe("Security exception accessing URL: " + e);
        } catch (IllegalArgumentException e) {
            LOGGER.severe("Illegal argument: " + e);
        } catch (IllegalStateException e) {
            LOGGER.severe("Illegal state: " + e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error: " + e);
        } finally {
            connection.disconnect();
        }
        return parseTxtFile(content.toString());
    }

    /**
     * The parseTxtFile method parses the content of the .txt file to get the recommended version for each library.
     * The content of the file is in the format "artifactId: minor version".
     * The method returns a map where the key is the artifactId and the value is the recommended version.
     *
     * @param content The content of the .txt file
     * @return A map of the recommended version for each library
     */
    private static Map<String, String> parseTxtFile(String content) {

        Map<String, String> fileContent = new ConcurrentHashMap<>();

        if (content != null) {
            String[] lines = content.toString().split("\n");

            for (String line : lines) {
                String[] parts = line.split(";");
                if (parts.length > 1) {
                    String artifactId = parts[0];
                    String[] versionParts = parts[1].split("\\.");
                    fileContent.put(artifactId, versionParts[0] + "." + versionParts[1]); // key is artifactId, value is recommended version
                }
            }
        }
        return fileContent;
    }

    /**
     * The loadJsonDataFromUrl method fetches a .json file from GitHub and parses it to get the data.
     * This method is used to fetch the data for the libraries in the pom.xml file. It is used by the IncompatibleDependencyCheck inspection.
     *
     * @param jsonUrl The URL of the .json file to fetch
     * @return A map of the data for the libraries
     */
    static Map<String, Set<String>> loadJsonDataFromUrl(String jsonUrl) {

        HttpURLConnection connection;
        try {
            URL url = new URL(jsonUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (InputStream inputStream = connection.getInputStream(); JsonReader jsonReader = JsonProviders.createReader(inputStream)) {
                return parseJson(jsonReader);
            }
        } catch (IOException e) {
            LOGGER.severe("Error reading file from GitHub: " + e);
        } catch (SecurityException e) {
            LOGGER.severe("Security exception accessing URL: " + e);
        } catch (IllegalArgumentException e) {
            LOGGER.severe("Illegal argument: " + e);
        } catch (IllegalStateException e) {
            LOGGER.severe("Illegal state: " + e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error: " + e);
        }
        return new HashMap<>();
    }

    /**
     * Method to parse JSON data into a nested map structure
     * The method reads the JSON data from the JsonReader and parses it to get the data for the libraries.
     * The data is in a Map<String, Set<String>> format where the key is the group and the value is a set of artifactIds.
     * For example, the data for the Jackson library is in the format
     * "jackson_2.10: [com.fasterxml.jackson.core:jackson-annotations, com.fasterxml.jackson.core:jackson-core, com.fasterxml.jackson.core:jackson-databind]".
     *
     * @param jsonReader The JsonReader object to read the JSON data
     * @return A map of the data for the libraries
     * @throws IOException If an error occurs while reading the JSON data
     */
    private static Map<String, Set<String>> parseJson(JsonReader jsonReader) throws IOException {
        Map<String, Set<String>> versionData = new ConcurrentHashMap<>();
        // Read the start of the JSON object
        if (jsonReader.nextToken() == JsonToken.START_OBJECT) {
            while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
                // Read the key for the current group (e.g., "jackson_2.10")
                String groupKey = jsonReader.getFieldName();
                Set<String> groupSet = new HashSet<>();

                // Ensure we're at the start of the array for the current group
                if (jsonReader.nextToken() == JsonToken.START_ARRAY) {
                    while (jsonReader.nextToken() != JsonToken.END_ARRAY) {
                        // Ensure we're at the start of an object within the array
                        if (jsonReader.nextToken() == JsonToken.FIELD_NAME) {
                            // Read the artifactId and version within the object
                            String groupAndArtifactId = jsonReader.getFieldName();
                            groupSet.add(groupAndArtifactId);
                        }
                        while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
                            // Do nothing just skip
                        }
                    }
                }
                versionData.put(groupKey, groupSet);
            }
        }
        return versionData;
    }
}
