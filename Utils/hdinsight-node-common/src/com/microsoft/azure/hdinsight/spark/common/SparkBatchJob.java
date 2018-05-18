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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Cache;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppAttempt;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppAttemptsResponse;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppResponse;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownServiceException;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Error;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.Log;
import static java.lang.Thread.sleep;
import static rx.exceptions.Exceptions.propagate;

public class SparkBatchJob implements ISparkBatchJob, ILogger {
    public enum DriverLogConversionMode {
        WITHOUT_PORT,
        WITH_PORT;

        @Nullable
        public static DriverLogConversionMode next(@Nullable DriverLogConversionMode current) {
            List<DriverLogConversionMode> modes = Arrays.asList(DriverLogConversionMode.values());

            if (current == null) {
                return modes.get(0);
            }

            int found = modes.indexOf(current);

            if (found + 1 >= modes.size()) {
                return null;
            }

            return modes.get(found + 1);
        }
    }

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
     * The global cache for fetched Yarn UI page by browser
     */
    private Cache globalCache = JobUtils.getGlobalCache();

    /**
     * The driver log conversion mode
     */
    @Nullable
    private DriverLogConversionMode driverLogConversionMode = null;

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
    protected String parseAmHostHttpAddressHost(@Nullable String driverHttpAddress) {
        if (driverHttpAddress == null) {
            return null;
        }

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

        throw new UnknownServiceException("Failed to get job Application ID: Unknown service error after " + --retries + " retries");
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

        throw new UnknownServiceException("Failed to get job Yarn application: Unknown service error after " + --retries + " retries");
    }

