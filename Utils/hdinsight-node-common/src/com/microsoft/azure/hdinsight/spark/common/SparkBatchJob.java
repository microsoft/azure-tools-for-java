/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.YarnCluster;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppAttempt;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppAttemptsResponse;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppResponse;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.htmlunit.Cache;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.subjects.PublishSubject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownServiceException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Error;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine.LIVY;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine.TOOL;
import static java.lang.Thread.sleep;
import static rx.exceptions.Exceptions.propagate;

public class SparkBatchJob implements ISparkBatchJob, ILogger {
    public static final String WebHDFSPathPattern = "^(https?://)([^/]+)(/.*)?(/webhdfs/v1)(/.*)?$";

    @Nullable
    private String currentLogUrl;
    @NotNull
    private final Observer<SparkLogLine> ctrlSubject;

    /**
     * Livy log fetching offset in Spark Batch Job context. Accessing with {@link #livyLogOffsetLock}
     */
    private int nextLivyLogOffset = 0;
    private final Object livyLogOffsetLock = new Object();

    @Nullable
    private String getCurrentLogUrl() {
        return currentLogUrl;
    }

    private void setCurrentLogUrl(@Nullable String currentLogUrl) {
        this.currentLogUrl = currentLogUrl;
    }

    public enum DriverLogConversionMode {
        WITHOUT_PORT,
        WITH_PORT,
        ORIGINAL;

        @Nullable
        public static DriverLogConversionMode next(@Nullable DriverLogConversionMode current) {
            final List<DriverLogConversionMode> modes = Arrays.asList(DriverLogConversionMode.values());

            if (current == null) {
                return modes.get(0);
            }

            final int found = modes.indexOf(current);

            if (found + 1 >= modes.size()) {
                return null;
            }

            return modes.get(found + 1);
        }
    }

    /**
     * The base connection URI for HDInsight Spark Job service, such as: http://livy:8998/batches
     */
    @Nullable
    private URI connectUri;

    /**
     * The base connection URI for HDInsight Yarn application service, such as: http://hn0-spark2:8088/cluster/app
     */
    @Nullable
    private URI yarnConnectUri;

    /**
     * The LIVY Spark batch job ID got from job submission
     */
    private int batchId;

    /**
     * The Spark Batch Job submission parameter
     */
    protected SparkSubmissionParameter submissionParameter;

    /**
     * The Spark Batch Job submission for RestAPI transaction
     */
    private final SparkBatchSubmission submission;

    /**
     * The setting of maximum retry count in RestAPI calling
     */
    private int retriesMax = 3;

    /**
     * The setting of delay seconds between tries in RestAPI calling
     */
    private int delaySeconds = 10;

    /**
     * The global cache for fetched Yarn UI page by browser
     */
    private final Cache globalCache = JobUtils.getGlobalCache();

    /**
     * The driver log conversion mode
     */
    @Nullable
    private DriverLogConversionMode driverLogConversionMode = null;

    @Nullable
    private IHDIStorageAccount storageAccount;

    @Nullable
    private final IClusterDetail cluster;
    /**
     * Access token used for uploading files to ADLS storage account
     */
    @Nullable
    private String accessToken;

    @Nullable
    private String destinationRootPath;

    @Nullable
    private HttpObservable httpObservable;

    @Nullable
    private final Deployable jobDeploy;

    public SparkBatchJob(
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission) {
        this(null, submissionParameter, sparkBatchSubmission, null, null, null);
    }

    public SparkBatchJob(
            @Nullable IClusterDetail cluster,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission,
            @Nullable IHDIStorageAccount storageAccount,
            @Nullable String accessToken,
            @Nullable String destinationRootPath) {
        this(cluster, submissionParameter, sparkBatchSubmission, storageAccount, accessToken, destinationRootPath, null, null);
    }

