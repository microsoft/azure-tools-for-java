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

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.UriUtil;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpStatus;
import rx.Observable;
import rx.Observer;
import rx.exceptions.Exceptions;

import java.io.File;
import java.net.URI;
import java.util.AbstractMap;

public class ADLSGen2Deploy implements Deployable, ILogger {
    @NotNull
    public HttpObservable http;

    @NotNull
    public String destinationRootPath;

    public ADLSGen2Deploy(@NotNull HttpObservable http, @NotNull String destinationRootPath) {
        this.destinationRootPath = destinationRootPath;
        this.http = http;
    }

    private URI getUploadDir() {
        return URI.create(destinationRootPath)
                .resolve(JobUtils.getFormatPathByDate() + "/");
    }

    @Override
    public Observable<String> deploy(File src,
                                     Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> logSubject) {
        // four steps to upload via adls gen2 rest api
        // 1.put request to create new dir
        // 2.put request to create new file(artifact) which is empty
        // 3.patch request to append data to file
        // 4.patch request to flush data to file

        URI destURI = getUploadDir();

        //remove request / end otherwise invalid url response
        String destStr = destURI.toString();
        String dirPath = destStr.endsWith("/") ? destStr.substring(0, destStr.length() - 1) : destStr;
        String filePath = String.format("%s/%s", dirPath, src.getName());

        ADLSGen2FSOperation op = new ADLSGen2FSOperation(this.http);
        return op.createDir(dirPath)
                .onErrorReturn(err -> {
                    if (err.getMessage()!= null && (err.getMessage().contains(String.valueOf(HttpStatus.SC_FORBIDDEN))
                            || err.getMessage().contains(String.valueOf(HttpStatus.SC_NOT_FOUND)))) {
                        // Sample destinationRootPath: https://accountName.dfs.core.windows.net/fsName/SparkSubmission/
                        String fileSystemRootPath = UriUtil.normalizeWithSlashEnding(URI.create(destinationRootPath)).resolve("../").toString();
                        String errorMessage = String.format("Failed to create folder %s when uploading Spark application artifacts with error: %s. %s",
                                dirPath, err.getMessage(), getForbiddenErrorHints(fileSystemRootPath));
                        throw new IllegalArgumentException(errorMessage);
                    } else {
                        throw Exceptions.propagate(err);
                    }
                })
                .doOnNext(ignore -> log().info(String.format("Create filesystem %s successfully.", dirPath)))
                .flatMap(ignore -> op.createFile(filePath))
                .flatMap(ignore -> op.uploadData(filePath, src))
                .doOnNext(ignore -> log().info(String.format("Append data to file %s successfully.", filePath)))
                .map(ignored -> AbfsUri.parse(filePath).getUri().toString());
    }

    public static String getForbiddenErrorHints(String fileSystemRootPath) {
        String signInUserEmail = AuthMethodManager.getInstance().getAuthMethodDetails().getAccountEmail();
        return new StringBuilder(" Please verify if\n")
                .append("1. The ADLS Gen2 root path matches with the access key if you enter the credential in the configuration.\n")
                .append("2. The signed in user " + signInUserEmail + " has Storage Blob Data Contributor or Storage Blob Data Owner role over the storage path " + fileSystemRootPath + ".\n")
                .append("   If the role is recently granted, please wait a while and try again later.\n")
                .append("   Find more details at https://docs.microsoft.com/en-us/azure/storage/common/storage-access-blobs-queues-portal#azure-ad-account")
                .toString();
    }
}