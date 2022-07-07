/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.UriUtil;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpStatus;
import rx.Observable;
import rx.Observer;
import rx.exceptions.Exceptions;

import java.io.File;
import java.net.URI;

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
    public Observable<String> deploy(File src, Observer<SparkLogLine> logSubject) {
        // four steps to upload via adls gen2 rest api
        // 1.put request to create new dir
        // 2.put request to create new file(artifact) which is empty
        // 3.patch request to append data to file
        // 4.patch request to flush data to file

        final URI destURI = getUploadDir();

        //remove request / end otherwise invalid url response
        final String destStr = destURI.toString();
        final String dirPath = destStr.endsWith("/") ? destStr.substring(0, destStr.length() - 1) : destStr;
        final String filePath = String.format("%s/%s", dirPath, src.getName());

        final ADLSGen2FSOperation op = new ADLSGen2FSOperation(this.http);
        return op.createDir(dirPath, "0755")
                 .onErrorReturn(err -> {
                     if (err.getMessage() != null && (err.getMessage().contains(String.valueOf(HttpStatus.SC_FORBIDDEN))
                             || err.getMessage().contains(String.valueOf(HttpStatus.SC_NOT_FOUND)))) {
                         // Sample destinationRootPath: https://accountName.dfs.core.windows.net/fsName/SparkSubmission/
                         String fileSystemRootPath = UriUtil.normalizeWithSlashEnding(URI.create(destinationRootPath))
                                                            .resolve("../")
                                                            .toString();
                         String errorMessage = String.format(
                                 "Failed to create folder %s when uploading Spark application artifacts with error: %s. %s",
                                 dirPath,
                                 err.getMessage(),
                                 getForbiddenErrorHints(fileSystemRootPath));
                         throw new IllegalArgumentException(errorMessage);
                     } else {
                         throw Exceptions.propagate(err);
                     }
                 })
                 .doOnNext(ignore -> log().info(String.format("Create filesystem %s successfully.", dirPath)))
                 .flatMap(ignore -> op.createFile(filePath, "0755"))
                 .flatMap(ignore -> op.uploadData(filePath, src))
                 .doOnNext(ignore -> log().info(String.format("Append data to file %s successfully.", filePath)))
                 .map(ignored -> AbfsUri.parse(filePath).getUri().toString());
    }

    public static String getForbiddenErrorHints(String fileSystemRootPath) {
        final String signInUserEmail = Azure.az(AzureAccount.class).account().getUsername();
        return " Please verify if\n"
                + "1. The ADLS Gen2 root path matches with the access key if you enter the credential in the configuration.\n"
                + "2. The signed in user "
                + signInUserEmail
                + " has Storage Blob Data Contributor or Storage Blob Data Owner role over the storage path "
                + fileSystemRootPath
                + ".\n"
                + "   If the role is recently granted, please wait a while and try again later.\n"
                + "   Find more details at https://docs.microsoft.com/en-us/azure/storage/common/storage-access-blobs-queues-portal#azure-ad-account";
    }
}