    public SparkBatchJob(
            @Nullable IClusterDetail cluster,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission,
            @Nullable IHDIStorageAccount storageAccount,
            @Nullable String accessToken,
            @Nullable String destinationRootPath,
            @Nullable HttpObservable httpObservable,
            @Nullable Deployable jobDeploy) {
        this.cluster = cluster;
        this.submissionParameter = submissionParameter;
        this.storageAccount = storageAccount;
        this.submission = sparkBatchSubmission;
        this.ctrlSubject = PublishSubject.create();
        this.accessToken = accessToken;
        this.destinationRootPath = destinationRootPath;
        this.httpObservable = httpObservable;
        this.jobDeploy = jobDeploy;
    }

    public SparkBatchJob(
            @Nullable IClusterDetail cluster,
            SparkSubmissionParameter submissionParameter,
            SparkBatchSubmission sparkBatchSubmission,
            @Nullable Deployable jobDeploy) {
        this.cluster = cluster;
        this.submissionParameter = submissionParameter;
        this.submission = sparkBatchSubmission;
        this.ctrlSubject = PublishSubject.create();
        this.jobDeploy = jobDeploy;
    }

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
    @Nullable
    @Override
    public URI getConnectUri() {
        if (connectUri == null) {
            final Optional<IClusterDetail> cluster = getCluster() != null
                    ? Optional.of(getCluster())
                    : ClusterManagerEx.getInstance().getClusterDetailByName(getSubmissionParameter().getClusterName());

            if (cluster.isPresent()) {
                try {
                    getSubmission().setUsernamePasswordCredential(
                            cluster.get().getHttpUserName(),
                            cluster.get().getHttpPassword()
                    );
                } catch (final HDIException e) {
                    log().warn("No credential provided for Spark batch job.");
                }

                this.connectUri = cluster.filter(c -> c instanceof LivyCluster)
                        .map(c -> ((LivyCluster) c).getLivyBatchUrl())
                        .map(URI::create)
                        .orElse(null);
            } else {
                log().warn("No cluster found for " + getSubmissionParameter().getClusterName());
            }
        }

        return connectUri;
    }

    @Nullable
    public URI getYarnNMConnectUri() {
        if (yarnConnectUri == null) {
            final Optional<IClusterDetail> cluster = getCluster() != null
                    ? Optional.of(getCluster())
                    : ClusterManagerEx.getInstance().getClusterDetailByName(getSubmissionParameter().getClusterName());

            if (cluster.isPresent()) {
                try {
                    getSubmission().setUsernamePasswordCredential(
                            cluster.get().getHttpUserName(),
                            cluster.get().getHttpPassword()
                    );
                } catch (final HDIException e) {
                    log().warn("No credential provided for Spark batch job.");
                }

                this.yarnConnectUri = cluster.filter(c -> c instanceof YarnCluster)
                        .map(c -> ((YarnCluster) c).getYarnNMConnectionUrl())
                        .map(URI::create)
                        .orElse(null);
            } else {
                log().warn("No cluster found for " + getSubmissionParameter().getClusterName());
            }
        }

        return yarnConnectUri;
    }

    @Nullable
    public IClusterDetail getCluster() {
        return cluster;
    }

    @Nullable
    public IHDIStorageAccount getStorageAccount() {
        return storageAccount;
    }

    @Nullable
    public Deployable getJobDeploy() {
        return jobDeploy;
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
    protected void setBatchId(int batchId) {
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
     * Create a batch Spark job
     *
     * @return the current instance for chain calling
     * @throws IOException the exceptions for networking connection issues related
     */
    private SparkBatchJob createBatchJob()
            throws IOException {
        if (getConnectUri() == null) {
            throw new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted.");
        }

        // Submit the batch job
        final HttpResponse httpResponse = this.getSubmission().createBatchSparkJob(
                this.getConnectUri().toString(), this.getSubmissionParameter());

        // Get the batch ID from response and save it
        if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
            final SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                    httpResponse.getMessage(), SparkSubmitResponse.class)
                                                                  .orElseThrow(() -> new UnknownServiceException(
                            "Bad spark job response: " + httpResponse.getMessage()));
            this.setBatchId(jobResp.getId());

            getCtrlSubject().onNext(
                    new SparkLogLine(TOOL,
                                     Info,
                                     "Spark Batch submission " + httpResponse.toString()));

            return this;
        }

        throw new UnknownServiceException(String.format(
                "Failed to submit Spark batch job. error code: %d, type: %s, reason: %s.",
                httpResponse.getCode(), httpResponse.getContent(), httpResponse.getMessage()));
    }

