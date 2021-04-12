/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;


public class BlobDirectory implements ServiceTreeItem, BlobItem {
    private boolean loading;
    private String name;
    private String uri;
    private String containerName;
    private String path;

    public BlobDirectory(@NotNull String name,
                         @NotNull String uri,
                         @NotNull String containerName,
                         @NotNull String path) {
        this.name = name;
        this.uri = uri;
        this.containerName = containerName;
        this.path = path;
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

    @NotNull
    @Override
    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(@NotNull String containerName) {
        this.containerName = containerName;
    }

    @NotNull
    @Override
    public String getPath() {
        return path;
    }

    public void setPath(@NotNull String path) {
        this.path = path;
    }

    @NotNull
    @Override
    public BlobItemType getItemType() {
        return BlobItemType.BlobDirectory;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}
