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

package com.microsoft.azure.hdinsight.spark.common

import com.github.tomakehurst.wiremock.http.RequestMethod
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.doReturn

class SparkBatchAzureSubmissionScenario {
    private var httpServerMock: MockHttpService? = null
    private var accessTokenMock: String? = null
    private var azureSubmissionMock = Mockito.mock(SparkBatchAzureSubmission::class.java, CALLS_REAL_METHODS)

    @Before
    fun setUp() {
        httpServerMock = MockHttpService()
    }

    @Given("^mock a http service in SparkBatchAzureSubmissionScenario for (.+) request '(.+)' to return '(.*)' with status code (\\d+)$")
    fun mockHttpService(method: String, url: String, response: String, responseCode: Int) {
        httpServerMock!!.stub(method, url, responseCode, response)
    }

    @Given("^mock a Spark batch job Azure submission with access token '(.*)'$")
    fun mockSparkBatchAzureSubmission(accessTokenMock: String) {
        this.accessTokenMock = accessTokenMock
        doReturn(accessTokenMock).`when`(azureSubmissionMock).accessToken
    }

    @Then("^check GET request to '(.+)' should be with the following headers$")
    fun checkGetHeaders(url: String, headersExpect: Map<String, String>) {
        val response = azureSubmissionMock.getHttpResponseViaGet(httpServerMock!!.completeUrl(url))

        assertThat(response.code).isEqualTo(200)

        val requestHeadersGot = httpServerMock!!.livyServerMock.allServeEvents
                .find { it.request.url == url && it.request.method == RequestMethod.GET } !!
                .request.headers.all().map { it.key() to it.values().joinToString("\n") }
                .toMap()

        assertThat(requestHeadersGot).containsAllEntriesOf(headersExpect)
    }
}