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
