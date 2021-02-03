/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.storage;


import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class Queue implements StorageServiceTreeItem {
    private boolean loading;
    private String name;
    private String uri;
    private long approximateMessageCount;

    public Queue(@NotNull String name,
                 @NotNull String uri,
                 long approximateMessageCount) {
        this.name = name;
        this.uri = uri;
        this.approximateMessageCount = approximateMessageCount;
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
    public String getName() {
        return name;
    }

    @NotNull
    public String getUri() {
        return uri;
    }

    public void setUri(@NotNull String uri) {
        this.uri = uri;
    }

    public long getApproximateMessageCount() {
        return approximateMessageCount;
    }

    public void setApproximateMessageCount(long approximateMessageCount) {
        this.approximateMessageCount = approximateMessageCount;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}
