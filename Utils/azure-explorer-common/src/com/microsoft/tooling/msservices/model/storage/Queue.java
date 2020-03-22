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