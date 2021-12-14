/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.launch.local;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.StandardVMRunner;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class AzureFunctionVMRunner extends StandardVMRunner {
    private final String funcPath;
    private final String stagingFolder;

    /**
     * Constructor
     *
     * @param vmInstance the VM
     */
    public AzureFunctionVMRunner(IVMInstall vmInstance, String funcPath, String stagingFolder) {
        super(vmInstance);
        this.funcPath = funcPath;
        this.stagingFolder = stagingFolder;

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

    protected String[] validateCommandLine(ILaunchConfiguration configuration, String[] cmdLine) {
        if (cmdLine.length > 0 && Objects.equals(new File(cmdLine[0]), new File(funcPath))) {
            // change the duplicate command line to `func host start`
            return new String[] {
                funcPath,
                "host",
                "start",
                "--verbose"
            };
        }
        return cmdLine;
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
