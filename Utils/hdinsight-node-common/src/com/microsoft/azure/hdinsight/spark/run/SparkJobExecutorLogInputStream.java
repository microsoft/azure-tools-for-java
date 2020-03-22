/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
