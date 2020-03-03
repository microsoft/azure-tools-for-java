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
