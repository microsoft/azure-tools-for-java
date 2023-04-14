/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.container.pojo;

import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushImageRunModel {
    private PrivateRegistryImageSetting privateRegistryImageSetting = new PrivateRegistryImageSetting();
    private DockerHostRunSetting dockerHostRunSetting = new DockerHostRunSetting();
    private String containerRegistryId;
    private String targetPath;
    private String targetName;
    private String dockerFilePath;
}
