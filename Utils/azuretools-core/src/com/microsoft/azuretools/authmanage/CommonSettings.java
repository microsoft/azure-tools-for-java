/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.google.gson.*;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.azurecommons.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import static com.microsoft.azuretools.Constants.*;


public class CommonSettings {

    private static final Logger LOGGER = Logger.getLogger(CommonSettings.class.getName());
    private static final String ENV_NAME_KEY = "EnvironmentName";
    private static final String MOVE_RESOURCE_FILE_FAIL = "Fail to move Azure Toolkit resource file %s to %s";
    private static final String CLEAN_DEPRECATED_FOLDER_FAIL = "Fail to clean deprecated folder %s";
    private static final List<String> RESOURCE_FILE_LIST = Arrays.asList(
            FILE_NAME_AAD_PROVIDER,
            FILE_AUTH_AUTH_CACHE,
            FILE_NAME_CORE_LIB_LOG,
            FILE_NAME_SURVEY_CONFIG
    );

    private static String settingsBaseDir = null;
    private static Environment env = Environment.GLOBAL;

    public static String getSettingsBaseDir() {
        return settingsBaseDir;
    }

    public static void setUpEnvironment(Environment environment) {
        env = environment;
    }

    public static void setUpEnvironment(@NotNull String basePath, String deprecatedPath) throws IOException {
        // If base dir doesn't exist or is empty, move resources from oldBaseDir base folder
        if (isUsingDeprecatedBaseFolder(basePath, deprecatedPath)) {
            moveResourcesToBaseFolder(basePath, deprecatedPath);
        }
        initBaseDir(basePath);
        setUpEnvironment(basePath);
    }

    public static void setUpEnvironment(@NotNull String baseDir) {
        settingsBaseDir = baseDir;
        String aadProfilderFile = Paths.get(CommonSettings.settingsBaseDir, FILE_NAME_AAD_PROVIDER).toString();
        File f = new File(aadProfilderFile);
        if (!f.exists() || !f.isFile()) {
            return;
        }

        try (FileReader fileReader = new FileReader(aadProfilderFile)) {
            JsonParser parser = new JsonParser();
            JsonElement jsonTree = parser.parse(fileReader);
            if (jsonTree.isJsonObject()) {
                JsonObject jsonObject = jsonTree.getAsJsonObject();
                JsonElement envElement = jsonObject.get(ENV_NAME_KEY);
                String envName = (envElement != null ? envElement.getAsString() : null);
                if (null != envName) {
                    // Provider file firstly
                    ProvidedEnvironment providedEnv = null;

                    JsonArray envs = jsonObject.getAsJsonArray("Environments");
                    if (envs != null) {
                        JsonElement providedEnvElem = StreamSupport.stream(envs.spliterator(), false)
                                .map(JsonElement::getAsJsonObject)
                                .filter(obj -> obj != null &&
                                        obj.get("envName") != null &&
                                        obj.get("envName").getAsString().equals(envName))
                                .findFirst()
                                .orElse(null);

                        if (providedEnvElem != null) {
                            try {
                                providedEnv = new Gson().fromJson(providedEnvElem, ProvidedEnvironment.class);
                            } catch (Exception e) {
                                LOGGER.warning("Parsing JSON String from " + providedEnvElem +
                                        "as provided environment failed, got the exception: " + e);
                            }
                        }
                    }

                    if (providedEnv == null) {
                        setEnvironment(envName, null);
                    } else {
                        env = providedEnv;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AzureEnvironment getAdEnvironment() {
        return env.getAzureEnvironment();
    }

    public static Environment getEnvironment() {
        return env;
    }

    public static String USER_AGENT = "Azure Toolkit";

    /**
     * Need this as a static method when we call this class directly from Eclipse or IntelliJ plugin to know plugin version
     */
    public static void setUserAgent(String userAgent) {
        USER_AGENT = userAgent;
    }

    private static void setEnvironment(@NotNull String env, Map<String, String> endPointMap) {
        // TODO: endPointMap currently is not used. Leave it in the api in case there is later change.
        try {
            CommonSettings.env = Environment.valueOf(env.toUpperCase());
        } catch (Exception e) {
            CommonSettings.env = Environment.GLOBAL;
        }
    }

    private static boolean isUsingDeprecatedBaseFolder(String basePath, String deprecatedPath) {
        File baseDir = new File(basePath);
        return !baseDir.exists() && FileUtil.isNonEmptyFolder(deprecatedPath);
    }

    private static void initBaseDir(@NotNull String basePath) throws IOException {
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            FileUtils.forceMkdir(baseDir);
        }
        if (Utils.isWindows()) {
            Files.setAttribute(baseDir.toPath(), "dos:hidden", true);
        }
    }

    private static void moveResourcesToBaseFolder(String basePath, String deprecatedPath) {
        final File baseDir = new File(basePath);
        final File deprecatedDir = new File(deprecatedPath);
        Arrays.stream(deprecatedDir.listFiles())
                .filter(CommonSettings::isToolkitResourceFile)
                .forEach(file -> moveToolkitResourceFileToFolder(file, baseDir));
        cleanDeprecatedFolder(deprecatedDir);
    }

    private static boolean isToolkitResourceFile(File file) {
        return file.isFile() && RESOURCE_FILE_LIST.stream()
                .anyMatch(resource -> StringUtils.containsIgnoreCase(file.getName(), resource));
    }

    private static void moveToolkitResourceFileToFolder(File resourceFile, File baseDir) {
        try {
            FileUtils.moveToDirectory(resourceFile, baseDir, true);
        } catch (IOException e) {
            LOGGER.warning(String.format(MOVE_RESOURCE_FILE_FAIL, resourceFile, baseDir));
        }
    }

    private static void cleanDeprecatedFolder(File deprecatedDir) {
        if (ArrayUtils.isEmpty(deprecatedDir.list())) {
            try {
                FileUtils.deleteDirectory(deprecatedDir);
            } catch (IOException e) {
                LOGGER.warning(String.format(CLEAN_DEPRECATED_FOLDER_FAIL, deprecatedDir.getName()));
            }
        }
    }
}
