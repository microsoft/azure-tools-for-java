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

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;

import java.io.IOException;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SparkBatchRemoteDebugJob extends SparkBatchJob implements ISparkBatchDebugJob, ILogger {
     SparkBatchRemoteDebugJob(
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission) {
        super(submissionParameter, sparkBatchSubmission);
    }

    public SparkBatchRemoteDebugJob(
            @Nullable IClusterDetail cluster,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission,
            @Nullable Deployable jobDeploy) {
        super(cluster, submissionParameter, sparkBatchSubmission, jobDeploy);
    }


    /**
     * Get the Yarn container JDB listening port
     *
     * @param containerLogUrl container Yarn log URL
     * @return port number
     * @throws UnknownServiceException when there is not JDB listening port
     */
    public int getYarnContainerJDBListenPort(String containerLogUrl) throws UnknownServiceException {
        int port = this.parseJvmDebuggingPort(JobUtils.getInformationFromYarnLogDom(
                this.getSubmission().getAuthCode(),
                containerLogUrl,
                "stdout",
                -4096,
                0));

        if (port > 0) {
            return port;
        }

        throw new UnknownServiceException("JVM debugging port is not listening");
    }

    /**
     * Get Spark Batch job driver debugging port number
     *
     * @return Spark driver node debugging port
     * @throws IOException exceptions for the driver debugging port not found
     */
    @Override
    public Observable<Integer> getSparkDriverDebuggingPort() {
        return this.getSparkJobDriverLogUrlObservable()
                .flatMap(driverLogUrl -> {
                    try {
                        return Observable.just(getYarnContainerJDBListenPort(driverLogUrl));
                    } catch (UnknownServiceException e) {
                        return Observable.error(e);
                    }
                });
    }

    /**
     * Parse JVM debug port from listening string
     *
     * @param listening the listening message
     * @return the listening port found, otherwise -1
     */
    protected int parseJvmDebuggingPort(String listening) {
        /*
         * The content about JVM debug port listening message looks like:
         *     Listening for transport dt_socket at address: 6006
         */

        Pattern debugPortRegex = Pattern.compile("Listening for transport dt_socket at address: (?<port>\\d+)\\s*");
        Matcher debugPortMatcher = debugPortRegex.matcher(listening);

        return debugPortMatcher.matches() ? Integer.parseInt(debugPortMatcher.group("port")) : -1;
    }

    static public SparkSubmissionParameter convertToDebugParameter(SparkSubmissionParameter submissionParameter)
            throws DebugParameterDefinedException {
        final String debugJvmOption = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0";
        final String sparkJobDriverJvmOptionConfKey = "spark.driver.extraJavaOptions";
        final String sparkJobExecutorJvmOptionConfKey = "spark.executor.extraJavaOptions";
        final String sparkJobDriverRetriesConfKey = "spark.yarn.maxAppAttempts";
        final String sparkJobDriverNetworkTimeoutKey = "spark.network.timeout";
        final String sparkJobExecutorMaxFailuresKey = "spark.yarn.max.executor.failures";
        final String numExecutorsKey = "numExecutors";
        final String jvmDebugOptionPattern = ".*\\bsuspend=(?<isSuspend>[yn]),address=(?<port>\\d+).*";

        Map<String, Object> jobConfig = submissionParameter.getJobConfig();
        Object sparkConfigEntry = jobConfig.get(SparkSubmissionParameter.Conf);
        SparkConfigures sparkConf = (sparkConfigEntry != null && sparkConfigEntry instanceof Map) ?
                new SparkConfigures(sparkConfigEntry) :
                new SparkConfigures();


        // Check the conflict configurations
        String carriedDriverOption = sparkConf.getOrDefault(sparkJobDriverJvmOptionConfKey,"").toString();
        if (Pattern.compile(jvmDebugOptionPattern).matcher(carriedDriverOption).matches()) {
            throw new DebugParameterDefinedException(
                    "The driver Debug parameter is defined in Spark job configuration: " +
                            sparkConf.get(sparkJobDriverJvmOptionConfKey));
        }

        String carriedExecutorOption = sparkConf.getOrDefault(sparkJobExecutorJvmOptionConfKey,"").toString();
        if (Pattern.compile(jvmDebugOptionPattern).matcher(carriedExecutorOption).matches()) {
            throw new DebugParameterDefinedException(
                    "The executor Debug parameter is defined in Spark job configuration: " +
                            sparkConf.get(sparkJobExecutorJvmOptionConfKey));
        }

        DebugParameterDefinedException checkingErr = Stream.of(sparkJobDriverNetworkTimeoutKey,
                                                               sparkJobExecutorMaxFailuresKey,
                                                               sparkJobDriverRetriesConfKey)
                .filter(sparkConf::containsKey)
                .map(key -> new DebugParameterDefinedException(
                                "The " + key + " is defined in Spark job configuration: " + sparkConf.get(key)))
                .findFirst()
                .orElse(null);

        if (checkingErr != null) {
            throw checkingErr;
        }

        // Append or overwrite the Spark job driver JAVA option
        String driverOption = (carriedDriverOption.trim() + " " + debugJvmOption).trim();
        String executorOption = (carriedExecutorOption.trim() + " " + debugJvmOption).trim();
        HashMap<String, Object> jobConfigWithDebug = new HashMap<>(submissionParameter.getJobConfig());
        SparkConfigures sparkConfigWithDebug = new SparkConfigures(sparkConf);

        sparkConfigWithDebug.put(sparkJobDriverJvmOptionConfKey, driverOption);
        sparkConfigWithDebug.put(sparkJobExecutorJvmOptionConfKey, executorOption);
        sparkConfigWithDebug.put(sparkJobExecutorMaxFailuresKey, "1");
        sparkConfigWithDebug.put(sparkJobDriverRetriesConfKey, "1");
        sparkConfigWithDebug.put(sparkJobDriverNetworkTimeoutKey, "10000000");

        jobConfigWithDebug.put(SparkSubmissionParameter.Conf, sparkConfigWithDebug);

        return new SparkSubmissionParameter(
                submissionParameter.getClusterName(),
                submissionParameter.isLocalArtifact(),
                submissionParameter.getArtifactName(),
                submissionParameter.getLocalArtifactPath(),
                submissionParameter.getFile(),
                submissionParameter.getMainClassName(),
                submissionParameter.getReferencedFiles(),
                submissionParameter.getReferencedJars(),
                submissionParameter.getArgs(),
                jobConfigWithDebug);
    }

    @Override
    public boolean isRunning(@NotNull String state) {
        try {
            // The Debugging enabled job will wait for the JDB to connect in STARTING state to run
            return (SparkBatchJobState.valueOf(state.toUpperCase()) == SparkBatchJobState.STARTING &&
                            getSparkDriverDebuggingPort().toBlocking().singleOrDefault(-1) > 0) ||
                    super.isRunning(state);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SparkBatchRemoteDebugJob clone() {
        return new SparkBatchRemoteDebugJob(
                this.getCluster(),
                this.getSubmissionParameter(),
                this.getSubmission(),
                this.getJobDeploy()
        );
    }
}
