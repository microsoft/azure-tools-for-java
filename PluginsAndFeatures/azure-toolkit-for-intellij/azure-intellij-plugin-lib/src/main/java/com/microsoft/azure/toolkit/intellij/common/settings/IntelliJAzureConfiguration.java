/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureConfigurationProvider;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@State(name = "Azure Toolkit for IntelliJ", storages = {@Storage("azure-data.xml")})
public class IntelliJAzureConfiguration implements AzureConfiguration, PersistentStateComponent<IntelliJAzureConfiguration.State> {

    private final State myState = new State();

    @Getter
    @Setter
    private ProxyInfo proxyInfo;
    @Getter
    @Setter
    private SSLContext sslContext;

    @Nullable
    @Override
    public String get(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key) {
        return myState.properties.get(key);
    }

    public void set(@Nonnull @PropertyKey(resourceBundle = "configuration.keys") String key, String value) {
        myState.properties.put(key, value);
    }

    public State getState() {
        return myState;
    }

    public String getInstallationId() {
        String id = AzureConfiguration.super.getInstallationId();
        if (StringUtils.isBlank(id) || !InstallationIdUtils.isValidHashMac(id)) {
            id = InstallationIdUtils.getHashMac();
            if (StringUtils.isBlank(id) || !InstallationIdUtils.isValidHashMac(id)) {
                id = InstallationIdUtils.hash(PermanentInstallationID.get());
            }
            if (StringUtils.isNotBlank(id)) {
                this.setInstallationId(id);
            }
        }
        return id;
    }

    @Override
    public void loadState(@Nonnull State state) {
        this.myState.properties.putAll(state.properties);
    }

    @Override
    public Map<String, String> getSettings() {
        return Collections.unmodifiableMap(this.myState.properties);
    }

    @Override
    public void setSettings(Map<String, String> settings) {
        this.myState.properties.putAll(settings);
    }

    public void suppressAction(String actionId) {
        this.myState.suppressedActions.put(actionId, true);
    }

    public boolean isActionSuppressed(String actionId) {
        return this.myState.suppressedActions.getOrDefault(actionId, false);
    }

    public static class State {
        @Getter
        @Setter
        private Map<String, String> properties;

        @Getter
        @MapAnnotation(surroundKeyWithTag = false, surroundValueWithTag = false, surroundWithTag = false, entryTagName = "action", keyAttributeName = "id")
        private final Map<String, Boolean> suppressedActions = Collections.synchronizedMap(new HashMap<>());
    }

    public static class Provider implements AzureConfigurationProvider {
        @Override
        public AzureConfiguration getConfiguration() {
            return ApplicationManager.getApplication().getService(AzureConfiguration.class);
        }
    }
}
