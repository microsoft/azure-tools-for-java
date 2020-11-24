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

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.util.HashMap;
import java.util.Map;

public final class HDIEnvironment implements IEnvironment {

    private final Map<String, String> endpoints;
    private final Environment environment;

    private static final HDIEnvironment GLOBAL = new HDIEnvironment(new HashMap<String, String>() {
        {
            put("connectionString", "https://%s.azurehdinsight.net/");
            put("blobFullName", "%s.blob.core.windows.net");
            put("portalUrl", "https://portal.azure.com/");
        }
    }, Environment.GLOBAL);

    private static final HDIEnvironment CHINA = new HDIEnvironment(new HashMap<String, String>() {
        {
            put("connectionString", "https://%s.azurehdinsight.cn/");
            put("blobFullName", "%s.blob.core.chinacloudapi.cn");
            put("portalUrl", "https://portal.azure.cn/");
        }
    }, Environment.CHINA);

    private static final HDIEnvironment US_GOVERNMENT = new HDIEnvironment(new HashMap<String, String>() {
        {
            put("connectionString", "https://%s.azurehdinsight.us/");
            put("blobFullName", "%s.blob.core.usgovcloudapi.net");
            put("portalUrl", "https://manage.windowsazure.us/");
        }
    }, Environment.US_GOVERNMENT);

    private static final HDIEnvironment GERMANY = new HDIEnvironment(new HashMap<String, String>() {
        {
            put("connectionString", "https://%s.azurehdinsight.de/");
            put("blobFullName", "%s.blob.core.cloudapi.de");
            put("portalUrl", "https://portal.microsoftazure.de/");
        }
    }, Environment.GERMAN);

    private HDIEnvironment(Map<String, String> endpoints, Environment environment) {
        this.endpoints = endpoints;
        this.environment = environment;
    }

    public HDIEnvironment(@NotNull Environment environment) {
        if (Environment.GLOBAL.equals(environment)) {
            this.environment = Environment.GLOBAL;
            this.endpoints = HDIEnvironment.GLOBAL.endpoints;

        } else if (Environment.CHINA.equals(environment)) {
            this.environment = Environment.CHINA;
            this.endpoints = HDIEnvironment.CHINA.endpoints;

        } else if (Environment.GERMAN.equals(environment)) {
            this.environment = Environment.GERMAN;
            this.endpoints = HDIEnvironment.GERMANY.endpoints;

        } else if (Environment.US_GOVERNMENT.equals(environment)) {
            this.environment = Environment.US_GOVERNMENT;
            this.endpoints = HDIEnvironment.US_GOVERNMENT.endpoints;

        } else {
            this.environment = Environment.GLOBAL;
            this.endpoints = HDIEnvironment.GLOBAL.endpoints;
        }
    }
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public String getClusterConnectionFormat() {
        return endpoints.get("connectionString");
    }

    @Override
    public String getBlobFullNameFormat() {
        return endpoints.get("blobFullName");
    }

    @Override
    public String getPortal() {
        return endpoints.get("portalUrl");
    }

    public static HDIEnvironment getHDIEnvironment() {
        AzureManager azureManager = null;
        Environment env = Environment.GLOBAL;

        azureManager = AuthMethodManager.getInstance().getAzureManager();

        if (azureManager != null) {
            env = azureManager.getEnvironment();
        }
        if (Environment.GLOBAL.equals(env)) {
            return HDIEnvironment.GLOBAL;
        } else if (Environment.CHINA.equals(env)) {
            return HDIEnvironment.CHINA;
        } else if (Environment.GERMAN.equals(env)) {
            return HDIEnvironment.GERMANY;
        } else if (Environment.US_GOVERNMENT.equals(env)) {
            return HDIEnvironment.US_GOVERNMENT;
        } else {
            return HDIEnvironment.GLOBAL;
        }
    }
}
