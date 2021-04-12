/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.Calendar;

public class BlobContainer implements StorageServiceTreeItem {
    private boolean loading;
    private String name;
    private String uri;
    private String eTag;
    private Calendar lastModified;
    private String publicReadAccessType;

    public BlobContainer(@NotNull String name,
                         @NotNull String uri,
                         @NotNull String eTag,
                         @NotNull Calendar lastModified,
                         @NotNull String publicReadAccessType) {
        this.name = name;
        this.uri = uri;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.publicReadAccessType = publicReadAccessType;
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
    public String getETag() {
        return eTag;
    }

    public void setETag(@NotNull String eTag) {
        this.eTag = eTag;
    }

    @NotNull
    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(@NotNull Calendar lastModified) {
        this.lastModified = lastModified;
    }

    @NotNull
    public String getPublicReadAccessType() {
        return publicReadAccessType;
    }

    public void setPublicReadAccessType(@NotNull String publicReadAccessType) {
        this.publicReadAccessType = publicReadAccessType;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}
