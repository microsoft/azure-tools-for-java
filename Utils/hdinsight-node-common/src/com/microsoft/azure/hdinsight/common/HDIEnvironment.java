package com.microsoft.azure.hdinsight.common;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class HDIEnvironment implements IEnvironment {

    private final Map<String, String> endpoints;
    private final Environment environment;

    private static final HDIEnvironment GLOBAL = new HDIEnvironment(new HashMap<String, String>() {{
            put("connectionString", "https://%s.azurehdinsight.net/");
            put("blobFullName", "%s.blob.core.windows.net");
            put("portalUrl", "https://portal.azure.com/");
        }}, Environment.GLOBAL);

    private static final HDIEnvironment CHINA = new HDIEnvironment(new HashMap<String, String>() {{
        put("connectionString", "https://%s.azurehdinsight.cn/");
        put("blobFullName", "%s.blob.core.chinacloudapi.cn");
        put("portalUrl", "https://portal.azure.cn/");
    }}, Environment.CHINA);

    private static final HDIEnvironment US_GOVERNMENT = new HDIEnvironment(new HashMap<String, String>() {{
        put("connectionString", "https://%s.azurehdinsight.us/");
        put("blobFullName", "%s.blob.core.usgovcloudapi.net");
        put("portalUrl", "https://manage.windowsazure.us/");
    }}, Environment.US_GOVERNMENT);

    private static final HDIEnvironment GERMANY = new HDIEnvironment(new HashMap<String, String>() {{
        put("connectionString", "https://%s.azurehdinsight.de/");
        put("blobFullName", "%s.blob.core.cloudapi.de");
        put("portalUrl", "https://portal.microsoftazure.de/");
    }}, Environment.GERMAN);

    private HDIEnvironment(Map<String, String> endpoints, Environment environment) {
        this.endpoints = endpoints;
        this.environment = environment;
    }

    public HDIEnvironment(@NotNull Environment environment) {
        switch (environment) {
            case GLOBAL:
                this.environment = Environment.GLOBAL;
                this.endpoints = HDIEnvironment.GLOBAL.endpoints;
                break;
            case CHINA:
                this.environment = Environment.CHINA;
                this.endpoints = HDIEnvironment.CHINA.endpoints;
                break;
            case GERMAN:
                this.environment = Environment.GERMAN;
                this.endpoints = HDIEnvironment.GERMANY.endpoints;
                break;
            case US_GOVERNMENT:
                this.environment = Environment.US_GOVERNMENT;
                this.endpoints = HDIEnvironment.US_GOVERNMENT.endpoints;
                break;
            default:
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

        try {
            azureManager = AuthMethodManager.getInstance().getAzureManager();
        } catch (IOException ignored) {
            // ignore the exception
        }

        if (azureManager != null) {
            env = azureManager.getEnvironment();
        }
        switch (env) {
            case GLOBAL:
                return HDIEnvironment.GLOBAL;
            case CHINA:
                return HDIEnvironment.CHINA;
            case GERMAN:
                return HDIEnvironment.GERMANY;
            case US_GOVERNMENT:
                return HDIEnvironment.US_GOVERNMENT;
            default:
                return HDIEnvironment.GLOBAL;
        }
    }
}
