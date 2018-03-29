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

import com.intellij.credentialStore.*;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.util.InvalidDataException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class SparkSubmitAdvancedConfigModel extends SparkBatchRemoteDebugJobSshAuth {
    public static final String SUBMISSION_CONTENT_SSH_CERT= "ssh_cert";
    private static final String SUBMISSION_ATTRIBUTE_SSH_CERT_AUTHTYPE_NAME= "auth_type";
    private static final String SUBMISSION_ATTRIBUTE_SSH_CERT_USER_NAME= "user";
    private static final String SUBMISSION_ATTRIBUTE_SSH_CERT_PRIVATE_KEYPATH_NAME= "private_key_path";
    private static final String SUBMISSION_ATTRIBUTE_SSH_CERT_STORE_ACCOUNT_NAME= "store_account";
    private static final String SERVICE_NAME_PREFIX = "Azure IntelliJ Plugin Spark Debug SSH - ";
    @Nullable
    private String clusterName;

    public boolean enableRemoteDebug = false;

    @Nullable
    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(@Nullable String clusterName) {
        this.clusterName = clusterName;
    }

    public URI getServiceURI() throws URISyntaxException {
        return new URI("ssh", sshUserName, getClusterName(), 22, "/", null, null);
    }

    public String getCredentialStoreAccount() {
        try {
            return SERVICE_NAME_PREFIX + getServiceURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    String.format("Wrong arguments: cluster(%s), user(%s)", getClusterName(), sshUserName), e);
        }
    }

    public Element exportToElement() {
        Element sshCertElement = new Element(SUBMISSION_CONTENT_SSH_CERT);
        sshCertElement.setAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_USER_NAME, this.sshUserName);
        sshCertElement.setAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_AUTHTYPE_NAME, this.sshAuthType.name());
        if (this.sshAuthType == SparkSubmitAdvancedConfigModel.SSHAuthType.UseKeyFile) {
            sshCertElement.setAttribute(
                    SUBMISSION_ATTRIBUTE_SSH_CERT_PRIVATE_KEYPATH_NAME,
                    Optional.ofNullable(this.sshKeyFile).map(File::toString).orElse(""));
        } else if (StringUtils.isNotBlank(sshPassword)){
            PasswordSafe.getInstance().setPassword(
                    new CredentialAttributes(getCredentialStoreAccount(), sshUserName), sshPassword);

            sshCertElement.setAttribute(
                    SUBMISSION_ATTRIBUTE_SSH_CERT_STORE_ACCOUNT_NAME,
                    getCredentialStoreAccount());
        }

        return sshCertElement;
    }

    public SparkSubmitAdvancedConfigModel factoryFromElement(@NotNull Element sshCertElem)
            throws InvalidDataException {
        this.enableRemoteDebug = true;

        Optional.ofNullable(sshCertElem.getAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_USER_NAME))
                .ifPresent(attribute -> sshUserName = attribute.getValue());
        Optional.ofNullable(sshCertElem.getAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_AUTHTYPE_NAME))
                .ifPresent(attribute -> sshAuthType =
                        SparkSubmitAdvancedConfigModel.SSHAuthType.valueOf(attribute.getValue()));
        Optional.ofNullable(sshCertElem.getAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_PRIVATE_KEYPATH_NAME))
                .ifPresent(attribute -> sshKeyFile = new File(attribute.getValue()));
        Optional.ofNullable(sshCertElem.getAttribute(SUBMISSION_ATTRIBUTE_SSH_CERT_STORE_ACCOUNT_NAME))
                .ifPresent(attribute -> sshPassword =
                        PasswordSafe.getInstance().getPassword(
                                new CredentialAttributes(getCredentialStoreAccount(), sshUserName)));

        return this;
    }
}
