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

package com.microsoft.azure.hdinsight.sdk.storage.adlsgen2;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.RemoteFile;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.api.GetRemoteFilesResponse;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHeader;
import rx.Observable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class ADLSGen2FSOperation {
    public static final String DEFAULT_UMASK = "0000";
    public static final String PERMISSIONS_HEADER = "x-ms-permissions";
    public static final String UMASK_HEADER = "x-ms-umask";

    private HttpObservable http;

    @NotNull
    private List<NameValuePair> createDirReqParams;

    @NotNull
    private List<NameValuePair> createFileReqParams;

    @NotNull
    private List<NameValuePair> appendReqParams;

    @NotNull
    private ADLSGen2ParamsBuilder listReqBuilder;

    @NotNull
    private ADLSGen2ParamsBuilder flushReqParamsBuilder;

    public ADLSGen2FSOperation(@NotNull HttpObservable http) {
        this.http = http;
        this.createDirReqParams = new ADLSGen2ParamsBuilder()
                .setResource("directory")
                .build();

        this.createFileReqParams = new ADLSGen2ParamsBuilder()
                .setResource("file")
                .build();

        this.appendReqParams = new ADLSGen2ParamsBuilder()
                .setAction("append")
                .setPosition(0)
                .build();

        this.flushReqParamsBuilder = new ADLSGen2ParamsBuilder()
                .setAction("flush");
    }

    public Observable<Boolean> createDir(String dirPath) {
        return createDir(dirPath, null);
    }

    public Observable<Boolean> createDir(String dirPath, String permission) {
        return createDir(dirPath, permission, DEFAULT_UMASK);
    }

    public Observable<Boolean> createDir(String dirPath, @Nullable String permission, @Nullable String uMask) {
        HttpPut req = new HttpPut(dirPath);
        // We will filter out these headers if OAuth is used as authorization method.
        // Check class ADLSGen2OAuthHttpObservable for more details
        final List<Header> headers = permission != null && uMask != null
                                     ? ImmutableList.of(new BasicHeader(PERMISSIONS_HEADER, permission),
                                                        new BasicHeader(UMASK_HEADER, uMask))
                                     : null;
        return http.executeReqAndCheckStatus(req, null, this.createDirReqParams, headers, 201)
                   .map(ignore -> true);
    }

    public Observable<Boolean> createFile(String filePath) {
        return createFile(filePath, null);
    }

    public Observable<Boolean> createFile(String filePath, String permission) {
        return createFile(filePath, permission, DEFAULT_UMASK);
    }

    public Observable<Boolean> createFile(String filePath, @Nullable String permission, @Nullable String uMask) {
        HttpPut req = new HttpPut(filePath);
        // We will filter out these headers if OAuth is used as authorization method.
        // Check class ADLSGen2OAuthHttpObservable for more details
        final List<Header> headers = permission != null && uMask != null
                                     ? ImmutableList.of(new BasicHeader(PERMISSIONS_HEADER, permission),
                                                        new BasicHeader(UMASK_HEADER, uMask))
                                     : null;
        return http.executeReqAndCheckStatus(req, null, this.createFileReqParams, headers, 201)
                .map(ignore -> true);
    }

    public Observable<Boolean> uploadData(String destFilePath, File src) {
        return appendData(destFilePath, src)
                .flatMap(len -> flushData(destFilePath, len));
    }

    public Observable<RemoteFile> list(String rootPath, String relativePath) {
        this.listReqBuilder = new ADLSGen2ParamsBuilder()
                .enableRecursive(false)
                .setResource("filesystem");

        return http.get(
                StringUtils.stripEnd(rootPath, "/"),
                listReqBuilder.setDirectory(relativePath).build(),
                null,
                GetRemoteFilesResponse.class)
                .flatMap(pathList -> Observable.from(pathList.getRemoteFiles()));
    }

    private Observable<Long> appendData(String filePath, File src) {
        try {
            InputStreamEntity reqEntity = new InputStreamEntity(
                    new FileInputStream(src),
                    -1,
                    ContentType.APPLICATION_OCTET_STREAM);
            BufferedHttpEntity entity = new BufferedHttpEntity(reqEntity);
            long len = entity.getContentLength();

            HttpPatch req = new HttpPatch(filePath);
            http.setContentType("application/octet-stream");

            return http.executeReqAndCheckStatus(req, entity, this.appendReqParams, null, 202)
                    .map(ignore -> len);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(new IllegalArgumentException("Can not find the aritifact"));
        } catch (IOException e) {
            throw new RuntimeException(new IllegalArgumentException("Can not read the aritfact"));
        }
    }

    private Observable<Boolean> flushData(String filePath, long flushLen) {
        HttpPatch req = new HttpPatch(filePath);
        List<NameValuePair> flushReqParams = this.flushReqParamsBuilder.setPosition(flushLen).build();
        http.setContentType("application/json");

        return http.executeReqAndCheckStatus(req, null, flushReqParams, null, 200)
                .map(ignore -> true);
    }
}
