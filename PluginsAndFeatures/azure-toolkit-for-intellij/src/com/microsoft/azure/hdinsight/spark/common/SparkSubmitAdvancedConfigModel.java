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

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

@Tag("ssh_cert")
public class SparkSubmitAdvancedConfigModel extends SparkBatchRemoteDebugJobSshAuth {
    private static final String SERVICE_NAME_PREFIX = "Azure IntelliJ Plugin Spark Debug SSH - ";
    @Transient
    @Nullable
    private String clusterName;

    @Transient
    private boolean isChecking = false;

    @Transient
    @NotNull
    private String checkingMessage = "";

    @Attribute("remote_debug_enabled")
    public boolean enableRemoteDebug = false;

    @Transient
    private boolean isUIExpanded = false;

    @Transient
    @Nullable
    public String getClusterName() {
        return clusterName;
    }

    @Transient
    public void setClusterName(@Nullable String clusterName) {
        this.clusterName = clusterName;
    }

    @Transient
    public URI getServiceURI() throws URISyntaxException {
        return new URI("ssh", getSshUserName(), getClusterName(), 22, "/", null, null);
    }

    @Transient
    public String getCredentialStoreAccount() {
        try {
            return SERVICE_NAME_PREFIX + getServiceURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    String.format("Wrong arguments: cluster(%s), user(%s)", getClusterName(), getSshUserName()), e);
        }
    }

    @Attribute("user")
    @Override
    public void setSshUserName(String sshUserName) {
        super.setSshUserName(sshUserName);
    }

    @Attribute("auth_type")
    @Override
    public void setSshAuthType(SSHAuthType authType) {
        super.setSshAuthType(authType);
    }

    @Attribute("private_key_path")
    public String getSshPrivateKeyPath() {
        return super.getSshKeyFile() == null ? "" : super.getSshKeyFile().toString();
    }

    @Attribute("private_key_path")
    public void setSshPrivateKeyPath(String path) {
        super.setSshKeyFile(new File(path));
    }

    @Transient
    @Override
    public File getSshKeyFile() {
        return super.getSshKeyFile();
    }

    @Transient
    public void setSshPassword(@Nullable String password) {
        super.setSshPassword(password);
    }

    @Transient
    @Nullable
    public String getSshPassword() {
        return super.getSshPassword();
    }

    @Transient
    public boolean isChecking() {
        return isChecking;
    }

    @Transient
    public void setChecking(boolean checking) {
        isChecking = checking;
    }

    @Transient
    @NotNull
    public String getCheckingMessage() {
        return checkingMessage;
    }

    @Transient
    public void setCheckingMessage(@NotNull String checkingMessage) {
        this.checkingMessage = checkingMessage;
    }

    @Transient
    public boolean isUIExpanded() {
        return isUIExpanded;
    }

    @Transient
    public void setUIExpanded(boolean UIExpanded) {
        isUIExpanded = UIExpanded;
    }
}
