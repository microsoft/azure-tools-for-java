/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.microsoft.azure.toolkit.ide.common.store.IIdeStore;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@State(name = "Azure Toolkit for IntelliJ", storages = {@Storage("azure-data.xml")})
public class AzureConfigurations implements PersistentStateComponent<AzureConfigurations.AzureConfigurationData>, IIdeStore {
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

    public String getProperty(@Nullable String service, @Nonnull String key) {
        return currentConfiguration.map().get(combineKey(service, key));
    }

    public String getProperty(@Nullable String service, @Nonnull String key, @org.jetbrains.annotations.Nullable String defaultValue) {
        return StringUtils.firstNonBlank(currentConfiguration.map().get(combineKey(service, key)), defaultValue);
    }

    public void setProperty(@Nullable String service, @Nonnull String key, @Nullable String value) {
        if (value == null) {
            currentConfiguration.map().remove(combineKey(service, key));
            return;
        }
        currentConfiguration.map().put(combineKey(service, key), value);
    }

    private static String combineKey(String service, String key) {
        return StringUtils.isBlank(service) ? key : String.format("%s.%s", service, key);
    }

    public static class AzureConfigurationData {
        @MapAnnotation
        private Map<String, String> properties;

        @Getter
        @MapAnnotation(surroundKeyWithTag = false, surroundValueWithTag = false, surroundWithTag = false, entryTagName = "action", keyAttributeName = "id")
        private final Map<String, Boolean> suppressedActions = Collections.synchronizedMap(new HashMap<>());

        @Nonnull
        private Map<String, String> map() {
            if (properties == null) {
                properties = new HashMap<>();
            }
            return properties;
        }
    }
}
