/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import rx.Observable;
import rx.Observer;

import java.io.File;

public interface Deployable {
    /**
     * Deploy the job artifact into cluster
     *
     * @param src        the artifact to deploy
     * @param logSubject the subject to help print logs during deploying
     * @return Observable: upload path
     * Observable Error: IOException;
     */
    Observable<String> deploy(File src, Observer<SparkLogLine> logSubject);
}
