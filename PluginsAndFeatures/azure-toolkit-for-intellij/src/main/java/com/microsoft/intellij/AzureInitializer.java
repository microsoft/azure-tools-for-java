/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY;
import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY_PLUGIN_VERSION;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class AzureInitializer {
    public static void initialize() {
        ProxyUtils.initProxy();
        initializeAzureConfiguration();
    }

    private static void initializeAzureConfiguration() {
        IntellijConfigInitializer.initialize();
        initializeTelemetry();
    }

    private static void initializeTelemetry() {
        final String oldVersion = AzureStoreManager.getInstance().getIdeStore().getProperty(TELEMETRY, TELEMETRY_PLUGIN_VERSION);
        // the code of saving current plugin version is at AzurePlugin
        AppInsightsClient.setAppInsightsConfiguration(new AppInsightsConfigurationImpl());
        if (StringUtils.isBlank(oldVersion)) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_INSTALL, null, null);
        } else if (StringUtils.isNotBlank(oldVersion) && !AzurePlugin.PLUGIN_VERSION.equalsIgnoreCase(oldVersion)) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UPGRADE, null, null);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Load, null, true);
        EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_LOAD, null, null);
        if (StringUtils.isNotBlank(Azure.az().config().getProxySource())) {
            final Map<String, String> map = Optional.ofNullable(AzureTelemeter.getCommonProperties()).map(HashMap::new).orElse(new HashMap<>());
            map.put(PROXY, "true");
            AzureTelemeter.setCommonProperties(map);
        }
    }
}
