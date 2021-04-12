/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.net.URI;

public class SparkBatchJobExecutorCreatedEvent implements SparkBatchJobSubmissionEvent {
    @NotNull
    private final URI hostUri;
    @NotNull
    private final String containerId;

    public SparkBatchJobExecutorCreatedEvent(@NotNull URI hostUri, @NotNull String containerId) {
        this.hostUri = hostUri;
        this.containerId = containerId;
    }

    @NotNull
    public URI getHostUri() {
        return hostUri;
    }

    @NotNull
    public String getContainerId() {
        return containerId;
    }
}
