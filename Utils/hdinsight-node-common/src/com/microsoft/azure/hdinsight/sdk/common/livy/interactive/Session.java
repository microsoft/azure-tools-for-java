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

package com.microsoft.azure.hdinsight.sdk.common.livy.interactive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.ApplicationNotStartException;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.SessionNotStartException;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.StatementExecutionError;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.SessionKind;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.SessionState;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api.PostSessions;
import com.microsoft.azure.hdinsight.spark.common.Deployable;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.entity.StringEntity;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Debug;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.Info;
import static java.lang.Thread.sleep;
import static rx.exceptions.Exceptions.propagate;

public abstract class Session implements AutoCloseable, Closeable, ILogger {
    private static final String REST_SEGMENT_SESSION = "sessions";

    // @NotNull annotation is removed since Not Null is by default,
    // refer to https://checkerframework.org/manual/#null-defaults
    private final URI baseUrl;            // Session base URL

    private int id;                 // Session ID of server

    @Nullable
    private String appId = null;           // Application ID of server

    private final HttpObservable http;    // Http connection

    private final String name;            // Session name

    private SessionState lastState; // Last session state gotten

    private List<String> lastLogs = Collections.emptyList();  // Last session logs

    @Nullable
    private Deployable deploy = null;      // Deploy delegate

    private final List<String> artifactsToDeploy = new ArrayList<>(); // Artifacts to deploy

    private ImmutableList<String> uploadedArtifactsUris = ImmutableList.of();   // To store uploaded artifacts URI

    private int executorCores = 1;  // Default cores per executor to create

    private int executorNum = 2;    // Default executor count to create

    private int driverCores = 2;    // Default driver cores to create

    private String driverMemory = "4G";     // Default driver memory to create

    private String executorMemory = "4G";   // Default executor memory to create

    private List<String> files = new ArrayList<>(); // Files for reference

    private List<String> jars = new ArrayList<>(); // Jars for reference

    private Map<? extends String, ? extends String> conf = new HashMap<>(); // Job Configs

    final private PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject;

    /*
     * Constructor
     */

    public Session(final String name, final URI baseUrl) {
        this(name, baseUrl, null, null);
    }

    /**
     * Create a Livy session instance.
     *
     * @param name the session name which will be found in resource manager, such as Yarn
     * @param baseUrl the connect URL of Livy, also the parent URL of submitting POST Livy session request,
     *                ending with '/'
     * @param username the username of Basic Authentication, leave NULL for other authentication methods
     * @param password the password of Basic Authentication, leave NULL for other authentication methods
     */
    public Session(final String name,
                   final URI baseUrl,
                   final @Nullable String username,
                   final @Nullable String password) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.lastState = SessionState.NOT_STARTED;

        if (username == null || password == null) {
            this.http = new HttpObservable();
        } else {
            this.http = new HttpObservable(username, password);
        }

