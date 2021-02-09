/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import java.util.List;

class ClustersCollection {
    private List<ClusterRawInfo> value;

    public List<ClusterRawInfo> getValue() {
        return value;
    }

    public void setValue(List<ClusterRawInfo> value) {
        this.value = value;
    }
}
