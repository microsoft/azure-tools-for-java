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

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.ProcessInfo;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WindowFlightRecorderStarter extends FlightRecorderStarterBase {
    private static final String HOME_PATH = "d:/home";

    public WindowFlightRecorderStarter(final WebAppBase appService) {
        super(appService);
    }

    public List<ProcessInfo> listProcess() {
        return client.listProcess().map(processInfos -> Arrays.stream(processInfos)
                                                   .filter(processInfo -> StringUtils.equalsIgnoreCase(processInfo.getName(), "java"))
                                                   .collect(Collectors.toList())).toBlocking().first();
    }

    public CommandOutput startFlightRecorder(int pid, int timeInSeconds, String fileName) {
        return client.execute(constructJcmdCommand(pid, timeInSeconds, fileName), HOME_PATH)
                     .toBlocking()
                     .first();
    }

    @Override
    public String getFinalJfrPath(String filename) {
        return Paths.get(HOME_PATH, filename).toString();
    }
}
