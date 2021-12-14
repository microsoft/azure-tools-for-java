/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.launch.local;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.StandardVMDebugger;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AzureFunctionVMDebugger extends StandardVMDebugger {
    private final String funcPath;
    private final String stagingFolder;

    public AzureFunctionVMDebugger(IVMInstall vmInstance, String funcPath, String stagingFolder) {
        super(vmInstance);
        this.funcPath = funcPath;
        this.stagingFolder = stagingFolder;
    }

    protected String[] validateCommandLine(ILaunchConfiguration configuration, String[] cmdLine) {
        if (cmdLine.length > 0 && Objects.equals(new File(cmdLine[0]), new File(getFuncPath()))) {
            // change the duplicate command line to `func host start`
            String cmd = Arrays.stream(cmdLine).filter(args -> args.contains("-agentlib")).findFirst().orElse(null);
            if (cmd != null) {
                return new String[]{
                    getFuncPath(),
                    "host",
                    "start",
                    "--verbose",
                    "--language-worker",
                    "--",
                    cmd
                };
            } else {
                throw new AzureToolkitRuntimeException("Cannot find -agentlib in command line arguments:" + StringUtils.join(cmdLine, " "));
            }
        }
        return cmdLine;
    }

    @Override
    protected File getWorkingDir(VMRunnerConfiguration config) {
        // by pass the inner logic of StandardVMRunner
        return new File(stagingFolder);
    }

    @Override
    protected String constructProgramString(VMRunnerConfiguration config) {
        // by pass the inner logic of StandardVMRunner
        return funcPath;
    }

    @Override
    protected String[] ensureEncoding(ILaunch launch, String[] vmargs) {
        // by pass the inner logic of StandardVMRunner
        return vmargs;
    }

    @Override
    protected void addBootClassPathArguments(List<String> arguments, VMRunnerConfiguration config) {
        // by pass the inner logic of StandardVMRunner
    }

    protected String[] combineVmArgs(VMRunnerConfiguration configuration, IVMInstall vmInstall) {
        return new String[0];
    }

    public String getFuncPath() {
        return funcPath;
    }

    public String getStagingFolder() {
        return stagingFolder;
    }
}
