/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class ApplicationMasterLogs {
    private String stderr;
    private String stdout;
    private String directoryInfo;

    public ApplicationMasterLogs(@NotNull String standout, @NotNull String standerr, @NotNull String directoryInfo) {
        this.stdout = standout;
        this.stderr = standerr;
        this.directoryInfo = directoryInfo;
    }

    public ApplicationMasterLogs() {

    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getDirectoryInfo() {
        return directoryInfo;
    }

    public void setDirectoryInfo(String directoryInfo) {
        this.directoryInfo = directoryInfo;
    }
}
