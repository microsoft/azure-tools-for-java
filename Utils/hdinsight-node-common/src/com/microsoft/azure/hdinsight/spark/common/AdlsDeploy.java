/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.Observer;

import java.io.File;

// for cluster with adls gen1 account to deploy using ADLS storage account type
public class AdlsDeploy implements Deployable {
    @NotNull
    private final String adlsRootPath;
    @NotNull
    private final String accessToken;

    public AdlsDeploy(@NotNull String adlsRootPath,
                      @NotNull String accessToken) {
        this.adlsRootPath = adlsRootPath;
        this.accessToken = accessToken;
    }

    @NotNull
    @Override
    public Observable<String> deploy(File src, Observer<SparkLogLine> logSubject) {
        return JobUtils.deployArtifactToADLS(src.getAbsolutePath(), adlsRootPath, accessToken);
    }
}
