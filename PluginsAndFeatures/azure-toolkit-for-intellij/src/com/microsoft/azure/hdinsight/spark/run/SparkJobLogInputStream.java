/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Optional;

public class SparkJobLogInputStream extends InputStream {
    @NotNull
    private String logType;
    @Nullable
    private SparkBatchJob sparkBatchJob;
    @Nullable
    private String logUrl;

    private long offset = 0;
    @NotNull
    private byte[] buffer = new byte[0];
    private int bufferPos;

    public SparkJobLogInputStream(@NotNull String logType) {
        this.logType = logType;
    }

    public void attachJob(@NotNull SparkBatchJob sparkJob) throws IOException {
        this.sparkBatchJob = sparkJob;
        this.logUrl = sparkJob.getSparkJobDriverLogUrl(sparkJob.getConnectUri(), sparkJob.getBatchId());
    }

    private synchronized Optional<String> fetchLog(long logOffset, int fetchSize) {
        return getAttachedJob()
                .flatMap(job -> getLogUrl().map(url -> new AbstractMap.SimpleImmutableEntry<>(job, url)))
                .map(jobUrlPair -> {
                    SparkBatchJob job = jobUrlPair.getKey();

                    return JobUtils.getInformationFromYarnLogDom(
                            job.getSubmission().getCredentialsProvider(),
                            jobUrlPair.getValue(),
                            getLogType(),
                            logOffset,
                            fetchSize);
                })
                .filter(slice -> !slice.isEmpty());
    }

    public Optional<SparkBatchJob> getAttachedJob() {
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
                    .map(slice -> {
                        buffer = slice.getBytes();
                        bufferPos = 0;
                        offset += slice.length();

                        return buffer.length;
                    }).orElse(0);
        } else {
            return buffer.length - bufferPos;
        }
    }

    public Optional<String> getLogUrl() {
        return Optional.ofNullable(logUrl);
    }

    @NotNull
    public String getLogType() {
        return logType;
    }
}
