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

package com.microsoft.azure.sqlbigdata.spark.common;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.Deployable;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;

public class ArisSparkBatchJob extends SparkBatchJob {
    public ArisSparkBatchJob(
            @Nullable IClusterDetail cluster,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission,
            @Nullable Deployable jobDeploy) {
        super(cluster, submissionParameter, sparkBatchSubmission, jobDeploy);
    }

    @Override
    protected Observable<String> getSparkJobDriverLogUrlObservable() {
        return getSparkJobYarnCurrentAppAttempt()
                .map(appAttempt -> {
                    String nodeId = appAttempt.getNodeId();
                    String containerId = appAttempt.getContainerId();
                    String connectUrl = getCluster().getConnectionUrl();
                    return String.format("%s/jobhistory/joblogs/%s/%s/%s/root", connectUrl, nodeId, containerId, containerId);
                });
    }

    @Override
    public ArisSparkBatchJob clone() {
        return new ArisSparkBatchJob(
                this.getCluster(),
                this.getSubmissionParameter(),
                this.getSubmission(),
                this.getJobDeploy()
        );
    }
}
