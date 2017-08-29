/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.spark.common;

import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;

public class SparkSubmitAdvancedConfigModel {
    public static final String SUBMISSION_CONTENT_SSH_CERT= "ssh_cert";
    private static final String SUBMISSION_ATTRIBUTE_SSH_CERT_AUTHTYPE_NAME= "auth_type";
    private static final String SUBMISSION_ATTRIBUTE_SSH_CERT_USER_NAME= "user";
    private static final String SUBMISSION_ATTRIBUTE_SSH_CERT_PRIVATE_KEYPATH_NAME= "private_key_path";

    public boolean enableRemoteDebug = false;

    public String sshUserName = "sshuser";

    public SSHAuthType sshAuthType = SSHAuthType.UsePassword;
    public File sshKeyFile;
    public String sshPassword = "";

    public enum SSHAuthType {
        UsePassword,
        UseKeyFile
    }

    public void apply(SparkSubmitAdvancedConfigModel source) {
        this.enableRemoteDebug = source.enableRemoteDebug;
        this.sshUserName = source.sshUserName;
        this.sshAuthType = source.sshAuthType;
        this.sshKeyFile = source.sshKeyFile;
        this.sshPassword = source.sshPassword;
    }

    public Element exportToElement() {
        Element sshCertElement = new Element(SUBMISSION_CONTENT_SSH_CERT);
        sshCertElement.setAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_USER_NAME, this.sshUserName);
        sshCertElement.setAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_AUTHTYPE_NAME, this.sshAuthType.name());
        if (this.sshAuthType == SparkSubmitAdvancedConfigModel.SSHAuthType.UseKeyFile) {
            sshCertElement.setAttribute(
                    SUBMISSION_ATTRIBUTE_SSH_CERT_PRIVATE_KEYPATH_NAME,
                    Optional.ofNullable(this.sshKeyFile).map(File::toString).orElse(""));
        }

        return sshCertElement;
    }

    static public SparkSubmitAdvancedConfigModel factoryFromElement(@NotNull Element sshCertElem)
            throws InvalidDataException {
        SparkSubmitAdvancedConfigModel advConfigModel = new SparkSubmitAdvancedConfigModel();

        advConfigModel.enableRemoteDebug = true;

        Optional.ofNullable(sshCertElem.getAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_USER_NAME))
                .ifPresent(attribute -> advConfigModel.sshUserName = attribute.getValue());
        Optional.ofNullable(sshCertElem.getAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_AUTHTYPE_NAME))
                .ifPresent(attribute -> advConfigModel.sshAuthType =
                        SparkSubmitAdvancedConfigModel.SSHAuthType.valueOf(attribute.getValue()));
        Optional.ofNullable(sshCertElem.getAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_PRIVATE_KEYPATH_NAME))
                .ifPresent(attribute -> advConfigModel.sshKeyFile = new File(attribute.getValue()));

        return advConfigModel;
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
