/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.application.ApplicationInfo;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azuretools.telemetry.AppInsightsConfiguration;
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
        return Azure.az().config().getMachineId();
    }

    @Override
    public String preferenceVal() {
        return String.valueOf(Azure.az().config().getTelemetryEnabled());
    }

    @Override
    public boolean validated() {
        return StringUtils.isNotBlank(Azure.az().config().getMachineId());
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
