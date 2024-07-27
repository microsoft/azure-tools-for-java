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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
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
     * The parsePomFile method fetches the pom.xml file from the URL and parses it to get the dependencies.
     * This method is used to fetch the pom.xml file from the URL and parse it to get the dependencies.
     * It is used by the UpgradeLibraryVersionCheck inspection.
     *
     * @param pomUrl The URL of the pom.xml file to fetch
     * @return A map of the dependencies in the pom.xml file
     * @throws IOException                  If an error occurs while reading the file
     * @throws ParserConfigurationException If an error occurs while configuring the parser
     * @throws SAXException                 If an error occurs while parsing the file
     */
    static Map<String, String> parsePomFile(String pomUrl) {

        // Fetch the pom.xml file from the URL and parse it to get the dependencies
        Document pomDoc = fetchXmlDocument(pomUrl);

        // Get the list of dependencies from the pom.xml file
        NodeList dependencies = pomDoc.getElementsByTagName("dependency");

        // Parse the dependencies and get the groupId, artifactId, and version
        Map<String, String> artifactVersionMap = new HashMap<>();

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

        return artifactVersionMap;
    }

    /**
     * The getLatestVersion method fetches the latest Azure Client release versions from Maven Central.
     * This method is used to fetch the latest version of the library from the metadata file hosted on Maven Central.
     * It is used by the UpgradeLibraryVersionCheck inspection.
     *
     * @param metadataUrl The URL of the metadata file to fetch
     * @return The latest version of the library
     * @throws IOException                  If an error occurs while reading the file
     * @throws ParserConfigurationException If an error occurs while configuring the parser
     * @throws SAXException                 If an error occurs while parsing the file
     */
    static String getLatestVersion(String metadataUrl) {

        // Fetch the metadata file from the URL and parse it
        Document metadataDoc = fetchXmlDocument(metadataUrl);

        // Get the list of versions from the metadata file
        NodeList versions = metadataDoc.getElementsByTagName("version");

        // Return the latest version from the list of versions
        return versions.item(versions.getLength() - 1).getTextContent();
    }

    /**
     * The fetchXmlDocument method fetches an XML document from a URL and parses it.
     * This method is used to fetch the pom.xml file from the URL and parse it to get the dependencies.
     * It is used by the UpgradeLibraryVersionCheck inspection.
     *
     * @param urlString The URL of the XML document to fetch
     * @return The parsed XML document
     * @throws IOException                  If an error occurs while reading the document
     * @throws ParserConfigurationException If an error occurs while configuring the parser
     * @throws SAXException                 If an error occurs while parsing the document
     */
    private static Document fetchXmlDocument(String urlString) {

        // Open connection to the URL and get the input stream
        HttpURLConnection conn = null;
        InputStream inputStream = null;

        try {
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            // Open connection to the URL
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                conn.setRequestMethod("GET");
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            }

            // Get the input stream from the connection
            try {
                inputStream = conn.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Create a document builder to parse the XML document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Parse the document from the connection input stream
            DocumentBuilder builder = null;
            try {
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }

            try {
                return builder.parse(inputStream);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


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
