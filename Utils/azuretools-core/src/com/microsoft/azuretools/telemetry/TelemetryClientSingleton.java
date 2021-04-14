/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import org.apache.commons.lang3.StringUtils;

public final class TelemetryClientSingleton {
    private TelemetryClient telemetry = null;
    private AppInsightsConfiguration configuration = null;

    private static final class SingletonHolder {
        private static final TelemetryClientSingleton INSTANCE = new TelemetryClientSingleton();
    }

    public static TelemetryClient getTelemetry() {
        return SingletonHolder.INSTANCE.telemetry;
    }

    public static void setConfiguration(final AppInsightsConfiguration configuration) {
        SingletonHolder.INSTANCE.configuration = configuration;
    }

    private TelemetryClientSingleton() {
        telemetry = new TelemetryClient() {
            @Override
            public boolean isDisabled() {
                if (configuration == null) {
                    return true;
                }
                return (StringUtils.isNotEmpty(configuration.preferenceVal()) && !Boolean.valueOf(configuration.preferenceVal())) || super.isDisabled();
            }
        };
    }
}