    /**
     * New RxAPI: Get current job application Id
     *
     * @return Application Id Observable
     */
    public Observable<String> getSparkJobApplicationIdObservable() {
        return Observable.create(ob -> {
            try {
                HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                        getConnectUri().toString(), getBatchId());

                if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                    SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                            httpResponse.getMessage(), SparkSubmitResponse.class)
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark job response: " + httpResponse.getMessage()));

                    if (jobResp.getAppId() != null) {
                        ob.onNext(jobResp.getAppId());
                    }
                }
            } catch (IOException ex) {
                log().warn("Got exception " + ex.toString());
                ob.onError(ex);
            } finally {
                ob.onCompleted();
            }
        });
    }

    /**
     * New RxAPI: Get the current Spark job Yarn application most recent attempt
     *
     * @return Yarn Application Attempt info Observable
     */
    public Observable<AppAttempt> getSparkJobYarnCurrentAppAttempt() {
        return getSparkJobApplicationIdObservable()
                .flatMap(appId -> {
                    URI getYarnAppAttemptsURI = getConnectUri().resolve("/yarnui/ws/v1/cluster/apps/" + appId +
                                                                        "/appattempts");

                    try {
                        HttpResponse httpResponse = this.getSubmission()
                                .getHttpResponseViaGet(getYarnAppAttemptsURI.toString());

                        if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                            Optional<AppAttemptsResponse> appResponse = ObjectConvertUtils.convertJsonToObject(
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
                    } catch (IOException ex) {
                        log().warn("Got exception " + ex.toString());
                        throw propagate(ex);
                    }

                    return Observable.empty();
                });
    }

    /**
     * New RxAPI: Get the current Spark job Yarn application attempt containers
     *
     * @return The string pair Observable of Host and Container Id
     */
    public Observable<SimpleImmutableEntry<URI, String>> getSparkJobYarnContainersObservable(@NotNull AppAttempt appAttempt) {
        return loadPageByBrowserObservable(getConnectUri().resolve("/yarnui/hn/cluster/appattempt/")
                                                          .resolve(appAttempt.getAppAttemptId()).toString())
                .retry(getRetriesMax())
                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                .filter(this::isSparkJobYarnAppAttemptNotJustLaunched)
                .map(htmlPage -> htmlPage.getFirstByXPath("//*[@id=\"containers\"]/tbody")) // Get the container table by XPath
                .filter(Objects::nonNull)       // May get null in the last step
                .map(HtmlTableBody.class::cast)
                .map(HtmlTableBody::getRows)    // To container rows
                .buffer(2, 1)
                // Wait for last two refreshes getting the same rows count, which means the yarn application
                // launching containers finished
                .takeUntil(buf -> buf.size() == 2 && buf.get(0).size() == buf.get(1).size())
                .filter(buf -> buf.size() == 2 && buf.get(0).size() == buf.get(1).size())
                .map(buf -> buf.get(1))
                .flatMap(Observable::from)  // From rows to row one by one
                .filter(containerRow -> {
                    try {
                        // Read container URL from YarnUI page
                        String urlFromPage = ((HtmlAnchor) containerRow.getCell(3).getFirstChild()).getHrefAttribute();
                        URI containerUri = getConnectUri().resolve(urlFromPage);

                        return loadPageByBrowserObservable(containerUri.toString())
                                .map(this::isSparkJobYarnContainerLogAvailable)
                                .toBlocking()
                                .singleOrDefault(false);
                    } catch (Exception ignore) {
                        return false;
                    }
                })
                .map(row -> {
                    URI hostUrl = URI.create(row.getCell(1).getTextContent().trim());
                    String containerId = row.getCell(0).getTextContent().trim();

                    return new SimpleImmutableEntry<>(hostUrl, containerId);
                });
    }

    /*
     * Parsing the Application Attempt HTML page to determine if the attempt is running
     */
    private Boolean isSparkJobYarnAppAttemptNotJustLaunched(@NotNull HtmlPage htmlPage) {
        // Get the info table by XPath
        @Nullable
        HtmlTableBody infoBody = htmlPage.getFirstByXPath("//*[@class=\"info\"]/tbody");

        if (infoBody == null) {
            return false;
        }

        return infoBody
                .getRows()
                .stream()
                .filter(row -> row.getCells().size() >= 2)
                .filter(row -> row.getCell(0)
                                  .getTextContent()
                                  .trim()
                                  .toLowerCase()
                                  .equals("application attempt state:"))
                .map(row -> !row.getCell(1)
                                .getTextContent()
                                .trim()
                                .toLowerCase()
                                .equals("launched"))
                .findFirst()
                .orElse(false);
    }

    private Boolean isSparkJobYarnContainerLogAvailable(@NotNull HtmlPage htmlPage) {
        Optional<DomElement> firstContent = Optional.ofNullable(
                htmlPage.getFirstByXPath("//*[@id=\"layout\"]/tbody/tr/td[2]"));

        return firstContent.map(DomElement::getTextContent)
                           .map(line -> !line.trim()
                                            .toLowerCase()
                                            .contains("no logs available"))
                           .orElse(false);
    }

    private Observable<HtmlPage> loadPageByBrowserObservable(String url) {
        final WebClient HTTP_WEB_CLIENT = new WebClient(BrowserVersion.CHROME);
        HTTP_WEB_CLIENT.setCache(globalCache);

        if (getSubmission().getCredentialsProvider() != null) {
            HTTP_WEB_CLIENT.setCredentialsProvider(getSubmission().getCredentialsProvider());
        }

        return Observable.create(ob -> {
            try {
                ob.onNext(HTTP_WEB_CLIENT.getPage(url));
            } catch (ScriptException ignored) {
                log().debug("get Spark job Yarn attempts detail browser rendering Error", ignored);
            } catch (IOException e) {
                ob.onError(e);
            } finally {
                ob.onCompleted();
            }
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

        throw new UnknownServiceException("Failed to get job driver log URL: Unknown service error after " + --retries + " retries");
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
        // Those lines are carried per response,
        // if there is no value followed, the line should not be sent to console
        final Set<String> ignoredEmptyLines = new HashSet<>(Arrays.asList(
                "stdout:",
                "stderr:",
                "yarn diagnostics:"));

        return Observable.create(ob -> {
            try {
                int start = 0;
                final int maxLinesPerGet = 128;
                int linesGot;
                boolean isSubmitting = true;

                while (isSubmitting) {
                    Boolean isAppIdAllocated = !this.getSparkJobApplicationIdObservable().isEmpty().toBlocking().lastOrDefault(true);
                    String logUrl = String.format("%s/%d/log?from=%d&size=%d",
                            this.getConnectUri().toString(), batchId, start, maxLinesPerGet);

                    HttpResponse httpResponse = this.getSubmission().getHttpResponseViaGet(logUrl);

                    SparkJobLog sparkJobLog = ObjectConvertUtils.convertJsonToObject(httpResponse.getMessage(),
                            SparkJobLog.class)
                            .orElseThrow(() -> new UnknownServiceException(
                                    "Bad spark log response: " + httpResponse.getMessage()));

                    // To subscriber
                    sparkJobLog.getLog().stream()
                            .filter(line -> !ignoredEmptyLines.contains(line.trim().toLowerCase()))
                            .forEach(line -> ob.onNext(new SimpleImmutableEntry<>(Log, line)));

                    linesGot = sparkJobLog.getLog().size();
                    start += linesGot;

                    // Retry interval
                    if (linesGot == 0) {
                        isSubmitting = this.getState().equals("starting") && !isAppIdAllocated;

                        sleep(TimeUnit.SECONDS.toMillis(this.getDelaySeconds()));
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

        throw new UnknownServiceException("Failed to detect job activity: Unknown service error after " + --retries + " retries");
    }

    public Observable<SimpleImmutableEntry<SparkBatchJobState, String>> getJobDoneObservable() {
        return Observable.create((Subscriber<? super SimpleImmutableEntry<SparkBatchJobState, String>> ob) -> {
            try {
                boolean isJobActive;
                SparkBatchJobState state = SparkBatchJobState.NOT_STARTED;
                String diagnostics = "";

                do {
                    HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
                            this.getConnectUri().toString(), batchId);

                    if (httpResponse.getCode() >= 200 && httpResponse.getCode() < 300) {
                        SparkSubmitResponse jobResp = ObjectConvertUtils.convertJsonToObject(
                                httpResponse.getMessage(), SparkSubmitResponse.class)
                                .orElseThrow(() -> new UnknownServiceException(
                                        "Bad spark job response: " + httpResponse.getMessage()));

                        state = SparkBatchJobState.valueOf(jobResp.getState().toUpperCase());
                        diagnostics = String.join("\n", jobResp.getLog());

                        isJobActive = !state.isJobDone();
                    } else {
                        isJobActive = false;
                    }


                    // Retry interval
                    sleep(1000);
                } while (isJobActive);

                ob.onNext(new SimpleImmutableEntry<>(state, diagnostics));
                ob.onCompleted();
            } catch (IOException ex) {
                ob.onError(ex);
            } catch (InterruptedException ignored) {
                ob.onCompleted();
            }
        });
    }

    public Observable<String> getJobLogAggregationDoneObservable() {
        return getSparkJobApplicationIdObservable()
                .flatMap(applicationId ->
                        Observable.fromCallable(() -> getSparkJobYarnApplication(this.getConnectUri(), applicationId))
                                .repeatWhen(ob -> ob.delay(getDelaySeconds(), TimeUnit.SECONDS))
                                .takeUntil(this::isYarnAppLogAggregationDone)
                                .filter(this::isYarnAppLogAggregationDone))
                .map(yarnApp -> yarnApp.getLogAggregationStatus().toUpperCase());
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
    public Observable<String> getSparkJobDriverLogUrlObservable() {
        return getSparkJobYarnCurrentAppAttempt()
                .map(AppAttempt::getLogsLink)
                .map(URI::create)
                .filter(uri -> StringUtils.isNotEmpty(uri.getHost()))
                .flatMap(this::convertToPublicLogUri)
                .map(URI::toString);
    }

    public boolean isUriValid(@NotNull URI uriProbe) throws IOException {
        return getSubmission().getHttpResponseViaGet(uriProbe.toString()).getCode() < 300;
    }

    private Optional<URI> convertToPublicLogUri(@Nullable DriverLogConversionMode mode, @NotNull URI internalLogUrl) {
        String normalizedPath = Optional.of(internalLogUrl.getPath()).filter(StringUtils::isNotBlank).orElse("/");

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
                        Optional<URI> uri = convertToPublicLogUri(probeMode, internalLogUri)
                                .filter(uriProbe -> {
                                    try {
                                        return isUriValid(uriProbe);
                                    } catch (IOException e) {
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

    /**
     * New RxAPI: Submit the job
     *
     * @return Spark Job observable
     */
    public Observable<SparkBatchJob> submit() {
        return Observable.fromCallable(() -> {
                Optional<IClusterDetail> cluster = ClusterManagerEx.getInstance()
                        .getClusterDetailByName(getSubmissionParameter().getClusterName());

                if (cluster.isPresent()) {
                    SparkBatchSubmission.getInstance()
                            .setCredentialsProvider(cluster.get().getHttpUserName(), cluster.get().getHttpPassword());

                    return (SparkBatchJob) createBatchJob();
                }

                throw new SparkJobException("Can't get cluster " + getSubmissionParameter().getClusterName() + " to submit.");
        });
    }

    /**
     * New RxAPI: Get the job status (from livy)
     *
     * @return Spark Job observable
     */
    @NotNull
    public Observable<SparkSubmitResponse> getStatus() {
        return Observable.fromCallable(() -> {
            HttpResponse httpResponse = this.getSubmission().getBatchSparkJobStatus(
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
}
