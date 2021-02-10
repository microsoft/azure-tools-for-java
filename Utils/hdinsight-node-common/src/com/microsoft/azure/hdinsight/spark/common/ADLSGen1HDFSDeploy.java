/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

// for cluster with adls account to deploy using webhdfs storage account type
public class ADLSGen1HDFSDeploy extends WebHDFSDeploy {
    public ADLSGen1HDFSDeploy(IClusterDetail cluster, HttpObservable http, String destinationRootPath) {
        super(cluster, http, destinationRootPath);
    }

    @Override
    @Nullable
    public String getArtifactUploadedPath(String rootPath) throws URISyntaxException {
        // convert https://xx/webhdfs/v1/hdi-root/SparkSubmission/artifact.jar to adl://xx/hdi-root/SparkSubmission/artifact.jar
        URIBuilder builder = new URIBuilder(rootPath.replace("/webhdfs/v1", ""));
        builder.setScheme(cluster.getStorageAccount().getDefaultStorageSchema());
        return builder.build().toString();
    }
}
