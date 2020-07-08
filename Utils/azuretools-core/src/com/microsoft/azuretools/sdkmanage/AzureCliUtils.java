/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.sdkmanage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

import static com.microsoft.azuretools.azurecommons.util.Utils.isWindows;

public class AzureCliUtils {

    public static AzureCliAccountInfo getAzureCliAccount() {
        final Gson gson = new Gson();
        final String accountInfo;
        try {
            accountInfo = executeMultipleLineOutput("az account show", null);
            final JsonObject accountObject = gson.fromJson(accountInfo, JsonObject.class);
            String tenantId = accountObject.get("tenantId").getAsString();
            String environment = accountObject.get("environmentName").getAsString();
            String subscriptionId = accountObject.get("id").getAsString();
            String userName = accountObject.get("user").getAsJsonObject().get("name").getAsString();
            String userType = accountObject.get("user").getAsJsonObject().get("type").getAsString();
            return new AzureCliAccountInfo(tenantId, environment, userName, userType, subscriptionId);
        } catch (InterruptedException | IOException | JsonParseException e) {
            // Return null when azure cli is not signed in
            return null;
        }
    }

    private static String executeMultipleLineOutput(final String cmd, File cwd)
            throws IOException, InterruptedException {
        final String[] cmds = isWindows() ? new String[]{"cmd.exe", "/c", cmd} : new String[]{"bash", "-c", cmd};
        final Process p = Runtime.getRuntime().exec(cmds, null, cwd);
        final int exitCode = p.waitFor();
        if (exitCode != 0) {
            return IOUtils.toString(p.getErrorStream(), "utf8");
        }
        return IOUtils.toString(p.getInputStream(), "utf8");
    }

    private static class LazyLoader {
        static final AzureCliAzureManager INSTANCE = new AzureCliAzureManager();
    }

    public static class AzureCliAccountInfo {
        private String tenantId;
        private String environment;
        private String userName;
        private String userType;
        private String subscriptionId;

        public AzureCliAccountInfo(String tenantId, String environment, String userName, String userType, String subscriptionId) {
            this.tenantId = tenantId;
            this.environment = environment;
            this.userName = userName;
            this.userType = userType;
            this.subscriptionId = subscriptionId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getEnvironment() {
            return environment;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserType() {
            return userType;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }
    }

    private AzureCliUtils() {

    }
}
