/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import java.net.URI;

public interface LivyCluster {
    String getLivyConnectionUrl();

    default String getLivyBatchUrl() {
        return URI.create(getLivyConnectionUrl()).resolve("batches").toString();
    }

    default String getLivySessionUrl() {
        return URI.create(getLivyConnectionUrl()).resolve("sessions").toString();
    }

}
