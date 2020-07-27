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

import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.AppAttempt;
import com.microsoft.azuretools.utils.Pair;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import rx.Observable;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SparkBatchRemoteDebugJobScenario {
    private SparkBatchSubmission submissionMock;
    private Throwable caught;
    private ArgumentCaptor<SparkSubmissionParameter> submissionParameterArgumentCaptor;
    private MockHttpService httpServerMock;
    private SparkBatchRemoteDebugJob debugJobMock;
    private Logger loggerMock;
    private SparkSubmissionParameter debugSubmissionParameter;

    @Before
    public void setUp() throws Throwable {
        submissionParameterArgumentCaptor = ArgumentCaptor.forClass(SparkSubmissionParameter.class);
        submissionMock = mock(SparkBatchSubmission.class);
        when(submissionMock.getBatchSparkJobStatus(anyString(), anyInt())).thenCallRealMethod();
        when(submissionMock.getHttpResponseViaGet(anyString())).thenCallRealMethod();
        when(submissionMock.getHttpResponseViaGet(anyString(), any(), any())).thenCallRealMethod();
        when(submissionMock.getHttpClientWithoutCredentialAndRedirect()).thenCallRealMethod();
        when(submissionMock.getHttpClient()).thenCallRealMethod();
        when(submissionMock.createBatchSparkJob(anyString(), submissionParameterArgumentCaptor.capture())).thenCallRealMethod();

        debugJobMock = mock(SparkBatchRemoteDebugJob.class, CALLS_REAL_METHODS);
        when(debugJobMock.getSubmission()).thenReturn(submissionMock);

        loggerMock = mock(Logger.class);
        when(debugJobMock.log()).thenReturn(loggerMock);

        caught = null;

        this.httpServerMock = new MockHttpService();
    }

    @Given("^create batch Spark Job with driver debugging for '(.+)' with following parameters$")
    public void createBatchSparkJobWithDriverDebuggingConfig(
            String connectUrl,
            Map<String, Object> sparkConfig) throws Throwable {
        SparkSubmissionParameterScenario scenario = new SparkSubmissionParameterScenario();
        scenario.sparkSubmissionParameter.applyFlattedJobConf(new ArrayList<Pair<String, String>>() {{
                sparkConfig.forEach((k, v) -> add(new Pair<>(k, (String) v)));
        }});

        caught = null;

        try {
            debugSubmissionParameter = SparkBatchRemoteDebugJob.convertToDebugParameter(scenario.sparkSubmissionParameter);
        } catch (Exception e) {
            caught = e;
        }
    }

    @Then("^throw exception '(.+)' with checking type only$")
    public void checkException(String exceptedName) throws Throwable {
        assertNotNull(caught);
        assertEquals(exceptedName, caught.getClass().getName());
    }

    @Then("^throw exception '(.+)' with message '(.*)'$")
    public void checkExceptionWithMessage(String exceptedName, String expectedMessage) throws Throwable {
        assertNotNull(caught);
        assertEquals(exceptedName, caught.getClass().getName());
        assertEquals(expectedMessage, caught.getMessage());
    }

    @Then("^the Spark driver JVM option should be '(.+)'$")
    public void checkSparkDriverJVMOption(String expectedDriverJvmOption) throws Throwable {
        assertNull(caught);

        String submittedDriverJavaOption =
                ((SparkConfigures) debugSubmissionParameter.getJobConfig().get("conf")).get("spark.driver.extraJavaOptions").toString();

        assertEquals(expectedDriverJvmOption, submittedDriverJavaOption);
    }

    @Then("^the Spark driver max retries should be '(.+)'$")
    public void checkSparkDriverMaxRetries(String expectedMaxRetries) throws Throwable {
        assertNull(caught);

        String maxRetries =
                ((SparkConfigures) debugSubmissionParameter.getJobConfig().get("conf")).get("spark.yarn.maxAppAttempts").toString();

        assertEquals(expectedMaxRetries, maxRetries);
    }

    @Given("^setup a mock livy service for (.+) request '(.+)' to return '(.+)' with status code (\\d+)$")
    public void mockLivyService(String action, String serviceUrl, String response, int statusCode) throws Throwable {
        httpServerMock.stub(action, serviceUrl, statusCode, response);
    }

    @Then("^getting spark job url '(.+)', batch ID (\\d+)'s application id should be '(.+)'$")
    public void checkGetSparkJobApplicationId(
            String connectUrl,
            int batchId,
            String expectedApplicationId) throws Throwable {
        caught = null;
        try {
            assertEquals(expectedApplicationId, debugJobMock.getSparkJobApplicationId(
                    new URI(httpServerMock.completeUrl(connectUrl)), batchId));
        } catch (Exception e) {
            caught = e;
            assertEquals(expectedApplicationId, "__exception_got__" + e);
        }
    }

    @Then("^getting spark job url '(.+)', batch ID (\\d+)'s application id, '(.+)' should be got with (\\d+) times retried$")
    public void checkGetSparkJobApplicationIdRetryCount(
            String connectUrl,
            int batchId,
            String getUrl,
            int expectedRetriedCount) throws Throwable {
        when(debugJobMock.getDelaySeconds()).thenReturn(1);
        when(debugJobMock.getRetriesMax()).thenReturn(3);

        try {
            debugJobMock.getSparkJobApplicationId(new URI(httpServerMock.completeUrl(connectUrl)), batchId);
        } catch (Exception ignore) { }

        verify(expectedRetriedCount, getRequestedFor(urlEqualTo(getUrl)));
    }

    @Then("^getting spark job url '(.+)', batch ID (\\d+)'s driver log URL should be '(.+)'$")
    public void checkGetSparkJobDriverLogUrl(
            String connectUrl,
            int batchId,
            String expectedDriverLogURL) throws Throwable {
        assertEquals(expectedDriverLogURL, debugJobMock.getSparkJobDriverLogUrl(
                new URI(httpServerMock.completeUrl(connectUrl)), batchId));
    }

    @Then("^Parsing driver HTTP address '(.+)' should get host '(.+)'$")
    public void checkParsingDriverHTTPAddressHost(
            String httpAddress,
            String expectedHost) throws Throwable {

        assertEquals(expectedHost, debugJobMock.parseAmHostHttpAddressHost(httpAddress));
    }

    @Then("^Parsing driver HTTP address '(.+)' should be null$")
    public void checkParsingDriverHTTPAddressHostFailure(String httpAddress) throws Throwable {
        assertNull(debugJobMock.parseAmHostHttpAddressHost(httpAddress));
    }

    @Then("^parsing JVM debugging port should be ([-]?\\d+) for the following listens:$")
    public void checkParsingJVMDebuggingPort(String expectedPort, List<String> listens) throws Throwable {

        listens.forEach((listen) -> assertEquals(
                Integer.parseInt(expectedPort),
                debugJobMock.parseJvmDebuggingPort(listen)));
    }

    @Then("^getting Spark driver debugging port from URL '(.+)', batch ID (\\d+) should be (\\d+)$")
    public void checkGetSparkDriverDebuggingPort(
            String connectUrl,
            int batchId,
            int expectedPort) throws Throwable {
        URI mockConnUri = new URI(httpServerMock.completeUrl(connectUrl));
        when(debugJobMock.getConnectUri()).thenReturn(mockConnUri);
        doReturn(mockConnUri.resolve("/yarnui/ws/v1/cluster/apps/")).when(debugJobMock).getYarnNMConnectUri();
        when(debugJobMock.getBatchId()).thenReturn(batchId);

        try {
            assertEquals(expectedPort, debugJobMock.getSparkDriverDebuggingPort().toBlocking().single().intValue());
        } catch (Exception e) {
            caught = e.getCause();
            assertEquals(expectedPort, 0);
        }

    }

    @Then("^getting Spark driver host from URL '(.+)', batch ID (\\d+) should be '(.+)'$")
    public void checkGetSparkDriverHost(
            String connectUrl,
            int batchId,
            String expectedHost) throws Throwable {
        URI mockConnUri = new URI(httpServerMock.completeUrl(connectUrl));
        when(debugJobMock.getConnectUri()).thenReturn(mockConnUri);
        doReturn(mockConnUri.resolve("/yarnui/ws/v1/cluster/apps/")).when(debugJobMock).getYarnNMConnectUri();
        when(debugJobMock.getBatchId()).thenReturn(batchId);

        try {
            assertEquals(expectedHost, debugJobMock.getSparkDriverHost().toBlocking().single());
        } catch (Exception e) {
            caught = e.getCause();
            assertEquals(expectedHost, "__exception_got__");
        }
    }

    @And("^mock method getSparkJobApplicationIdObservable to return '(.+)' Observable$")
    public void mockMethodGetSparkJobApplicationIdObservable(String appIdMock) {
        when(debugJobMock.getSparkJobApplicationIdObservable()).thenReturn(Observable.just(appIdMock));
    }

    @Then("^getting current Yarn App attempt should be '(.+)'$")
    public void checkGetCurrentYarnAppAttemptResult(String appAttemptIdExpect) {
        URI mockConnUri = URI.create(httpServerMock.completeUrl("/"));
        when(debugJobMock.getConnectUri()).thenReturn(mockConnUri);
        doReturn(mockConnUri.resolve("/yarnui/ws/v1/cluster/apps/")).when(debugJobMock).getYarnNMConnectUri();

        AppAttempt appAttempt = debugJobMock
                .getSparkJobYarnCurrentAppAttempt()
                .toBlocking()
                .first();

        assertEquals(appAttemptIdExpect, appAttempt.getAppAttemptId());
    }

    @Given("^mock getSparkJobYarnCurrentAppAttempt with the following response:$")
    public void mockGetSparkJobYarnCurrentAppAttemptWithTheFollowingResponse(Map<String, String> respMock) throws Throwable {
        doReturn(Observable.just(new AppAttempt())
                           .doOnNext(appAttempt -> {
                               Optional.ofNullable(respMock.get("logsLink")).ifPresent(appAttempt::setLogsLink);
                           }))
                .when(debugJobMock).getSparkJobYarnCurrentAppAttempt();
    }

    @Then("^getting Spark Job driver log URL Observable should be '(.+)'$")
    public void checkSparkJobDriverLogURLObservable(String expect) throws Throwable {
        String url = debugJobMock.getSparkJobDriverLogUrlObservable().toBlocking().last();

        assertEquals(expect, url);
    }

    @And("^mock Spark job connect URI to be '(.+)'$")
    public void mockSparkJobConnectURI(String mock) throws Throwable {
        doReturn(URI.create(mock)).when(debugJobMock).getConnectUri();
    }

    @Then("^getting Spark Job driver log URL Observable should be empty$")
    public void gettingSparkJobDriverLogURLObservableShouldBeEmpty() throws Throwable {
        assertTrue(debugJobMock.getSparkJobDriverLogUrlObservable().isEmpty().toBlocking().last());
    }

    @And("^mock Spark job uri '(.*)' is (valid|invalid)$")
    public void mockSparkJobUriIsValidOrNot(String uriToCheck, String validOrNot) throws Throwable {
        doReturn(validOrNot.equals("valid")).when(debugJobMock).isUriValid(URI.create(uriToCheck));
    }

    @And("^submit Spark job$")
    public void submitSparkJob() {
        caught = null;

        try {
            debugJobMock = (SparkBatchRemoteDebugJob) debugJobMock.submit().toBlocking().singleOrDefault(null);
        } catch (Exception e) {
            caught = e;
        }
    }
}
