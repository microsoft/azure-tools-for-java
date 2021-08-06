/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.jfr;

import com.azure.core.util.FluxUtil;
import com.microsoft.azure.toolkit.lib.appservice.TunnelProxy;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LinuxFlightRecorderStarter extends FlightRecorderStarterBase {
    private static final String HOME_PATH = "/home";
    private TunnelProxy proxy;

    public LinuxFlightRecorderStarter(final IAppService app) {
        super(app);
        proxy = new TunnelProxy(app);
    }

    @Override
    public List<ProcessInfo> listProcess() throws IOException {
        String output = proxy.executeCommandViaSSH("ps -ef -o pid,comm,args |grep 'java\\|PID'");
        return parsePsCommandOutput(output);
    }

    private static List<ProcessInfo> parsePsCommandOutput(String output) {
        String []lines = output.split("\\r?\\n");
        String headLine = lines[0];
        List<ProcessInfo> res = new ArrayList<>();
        if (StringUtils.equalsIgnoreCase(headLine.replaceAll("\\s+", ""), "PIDCOMMANDCOMMAND")) {
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (StringUtils.isNotBlank(line)) {
                    String[] comps = StringUtils.split(line, null, 3);
                    if (comps.length != 3) {
                        continue;
                    }
                    if (NumberUtils.isParsable(comps[0]) && StringUtils.equalsIgnoreCase(comps[1], "java")) {
                        ProcessInfo pi = new ProcessInfo();
                        pi.setId(NumberUtils.toInt(comps[0]));
                        pi.setName(comps[2]);
                        res.add(pi);
                    }

                }
            }
        }
        return res;
    }

    @Override
    public CommandOutput startFlightRecorder(int pid, int timeInSeconds, String fileName) throws IOException {
        String command = constructJcmdCommand(pid, timeInSeconds, fileName);
        String output = proxy.executeCommandViaSSH(command);
        CommandOutput commandOutput = new CommandOutput();
        // check jcmd output
        if (StringUtils.contains(output, "Started recording")
                && StringUtils.contains(output, "The result will be "
                + "written to")
        ) {
            commandOutput.setOutput(output);
        } else {
            commandOutput.setExitCode(1);
            commandOutput.setError("Unexpected output:" + StringUtils.trim(output));
        }

        return commandOutput;
    }

    @Override
    public String getFinalJfrPath(String filename) {
        return Paths.get(HOME_PATH, filename).toString().replaceAll("\\\\", "/");
    }

    public byte[] downloadJFRFile(String fileName) {
        // linux kudu vfs api doesn't support absolute path
        return FluxUtil.collectBytesInByteBufferStream(appService.getFileContent(fileName)).blockOptional()
                .orElseThrow(() -> new AzureToolkitRuntimeException(String.format("Failed to download JFR file %s from %s", fileName, appService.name())));
    }
}
