/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.appservice.jfr;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebAppBase;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlightRecorderManager {
    private static Map<String, FlightRecorderStarterBase> jfrStarters = new ConcurrentHashMap<>();

    public static FlightRecorderStarterBase getFlightRecorderStarter(@NotNull WebAppBase appService) {
        return jfrStarters.computeIfAbsent(appService.id(), id -> {
            if (appService.operatingSystem() == OperatingSystem.LINUX) {
                return new LinuxFlightRecorderStarter(appService);
            } else if (appService.operatingSystem() == OperatingSystem.WINDOWS) {
                return new WindowFlightRecorderStarter(appService);
            } else {
                throw new IllegalStateException(String.format("Unknown os for app service(%s):",
                                                              appService.name(),
                                                              appService.operatingSystem()));
            }
        });
    }
}
