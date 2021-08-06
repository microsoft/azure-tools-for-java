/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.jfr;

import com.azure.core.util.FluxUtil;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public abstract class FlightRecorderStarterBase {
    protected IAppService appService;

    public FlightRecorderStarterBase(@NotNull IAppService appService) {
        this.appService = appService;
    }

    public abstract List<ProcessInfo> listProcess() throws IOException;

    abstract String getFinalJfrPath(String fileName);

    protected String constructJcmdCommand(int pid, int timeInSeconds, String fileName) {
        return String.format("jcmd %d JFR.start name=TimedRecording settings=profile duration=%ds filename=%s", pid,
                             timeInSeconds, getFinalJfrPath(fileName));
    }

    public abstract CommandOutput startFlightRecorder(int pid, int timeInSeconds, String fileName) throws IOException;

    public byte[] downloadJFRFile(String fileName) {
        return FluxUtil.collectBytesInByteBufferStream(appService.getFileContent(getFinalJfrPath(fileName))).blockOptional()
                .orElseThrow(() -> new AzureToolkitRuntimeException(String.format("Failed to download JFR file %s from %s", fileName, appService.name())));
    }
}
