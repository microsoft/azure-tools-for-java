/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.authmanage;

import com.google.gson.*;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azuretools.authmanage.interact.IUIFactory;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import net.minidev.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

public class CommonSettings {
    private static final Logger LOGGER = Logger.getLogger(AdAuthManager.class.getName());
    public static final String authMethodDetailsFileName = "AuthMethodDetails.json";

    private static String settingsBaseDir = null;
    private static final String AAD_PROVIDER_FILENAME = "AadProvider.json";
    private static final String ENV_NAME_KEY = "EnvironmentName";
    private static final String COSMOS_SERVERLESS_KEY = "EnableCosmosSeverlessSpark";
    private static IUIFactory uiFactory;
    private static Environment ENV = Environment.GLOBAL;
    public static boolean ENABLE_COSMOS_SERVERLESS = false;

    public static String getSettingsBaseDir() {
        return settingsBaseDir;
    }

    public static void setUpEnvironment(@NotNull String baseDir) {
        settingsBaseDir = baseDir;
        String aadProfilderFile = Paths.get(CommonSettings.settingsBaseDir, AAD_PROVIDER_FILENAME).toString();
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
                if (null != envName){
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
                                        "as provided environment failed, got the exception: " + e );
                            }
                        }
                    }

                    if (providedEnv == null) {
                        setEnvironment(envName, null);
                    } else {
                        ENV = providedEnv;
                    }
                }

                JsonElement serverlessElement = jsonObject.get(COSMOS_SERVERLESS_KEY);
                if(serverlessElement != null) {
                    ENABLE_COSMOS_SERVERLESS = serverlessElement.getAsBoolean();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IUIFactory getUiFactory() {
        return uiFactory;
    }
    public static void setUiFactory(IUIFactory uiFactory) {
        CommonSettings.uiFactory = uiFactory;
    }

    public static AzureEnvironment getAdEnvironment() {
    	return ENV.getAzureEnvironment();
    }

    public static Environment getEnvironment() {
        return ENV;
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
            ENV = Environment.valueOf(env.toUpperCase());
        } catch (Exception e) {
            ENV = Environment.GLOBAL;
        }
    }
}
