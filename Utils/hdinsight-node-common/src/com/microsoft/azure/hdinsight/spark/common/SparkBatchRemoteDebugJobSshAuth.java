/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
