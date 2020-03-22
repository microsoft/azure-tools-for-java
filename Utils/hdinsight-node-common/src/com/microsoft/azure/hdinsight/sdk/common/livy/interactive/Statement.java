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
package com.microsoft.azure.hdinsight.sdk.common.livy.interactive;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.SessionNotStartException;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.StatementNotStartException;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.StatementOutput;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.StatementState;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api.session.PostStatements;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.StringEntity;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class Statement implements ILogger {
    public static final String REST_SEGMENT_STATEMENTS = "statements";

    @NotNull
    private Session session;            // Statement owner session

    private int id = -1;                // Statement ID of server

    @Nullable
    private StatementOutput output;     // Statement outputs

    @Nullable
    private InputStream codeStream;     // Codes to run

    @Nullable
    private StatementState lastState;   // Last statement state gotten

    public Statement(@NotNull Session session, int id) {
        this(session, null);

        this.id = id;
    }

    public Statement(@NotNull Session session, @Nullable InputStream codeStream) {
        this.session = session;
        this.codeStream = codeStream;
    }

    /*
     * Getter / Setter
     */

    @Nullable
    public InputStream getCodeInputStream() {
        return codeStream;
    }

    @Nullable
    public StatementState getLastState() {
        return lastState;
    }

    public void setLastState(@Nullable StatementState lastState) {
        this.lastState = lastState;
    }

    @NotNull
    public URI getUri() throws StatementNotStartException, SessionNotStartException {
        return URI.create(getSession().getUri().toString() + "/" + REST_SEGMENT_STATEMENTS + "/" + String.valueOf(getId()));
    }

    public int getId() throws StatementNotStartException {
        if (id < 0) {
            throw new StatementNotStartException("The statement isn't created. Call run() or get() firstly before getting ID.");
        }

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public Session getSession() {
        return this.session;
    }

    @NotNull
    public HttpObservable getHttp() {
        return getSession().getHttp();
    }

    @Nullable
    public String getUserAgent() {
        return getSession().getUserAgent();
    }

    @Nullable
    public StatementOutput getOutput() {
        return output;
    }

    private void setOutput(@Nullable StatementOutput output) {
        this.output = output;
    }

    /*
     * Observable APIs, all IO operations
     */

    public Observable<StatementOutput> run() {
        return runStatementRequest()
                .map(this::updateWithResponse)
                .flatMap(statement -> statement.get()                    // Get statement result
                        .repeatWhen(ob -> ob.delay(1, TimeUnit.SECONDS)) // The unmet state won't trigger retries,
                                                                         // which is handled by repeatWhen()
                        .takeUntil(Statement::isDone)
                        .filter(Statement::isDone)
                )
                .map(Statement::getOutput);
    }

    public boolean isDoneWithError()
    {
        return getLastState() == StatementState.ERROR || getLastState() == StatementState.CANCELLED;
    }

    public boolean isDoneWithSuccess() {
        return getLastState() == StatementState.AVAILABLE;
    }

    public boolean isDone() {
        return isDoneWithError() || isDoneWithSuccess();
    }

    private Statement updateWithResponse(com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Statement statementResp) {
        this.setId(statementResp.getId());
        this.setLastState(statementResp.getState());
        this.setOutput(statementResp.getOutput());

        return this;
    }

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Statement> runStatementRequest() {
        if (getCodeInputStream() == null) {
            assert false : "Shouldn't run statement without codes input stream";
            log().warn("Execute empty statement.");

            return Observable.empty();
        }

        URI uri;
        PostStatements postBody = new PostStatements();

        try {
            uri = URI.create(getSession().getUri().toString() + "/" + REST_SEGMENT_STATEMENTS);
            postBody.setCode(IOUtils.toString(getCodeInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Observable.error(e);
        }

        String json = postBody.convertToJson()
                .orElseThrow(() -> new IllegalArgumentException("Bad statement arguments to post."));

        StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);
        entity.setContentType("application/json");

        return getHttp()
                .setUserAgent(getUserAgent())
                .post(uri.toString(), entity, null, null, com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Statement.class);
    }

    public Observable<Statement> get() {
        return getStatementRequest()
                .map(this::updateWithResponse)
                .defaultIfEmpty(this);
    }

    private Observable<com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Statement> getStatementRequest() {
        URI uri;

        try {
            uri = getUri();
        } catch (StatementNotStartException | SessionNotStartException e) {
            return Observable.empty();
        }

        return getHttp()
                .setUserAgent(getUserAgent())
                .get(uri.toString(), null, null, com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Statement.class);
    }

    public Observable<Statement> cancel() {
        throw new UnsupportedOperationException();
    }
}
