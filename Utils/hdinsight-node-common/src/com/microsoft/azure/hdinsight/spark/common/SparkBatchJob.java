/*
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

import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppResponse;
import rx.Observable;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownServiceException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Error;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.Log;
import static java.lang.Thread.sleep;
import static rx.exceptions.Exceptions.propagate;

public class SparkBatchJob implements ISparkBatchJob, ILogger {
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

    public SparkBatchJob(
            URI connectUri,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission) {
        this.connectUri = connectUri;
        this.submissionParameter = submissionParameter;
        this.submission = sparkBatchSubmission;
    }

    /**
     * Getter of Spark Batch Job submission parameter
     *
     * @return the instance of Spark Batch Job submission parameter
     */
    @Override
    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }

    /**
     * Getter of the Spark Batch Job submission for RestAPI transaction
     *
     * @return the Spark Batch Job submission
     */
    @Override
    public SparkBatchSubmission getSubmission() {
        return submission;
    }

    /**
     * Getter of the base connection URI for HDInsight Spark Job service
     *
     * @return the base connection URI for HDInsight Spark Job service
     */
    @Override
    public URI getConnectUri() {
        return connectUri;
    }

    /**
     * Getter of the LIVY Spark batch job ID got from job submission
     *
     * @return the LIVY Spark batch job ID
     */
    @Override
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
    @Override
    public int getRetriesMax() {
        return retriesMax;
    }

    /**
     * Setter of the maximum retry count in RestAPI calling
     * @param retriesMax the maximum retry count in RestAPI calling
     */
    @Override
    public void setRetriesMax(int retriesMax) {
        this.retriesMax = retriesMax;
    }

    /**
     * Getter of the delay seconds between tries in RestAPI calling
     *
     * @return the delay seconds between tries in RestAPI calling
     */
    @Override
    public int getDelaySeconds() {
        return delaySeconds;
    }

    /**
     * Setter of the delay seconds between tries in RestAPI calling
     * @param delaySeconds the delay seconds between tries in RestAPI calling
     */
    @Override
    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    /**
     * Create a batch Spark job with driver debugging enabled
     *
     * @return the current instance for chain calling
     * @throws IOException the exceptions for networking connection issues related
     */
    @Override
    public ISparkBatchJob createBatchJob()
            throws IOException {
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
    public ISparkBatchJob killBatchJob() throws IOException {
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
     * Get Spark Job Yarn application state with retries
     *
     * @return the Yarn application state got
     * @throws IOException exceptions in transaction
     */
    public String getState() throws IOException {
        int retries = 0;

        do {
            try {
                HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        this.getConnectUri().toString(), batchId);

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    return jobResp.getState();
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

    public Observable<SimpleImmutableEntry<MessageInfoType, String>> getSubmissionLog() {
        return Observable.create(ob -> {
            try {
                int start = 0;
                final int maxLinesPerGet = 128;
                int linesGot = 0;
                boolean isJobActive = true;

                while (isJobActive) {
                    String logUrl = String.format("%s/%d/log?from=%d&size=%d",
                            this.getConnectUri().toString(), batchId, start, maxLinesPerGet);

                    HttpResponse httpResponse = this.getSubmission().getHttpResponseViaGet(logUrl);

                    SparkJobLog sparkJobLog = ObjectConvertUtils.convertJsonToObject(httpResponse.getMessage(),
                            SparkJobLog.class)
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark log response: " + httpResponse.getMessage()));

                    // To subscriber
                    sparkJobLog.getLog().forEach(line -> ob.onNext(new SimpleImmutableEntry<>(Log, line)));

                    linesGot = sparkJobLog.getLog().size();
                    start += linesGot;

                    // Retry interval
                    if (linesGot == 0) {
                        sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
                        isJobActive = this.isActive();
                    }
                }
            } catch (IOException ex) {
                ob.onNext(new SimpleImmutableEntry<>(Error, ex.getMessage()));
            } catch (InterruptedException ignored) {
            } finally {
                ob.onCompleted();
            }
        });
    }

    public boolean isActive() throws IOException {
        int retries = 0;

        do {
            try {
                HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        this.getConnectUri().toString(), batchId);

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    return jobResp.isAlive();
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

    public boolean isLogAggregated() throws IOException {
        String applicationId = this.getSparkJobApplicationId(this.getConnectUri(), this.getBatchId());
        App yarnApp = this.getSparkJobYarnApplication(this.getConnectUri(), applicationId);

        switch (yarnApp.getLogAggregationStatus().toUpperCase()) {
            case "SUCCEEDED":
                return true;
            case "DISABLED":
            case "NOT_START":
            case "RUNNING":
            case "RUNNING_WITH_FAILURE":
            case "FAILED":
            case "TIME_OUT":
            default:
                return false;
        }
    }

    public Observable<SparkBatchJobState> getJobDoneObservable() {
        return Observable.interval(200, TimeUnit.MILLISECONDS)
                .map((times) -> {
                    try {
                        return getState();
                    } catch (IOException e) {
                        throw propagate(e);
                    }
                })
                .map(s -> SparkBatchJobState.valueOf(s.toUpperCase()))
                .filter(SparkBatchJobState::isJobDone)
                .filter((state) -> {
                    try {
                        return isLogAggregated();
                    } catch (IOException e) {
                        throw propagate(e);
                    }
                })
                .delay(3, TimeUnit.SECONDS);
    }
}
