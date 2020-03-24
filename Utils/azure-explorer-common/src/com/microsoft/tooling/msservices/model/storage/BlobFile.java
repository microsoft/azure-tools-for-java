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

public class BlobFile implements ServiceTreeItem, BlobItem {
    private boolean loading;
    private String name;
    private String uri;
    private String containerName;
    private String path;
    private String type;
    private String cacheControlHeader;
    private String contentEncoding;
    private String contentLanguage;
    private String contentType;
    private String contentMD5Header;
    private String eTag;
    private Calendar lastModified;
    private long size;

    public BlobFile(@NotNull String name,
                    @NotNull String uri,
                    @NotNull String containerName,
                    @NotNull String path,
                    @NotNull String type,
                    @NotNull String cacheControlHeader,
                    @NotNull String contentEncoding,
                    @NotNull String contentLanguage,
                    @NotNull String contentType,
                    @NotNull String contentMD5Header,
                    @NotNull String eTag,
                    @NotNull Calendar lastModified,
                    @NotNull long size) {
        this.name = name;
        this.uri = uri;
        this.containerName = containerName;
        this.path = path;
        this.type = type;
        this.cacheControlHeader = cacheControlHeader;
        this.contentEncoding = contentEncoding;
        this.contentLanguage = contentLanguage;
        this.contentType = contentType;
        this.contentMD5Header = contentMD5Header;
        this.eTag = eTag;
        this.lastModified = lastModified;
        this.size = size;
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
        return BlobItemType.BlobFile;
    }

    @NotNull
    public String getType() {
        return type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

    @NotNull
    public String getCacheControlHeader() {
        return cacheControlHeader;
    }

    public void setCacheControlHeader(@NotNull String cacheControlHeader) {
        this.cacheControlHeader = cacheControlHeader;
    }

    @NotNull
    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(@NotNull String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    @NotNull
    public String getContentLanguage() {
        return contentLanguage;
    }

    public void setContentLanguage(@NotNull String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    @NotNull
    public String getContentType() {
        return contentType;
    }

    public void setContentType(@NotNull String contentType) {
        this.contentType = contentType;
    }

    @NotNull
    public String getContentMD5Header() {
        return contentMD5Header;
    }

    public void setContentMD5Header(@NotNull String contentMD5Header) {
        this.contentMD5Header = contentMD5Header;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}
