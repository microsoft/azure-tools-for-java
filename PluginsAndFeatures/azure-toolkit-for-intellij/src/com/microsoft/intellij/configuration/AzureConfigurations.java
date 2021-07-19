/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@State(name = "Azure Toolkit for IntelliJ", storages = {@Storage("azure-data.xml")})
public class AzureConfigurations implements PersistentStateComponent<AzureConfigurations.AzureConfigurationData> {
    private static final String AZURE_ENVIRONMENT_KEY = "azure_environment";
    private static final String PASSWORD_SAVE_TYPE = "password_save_type";
    private static final String FUNCTION_CORE_TOOLS_PATH = "function_core_tools_path";
    private static final String TELEMETRY_ALLOW_TELEMETRY = "telemetry_allow_telemetry";
    private static final String TELEMETRY_INSTALLATION_ID = "telemetry_installation_id";
    private static final String TELEMETRY_PLUGIN_VERSION = "telemetry_plugin_version";

    private final AzureConfigurationData currentConfiguration = new AzureConfigurationData();

    public static AzureConfigurations getInstance() {
        return ServiceManager.getService(AzureConfigurations.class);
    }

    @Override
    public @Nonnull
    AzureConfigurationData getState() {
        return currentConfiguration;
    }

    @Override
    public void loadState(@Nonnull AzureConfigurationData state) {
        XmlSerializerUtil.copyBean(state, currentConfiguration);
    }

    public String environment() {
        return currentConfiguration.environment();
    }

    public String passwordSaveType() {
        return currentConfiguration.passwordSaveType();
    }

    public boolean allowTelemetry() {
        return currentConfiguration.allowTelemetry();
    }

    public String functionCoreToolsPath() {
        return currentConfiguration.functionCoreToolsPath();
    }

    public String installationId() {
        return currentConfiguration.installationId();
    }

    public String pluginVersion() {
        return currentConfiguration.pluginVersion();
    }

    public static class AzureConfigurationData {
        @MapAnnotation
        private Map<String, String> properties;

        @Nonnull
        private Map<String, String> map() {
            if (properties == null) {
                properties = new HashMap<>();
            }
            return properties;
        }

        public String environment() {
            return map().getOrDefault(AZURE_ENVIRONMENT_KEY, StringUtils.EMPTY);
        }

        public void environment(String environment) {
            map().put(AZURE_ENVIRONMENT_KEY, environment);
        }

        public String passwordSaveType() {
            return map().getOrDefault(PASSWORD_SAVE_TYPE, StringUtils.EMPTY);
        }

        public void passwordSaveType(String passwordSaveType) {
            map().put(PASSWORD_SAVE_TYPE, passwordSaveType);
        }

        public boolean allowTelemetry() {
            return Boolean.parseBoolean(map().getOrDefault(TELEMETRY_ALLOW_TELEMETRY, "true"));
        }

        public void allowTelemetry(boolean allowTelemetry) {
            map().put(TELEMETRY_ALLOW_TELEMETRY, String.valueOf(allowTelemetry));
        }

        public String functionCoreToolsPath() {
            return map().getOrDefault(FUNCTION_CORE_TOOLS_PATH, StringUtils.EMPTY);
        }

        public void functionCoreToolsPath(String path) {
            map().put(FUNCTION_CORE_TOOLS_PATH, path);
        }

        public String installationId() {
            return map().getOrDefault(TELEMETRY_INSTALLATION_ID, StringUtils.EMPTY);
        }

        public void installationId(String installationId) {
            map().put(TELEMETRY_INSTALLATION_ID, installationId);
        }

        public String pluginVersion() {
            return map().getOrDefault(TELEMETRY_PLUGIN_VERSION, StringUtils.EMPTY);
        }

        public void pluginVersion(String pluginVersion) {
            map().put(TELEMETRY_PLUGIN_VERSION, pluginVersion);
        }
    }
}
