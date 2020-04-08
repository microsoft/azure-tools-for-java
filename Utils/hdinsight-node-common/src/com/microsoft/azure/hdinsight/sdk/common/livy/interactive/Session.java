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
import com.microsoft.azure.hdinsight.sdk.common.livy.MemorySize;
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

    public static class CreateParameters {
        public static final String DRIVER_MEMORY = "driverMemory";
        public static final String DRIVER_MEMORY_DEFAULT_VALUE = "4G";

        public static final String DRIVER_CORES = "driverCores";
        public static final int DRIVER_CORES_DEFAULT_VALUE = 2;

        public static final String EXECUTOR_MEMORY = "executorMemory";
        public static final String EXECUTOR_MEMORY_DEFAULT_VALUE = "4G";

        public static final String NUM_EXECUTORS = "numExecutors";
        public static final int NUM_EXECUTORS_DEFAULT_VALUE = 2;

        public static final String EXECUTOR_CORES = "executorCores";
        public static final int EXECUTOR_CORES_DEFAULT_VALUE = 1;

        private static final List<String> jobConfigKeyBlackList = Arrays.asList(
                DRIVER_MEMORY, DRIVER_CORES, EXECUTOR_MEMORY, NUM_EXECUTORS, EXECUTOR_CORES);

        private @Nullable String name = null;

        private final SessionKind kind;

        private @Nullable String proxyUser = null;

        private final List<String> referenceFiles = new ArrayList<>();

        private final List<String> referencedJars = new ArrayList<>();

        private final List<String> archives = new ArrayList<>();

        private final List<String> pyFiles = new ArrayList<>();

        private final Map<String, String> jobConfig = new HashMap<>();

        private @Nullable String yarnQueue = null;

        private final List<String> uploadedArtifactsUris = new ArrayList<>();

        public CreateParameters(final SessionKind kind) {
            this.kind = kind;
        }

        /**
         * Set Spark session name.
         *
         * @param sessionName session name to set
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters name(final String sessionName) {
            this.name = sessionName;

            return this;
        }

        /**
         * Set Spark session proxy user.
         *
         * @param user proxy user name to set
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters proxyUser(final String user) {
            this.proxyUser = user;

            return this;
        }

        /**
         * Set Spark session reference artifactUrls.
         *
         * @param artifactUrls the artifactUrls for session references
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters uploadedArtifactUrls(final String... artifactUrls) {
            this.uploadedArtifactsUris.addAll(Arrays.asList(artifactUrls));

            return this;
        }

        /**
         * Set Spark session reference files.
         *
         * @param files session referece files to set into option
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters referFiles(final String... files) {
            Collections.addAll(this.referenceFiles, files);

            return this;
        }

        /**
         * Set Spark session reference Jar files.
         *
         * @param jars session reference Jar files to set into option
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters referJars(final String... jars) {
            Collections.addAll(this.referencedJars, jars);

            return this;
        }

        /**
         * Set Spark session archives.
         *
         * @param archives session archives to set into option
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters archives(final String... archives) {
            Collections.addAll(this.archives, archives);

            return this;
        }

        /**
         * Set Spark session configuration.
         *
         * @param key key for Spark configuration to set into option
         * @param value value for Spark configuration to set into option
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters conf(final String key, final String value) {
            jobConfig.put(key, value);

            return this;
        }

        /**
         * Set Spark session configuration.
         *
         * @param kvPairs key-value pairs for Spark configuration to set into option
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters conf(final Iterable<? extends SimpleImmutableEntry<String, String>> kvPairs) {
            for (final SimpleImmutableEntry<String, String> kv : kvPairs) {
                jobConfig.put(kv.getKey(), kv.getValue());
            }

            return this;
        }

        /**
         * Set Spark session Yarn queue.
         *
         * @param yarnQueue the Yarn queue for Spark session to schedule
         * @return current {@link CreateParameters} instance for fluent calling
         */
        public CreateParameters setYarnQueue(final String yarnQueue) {
            this.yarnQueue = yarnQueue;

            return this;
        }

        /**
         * Build POST request body for Spark session.
         * @return current {@link PostSessions} instance Post body
         */
        public PostSessions build() {
            final PostSessions postBody = new PostSessions();

            if (StringUtils.isNoneBlank(this.name)) {
                postBody.setName(this.name);
            }

            postBody.setKind(this.kind);

            if (StringUtils.isNoneBlank(this.proxyUser)) {
                postBody.setProxyUser(this.proxyUser);
            }

            postBody.setExecutorCores(Integer.parseInt(jobConfig.getOrDefault(EXECUTOR_CORES,
                    Integer.toString(EXECUTOR_CORES_DEFAULT_VALUE))));
            postBody.setNumExecutors(Integer.parseInt(jobConfig.getOrDefault(NUM_EXECUTORS,
                    Integer.toString(NUM_EXECUTORS_DEFAULT_VALUE))));
            postBody.setDriverCores(Integer.parseInt(jobConfig.getOrDefault(DRIVER_CORES,
                    Integer.toString(DRIVER_CORES_DEFAULT_VALUE))));
            postBody.setDriverMemory(new MemorySize(jobConfig.getOrDefault(DRIVER_MEMORY,
                    DRIVER_MEMORY_DEFAULT_VALUE)).toString());
            postBody.setExecutorMemory(new MemorySize(jobConfig.getOrDefault(EXECUTOR_MEMORY,
                    EXECUTOR_MEMORY_DEFAULT_VALUE)).toString());

            if (!this.referenceFiles.isEmpty()) {
                postBody.setFiles(this.referenceFiles);
            }

            if (!this.archives.isEmpty()) {
                postBody.setArchives(this.archives);
            }

            final List<String> jars = new ImmutableList.Builder<String>()
                    .addAll(this.uploadedArtifactsUris)
                    .addAll(this.referencedJars)
                    .build();

            if (!jars.isEmpty()) {
                postBody.setJars(jars);
            }

            if (StringUtils.isNoneBlank(this.yarnQueue)) {
                postBody.setQueue(yarnQueue);
            }

            final ImmutableMap.Builder<String, String> confBuilder = new ImmutableMap.Builder<>();

            // Put only spark or yarn configurations
            this.jobConfig.entrySet().stream()
                    .filter(entry -> !jobConfigKeyBlackList.contains(entry.getKey()))
                    .forEach(confBuilder::put);

            // In Livy 0.5.0-incubating, we have to either specify code kind in post statement
            // or set it in post session body here, or else "Code type should be specified if session kind is shared"
            // error will be met in the response of the post statement request
            confBuilder.put("spark.__livy__.livy.rsc.session.kind", this.kind.toString().toLowerCase());

            final Map<String, String> configs = confBuilder.build();

            if (!configs.isEmpty()) {
                postBody.setConf(configs);
            }

            return postBody;
        }
    }

    private final CreateParameters createParameters;

    final private PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject;

    /*
     * Constructor
     */

    /**
     * Create a Livy session instance.
     *
     * @param name the session name which will be found in resource manager, such as Yarn
     * @param baseUrl the connect URL of Livy, also the parent URL of submitting POST Livy session request,
     *                ending with '/'
     * @param createParameters the session options for creation
     * @param username the username of Basic Authentication, leave NULL for other authentication methods
     * @param password the password of Basic Authentication, leave NULL for other authentication methods
     */
    public Session(final String name,
                   final URI baseUrl,
                   final CreateParameters createParameters,
                   final @Nullable String username,
                   final @Nullable String password) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.lastState = SessionState.NOT_STARTED;
        this.createParameters = createParameters;

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

    public CreateParameters getCreateParameters() {
        return createParameters;
    }

    @Nullable
    public Deployable getDeploy() {
        return deploy;
    }

    public void setDeploy(final @Nullable Deployable deploy) {
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
                    this.createParameters.uploadedArtifactsUris.addAll(uploadedUris);

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

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session> createSessionRequest() {
        final URI uri = baseUrl.resolve(REST_SEGMENT_SESSION);

        final PostSessions postBody = getCreateParameters().build();
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
