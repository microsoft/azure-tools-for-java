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
import java.io.Reader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class SparkJobLogReader extends Reader {
    @NotNull
    private String logType;
    @Nullable
    private SparkBatchJob sparkBatchJob;
    @Nullable
    private String logUrl;

    private Boolean isReady = true;
    @NotNull
    private final Timer delayReadyTimer;

    public SparkJobLogReader(String logType) {
        this.logType = logType;

        this.delayReadyTimer = new Timer("Spark job log " + logType + " reader delay timer.");
    }

    public void attachJob(@NotNull SparkBatchJob sparkJob) throws IOException {
        this.sparkBatchJob = sparkJob;
        this.logUrl = sparkJob.getSparkJobDriverLogUrl(sparkJob.getConnectUri(), sparkJob.getBatchId());
    }

    public Optional<SparkBatchJob> getAttachedJob() {
        return Optional.ofNullable(sparkBatchJob);
    }

    @Override
    public int read(@NotNull char[] buf, int off, int len) throws IOException {
        return getAttachedJob()
                .flatMap(job -> getLogUrl().map(url -> new SimpleImmutableEntry<>(job, url)))
                .map(jobUrlPair -> {
                    SparkBatchJob job = jobUrlPair.getKey();

                    String log = JobUtils.getInformationFromYarnLogDom(
                                            job.getSubmission().getCredentialsProvider(),
                                            jobUrlPair.getValue(),
                                            getLogType(),
                                            off,
                                            len);

                    if (log.length() > 0) {
                        log.getChars(0, log.length(), buf, 0);
                    } else {
                        // stop a while before next ready
                        delayReady(500);
                    }

                    return log.length();
                })
                .orElse(0);
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean ready() throws IOException {
        return isReady;
    }

    public Optional<String> getLogUrl() {
        return Optional.ofNullable(logUrl);
    }

    @NotNull
    public String getLogType() {
        return logType;
    }

    private void delayReady(long delayMs) {
        isReady = false;

        delayReadyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                isReady = true;
            }
        }, delayMs);
    }

}
