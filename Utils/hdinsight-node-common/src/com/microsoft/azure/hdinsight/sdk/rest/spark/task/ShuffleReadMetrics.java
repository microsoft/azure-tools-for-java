/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShuffleReadMetrics {
    private long recordsRead;

    private long remoteBytesRead;

    private long fetchWaitTime;

    private long remoteBlocksFetched;

    private long localBlocksFetched;

    private long localBytesRead;

    public long getRecordsRead() {
        return recordsRead;
    }

    public void setRecordsRead(long recordsRead) {
        this.recordsRead = recordsRead;
    }

    public long getRemoteBytesRead() {
        return remoteBytesRead;
    }

    public void setRemoteBytesRead(long remoteBytesRead) {
        this.remoteBytesRead = remoteBytesRead;
    }

    public long getFetchWaitTime() {
        return fetchWaitTime;
    }

    public void setFetchWaitTime(long fetchWaitTime) {
        this.fetchWaitTime = fetchWaitTime;
    }

    public long getRemoteBlocksFetched() {
        return remoteBlocksFetched;
    }

    public void setRemoteBlocksFetched(long remoteBlocksFetched) {
        this.remoteBlocksFetched = remoteBlocksFetched;
    }

    public long getLocalBlocksFetched() {
        return localBlocksFetched;
    }

    public void setLocalBlocksFetched(long localBlocksFetched) {
        this.localBlocksFetched = localBlocksFetched;
    }

    public long getLocalBytesRead() {
        return localBytesRead;
    }

    public void setLocalBytesRead(long localBytesRead) {
        this.localBytesRead = localBytesRead;
    }
}
