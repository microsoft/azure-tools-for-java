/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.properties;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SpringCloudAppViewModel {
    private String subscriptionName;
    private String resourceGroup;
    private String region;
    private String clusterName;
    private String appName;
    private String runtimeVersion;

    private Integer cpu;
    private Integer memoryInGB;
    private String jvmOptions;

    private List<SpringCloudAppInstanceViewModel> instance;

    private String javaVersion;
    private boolean enablePersistentStorage;
    private boolean enablePublicUrl;
    private Map<String, String> environment;

    // Read only
    private String publicUrl;
    private String testUrl;

    private String status;

    private Integer upInstanceCount;
    private Integer downInstanceCount;

    private Integer totalStorageInGB;
    private Integer usedStorageInGB;
    private String persistentMountPath;

    private boolean canStart;
    private boolean canStop;
    private boolean canReStart;
}
