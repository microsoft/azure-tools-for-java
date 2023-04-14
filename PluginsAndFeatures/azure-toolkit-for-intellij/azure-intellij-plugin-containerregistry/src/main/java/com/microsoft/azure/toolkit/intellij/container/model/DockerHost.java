/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.container.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class DockerHost {
    public static DockerHost DEFAULT_WINDOWS_HOST = new DockerHost("npipe:////./pipe/docker_engine", null);
    private String dockerHost;
    private String dockerCertPath;

    public boolean isTlsEnabled() {
        return StringUtils.isNotEmpty(dockerCertPath);
    }
}