        this.ctrlSubject = PublishSubject.create();
    }

    /*
     * Getter / Setter
     */
    public String getName() {
        return name;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public URI getUri() {
        return baseUrl.resolve(REST_SEGMENT_SESSION + "/" + getId());
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setExecutorCores(final int executorCores) {
        this.executorCores = executorCores;
    }

    public int getExecutorCores() {
        return executorCores;
    }

    public int getExecutorNum() {
        return executorNum;
    }

    public void setExecutorNum(final int executorNum) {
        this.executorNum = executorNum;
    }

    public int getDriverCores() {
        return driverCores;
    }

    public void setDriverCores(final int driverCores) {
        this.driverCores = driverCores;
    }

    public String getDriverMemory() {
        return driverMemory;
    }

    public void setDriverMemory(final String driverMemory) {
        this.driverMemory = driverMemory;
    }

    public String getExecutorMemory() {
        return executorMemory;
    }

    public void setExecutorMemory(final String executorMemory) {
        this.executorMemory = executorMemory;
    }

    public Observable<String> getAppId() {
        return appId != null ?
                Observable.just(appId) :
                this.get()
                    .repeatWhen(ob -> ob.delay(1, TimeUnit.SECONDS))
                    .takeUntil(session -> session.appId != null)
                    .filter(session -> session.appId != null)
                    .timeout(3, TimeUnit.MINUTES)
                    .map(session -> {
                        if (session.appId == null || session.appId.isEmpty()) {
                            throw propagate(new ApplicationNotStartException(
                                    getName() + " application isn't started in 3 minutes."));
                        }

                        return session.appId;
                    });
    }

    private void setAppId(final @Nullable String appId) {
        this.appId = appId;
    }

    public abstract SessionKind getKind();

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(final List<String> files) {
        this.files = files;
    }

    public void addFiles(final String... files) {
        this.files.addAll(Arrays.asList(files));
    }

    public List<String> getJars() {
        return jars;
    }

    public void setJars(final List<String> jars) {
        this.jars = jars;
    }

    public void addJars(final String... jars) {
        this.jars.addAll(Arrays.asList(jars));
    }

    public Map<? extends String, ? extends String> getConf() {
        return conf;
    }

    public void setConf(final Map<? extends String, ? extends String> conf) {
        this.conf = conf;
    }

    @Nullable
    public Deployable getDeploy() {
        return deploy;
    }

    public void setDeploy(final Deployable deploy) {
        this.deploy = deploy;
    }

    public List<String> getArtifactsToDeploy() {
        return artifactsToDeploy;
    }

    public HttpObservable getHttp() {
        return http;
    }

    public SessionState getLastState() {
        return lastState;
    }

    private void setLastState(final SessionState lastState) {
        this.lastState = lastState;
    }

    public void setLastLogs(final List<String> lastLogs) {
        this.lastLogs = lastLogs;
    }

    public List<String> getLastLogs() {
        return lastLogs;
    }

    public PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> getCtrlSubject() {
        return ctrlSubject;
    }

    /*
     * Overrides
     */
    @Override
    public void close() {
        kill().toBlocking().subscribe( session -> {}, err -> {
            log().warn("Kill session failed. " + ExceptionUtils.getStackTrace(err));
        });

        this.ctrlSubject.onCompleted();
    }

    /*
     * Helper APIs
     */
    public boolean isStarted() {
        return getLastState() != SessionState.STARTING &&
                getLastState() != SessionState.NOT_STARTED;
    }

    public boolean isStop() {
        return getLastState() == SessionState.SHUTTING_DOWN ||
                getLastState() == SessionState.DEAD ||
                getLastState() == SessionState.KILLED ||
                getLastState() == SessionState.ERROR ||
                getLastState() == SessionState.SUCCESS;
    }

    public boolean isStatementRunnable() {
        return getLastState() == SessionState.IDLE ||
                getLastState() == SessionState.BUSY;
    }

    public String getInstallationID() {
        if (HDInsightLoader.getHDInsightHelper() == null) {
            return "";
        }

        return HDInsightLoader.getHDInsightHelper().getInstallationId();
    }

    @Nullable
    public String getUserAgent() {
        final String userAgentPrefix = getHttp().getUserAgentPrefix();
        final String requestId = AppInsightsClient.getConfigurationSessionId() == null ?
                UUID.randomUUID().toString() :
                AppInsightsClient.getConfigurationSessionId();

        return String.format("%s %s", userAgentPrefix.trim(), requestId);
    }

    /*
     * Observable APIs, all IO operations
     */

    public Observable<Session> deploy() {
        final Deployable deployDelegate = getDeploy();

        if (deployDelegate == null) {
            return Observable.just(this);
        }

        return Observable.from(getArtifactsToDeploy())
                .doOnNext(artifactPath -> ctrlSubject.onNext(new SimpleImmutableEntry<>(
                        Info, "Start uploading artifact " + artifactPath)))
                .flatMap(artifactPath -> deployDelegate.deploy(new File(artifactPath), ctrlSubject))
                .doOnNext(uri -> ctrlSubject.onNext(new SimpleImmutableEntry<>(Info, "Uploaded to " + uri)) )
                .toList()
                .map(uploadedUris -> {
                    this.uploadedArtifactsUris = ImmutableList.copyOf(uploadedUris);

                    return this;
                })
                .defaultIfEmpty(this);
    }

    /**
     * To create a session with specified kind.
     *
     * @return An updated Session instance Observable
     */
    public Observable<Session> create() {
        return createSessionRequest()
                .map(this::updateWithResponse);
    }

    private Session updateWithResponse(
            final com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session sessionResp) {
        this.setId(sessionResp.getId());
        this.setAppId(sessionResp.getAppId());
        this.setLastState(sessionResp.getState());
        this.setLastLogs(sessionResp.getLog());

        return this;
    }

    private PostSessions preparePostSessions() {
        final PostSessions postBody = new PostSessions();
        postBody.setName(getName());
        postBody.setKind(getKind());
        postBody.setExecutorCores(getExecutorCores());
        postBody.setNumExecutors(getExecutorNum());
        postBody.setDriverCores(getDriverCores());
        postBody.setDriverMemory(getDriverMemory());
        postBody.setExecutorMemory(getExecutorMemory());
        postBody.setFiles(getFiles());
        postBody.setJars(new ImmutableList.Builder<String>()
                .addAll(this.uploadedArtifactsUris)
                .addAll(getJars())
                .build());

        // In Livy 0.5.0-incubating, we need to either specify code kind in post statement
        // or set it in post session body here, or else "Code type should be specified if session kind is shared" error
        // will be met in the response of the post statement request
        postBody.setConf(new ImmutableMap.Builder<String, String>()
                .putAll(getConf())
                .put("spark.__livy__.livy.rsc.session.kind", getKind().toString().toLowerCase())
                .build());

        return postBody;
    }

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session> createSessionRequest() {
        final URI uri = baseUrl.resolve(REST_SEGMENT_SESSION);

        final PostSessions postBody = preparePostSessions();
        final String json = postBody.convertToJson()
                .orElseThrow(() -> new IllegalArgumentException("Bad session arguments to post."));

        getCtrlSubject().onNext(new SimpleImmutableEntry<>(Debug,
                "Create Livy Session by sending request to " + uri + " with body " + json));

        final StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);
        entity.setContentType("application/json");

        return getHttp()
                .setUserAgent(getUserAgent())
                .post(uri.toString(),
                      entity,
                      null,
                      null,
                      com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session.class);
    }

    /**
     * To kill a session, if it's opened, cancel all running statements and close it, otherwise, do nothing
     *
     * @return an updated Session instance Observable
     */
    public Observable<Session> kill() {
        return deleteSessionRequest()
                .map(resp -> {
                    lastState = SessionState.SHUTTING_DOWN;
                    return this;
                })
                .defaultIfEmpty(this);
    }

    private Observable<HttpResponse> deleteSessionRequest() {
        final URI uri = getUri();

        return getHttp()
                .setUserAgent(getUserAgent())
                .delete(uri.toString(), null, null);
    }

    /**
     * To get a session status.
     *
     * @return an updated Session instance Observable
     */
    public Observable<Session> get() {
        return getSessionRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session> getSessionRequest() {
        final URI uri = getUri();

        return getHttp()
                .setUserAgent(getUserAgent())
                .get(uri.toString(), null, null, com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session.class);
    }

    public Observable<Map<String, String>> runStatement(final Statement statement) {
        return awaitReady()
            .flatMap(session -> statement
                    .run()
                    .map(result -> {
                        if (!"ok".equalsIgnoreCase(result.getStatus())) {
                            throw propagate(new StatementExecutionError(
                                    result.getEname(), result.getEvalue(), result.getTraceback()));
                        }

                        return result.getData();
                    }));
    }

    public Observable<Session> awaitReady(final @Nullable Scheduler scheduler) {
        return get()
                .repeatWhen(ob -> scheduler != null ?
                                // Use specified scheduler to delay
                                ob.doOnNext(any -> { try { sleep(1000); } catch (InterruptedException ignored) { } }) :
                                // Use the default delay scheduler if scheduler not specified
                                ob.delay(1, TimeUnit.SECONDS),
                            scheduler != null ? scheduler : Schedulers.trampoline())
                .takeUntil(Session::isStatementRunnable)
                .reduce(new ImmutablePair<>(this, getLastLogs()), (sesLogsPair, ses) -> {
                    List<String> currentLogs = ses.getLastLogs();

                    if (ses.isStop()) {
                        String exceptionMessage = StringUtils.join(sesLogsPair.right)
                                                        .equals(StringUtils.join(currentLogs))
                                ? StringUtils.join(currentLogs, " ; ")
                                : StringUtils.join(Stream.of(sesLogsPair.right, currentLogs)
                                                       .flatMap(Collection::stream)
                                                       .collect(Collectors.toList()),
                                                 " ; ");

                        throw propagate(new SessionNotStartException(
                                "Session " + getName() + " is " + getLastState() + ". " + exceptionMessage));
                    }

                    return new ImmutablePair<>(ses, currentLogs);
                })
                .map(ImmutablePair::getLeft)
                .filter(Session::isStatementRunnable);
    }

    public Observable<Session> awaitReady() {
        return awaitReady(null);
    }

    public Observable<Map<String, String>> runCodes(final String codes) {
        return runStatement(new Statement(this, new ByteArrayInputStream(codes.getBytes(StandardCharsets.UTF_8))));
    }

    public Observable<String> getLog() {
        throw new UnsupportedOperationException();
    }
}
