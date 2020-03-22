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
package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

import java.util.Calendar;

public class QueueMessage implements ServiceTreeItem {
    private boolean loading;
    private String id;
    private String queueName;
    private String content;
    private Calendar insertionTime;
    private Calendar expirationTime;
    private int dequeueCount;

    public QueueMessage(@NotNull String id,
                        @NotNull String queueName,
                        @NotNull String content,
                        @NotNull Calendar insertionTime,
                        @NotNull Calendar expirationTime,
                        int dequeueCount) {
        this.id = id;
        this.queueName = queueName;
        this.content = content;
        this.insertionTime = insertionTime;
        this.expirationTime = expirationTime;
        this.dequeueCount = dequeueCount;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    @NotNull
    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(@NotNull String queueName) {
        this.queueName = queueName;
    }

    @NotNull
    public String getContent() {
        return content;
    }

    public void setContent(@NotNull String content) {
        this.content = content;
    }

    @NotNull
    public Calendar getInsertionTime() {
        return insertionTime;
    }

    public void setInsertionTime(@NotNull Calendar insertionTime) {
        this.insertionTime = insertionTime;
    }

    @NotNull
    public Calendar getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(@NotNull Calendar expirationTime) {
        this.expirationTime = expirationTime;
    }

    public int getDequeueCount() {
        return dequeueCount;
    }

    public void setDequeueCount(int dequeueCount) {
        this.dequeueCount = dequeueCount;
    }

    @Override
    public String toString() {
        return id + (loading ? " (loading...)" : "");
    }
}