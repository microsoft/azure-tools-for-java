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
package com.microsoft.azure.hdinsight.sdk.rest.spark.task;

public class ShuffleReadMetrics {
    private long recordsRead;

    private long remoteBytesRead;

    private long fetchWaitTime;

    private long remoteBlocksFetched;

    private long localBlocksFetched;

    private long localBytesRead;

    public long getRecordsRead ()
    {
        return recordsRead;
    }

    public void setRecordsRead (long recordsRead)
    {
        this.recordsRead = recordsRead;
    }

    public long getRemoteBytesRead ()
    {
        return remoteBytesRead;
    }

    public void setRemoteBytesRead (long remoteBytesRead)
    {
        this.remoteBytesRead = remoteBytesRead;
    }

    public long getFetchWaitTime ()
    {
        return fetchWaitTime;
    }

    public void setFetchWaitTime (long fetchWaitTime)
    {
        this.fetchWaitTime = fetchWaitTime;
    }

    public long getRemoteBlocksFetched ()
    {
        return remoteBlocksFetched;
    }

    public void setRemoteBlocksFetched (long remoteBlocksFetched)
    {
        this.remoteBlocksFetched = remoteBlocksFetched;
    }

    public long getLocalBlocksFetched ()
    {
        return localBlocksFetched;
    }

    public void setLocalBlocksFetched (long localBlocksFetched)
    {
        this.localBlocksFetched = localBlocksFetched;
    }

    public long getLocalBytesRead ()
    {
        return localBytesRead;
    }

    public void setLocalBytesRead (long localBytesRead)
    {
        this.localBytesRead = localBytesRead;
    }

}
