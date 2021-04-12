/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
