/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.spark.common.MockHttpService;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.apache.http.client.CredentialsProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class JobUtilsScenario {
    private MockHttpService httpServerMock;

    @Before
    public void setUp() {
        httpServerMock = new MockHttpService();
    }

    @Given("^mock a http service in JobUtilsScenario for (.+) request '(.+)' to return '(.+)' with status code (\\d+)$")
    public void mockHttpService(String action, String serviceUrl, String response, int statusCode) throws Throwable {
        httpServerMock.stub(action, serviceUrl, statusCode, response);
    }

    @Then("^Yarn log observable from '(.*)' should produce events:$")
    public void checkYarnLogObservable(String logUrl, List<String> logs) throws Throwable {
        List<String> logsGot = JobUtils.createYarnLogObservable(null, null, httpServerMock.completeUrl(logUrl), "stderr", 10)
                .takeUntil(line -> line.isEmpty())
                .filter(line -> !line.isEmpty())
                .toList()
                .toBlocking()
                .singleOrDefault(null);

        assertTrue("There are unmatched requests. All requests (reversed) are: \n" +
                        httpServerMock.getLivyServerMock().getAllServeEvents().stream()
                            .map(event -> event.getRequest().getUrl())
                            .reduce("", (a, b) -> a + "\n" + b),
                httpServerMock.getLivyServerMock().findAllUnmatchedRequests().isEmpty());

        assertThat(logsGot).containsExactlyElementsOf(logs);

    }

    @Then("^get YarnUI log '(.+)' from '(.+)' should return '(.*)'$")
    public void checkGetYarnUILogType(String type, String logUrl, String expect) throws Throwable {
        String actual = JobUtils.getInformationFromYarnLogDom(null, httpServerMock.completeUrl(logUrl), type, 0, -1);

        assertTrue("There are unmatched requests. All requests (reversed) are: \n" +
                        httpServerMock.getLivyServerMock().getAllServeEvents().stream()
                                .map(event -> event.getRequest().getUrl())
                                .reduce("", (a, b) -> a + "\n" + b),
                httpServerMock.getLivyServerMock().findAllUnmatchedRequests().isEmpty());

        assertThat(actual).isEqualTo(expect);
    }
}
