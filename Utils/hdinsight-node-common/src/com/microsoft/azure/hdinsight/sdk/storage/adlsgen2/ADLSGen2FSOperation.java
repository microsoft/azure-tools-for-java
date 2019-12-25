/*
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

package com.microsoft.azure.hdinsight.sdk.storage.adlsgen2;

import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.RemoteFile;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.api.GetRemoteFilesResponse;
import com.microsoft.azure.hdinsight.sdk.storage.StoragePathInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import rx.Observable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADLSGen2FSOperation {
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

    public Observable<Boolean> createDir(String dirpath) {
        HttpPut req = new HttpPut(dirpath);
        return http.executeReqAndCheckStatus(req, 201, this.createDirReqParams)
                .map(ignore -> true);
    }

    public Observable<Boolean> createFile(String filePath) {
        HttpPut req = new HttpPut(filePath);
        return http.executeReqAndCheckStatus(req, 201, this.createFileReqParams)
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

        return http.get(rootPath, listReqBuilder.setDirectory(relativePath).build(), null, GetRemoteFilesResponse.class)
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
            req.setEntity(entity);
            http.setContentType("application/octet-stream");

            return http.executeReqAndCheckStatus(req, 202, this.appendReqParams)
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

        return http.executeReqAndCheckStatus(req, 200, flushReqParams)
                .map(ignore -> true);
    }

    private static Matcher getPatternMatcherWithValidation(String uri, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(uri);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("URI %s doesn't match with pattern %s.", uri, regex));
        }
        return matcher;
    }

    //convert https://accountname.dfs.core.windows.net/filesystem/subPath to abfs://filesystem@accountname.dfs.core.windows.net/subPath
    public static String convertToGen2Uri(URI root) {
        Matcher matcher = getPatternMatcherWithValidation(root.toString(), StoragePathInfo.AdlsGen2RestfulPathPattern);
        return String.format("abfs://%s@%s.dfs.core.windows.net%s",
                matcher.group("fileSystem"),
                matcher.group("accountName"),
                matcher.group("subPath"));
    }

    //convert abfs://filesystem@accountname.dfs.core.windows.net/subPath to https://accountname.dfs.core.windows.net/filesystem/subPath
    public static String convertToGen2Path(URI root) {
        Matcher matcher = getPatternMatcherWithValidation(root.toString(), StoragePathInfo.AdlsGen2PathPattern);
        return String.format("https://%s.dfs.core.windows.net/%s%s",
                matcher.group("accountName"),
                matcher.group("fileSystem"),
                matcher.group("subPath"));
    }

    // Get https://accountname.dfs.core.windows.net/filesystem from https://accountname.dfs.core.windows.net/filesystem/subPath
    public static String getGen2BaseRestfulPath(URI root) {
        Matcher matcher = getPatternMatcherWithValidation(root.toString(), StoragePathInfo.AdlsGen2RestfulPathPattern);
        return String.format("https://%s.dfs.core.windows.net/%s",
                matcher.group("accountName"),
                matcher.group("fileSystem"));
    }

    // get ab from abfs://filesystem@accountname.dfs.core.windows.net/ab
    // get / from abfs://filesystem@accountname.dfs.core.windows.net/
    public static String getDirectoryParam(URI root) {
        Matcher matcher = getPatternMatcherWithValidation(root.toString(), StoragePathInfo.AdlsGen2PathPattern);

        return matcher.group("subPath").length() == 0 || matcher.group("subPath").equals("/")
                ? "/"
                : matcher.group("subPath").substring(1);
    }
}
