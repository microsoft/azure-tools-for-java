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

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.ApplicationNotStartException;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.SessionNotStartException;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.StatementExecutionError;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.SessionKind;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.SessionState;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api.PostSessions;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.entity.StringEntity;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static rx.exceptions.Exceptions.propagate;

public abstract class Session implements AutoCloseable, Closeable, ILogger {
    private static final String REST_SEGMENT_SESSION = "sessions";

    @NotNull
    private URI baseUrl;            // Session base URL

    private int id;                 // Session ID of server

    @Nullable
    private String appId;           // Application ID of server

    @NotNull
    private HttpObservable http;    // Http connection

    @NotNull
    private String name;            // Session name

    @NotNull
    private SessionState lastState; // Last session state gotten

    @Nullable
    private List<String> lastLogs;  // Last session logs

    private int executorCores = 1;  // Default cores per executor to create

    private int executorNum = 2;    // Default executor count to create

    private int driverCores = 2;    // Default driver cores to create

    private String driverMemory = "4G";     // Default driver memory to create

    private String executorMemory = "4G";   // Default executor memory to create

    /*
     * Constructor
     */

    public Session(@NotNull String name, @NotNull URI baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.lastState = SessionState.NOT_STARTED;
        this.http = new HttpObservable();
    }

    public Session(@NotNull String name, @NotNull final URI baseUrl, @NotNull final String username, @NotNull final String password) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.lastState = SessionState.NOT_STARTED;
        this.http = new HttpObservable(username, password);
    }

    /*
     * Getter / Setter
     */
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public URI getBaseUrl() {
        return baseUrl;
    }

    @NotNull
    public URI getUri() {
        return baseUrl.resolve(REST_SEGMENT_SESSION + "/" + String.valueOf(getId()));
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setExecutorCores(int executorCores) {
        this.executorCores = executorCores;
    }

    public int getExecutorCores() {
        return executorCores;
    }

    public int getExecutorNum() {
        return executorNum;
    }

    public void setExecutorNum(int executorNum) {
        this.executorNum = executorNum;
    }

    public int getDriverCores() {
        return driverCores;
    }

    public void setDriverCores(int driverCores) {
        this.driverCores = driverCores;
    }

    @NotNull
    public String getDriverMemory() {
        return driverMemory;
    }

    public void setDriverMemory(@NotNull String driverMemory) {
        this.driverMemory = driverMemory;
    }

    @NotNull
    public String getExecutorMemory() {
        return executorMemory;
    }

    public void setExecutorMemory(@NotNull String executorMemory) {
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

    private void setAppId(@Nullable String appId) {
        this.appId = appId;
    }

    @NotNull
    public abstract SessionKind getKind();

    @NotNull
    public HttpObservable getHttp() {
        return http;
    }

    @NotNull
    public SessionState getLastState() {
        return lastState;
    }

    private void setLastState(@NotNull SessionState lastState) {
        this.lastState = lastState;
    }

    public void setLastLogs(@Nullable List<String> lastLogs) {
        this.lastLogs = lastLogs;
    }

    @NotNull
    public List<String> getLastLogs() {
        return lastLogs == null ? new ArrayList<>() : lastLogs;
    }

    /*
     * Overrides
     */
    @Override
    public void close() {
        kill().toBlocking().subscribe( session -> {}, err -> {
            log().warn("Kill session failed. " + ExceptionUtils.getStackTrace(err));
        });
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

    @NotNull
    public String getInstallationID() {
        if (HDInsightLoader.getHDInsightHelper() == null) {
            return "";
        }

        return HDInsightLoader.getHDInsightHelper().getInstallationId();
    }

    @Nullable
    public String getUserAgent() {
        String userAgentPrefix = getHttp().getUserAgentPrefix();
        String requestId = AppInsightsClient.getConfigurationSessionId() == null ?
                UUID.randomUUID().toString() :
                AppInsightsClient.getConfigurationSessionId();

        return String.format("%s %s", userAgentPrefix.trim(), requestId);
    }

    /*
     * Observable APIs, all IO operations
     */

    /**
     * To create a session with specified kind.
     *
     * @return An updated Session instance Observable
     */
    public Observable<Session> create() {
        return createSessionRequest()
                .map(this::updateWithResponse);
    }

    private Session updateWithResponse(com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session sessionResp) {
        this.setId(sessionResp.getId());
        this.setAppId(sessionResp.getAppId());
        this.setLastState(sessionResp.getState());
        this.setLastLogs(sessionResp.getLog());

        return this;
    }

    @NotNull
    private PostSessions preparePostSessions() {
        PostSessions postBody = new PostSessions();
        postBody.setName(getName());
        postBody.setKind(getKind());
        postBody.setExecutorCores(getExecutorCores());
        postBody.setNumExecutors(getExecutorNum());
        postBody.setDriverCores(getDriverCores());
        postBody.setDriverMemory(getDriverMemory());
        postBody.setExecutorMemory(getExecutorMemory());

        // In Livy 0.5.0-incubating, we need to either specify code kind in post statement
        // or set it in post session body here, or else "Code type should be specified if session kind is shared" error
        // will be met in the response of the post statement request
        postBody.setConf(ImmutableMap.of("spark.__livy__.livy.rsc.session.kind", getKind().toString().toLowerCase()));

        return postBody;
    }

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session> createSessionRequest() {
        URI uri = baseUrl.resolve(REST_SEGMENT_SESSION);

        PostSessions postBody = preparePostSessions();
        String json = postBody.convertToJson()
                .orElseThrow(() -> new IllegalArgumentException("Bad session arguments to post."));

        StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);
        entity.setContentType("application/json");

        return getHttp()
                .setUserAgent(getUserAgent())
                .post(uri.toString(), entity, null, null, com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session.class);
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
        URI uri = getUri();

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
        URI uri = getUri();

        return getHttp()
                .setUserAgent(getUserAgent())
                .get(uri.toString(), null, null, com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session.class);
    }

    public Observable<Map<String, String>> runStatement(@NotNull Statement statement) {
        return awaitReady()
            .flatMap(session -> statement
                    .run()
                    .map(result -> {
                        if (!result.getStatus().toLowerCase().equals("ok")) {
                            throw propagate(new StatementExecutionError(
                                    result.getEname(), result.getEvalue(), result.getTraceback()));
                        }

                        return result.getData();
                    }));
    }

    public Observable<Session> awaitReady(@Nullable Scheduler scheduler) {
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
                        String exceptionMessage = StringUtils.join(sesLogsPair.right).equals(StringUtils.join(currentLogs)) ?
                                StringUtils.join(currentLogs, " ; ") :
                                StringUtils.join(Stream.of(sesLogsPair.right, currentLogs)
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

    public Observable<Map<String, String>> runCodes(@NotNull String codes) {
        return runStatement(new Statement(this, new ByteArrayInputStream(codes.getBytes(StandardCharsets.UTF_8))));
    }

    public Observable<String> getLog() {
        throw new UnsupportedOperationException();
    }
}
