/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.jfr;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class WindowFlightRecorderStarter extends FlightRecorderStarterBase {
    private static final String HOME_PATH = "d:/home";

    public WindowFlightRecorderStarter(final IAppService appService) {
        super(appService);
    }

    public List<ProcessInfo> listProcess() {
        return appService.listProcess().stream()
                .filter(processInfo -> StringUtils.equalsIgnoreCase(processInfo.getName(), "java"))
                .collect(Collectors.toList());
    }

    public CommandOutput startFlightRecorder(int pid, int timeInSeconds, String fileName) {
        return appService.execute(constructJcmdCommand(pid, timeInSeconds, fileName), HOME_PATH);
    }

    @Override
    public String getFinalJfrPath(String filename) {
        return Paths.get(HOME_PATH, filename).toString();
    }
}
