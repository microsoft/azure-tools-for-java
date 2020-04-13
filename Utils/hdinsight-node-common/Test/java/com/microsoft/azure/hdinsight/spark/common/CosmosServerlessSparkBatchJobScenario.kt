/**
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
 *
 */

package com.microsoft.azure.hdinsight.spark.common

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.ApiVersion
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters
import com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogLine
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import org.apache.http.entity.StringEntity
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.slf4j.Logger
import rx.Observable
import rx.subjects.PublishSubject
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

class CosmosServerlessSparkBatchJobScenario {
    private val httpServerMock = MockHttpService()
    private val ctrlSubject = PublishSubject.create<SparkBatchJobLogLine>()
    private val ctrlSubscription = ctrlSubject.subscribe()
    private val jobUuid = "46c07889-3590-48f8-b2bc-7f52622b5a0b"
    private var serverlessJobMock = mock(CosmosServerlessSparkBatchJob::class.java, CALLS_REAL_METHODS)
    private val adlAccount = mock(AzureSparkServerlessAccount::class.java, CALLS_REAL_METHODS)
    private val loggerMock = mock(Logger::class.java)
    private var caught: Throwable? = null
    private var submissionParameter = CreateSparkBatchJobParameters()
    private val connectUri: URI = URI.create(httpServerMock.completeUrl("/activityTypes/spark/batchJobs"))
    private var http: AzureHttpObservable = object: AzureHttpObservable(ApiVersion.VERSION) {
        // we need to override getAccessToken() in AzureHttpObservable since it throws exception when user doesn't signed in
        override fun getAccessToken(): String {
            return "access_token"
        }
    }
    private var requestJsonBody: String = ""

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        doReturn(jobUuid).`when`(serverlessJobMock).jobUuid
        doReturn(loggerMock).`when`(serverlessJobMock).log()
        doReturn(http).`when`(serverlessJobMock).http
        doReturn(adlAccount).`when`(serverlessJobMock).account
        // Actually submissionParameter is merely set to make serverlessJobMock.getSubmissionParameter() not break
        // the real request body is defined with variable "requestJsonBody"
        doReturn(submissionParameter).`when`(serverlessJobMock).getSubmissionParameter()
        doReturn(connectUri).`when`(serverlessJobMock).connectUri
        doReturn(Observable.just(true)).`when`(serverlessJobMock).prepareSparkEventsLogFolder()
        doReturn(ctrlSubject).`when`(serverlessJobMock).ctrlSubject

        doReturn(URI.create(httpServerMock.completeUrl("/"))).`when`(adlAccount).uri
        doReturn(http).`when`(adlAccount).http
        doReturn("test").`when`(adlAccount).name
    }

    @After
    @Throws(Throwable::class)
    fun cleanUp() {
        ctrlSubscription.unsubscribe()
    }

    @Given("^setup a mock cosmos serverless service for '(.+)' detail request '(.+)' with body '(.+)' to return '(.+)' with status code (\\d+)$")
    @Throws(Throwable::class)
    fun mockCosmosServerlessService(action: String, serviceUrl: String, body: String, response: String, statusCode: Int) {
        httpServerMock.stubWithBody(action, serviceUrl, body, statusCode, response)
        requestJsonBody = body
    }

    @Given("^setup a mock cosmos serverless service for '(.+)' request '(.+)' to return '(.+)' with status code (\\d+)$")
    @Throws(Throwable::class)
    fun mockCosmosServerlessService(action: String, serviceUrl: String, response: String, statusCode: Int) {
        httpServerMock.stub(action, serviceUrl, statusCode, response)
    }

    @Given("^submit a cosmos serverless spark batch job$")
    fun submitServerlessSparkBatchJob() {
        // generate request json with submission parameter might lead to out-of-order in the json
        // therefore, we need to use json from .feature file to make the real request body exactly same as the mocked one
        val entity = StringEntity(requestJsonBody, StandardCharsets.UTF_8)
        val requestUrl: String = connectUri.toString() + "/" + jobUuid
        val request = http.withUuidUserAgent().put(requestUrl, entity, null, null, com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob::class.java)
        doReturn(request).`when`(adlAccount).createSparkBatchJobRequest(ArgumentMatchers.anyString(), ArgumentMatchers.eq(submissionParameter))

        caught = null

        try {
            serverlessJobMock.submit().toBlocking().single()
        } catch (ex: Exception) {
            caught = ex
        }
    }

    @Then("^verify '(.+)' request for '(.+)'$")
    fun verifyRequest(action: String, serviceUrl: String) {
        // this method is just used for debugging
        val allServerEvents = getAllServeEvents()
        when (action) {
            "PUT" -> WireMock.verify(putRequestedFor(urlEqualTo(serviceUrl)))
            "GET" -> WireMock.verify(getRequestedFor(urlEqualTo(serviceUrl)))
            else -> println("$action action for verifyRequest function is not found.")
        }
    }

    @Then("^batch ID should be (\\d+)\$")
    fun checkBatchId(batchId: Int) {
        assertEquals(batchId, serverlessJobMock.batchId)
    }

    @Then("^the return log line should be '(.+)'$")
    fun verifyReturnLog(logLine: String) {
        caught = null
        var logEntry: SparkBatchJobLogLine? = null
        try {
            logEntry = serverlessJobMock.submissionLog.toBlocking().last()
        } catch (ex: Exception) {
            caught = ex
        } finally {
            assertEquals(logEntry?.rawLog, logLine)
        }
    }
}