package com.microsoft.azure.sqlbigdata.spark.common;

import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.Deployable;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;
import rx.Observer;

import java.util.AbstractMap;

public class ArisSparkBatchJob extends SparkBatchJob {
    public ArisSparkBatchJob(
            @Nullable IClusterDetail cluster,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission,
            @NotNull Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject,
            @Nullable Deployable jobDeploy) {
        super(cluster, submissionParameter, sparkBatchSubmission, ctrlSubject, jobDeploy);
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
}
