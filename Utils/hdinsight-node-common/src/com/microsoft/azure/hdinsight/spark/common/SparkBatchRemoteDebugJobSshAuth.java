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

package com.microsoft.azure.hdinsight.spark.common;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class SparkBatchRemoteDebugJobSshAuth {
    private String sshUserName = "sshuser";

    private SSHAuthType sshAuthType = SSHAuthType.UsePassword;
    private File sshKeyFile;
    private String sshPassword = "";

    public enum SSHAuthType {
        UsePassword,
        UseKeyFile
    }

    public boolean isValid() {
        return StringUtils.isNotEmpty(sshUserName) &&
                (sshAuthType == SSHAuthType.UsePassword ? StringUtils.isNotEmpty(sshPassword) :
                                                          (sshKeyFile != null && sshKeyFile.exists()));
    }

    public String getSshUserName() {
        return sshUserName;
    }

    public void setSshUserName(String sshUserName) {
        this.sshUserName = sshUserName;
    }

    public SSHAuthType getSshAuthType() {
        return sshAuthType;
    }

    public void setSshAuthType(SSHAuthType sshAuthType) {
        this.sshAuthType = sshAuthType;
    }

    public File getSshKeyFile() {
        return sshKeyFile;
    }

    public void setSshKeyFile(File sshKeyFile) {
        this.sshKeyFile = sshKeyFile;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public static class UnknownSSHAuthTypeException extends SparkJobException {

        public UnknownSSHAuthTypeException(String message) {
            super(message);
        }

        public UnknownSSHAuthTypeException(String message, int errorCode) {
            super(message, errorCode);
        }

        public UnknownSSHAuthTypeException(String message, String errorLog) {
            super(message, errorLog);
        }

        public UnknownSSHAuthTypeException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    public static class NotAdvancedConfig extends SparkJobException {

        public NotAdvancedConfig(String message) {
            super(message);
        }
    }
}
