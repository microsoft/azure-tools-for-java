/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppResponse;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;

public class SparkBatchRemoteDebugJob implements ISparkBatchDebugJob, ILogger {
    /**
     * The base connection URI for HDInsight Spark Job service, such as: http://livy:8998/batches
     */
    private URI connectUri;

    /**
     * The LIVY Spark batch job ID got from job submission
     */
    private int batchId;

    /**
     * The Spark Batch Job submission parameter
     */
    private SparkSubmissionParameter submissionParameter;

    /**
     * The Spark Batch Job submission for RestAPI transaction
     */
    private SparkBatchSubmission submission;

    /**
     * The setting of maximum retry count in RestAPI calling
     */
    private int retriesMax = 3;

    /**
     * The setting of delay seconds between tries in RestAPI calling
     */
    private int delaySeconds = 10;

    /**
     * Getter of Spark Batch Job submission parameter
     *
     * @return the instance of Spark Batch Job submission parameter
     */
    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }

    /**
     * Getter of the Spark Batch Job submission for RestAPI transaction
     *
     * @return the Spark Batch Job submission
     */
    public SparkBatchSubmission getSubmission() {
        return submission;
    }

    /**
     * Getter of the base connection URI for HDInsight Spark Job service
     *
     * @return the base connection URI for HDInsight Spark Job service
     */
    public URI getConnectUri() {
        return connectUri;
    }

    /**
     * Getter of the LIVY Spark batch job ID got from job submission
     *
     * @return the LIVY Spark batch job ID
     */
    public int getBatchId() {
        return batchId;
    }

    /**
     * Setter of LIVY Spark batch job ID got from job submission
     *
     * @param batchId the LIVY Spark batch job ID
     */
    private void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    /**
     * Getter of the maximum retry count in RestAPI calling
     *
     * @return the maximum retry count in RestAPI calling
     */
    public int getRetriesMax() {
        return retriesMax;
    }

    /**
     * Setter of the maximum retry count in RestAPI calling
     * @param retriesMax the maximum retry count in RestAPI calling
     */
    public void setRetriesMax(int retriesMax) {
        this.retriesMax = retriesMax;
    }

    /**
     * Getter of the delay seconds between tries in RestAPI calling
     *
     * @return the delay seconds between tries in RestAPI calling
     */
    public int getDelaySeconds() {
        return delaySeconds;
    }

    /**
     * Setter of the delay seconds between tries in RestAPI calling
     * @param delaySeconds the delay seconds between tries in RestAPI calling
     */
    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    SparkBatchRemoteDebugJob(
            URI connectUri,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission) {
        this.connectUri = connectUri;
        this.submissionParameter = submissionParameter;
        this.submission = sparkBatchSubmission;
    }

    /**
     * Create a batch Spark job with driver debugging enabled
     *
     * @return the current instance for chain calling
     * @throws IOException the exceptions for networking connection issues related
     * @throws DebugParameterDefinedException the exception for debug option already defined in Spark submission parameter
     */
    @Override
    public SparkBatchRemoteDebugJob createBatchSparkJobWithDriverDebugging()
            throws IOException, DebugParameterDefinedException {
        // Submit the batch job
        HttpResponse httpResponse = this.getSubmission().createBatchSparkJob(
                this.getConnectUri().toString(), this.getSubmissionParameter());

        // Get the batch ID from response and save it
        if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
            SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                    httpResponse.getMessage(), SparkSubmitResponse.class)
                    .orElseThrow(() -> new UnknownServiceException(
                            "Bad spark job response: " + httpResponse.getMessage()));

            this.setBatchId(jobResp.getId());

            return this;
        }

        throw new UnknownServiceException(String.format(
                "Failed to submit Spark remove debug job. error code: %d, type: %s, reason: %s.",
                httpResponse.getCode(), httpResponse.getContent(), httpResponse.getMessage()));
    }

    /**
     * Kill the batch job specified by ID
     *
     * @return the current instance for chain calling
     * @throws IOException exceptions for networking connection issues related
     */
    @Override
    public SparkBatchRemoteDebugJob killBatchJob() throws IOException {
        HttpResponse deleteResponse = this.getSubmission().killBatchJob(
                this.getConnectUri().toString(), this.getBatchId());

        if (deleteResponse.getCode() > 300) {
            throw new UnknownServiceException(String.format(
                    "Failed to stop spark remote debug job. error code: %d, reason: %s.",
                    deleteResponse.getCode(), deleteResponse.getContent()));
        }

        return this;
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
                this.getSubmission().getCredentialsProvider(),
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
    public int getSparkDriverDebuggingPort() throws IOException {
        String driverLogUrl = this.getSparkJobDriverLogUrl(this.getConnectUri(), this.getBatchId());

        return getYarnContainerJDBListenPort(driverLogUrl);
    }

    /**
     * Get Spark batch job driver host by ID
     *
     * @return Spark driver node host
     * @throws IOException exceptions for the driver host not found
     */
    @Override
    public String getSparkDriverHost() throws IOException {
        String applicationId = this.getSparkJobApplicationId(this.getConnectUri(), this.getBatchId());

        App yarnApp = this.getSparkJobYarnApplication(this.getConnectUri(), applicationId);

        if (yarnApp.isFinished()) {
            throw new UnknownServiceException("The Livy job " + this.getBatchId() + " on yarn is not running.");
        }

        String driverHttpAddress = yarnApp.getAmHostHttpAddress();

        /*
         * The sample here is:
         *     host.domain.com:8900
         *       or
         *     10.0.0.15:30060
         */
        String driverHost = this.parseAmHostHttpAddressHost(driverHttpAddress);

        if (driverHost == null) {
            throw new UnknownServiceException(
                    "Bad amHostHttpAddress got from /yarnui/ws/v1/cluster/apps/" + applicationId);
        }

        return driverHost;
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

    /**
     * Parse host from host:port combination string
     *
     * @param driverHttpAddress the host:port combination string to parse
     * @return the host got, otherwise null
     */
    protected String parseAmHostHttpAddressHost(String driverHttpAddress) {

        Pattern driverRegex = Pattern.compile("(?<host>[^:]+):(?<port>\\d+)");
        Matcher driverMatcher = driverRegex.matcher(driverHttpAddress);

        return driverMatcher.matches() ? driverMatcher.group("host") : null;
    }

    /**
     * Get Spark Job Yarn application ID with retries
     *
     * @param batchBaseUri the connection URI
     * @param batchId the Livy batch job ID
     * @return the Yarn application ID got
     * @throws IOException exceptions in transaction
     */
    protected String getSparkJobApplicationId(URI batchBaseUri, int batchId) throws IOException {
        int retries = 0;

        do {
            try {
                HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        batchBaseUri.toString(), batchId);

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    if (jobResp.getAppId() != null) {
                        return jobResp.getAppId();
                    }
                }
            } catch (IOException ignore) {
                log().debug("Got exception " + ignore.toString() + ", waiting for a while to try",
                            ignore);
            }

            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Unknown service error after " + --retries + " retries");
    }

    /**
     * Get Spark Job Yarn application with retries
     *
     * @param batchBaseUri the connection URI of HDInsight Livy batch job, http://livy:8998/batches, the function will help translate it to Yarn connection URI.
     * @param applicationID the Yarn application ID
     * @return the Yarn application got
     * @throws IOException exceptions in transaction
     */
    protected App getSparkJobYarnApplication(URI batchBaseUri, String applicationID) throws IOException {
        int retries = 0;

        do {
            // TODO: An issue here when the yarnui not sharing root with Livy batch job URI
            URI getYarnClusterAppURI = batchBaseUri.resolve("/yarnui/ws/v1/cluster/apps/" + applicationID);

            try {
                HttpResponse httpResponse = this.getSubmission()
                        .getHttpResponseViaGet(getYarnClusterAppURI.toString());

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    Optional<AppResponse> appResponse = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), AppResponse.class);
                    return appResponse
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad response when getting from " + getYarnClusterAppURI + ", " +
                                            "response " + httpResponse.getMessage()))
                            .getApp();
                }
            } catch (IOException ignore) {
                log().debug("Got exception " + ignore.toString() + ", waiting for a while to try",
                        ignore);
            }

            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Unknown service error after " + --retries + " retries");
    }

    /**
     * Get Spark Job driver log URL with retries
     *
     * @param batchBaseUri the connection URI
     * @param batchId the Livy batch job ID
     * @return the Spark Job driver log URL
     * @throws IOException exceptions in transaction
     */
    public String getSparkJobDriverLogUrl(URI batchBaseUri, int batchId) throws IOException {
        int retries = 0;

        do {
            HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                    batchBaseUri.toString(), batchId);

            try {
                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    if (jobResp.getAppId() != null && jobResp.getAppInfo().get("driverLogUrl") != null) {
                        return jobResp.getAppInfo().get("driverLogUrl").toString();
                    }
                }
            } catch (IOException ignore) {
                log().debug("Got exception " + ignore.toString() + ", waiting for a while to try",
                        ignore);
            }


            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Unknown service error after " + --retries + " retries");
    }


    /**
     * The factory helper function to create a SparkBatchRemoteDebugJob instance
     *
     * @param connectUrl the base connection URI for HDInsight Spark Job service, such as: http://livy:8998/batches
     * @param submissionParameter the Spark Batch Job submission parameter
     * @param submission the Spark Batch Job submission
     * @return a new SparkBatchRemoteDebugJob instance
     * @throws DebugParameterDefinedException the exception for the Spark driver debug option exists
     * @throws URISyntaxException the exception for connectUrl syntax errors
     */
    static SparkBatchRemoteDebugJob factory(
            String connectUrl,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission submission) throws DebugParameterDefinedException, URISyntaxException {
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

        SparkSubmissionParameter debugSubmissionParameter = new SparkSubmissionParameter(
                submissionParameter.getClusterName(),
                submissionParameter.isLocalArtifact(),
                submissionParameter.getArtifactName(),
                submissionParameter.getLocalArtifactPath(),
                submissionParameter.getFile(),
                submissionParameter.getMainClassName(),
                submissionParameter.getReferencedFiles(),
                submissionParameter.getReferencedJars(),
                submissionParameter.getArgs(),
                jobConfigWithDebug
        );

        return new SparkBatchRemoteDebugJob(new URI(connectUrl), debugSubmissionParameter, submission);
    }
}
