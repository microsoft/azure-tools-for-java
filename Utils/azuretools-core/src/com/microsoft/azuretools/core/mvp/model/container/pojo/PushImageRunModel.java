/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.container.pojo;

import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;

public class PushImageRunModel {
    private PrivateRegistryImageSetting privateRegistryImageSetting = new PrivateRegistryImageSetting();
    private String targetPath;
    private String targetName;
    private String dockerFilePath;

    public PrivateRegistryImageSetting getPrivateRegistryImageSetting() {
        return privateRegistryImageSetting;
    }

    public void setPrivateRegistryImageSetting(PrivateRegistryImageSetting privateRegistryImageSetting) {
        this.privateRegistryImageSetting = privateRegistryImageSetting;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getDockerFilePath() {
        return dockerFilePath;
    }

    public void setDockerFilePath(String dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
    }
}
