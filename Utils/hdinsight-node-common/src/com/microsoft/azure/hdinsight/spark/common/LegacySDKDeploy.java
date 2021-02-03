/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import rx.Observable;
import rx.Observer;

import java.io.File;

// for cluster with blob/adls gen1 account to deploy using default storage account type
// will be replaced by AdlsDeploy/ADLSGen1HDFSDeploy
public class LegacySDKDeploy implements Deployable, ILogger {
    private IHDIStorageAccount storageAccount;

    public LegacySDKDeploy(IHDIStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    @Override
    public Observable<String> deploy(File src, Observer<SparkLogLine> logSubject) {
        return JobUtils.deployArtifact(src.getAbsolutePath(), storageAccount, logSubject);
    }
}
