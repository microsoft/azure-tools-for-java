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

import com.microsoft.azure.hdinsight.spark.common.SparkBatchDebugSession;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJob;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SparkBatchDebugJobJdbPortForwardedEvent extends SparkBatchJobSubmittedEvent {
    @NotNull
    private SparkBatchDebugSession debugSession;

    private final String remoteHost;
    private final int remoteJdbListenPort;
    private final int localJdbForwardedPort;
    private final boolean isDriver;

    public SparkBatchDebugJobJdbPortForwardedEvent(@NotNull SparkBatchRemoteDebugJob job,
                                                   @NotNull SparkBatchDebugSession debugSession,
                                                   String remoteHost,
                                                   int remoteJdbListenPort,
                                                   int localJdbForwardedPort,
                                                   boolean isDriver) {
        super(job);
        this.debugSession = debugSession;
        this.remoteHost = remoteHost;
        this.remoteJdbListenPort = remoteJdbListenPort;
        this.localJdbForwardedPort = localJdbForwardedPort;
        this.isDriver = isDriver;
    }

    @NotNull
    public SparkBatchDebugSession getDebugSession() {
        return debugSession;
    }

    public Optional<String> getRemoteHost() {
        return Optional.of(remoteHost)
                .filter(host -> !host.isEmpty());
    }

    public Optional<Integer> getRemoteJdbListenPort() {
        return Optional.of(remoteJdbListenPort)
                .filter(port -> port > 0);
    }

    public Optional<Integer> getLocalJdbForwardedPort() {
        return Optional.of(localJdbForwardedPort)
                .filter(port -> port > 0);
    }

    public boolean isDriver() {
        return isDriver;
    }

    @Override
    public SparkBatchRemoteDebugJob getJob() {
        return (SparkBatchRemoteDebugJob) super.getJob();
    }
}
