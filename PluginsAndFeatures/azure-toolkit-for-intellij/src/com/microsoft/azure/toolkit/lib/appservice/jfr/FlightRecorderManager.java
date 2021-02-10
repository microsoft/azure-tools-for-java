/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.jfr;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebAppBase;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FlightRecorderManager {
    private static Map<String, FlightRecorderStarterBase> jfrStarters = new ConcurrentHashMap<>();

    public static FlightRecorderStarterBase getFlightRecorderStarter(@NotNull WebAppBase appService) {
        return jfrStarters.computeIfAbsent(appService.id(), id -> {
            if (appService.operatingSystem() == OperatingSystem.LINUX) {
                return new LinuxFlightRecorderStarter(appService);
            } else if (appService.operatingSystem() == OperatingSystem.WINDOWS) {
                return new WindowFlightRecorderStarter(appService);
            } else {
                throw new IllegalStateException(message("appService.jfr.error.unknownOs", appService.operatingSystem(), appService.name()));
            }
        });
    }
}