    /**
     * Kill the batch job specified by ID
     *
     * @return the current instance for chain calling
     */
    @Override
    public Observable<? extends ISparkBatchJob> killBatchJob() {
        return Observable.fromCallable(() -> {
            if (getConnectUri() == null) {
                throw new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                        "please configure Spark cluster which the Spark job will be submitted.");
            }

            final HttpResponse deleteResponse = this.getSubmission().killBatchJob(
                    this.getConnectUri().toString(), this.getBatchId());

            if (deleteResponse.getCode() > 300) {
                throw new UnknownServiceException(String.format(
                        "Failed to stop spark job. error code: %d, reason: %s.",
                        deleteResponse.getCode(), deleteResponse.getContent()));
            }

            return this;
        });
    }

    @NotNull
    @Override
    public Observable<SimpleImmutableEntry<String, Long>> getDriverLog(@NotNull String type, long logOffset, int size) {
        return getSparkJobDriverLogUrlObservable()
                .flatMap(logUrl -> {
                    long offset = logOffset;

                    if (!StringUtils.equals(logUrl, getCurrentLogUrl())) {
                        setCurrentLogUrl(logUrl);
                        offset = 0;
                    }

                    if (getCurrentLogUrl() == null) {
                        return Observable.empty();
                    }

                    return getContainerLog(getCurrentLogUrl(), type, offset, size);
                });
    }

    @NotNull
    @Override
    public Observable<SimpleImmutableEntry<String, Long>> getContainerLog(@NotNull String containerLogUrl,
                                                                          @NotNull String type,
                                                                          long logOffset,
                                                                          int size) {
        final String logGot = JobUtils.getInformationFromYarnLogDom(
                getSubmission().getAuthCode(),
                containerLogUrl,
                type,
                logOffset,
                size);

        if (StringUtils.isEmpty(logGot)) {
            return Observable.empty();
        }

        return Observable.just(new SimpleImmutableEntry<>(logGot, logOffset));
    }

    /**
     * Parse host from host:port combination string
     *
     * @param driverHttpAddress the host:port combination string to parse
     * @return the host got, otherwise null
     */
    @Nullable
    String parseAmHostHttpAddressHost(@Nullable String driverHttpAddress) {
        if (driverHttpAddress == null) {
            return null;
        }

        final Pattern driverRegex = Pattern.compile("(?<host>[^:]+):(?<port>\\d+)");
        final Matcher driverMatcher = driverRegex.matcher(driverHttpAddress);

        return driverMatcher.matches() ? driverMatcher.group("host") : null;
    }

    /**
     * Get Spark Job Yarn application state with retries
     *
     * @return the Yarn application state got
     * @throws IOException exceptions in transaction
     */
    public String getState() throws IOException {
        if (getConnectUri() == null) {
            throw new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted.");
        }

        int retries = 0;

        do {
            try {
                final HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        this.getConnectUri().toString(), batchId);

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    final SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                                                                          .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    return jobResp.getState();
                }
            } catch (final IOException e) {
                log().debug("Got exception " + e.toString() + ", waiting for a while to try", e);
            }

            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (final InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Failed to get job state: Unknown service error after " + --retries + " retries");
    }

    /**
     * Get Spark Job Yarn application ID with retries
     *
     * @param batchBaseUri the connection URI
     * @param batchId the Livy batch job ID
     * @return the Yarn application ID got
     * @throws IOException exceptions in transaction
     */
    String getSparkJobApplicationId(URI batchBaseUri, int batchId) throws IOException {
        int retries = 0;

        do {
            try {
                final HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        batchBaseUri.toString(), batchId);

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    final SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                                                                          .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    if (jobResp.getAppId() != null) {
                        return jobResp.getAppId();
                    }
                }
            } catch (final IOException e) {
                log().debug("Got exception " + e.toString() + ", waiting for a while to try", e);
            }

            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (final InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Failed to get job Application ID: Unknown service error after " + --retries + " retries");
    }

    /**
     * Get Spark Job Yarn application with retries
     *
     * @param yarnConnectUri the connection URI of HDInsight Livy batch job, http://livy:8998/batches, the function will help translate it to Yarn connection URI.
     * @param applicationID the Yarn application ID
     * @return the Yarn application got
     * @throws IOException exceptions in transaction
     */
    @Nullable
    private App getSparkJobYarnApplication(URI yarnConnectUri, String applicationID) throws Exception {
        if (yarnConnectUri == null) {
            return null;
        }

        int retries = 0;

        do {
            // TODO: An issue here when the yarnui not sharing root with Livy batch job URI
            final URI getYarnClusterAppURI = URI.create(yarnConnectUri.toString() + applicationID);

            try {
                final HttpResponse httpResponse = this.getSubmission()
                                                      .getHttpResponseViaGet(getYarnClusterAppURI.toString());

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    final Optional<AppResponse> appResponse = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), AppResponse.class);
                    return appResponse
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad response when getting from " + getYarnClusterAppURI + ", " +
                                            "response " + httpResponse.getMessage()))
                            .getApp();
                }
            } catch (final IOException e) {
                log().debug("Got exception " + e.toString() + ", waiting for a while to try", e);
            }

            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (final InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Failed to get job Yarn application: Unknown service error after " + --retries + " retries");
    }

    /**
     * New RxAPI: Get current job application Id
     *
     * @return Application Id Observable
     */
    Observable<String> getSparkJobApplicationIdObservable() {
        if (getConnectUri() == null) {
            return Observable.error(new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted."));
        }

        return Observable.create(ob -> {
            try {
                final HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        getConnectUri().toString(), getBatchId());

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    final SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                                                                          .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    if (jobResp.getAppId() != null) {
                        ob.onNext(jobResp.getAppId());
                    }
                }

                ob.onCompleted();
            } catch (final IOException ex) {
                log().warn("Got exception " + ex.toString());
                ob.onError(ex);
            }
        });
    }

    /**
     * New RxAPI: Get the current Spark job Yarn application most recent attempt
     *
     * @return Yarn Application Attempt info Observable
     */
    protected Observable<AppAttempt> getSparkJobYarnCurrentAppAttempt() {
        if (getConnectUri() == null) {
            return Observable.error(new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted."));
        }

        return getSparkJobApplicationIdObservable()
                .flatMap(appId -> {
                    final URI getYarnAppAttemptsURI = URI.create(getYarnNMConnectUri() + appId + "/appattempts");

                    try {
                        final HttpResponse httpResponse = this.getSubmission()
                                                              .getHttpResponseViaGet(getYarnAppAttemptsURI.toString());

                        if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                            final Optional<AppAttemptsResponse> appResponse = ObjectConvertUtils.convertJsonToObject(
                                    httpResponse.getMessage(), AppAttemptsResponse.class);

                            return Observable.just(appResponse
                                    .flatMap(resp ->
                                            Optional.ofNullable(resp.getAppAttempts())
                                                .flatMap(appAttempts ->
                                                        appAttempts.appAttempt.stream()
                                                                .max(Comparator.comparingInt(AppAttempt::getId)))
                                    )
                                    .orElseThrow(() -> new UnknownServiceException(
                                            "Bad response when getting from " + getYarnAppAttemptsURI + ", " +
                                                    "response " + httpResponse.getMessage())));
                        }
                    } catch (final IOException ex) {
                        log().warn("Got exception " + ex.toString());
                        throw propagate(ex);
                    }

                    return Observable.empty();
                });
    }

    /**
     * Get Spark Job driver log URL with retries
     *
     * @deprecated
     * The Livy Rest API driver log Url field only get the running job.
     * Use getSparkJobDriverLogUrlObservable() please, with RxJava supported.
     *
     * @param batchBaseUri the connection URI
     * @param batchId the Livy batch job ID
     * @return the Spark Job driver log URL
     * @throws IOException exceptions in transaction
     */
    @Nullable
    @Deprecated
    public String getSparkJobDriverLogUrl(URI batchBaseUri, int batchId) throws IOException {
        int retries = 0;

        do {
            final HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                    batchBaseUri.toString(), batchId);

            try {
                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    final SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                                                                          .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    if (jobResp.getAppId() != null && jobResp.getAppInfo().get("driverLogUrl") != null) {
                        return jobResp.getAppInfo().get("driverLogUrl").toString();
                    }
                }
            } catch (final IOException e) {
                log().debug("Got exception " + e.toString() + ", waiting for a while to try", e);
            }


            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (final InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Failed to get job driver log URL: Unknown service error after " + --retries + " retries");
    }

    /**
     * Get Spark batch job driver host by ID
     *
     * @return Spark driver node host observable
     */
    @Override
    public Observable<String> getSparkDriverHost() {
        return Observable.fromCallable(() -> {
            if (getConnectUri() == null) {
                throw new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                        "please configure Spark cluster which the Spark job will be submitted.");
            }

            final String applicationId = this.getSparkJobApplicationId(this.getConnectUri(), this.getBatchId());

            final App yarnApp = this.getSparkJobYarnApplication(this.getYarnNMConnectUri(), applicationId);

            if (yarnApp == null) {
                throw new Exception("Can not access yarn applicaition since yarnConnectUri is null");
            }

            if (yarnApp.isFinished()) {
                throw new UnknownServiceException("The Livy job " + this.getBatchId() + " on yarn is not running.");
            }

            final String driverHttpAddress = yarnApp.getAmHostHttpAddress();

            /*
             * The sample here is:
             *     host.domain.com:8900
             *       or
             *     10.0.0.15:30060
             */
            final String driverHost = this.parseAmHostHttpAddressHost(driverHttpAddress);

            if (driverHost == null) {
                throw new UnknownServiceException(
                        "Bad amHostHttpAddress got from /yarnui/ws/v1/cluster/apps/" + applicationId);
            }

            return driverHost;
        });
    }

    @Override
    @NotNull
    public Observable<SparkLogLine> getSubmissionLog() {
        if (getConnectUri() == null) {
            return Observable.error(new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted."));
        }

        // Those lines are carried per response,
        // if there is no value followed, the line should not be sent to console
        final Set<String> ignoredEmptyLines = new HashSet<>(Arrays.asList(
                "stdout:",
                "stderr:",
                "yarn diagnostics:"));

        return Observable.create(ob -> {
            try {
                final int maxLinesPerGet = 128;
                int linesGot;
                boolean isFetching = true;

                while (isFetching) {
                    final int start = nextLivyLogOffset;
                    final boolean isAppIdAllocated = !this.getSparkJobApplicationIdObservable().isEmpty().toBlocking()
                            .lastOrDefault(true);
                    final String logUrl = String.format("%s/%d/log?from=%d&size=%d",
                                                        this.getConnectUri().toString(), batchId, start, maxLinesPerGet);

                    final HttpResponse httpResponse = this.getSubmission().getHttpResponseViaGet(logUrl);

                    final SparkJobLog sparkJobLog = ObjectConvertUtils.convertJsonToObject(httpResponse.getMessage(),
                                                                                           SparkJobLog.class)
                                                                      .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark log response: " + httpResponse.getMessage()));

                    synchronized (livyLogOffsetLock) {
                        if (start != nextLivyLogOffset) {
                            // The offset is moved by another fetching thread, re-do it with new offset
                            continue;
                        }

                        // To subscriber
                        sparkJobLog.getLog().stream()
                                .filter(line -> !ignoredEmptyLines.contains(line.trim().toLowerCase()))
                                .forEach(line -> ob.onNext(new SparkLogLine(LIVY, Log, line)));

                        linesGot = sparkJobLog.getLog().size();
                        nextLivyLogOffset += linesGot;
                    }

                    // Retry interval
                    if (linesGot == 0) {
                        isFetching = "starting".equals(this.getState()) && !isAppIdAllocated;

                        sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
                    }
                }
            } catch (final IOException ex) {
                ob.onNext(new SparkLogLine(TOOL, Error, ex.getMessage()));
            } catch (final InterruptedException ignored) {
            } finally {
                ob.onCompleted();
            }
        });
    }

    public boolean isActive() throws IOException {
        if (getConnectUri() == null) {
            throw new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted.");
        }

        int retries = 0;

        do {
            try {
                final HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        this.getConnectUri().toString(), batchId);

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    final SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                                                                          .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    return jobResp.isAlive();
                }
            } catch (final IOException e) {
                log().debug("Got exception " + e.toString() + ", waiting for a while to try", e);
            }

            try {
                // Retry interval
                sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
            } catch (final InterruptedException ex) {
                throw new IOException("Interrupted in retry attempting", ex);
            }
        } while (++retries < this.getRetriesMax());

        throw new UnknownServiceException("Failed to detect job activity: Unknown service error after " + --retries + " retries");
    }

    protected Observable<SimpleImmutableEntry<String, String>> getJobDoneObservable() {
        if (getConnectUri() == null) {
            return Observable.error(new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted."));
        }

        return Observable.create((Subscriber<? super SimpleImmutableEntry<String, String>> ob) -> {
            try {
                boolean isJobActive;
                SparkBatchJobState state = SparkBatchJobState.NOT_STARTED;
                String diagnostics = "";

                do {
                    final HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                            this.getConnectUri().toString(), batchId);

                    if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                        final SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                                httpResponse.getMessage(), SparkSubmitResponse.class)
                                                                              .orElseThrow(() -> new UnknownServiceException(
                                        "Bad spark job response: " + httpResponse.getMessage()));

                        state = SparkBatchJobState.valueOf(jobResp.getState().toUpperCase());
                        diagnostics = String.join("\n", jobResp.getLog());

                        isJobActive = !isDone(state.toString());
                    } else {
                        isJobActive = false;
                    }


                    // Retry interval
                    sleep(1000);
                } while (isJobActive);

                ob.onNext(new SimpleImmutableEntry<>(state.toString(), diagnostics));
                ob.onCompleted();
            } catch (final IOException ex) {
                ob.onError(ex);
            } catch (final InterruptedException ignored) {
                ob.onCompleted();
            }
        });
    }

    protected Observable<String> getJobLogAggregationDoneObservable() {
        return getSparkJobApplicationIdObservable()
                .flatMap(applicationId ->
                        Observable.fromCallable(() ->
                                getSparkJobYarnApplication(this.getYarnNMConnectUri(), applicationId))
                                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                                .filter(Objects::nonNull)
                                .takeUntil(this::isYarnAppLogAggregationDone)
                                .filter(this::isYarnAppLogAggregationDone))
                .map(yarnApp -> yarnApp.getLogAggregationStatus().toUpperCase());
    }

    public Observable<Integer> getYarnContainerLogUrlPort() {
        final int DEFAULT_YARN_CONTAINER_LOG_URL_PORT = 30060;
        return getSparkJobApplicationIdObservable()
                .flatMap(applicationId ->
                        Observable.fromCallable(() ->
                                getSparkJobYarnApplication(this.getYarnNMConnectUri(), applicationId)))
                .doOnError(err -> log().warn("Error getting yarn application. " + ExceptionUtils.getStackTrace(err)))
                .map(app -> {
                    String amHostHttpAddress = app.getAmHostHttpAddress();
                    int containerPort = DEFAULT_YARN_CONTAINER_LOG_URL_PORT;
                    Matcher portMatcher = Pattern.compile(":([0-9]+)").matcher(amHostHttpAddress);
                    if (portMatcher.find()) {
                        try {
                            containerPort = Integer.parseInt(portMatcher.group(1));
                        } catch (Exception ignore) {
                        }
                    }
                    return containerPort;
                })
                .onErrorResumeNext(Observable.just(DEFAULT_YARN_CONTAINER_LOG_URL_PORT));
    }

    private Boolean isYarnAppLogAggregationDone(App yarnApp) {
        switch (yarnApp.getLogAggregationStatus().toUpperCase()) {
            case "SUCCEEDED":
            case "FAILED":
            case "TIME_OUT":
                return true;
            case "DISABLED":
            case "NOT_START":
            case "RUNNING":
            case "RUNNING_WITH_FAILURE":
            default:
                return false;
        }
    }

    /**
     * New RxAPI: Get Job Driver Log URL from the container
     *
     * @return Job Driver log URL observable
     */
    protected Observable<String> getSparkJobDriverLogUrlObservable() {
        return getSparkJobYarnCurrentAppAttempt()
                .map(AppAttempt::getLogsLink)
                .map(URI::create)
                .filter(uri -> StringUtils.isNotEmpty(uri.getHost()))
                .flatMap(this::convertToPublicLogUri)
                .map(URI::toString);
    }

    boolean isUriValid(@NotNull URI uriProbe) throws IOException {
        return getSubmission().getHttpResponseViaGet(uriProbe.toString()).getCode() < 300;
    }

    private Optional<URI> convertToPublicLogUri(@Nullable DriverLogConversionMode mode, @NotNull URI internalLogUrl) {
        if (getConnectUri() == null) {
            return Optional.empty();
        }

        final String normalizedPath = Optional.of(internalLogUrl.getPath()).filter(StringUtils::isNotBlank).orElse("/");

        if (mode != null) {
            switch (mode) {
                case WITHOUT_PORT:
                    return Optional.of(getConnectUri().resolve(
                            String.format("/yarnui/%s%s", internalLogUrl.getHost(), normalizedPath)));
                case WITH_PORT:
                    return Optional.of(getConnectUri().resolve(
                            String.format("/yarnui/%s/port/%s%s",
                                    internalLogUrl.getHost(),
                                    internalLogUrl.getPort(),
                                    normalizedPath)));
                case ORIGINAL:
                    return Optional.of(internalLogUrl);
            }
        }

        return Optional.empty();
    }

    public Observable<URI> convertToPublicLogUri(@NotNull URI internalLogUri) {
        // New version, without port info in log URL
        return convertToPublicLogUri(getLogUriConversionMode(), internalLogUri)
                .map(Observable::just)
                .orElseGet(() -> {
                    // Probe usable driver log URI
                    DriverLogConversionMode probeMode = getLogUriConversionMode();

                    while ((probeMode = DriverLogConversionMode.next(probeMode)) != null) {
                        final Optional<URI> uri = convertToPublicLogUri(probeMode, internalLogUri)
                                .filter(uriProbe -> {
                                    try {
                                        return isUriValid(uriProbe);
                                    } catch (final IOException e) {
                                        return false;
                                    }
                                });

                        if (uri.isPresent()) {
                            // Find usable one
                            setDriverLogConversionMode(probeMode);

                            return Observable.just(uri.get());
                        }
                    }

                    // All modes were probed and all failed
                    return Observable.empty();
                });
    }

    @Nullable
    private DriverLogConversionMode getLogUriConversionMode() {
        return this.driverLogConversionMode;
    }

    private void setDriverLogConversionMode(@Nullable DriverLogConversionMode driverLogConversionMode) {
        this.driverLogConversionMode = driverLogConversionMode;
    }

    @NotNull
    @Override
    public Observer<SparkLogLine> getCtrlSubject() {
        return ctrlSubject;
    }

    @NotNull
    @Override
    public Observable<? extends ISparkBatchJob> deploy(@NotNull String artifactPath) {
        assert jobDeploy != null : "jobDeploy should not be null";
        return jobDeploy.deploy(new File(artifactPath), getCtrlSubject())
                .map(redirectPath -> {
                    getSubmissionParameter().setFilePath(redirectPath);
                    return this;
                });
    }

    /**
     * New RxAPI: Submit the job
     *
     * @return Spark Job observable
     */
    @Override
    @NotNull
    public Observable<? extends ISparkBatchJob> submit() {
        return Observable.fromCallable(() -> {
            if (getConnectUri() == null) {
                throw new SparkJobNotConfiguredException("Can't get cluster " +
                        getSubmissionParameter().getClusterName() + " to submit, " +
                        "please configure Spark cluster which the Spark job will be submitted.");
            }

            return createBatchJob();
        });
    }




    @Override
    public boolean isDone(@NotNull String state) {
        switch (SparkBatchJobState.valueOf(state.toUpperCase())) {
            case SHUTTING_DOWN:
            case ERROR:
            case DEAD:
            case SUCCESS:
                return true;
            case NOT_STARTED:
            case STARTING:
            case RUNNING:
            case RECOVERING:
            case BUSY:
            case IDLE:
            default:
                return false;
        }
    }

    @Override
    public boolean isRunning(@NotNull String state) {
        return SparkBatchJobState.valueOf(state.toUpperCase()) == SparkBatchJobState.RUNNING;
    }

    @Override
    public boolean isSuccess(@NotNull String state) {
        return SparkBatchJobState.valueOf(state.toUpperCase()) == SparkBatchJobState.SUCCESS;
    }

    /**
     * New RxAPI: Get the job status (from livy)
     *
     * @return Spark Job observable
     */
    @NotNull
    public Observable<? extends SparkSubmitResponse> getStatus() {
        if (getConnectUri() == null) {
            return Observable.error(new SparkJobNotConfiguredException("Can't get Spark job connection URI, " +
                    "please configure Spark cluster which the Spark job will be submitted."));
        }

        return Observable.fromCallable(() -> {
            final HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                    this.getConnectUri().toString(), getBatchId());

            if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                return ObjectConvertUtils.convertJsonToObject(
                        httpResponse.getMessage(), SparkSubmitResponse.class)
                        .orElseThrow(() -> new UnknownServiceException(
                                "Bad spark job response: " + httpResponse.getMessage()));
            }

            throw new SparkJobException("Can't get cluster " + getSubmissionParameter().getClusterName() + " status.");
        });
    }

    @NotNull
    @Override
    public Observable<String> awaitStarted() {
        return getStatus()
                .map(status -> new SimpleImmutableEntry<>(status.getState(), String.join("\n", status.getLog())))
                .retry(getRetriesMax())
                .repeatWhen(ob -> ob.doOnNext(ignored -> getCtrlSubject().onNext(
                        new SparkLogLine(TOOL, Info, "The Spark job is starting...")))
                                    .delay(getDelaySeconds(), TimeUnit.SECONDS))
                .takeUntil(stateLogPair -> isDone(stateLogPair.getKey()) || isRunning(stateLogPair.getKey()))
                .filter(stateLogPair -> isDone(stateLogPair.getKey()) || isRunning(stateLogPair.getKey()))
                .flatMap(stateLogPair -> {
                    if (isDone(stateLogPair.getKey()) && !isSuccess(stateLogPair.getKey())) {
                        return Observable.error(
                                new SparkJobException("The Spark job failed to start due to " + stateLogPair.getValue()));
                    }

                    return Observable.just(stateLogPair.getKey());
                });
    }

    @NotNull
    @Override
    public Observable<SimpleImmutableEntry<String, String>> awaitDone() {
        return getJobDoneObservable();
    }

    @NotNull
    @Override
    public Observable<String> awaitPostDone() {
        return getJobLogAggregationDoneObservable();
    }

    @Override
    public SparkBatchJob clone() {
        return new SparkBatchJob(
                this.getCluster(),
                this.getSubmissionParameter(),
                this.getSubmission(),
                this.getJobDeploy());
    }
}
