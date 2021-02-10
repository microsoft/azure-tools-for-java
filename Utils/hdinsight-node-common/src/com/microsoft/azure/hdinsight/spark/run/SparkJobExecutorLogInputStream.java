/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azure.hdinsight.spark.common.ISparkBatchJob;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;


class SparkJobExecutorLogInputStream extends SparkJobLogInputStream {
    private final String logUrl;

    public SparkJobExecutorLogInputStream(@NotNull String logType, @NotNull String logUrl) {
        super(logType);

        this.logUrl = logUrl;
    }

    @Override
    protected synchronized Optional<SimpleImmutableEntry<String, Long>> fetchLog(long logOffset, int fetchSize) {
        return getAttachedJob()
                .map(job -> job.getContainerLog(getLogUrl(), getLogType(), logOffset, fetchSize)
                        .toBlocking().singleOrDefault(null));
    }

    @Override
    public ISparkBatchJob attachJob(@NotNull ISparkBatchJob sparkJob) {
        setSparkBatchJob(sparkJob);

        return sparkJob;
    }

    public String getLogUrl() {
        return logUrl;
    }
}
