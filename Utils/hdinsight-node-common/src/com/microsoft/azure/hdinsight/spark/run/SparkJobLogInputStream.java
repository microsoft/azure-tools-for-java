/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azure.hdinsight.spark.common.ISparkBatchJob;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;

import static java.lang.Thread.sleep;

public class SparkJobLogInputStream extends InputStream {
    @NotNull
    private String logType;
    @Nullable
    private ISparkBatchJob sparkBatchJob;

    private long offset = 0;
    @NotNull
    private byte[] buffer = new byte[0];
    private int bufferPos;

    public SparkJobLogInputStream(@NotNull String logType) {
        this.logType = logType;
    }

    public ISparkBatchJob attachJob(@NotNull ISparkBatchJob sparkJob) {
        setSparkBatchJob(sparkJob);

        return sparkJob;
    }

    protected synchronized Optional<SimpleImmutableEntry<String, Long>> fetchLog(long logOffset, int fetchSize) {
        return getAttachedJob()
                .map(job -> job.getDriverLog(getLogType(), logOffset, fetchSize)
                               .toBlocking().singleOrDefault(null));
    }

    void setSparkBatchJob(@Nullable ISparkBatchJob sparkBatchJob) {
        this.sparkBatchJob = sparkBatchJob;
    }

    public Optional<ISparkBatchJob> getAttachedJob() {
        return Optional.ofNullable(sparkBatchJob);
    }

    @Override
    public int read() throws IOException {
        if (bufferPos >= buffer.length) {
            throw new IOException("Beyond the buffer end, needs a new log fetch");
        }

        return buffer[bufferPos++];
    }

    @Override
    public int available() throws IOException {
        if (bufferPos >= buffer.length) {
            return fetchLog(offset, -1)
                    .map(sliceOffsetPair -> {
                        buffer = sliceOffsetPair.getKey().getBytes();
                        bufferPos = 0;
                        offset = sliceOffsetPair.getValue() + sliceOffsetPair.getKey().length();

                        return buffer.length;
                    }).orElseGet(() -> {
                        try {
                            sleep(3000);
                        } catch (InterruptedException ignore) { }

                        return 0;
                    });
        } else {
            return buffer.length - bufferPos;
        }
    }

    @NotNull
    public String getLogType() {
        return logType;
    }
}
