/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.net.URI;

public class StoragePathInfo {
    public static final String BlobPathPattern = "^(wasb[s]?)://(.*)@(.*)$";
    public static final String AdlsPathPattern = "^adl://([^/.\\s]+\\.)+[^/.\\s]+(/[^/.\\s]+)*/?$";

    @NotNull
    public final URI path;

    @NotNull
    public final StorageAccountType storageType;

    public StoragePathInfo(@NotNull String path) {
        this.storageType = setStorageType(path);
        this.path = URI.create(path);
    }

    private StorageAccountType setStorageType(@NotNull String path) {
        if (path.matches(AbfsUri.AdlsGen2PathPattern) || path.matches(AbfsUri.AdlsGen2RestfulPathPattern)) {
            return StorageAccountType.ADLSGen2;
        }

        if (path.matches(BlobPathPattern)) {
            return StorageAccountType.BLOB;
        }

        if (path.matches(AdlsPathPattern)) {
            return StorageAccountType.ADLS;
        }

        throw new IllegalArgumentException("Cannot get valid storage type given storage root path " + path);
    }
}
