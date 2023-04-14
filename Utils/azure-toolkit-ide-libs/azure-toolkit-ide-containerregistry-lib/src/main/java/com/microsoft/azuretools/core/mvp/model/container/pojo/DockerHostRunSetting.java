/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.container.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class DockerHostRunSetting {
    private String dockerHost;
    private String dockerCertPath;
    private boolean tlsEnabled;

    private String imageName;
    private String tagName;
    private String targetPath;
    private String targetName;
    private String dockerFilePath;

    private Integer port;

}
