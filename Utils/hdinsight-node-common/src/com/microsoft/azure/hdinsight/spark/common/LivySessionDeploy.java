/**
 * Copyright (c) Microsoft Corporation
 * <p>
 * <p>
 * All rights reserved.
 * <p>
 * <p>
 * MIT License
 * <p>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p>
 * <p>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.Observer;

import java.io.File;
import java.net.URI;
import java.util.AbstractMap;

public class LivySessionDeploy implements Deployable, ILogger {
    private Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject;
    private String clusterName;

    public LivySessionDeploy(@NotNull String clusterName, @NotNull Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject) {
        this.ctrlSubject = ctrlSubject;
        this.clusterName = clusterName;
    }

    @Override
    public Observable<String> deploy(@NotNull File src) {
        return JobUtils.deployArtifact(src.getAbsolutePath(), clusterName, ctrlSubject)
                .map(clusterArtifactUriPair -> clusterArtifactUriPair.getValue())
                .toObservable();
    }
}
