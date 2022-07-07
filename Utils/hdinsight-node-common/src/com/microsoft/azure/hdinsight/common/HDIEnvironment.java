/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

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
        Environment env = Environment.GLOBAL;

        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            env = CommonSettings.getEnvironment();
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
