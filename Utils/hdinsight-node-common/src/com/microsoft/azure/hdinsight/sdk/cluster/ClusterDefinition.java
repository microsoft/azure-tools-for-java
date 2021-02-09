/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

public class ClusterDefinition {
    private String kind;
    private ComponentVersion componentVersion;

    public String getKind(){
        return kind;
    }

    public ComponentVersion getComponentVersion() {
        return componentVersion;
    }
}
