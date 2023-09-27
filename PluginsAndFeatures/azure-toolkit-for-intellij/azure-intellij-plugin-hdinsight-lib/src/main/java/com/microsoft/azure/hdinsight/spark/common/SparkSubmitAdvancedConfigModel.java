/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;

@Tag("ssh_cert")
public class SparkSubmitAdvancedConfigModel extends SparkBatchRemoteDebugJobSshAuth {
    private static final String SERVICE_NAME_PREFIX = "Azure IntelliJ Plugin Spark Debug SSH - ";
    @Transient
    @Nullable
    private String clusterName;

    // This variable has no real meaning and is intended to resolve the problem of
    //  the apply button not being enabled when a user changes a password
    public String draft = StringUtils.EMPTY;

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
        this.draft = encode(password);
    }

    @Transient
    public String encode(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(str.getBytes("utf-8"));
            byte[] digest = md.digest();
            return String.valueOf(Hex.encodeHex(digest));
        } catch (Exception e) {
            return null;
        }
    }

    @Transient
    @Nullable
    public String getSshPassword() {
        return super.getSshPassword();
    }

    @Transient
    public boolean isUIExpanded() {
        return isUIExpanded;
    }

    @Transient
    public void setUIExpanded(boolean UIExpanded) {
        isUIExpanded = UIExpanded;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparkSubmitAdvancedConfigModel)) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        SparkSubmitAdvancedConfigModel other = (SparkSubmitAdvancedConfigModel) obj;

        return this.enableRemoteDebug == other.enableRemoteDebug && this.getSshAuthType() == other.getSshAuthType() &&
                (this.getSshAuthType() == SSHAuthType.UseKeyFile ?
                        (FileUtil.compareFiles(this.getSshKeyFile(), other.getSshKeyFile()) == 0) :
                        (StringUtil.compare(this.getSshPassword(), other.getSshPassword(), false) == 0));
    }
}
