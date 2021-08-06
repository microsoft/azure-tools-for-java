/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.jfr;

import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FlightRecorderManager {
    private static Map<String, FlightRecorderStarterBase> jfrStarters = new ConcurrentHashMap<>();

    public static FlightRecorderStarterBase getFlightRecorderStarter(@NotNull IAppService appService) {
        return jfrStarters.computeIfAbsent(appService.id(), id -> {
            if (appService.getRuntime().getOperatingSystem() == OperatingSystem.LINUX) {
                return new LinuxFlightRecorderStarter(appService);
            } else if (appService.getRuntime().getOperatingSystem() == OperatingSystem.WINDOWS) {
                return new WindowFlightRecorderStarter(appService);
            } else {
                throw new IllegalStateException(message("appService.jfr.error.unknownOs", appService.getRuntime().getOperatingSystem(), appService.name()));
            }
        });
    }
}
