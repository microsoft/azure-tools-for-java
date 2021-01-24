/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.runner;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class SpringCloudModel {
    private boolean isCreateNewApp;

    private String artifactIdentifier;
    // app
    private boolean isPublic;
    private String subscriptionId;
    private String clusterId;
    private String appName;
    private String runtimeVersion;
    // deployment
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private boolean enablePersistentStorage;
    private Map<String, String> environment;

    public boolean isCreateNewApp() {
        return isCreateNewApp;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public Map<String, String> getTelemetryProperties() {
        Map result = new HashMap();
        try {
            result.put("runtime", this.getRuntimeVersion());
            result.put("subscriptionId", this.getSubscriptionId());
            result.put("isCreateNew", String.valueOf(this.isCreateNewApp()));
        } catch (Exception e) {
            // swallow exception as telemetry should not break users operation
        }
        return result;
    }
}
