/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.application.ApplicationInfo;
import com.microsoft.azuretools.telemetry.AppInsightsConfiguration;
import com.microsoft.intellij.configuration.AzureConfigurations;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class AppInsightsConfigurationImpl implements AppInsightsConfiguration {
    static final String EVENT_NAME_PREFIX_INTELLIJ = "AzurePlugin.Intellij.";
    // eventname for new telemetry
    static final String EVENT_NAME = "AzurePlugin.Intellij";
    static final String sessionId = UUID.randomUUID().toString();
    static final String ide = getIDE();

    private static final String getIDE() {
        ApplicationInfo info = ApplicationInfo.getInstance();
        return String.format("%s_%s_%s", info.getVersionName(), info.getFullVersion(), info.getBuild());
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String pluginVersion() {
        return AzurePlugin.PLUGIN_VERSION;
    }

    @Override
    public String installationId() {
        return AzureConfigurations.getInstance().getState().installationId();
    }

    @Override
    public String preferenceVal() {
        return String.valueOf(AzureConfigurations.getInstance().getState().allowTelemetry());
    }

    @Override
    public boolean validated() {
        return StringUtils.isNotBlank(AzureConfigurations.getInstance().getState().installationId());
    }

    @Override
    public String eventNamePrefix() {
        return EVENT_NAME_PREFIX_INTELLIJ;
    }

    @Override
    public String ide() {
        return ide;
    }

    @Override
    public String eventName() {
        return EVENT_NAME;
    }

}
