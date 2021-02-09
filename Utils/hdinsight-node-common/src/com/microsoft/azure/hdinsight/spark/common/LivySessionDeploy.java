/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.Observer;

import java.io.File;
import java.util.AbstractMap;

public class LivySessionDeploy implements Deployable, ILogger {
    private final String clusterName;

    public LivySessionDeploy(@NotNull String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public Observable<String> deploy(File src, Observer<SparkLogLine> logSubject) {
        return JobUtils.deployArtifact(src.getAbsolutePath(), clusterName, logSubject)
                       .map(AbstractMap.SimpleImmutableEntry::getValue)
                       .toObservable();
    }
}
