/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetry;

import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetryClient;
import org.apache.commons.lang3.StringUtils;

public final class TelemetryClientSingleton {
    private static AzureTelemetryClient telemetry;
    private static AppInsightsConfiguration configuration = null;

    public static synchronized AzureTelemetryClient getTelemetry() {
        if(TelemetryClientSingleton.telemetry==null){
            TelemetryClientSingleton.telemetry = AzureTelemeter.getClient();
        }
        return TelemetryClientSingleton.telemetry;
    }

    public static void setConfiguration(final AppInsightsConfiguration configuration) {
        TelemetryClientSingleton.configuration = configuration;
    }

    private TelemetryClientSingleton() {
    }
}
