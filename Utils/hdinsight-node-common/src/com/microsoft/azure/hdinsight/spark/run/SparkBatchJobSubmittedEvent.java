/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azure.hdinsight.spark.common.ISparkBatchJob;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob;
import org.jetbrains.annotations.NotNull;


public class SparkBatchJobSubmittedEvent implements SparkBatchJobSubmissionEvent {
    @NotNull
    private ISparkBatchJob job;

    public SparkBatchJobSubmittedEvent(@NotNull ISparkBatchJob job) {
        this.job = job;
    }

    public ISparkBatchJob getJob() {
        return job;
    }
}
