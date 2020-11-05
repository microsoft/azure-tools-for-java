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

import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.ProcessInfo;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public abstract class FlightRecorderStarterBase {
    protected WebAppBase appService;
    protected FlightRecorderKuduClient client;

    public FlightRecorderStarterBase(@NotNull WebAppBase appService) {
        this.appService = appService;
        client = new FlightRecorderKuduClient(appService);
    }

    public abstract List<ProcessInfo> listProcess() throws IOException;

    abstract String getFinalJfrPath(String fileName);

    protected String constructJcmdCommand(int pid, int timeInSeconds, String fileName) {
        return String.format("jcmd %d JFR.start name=TimedRecording settings=profile duration=%ds filename=%s", pid,
                             timeInSeconds, getFinalJfrPath(fileName));
    }

    public abstract CommandOutput startFlightRecorder(int pid, int timeInSeconds, String fileName) throws IOException;

    public byte[] downloadJFRFile(String fileName) {
        return client.getFileContent(getFinalJfrPath(fileName));
    }
}
