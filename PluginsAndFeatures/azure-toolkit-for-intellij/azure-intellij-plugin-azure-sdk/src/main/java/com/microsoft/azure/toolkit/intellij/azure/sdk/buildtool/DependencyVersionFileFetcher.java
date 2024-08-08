package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to fetch files from their corresponding data sources
 * The class fetches these sources and parses them to get the data.
 * The data is used to check the version of the libraries in the pom.xml file against the recommended version.
 */
class DependencyVersionFileFetcher {

    private static final Logger LOGGER = Logger.getLogger(DependencyVersionFileFetcher.class.getName());

    private static final DependencyVersionsDataCache<Map<String, String>> pomCache = new DependencyVersionsDataCache<>("pomCache.ser");
    private static final DependencyVersionsDataCache<String> versionCache = new DependencyVersionsDataCache<>("versionCache.ser");
    private static final DependencyVersionsDataCache<Map<String, Set<String>>> incompatibleVersionsCache = new DependencyVersionsDataCache<>("incompatibleVersionsCache.ser");

    /**
     * The parsePomFile method fetches the pom.xml file from the URL and parses it to get the dependencies.
     * This method is used to fetch the pom.xml file from the URL and parse it to get the dependencies.
     * It is used by the UpgradeLibraryVersionCheck inspection.
     *
     * @param pomUrl The URL of the pom.xml file to fetch
     * @return A map of the dependencies in the pom.xml file
     */
    static Map<String, String> parsePomFile(String pomUrl) {

        // Check the cache first
        Map<String, String> artifactVersionMap = pomCache.get(pomUrl);
        if (artifactVersionMap != null) {
            return artifactVersionMap;
        }
        // Fetch the pom.xml file from the URL and parse it to get the dependencies
        Document pomDoc = fetchXmlDocument(pomUrl);

        // Get the list of dependencies from the pom.xml file
        NodeList dependencies = pomDoc.getElementsByTagName("dependency");

        // Parse the dependencies and get the groupId, artifactId, and version
        artifactVersionMap = new HashMap<>();

        for (int i = 0; i < dependencies.getLength(); i++) {
            NodeList dependency = dependencies.item(i).getChildNodes();
            String groupId = null;
            String artifactId = null;
            String version = null;

            for (int j = 0; j < dependency.getLength(); j++) {
                if (dependency.item(j).getNodeName().equals("groupId")) {
                    groupId = dependency.item(j).getTextContent();
                } else if (dependency.item(j).getNodeName().equals("artifactId")) {
                    artifactId = dependency.item(j).getTextContent();
                } else if (dependency.item(j).getNodeName().equals("version")) {
                    version = dependency.item(j).getTextContent();
                }
            }

            if (groupId != null && artifactId != null && version != null) {

                // if we have version: 4.1.9 get 4.1
                String minorVersion = version.substring(0, version.lastIndexOf("."));

                // Add the groupId and artifactId to the map with the minor version as the value
                artifactVersionMap.put((groupId + ":" + artifactId), minorVersion);
            }
        }

        // Update the cache
        pomCache.put(pomUrl, artifactVersionMap);
        return artifactVersionMap;
    }

    /**
     * The getLatestVersion method fetches the latest Azure Client release versions from Maven Central.
     * This method is used to fetch the latest version of the library from the metadata file hosted on Maven Central.
     * It is used by the UpgradeLibraryVersionCheck inspection.
     *
     * @param metadataUrl The URL of the metadata file to fetch
     * @return The latest version of the library
     */
    static String getLatestVersion(String metadataUrl) {

        // Check the cache first
        String cachedVersion = versionCache.get(metadataUrl);
        if (cachedVersion != null) {
            return cachedVersion;
        }
        // Fetch the metadata file from the URL and parse it
        Document metadataDoc = fetchXmlDocument(metadataUrl);

        // Get the list of versions from the metadata file
        NodeList versions = metadataDoc.getElementsByTagName("version");

        String latestVersion = versions.item(versions.getLength() - 1).getTextContent();

        // Update the cache
        versionCache.put(metadataUrl, latestVersion);
        return latestVersion;
    }

    /**
     * The fetchXmlDocument method fetches an XML document from a URL and parses it.
     * This method is used to fetch the pom.xml file from the URL and parse it to get the dependencies.
     * It is used by the UpgradeLibraryVersionCheck inspection.
     *
     * @param urlString The URL of the XML document to fetch
     * @return The parsed XML document
     */
    private static Document fetchXmlDocument(String urlString) {

        // Open connection to the URL and get the input stream
        HttpURLConnection conn = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            inputStream = conn.getInputStream();

            // Create a document builder to parse the XML document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Parse the document from the connection input stream
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(inputStream);

        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Invalid URL: " + urlString, e);
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            LOGGER.log(Level.SEVERE, "Protocol error while fetching URL: " + urlString, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error while fetching URL: " + urlString, e);
            throw new RuntimeException(e);
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.log(Level.SEVERE, "Error parsing XML from URL: " + urlString, e);
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close input stream", e);
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * The loadJsonDataFromUrl method fetches a .json file from GitHub and parses it to get the data.
     * This method is used to fetch the data for the libraries in the pom.xml file. It is used by the IncompatibleDependencyCheck inspection.
     *
     * @param jsonUrl The URL of the .json file to fetch
     * @return A map of the data for the libraries
     */
    static Map<String, Set<String>> loadJsonDataFromUrl(String jsonUrl) {

        Map<String, Set<String>> jsonData = incompatibleVersionsCache.get(jsonUrl);

        if (jsonData != null) {
            return jsonData;
        }
        HttpURLConnection connection;
        try {
            URL url = new URL(jsonUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (InputStream inputStream = connection.getInputStream(); JsonReader jsonReader = JsonProviders.createReader(inputStream)) {
                jsonData = parseJson(jsonReader);
                incompatibleVersionsCache.put(jsonUrl, jsonData);
                return jsonData;
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
