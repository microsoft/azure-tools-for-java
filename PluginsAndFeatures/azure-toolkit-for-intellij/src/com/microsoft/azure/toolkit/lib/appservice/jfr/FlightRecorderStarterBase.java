/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
