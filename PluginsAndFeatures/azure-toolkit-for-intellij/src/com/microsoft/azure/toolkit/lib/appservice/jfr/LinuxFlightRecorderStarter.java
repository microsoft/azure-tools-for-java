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
import com.microsoft.azure.toolkit.lib.appservice.TunnelProxy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LinuxFlightRecorderStarter extends FlightRecorderStarterBase {
    private static final String HOME_PATH = "/home";
    private TunnelProxy proxy;

    public LinuxFlightRecorderStarter(final WebAppBase app) {
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
        return client.getFileContent(fileName);
    }
}
