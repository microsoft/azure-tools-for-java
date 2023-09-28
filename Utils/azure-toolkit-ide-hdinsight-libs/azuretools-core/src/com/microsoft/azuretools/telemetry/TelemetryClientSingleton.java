/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetry;

import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetryClient;
import org.apache.commons.lang3.StringUtils;

public final class TelemetryClientSingleton {
    private final AzureTelemetryClient telemetry;
    private AppInsightsConfiguration configuration = null;

    private static final class SingletonHolder {
        private static final TelemetryClientSingleton INSTANCE = new TelemetryClientSingleton();
    }

    public static AzureTelemetryClient getTelemetry() {
        return SingletonHolder.INSTANCE.telemetry;
    }

    public static void setConfiguration(final AppInsightsConfiguration configuration) {
        SingletonHolder.INSTANCE.configuration = configuration;
    }

    private TelemetryClientSingleton() {
        telemetry = AzureTelemeter.getClient();
    }
}
