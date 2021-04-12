/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class Table implements StorageServiceTreeItem {
    private boolean loading;
    private String name;
    private String uri;

    public Table(@NotNull String name,
                 @NotNull String uri) {
        this.name = name;
        this.uri = uri;
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

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}
