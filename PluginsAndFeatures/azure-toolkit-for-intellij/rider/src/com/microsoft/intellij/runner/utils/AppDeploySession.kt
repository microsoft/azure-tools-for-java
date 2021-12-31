/**
 * Copyright (c) 2018-2021 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.utils

import com.intellij.execution.ExecutionException
import com.jetbrains.rd.util.threading.SpinWait
import com.microsoft.azure.AzureEnvironment
import com.microsoft.azure.AzureResponseBuilder
import com.microsoft.azure.serializer.AzureJacksonAdapter
import com.microsoft.rest.RestClient
import com.microsoft.rest.credentials.BasicAuthenticationCredentials
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.jetbrains.plugins.azure.util.WaitForResponse
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppDeploySession(username: String, password: String) {

    private val credentials = BasicAuthenticationCredentials(username, password)

    /**
     * Publish a ZIP file using Azure KUDU Service through REST API
     *
     * More information: https://github.com/projectkudu/kudu/wiki/Deploying-from-a-zip-file-or-url#asynchronous-zip-deployment
     *
     * @param connectUrl           - URL for REST request
     * @param zipFile              - file to publish on Azure
     * @param readTimeoutMs        - timeout for reading from Kudu endpoint
     * @param pollingTimeoutMs     - number of ms between polls to the Kudu endpoint
     * @param pollingNumberOfTries - number of polling attempts before giving up
     *
     * @return {Response} object with publish response status
     * @throws IOException is throws for ZIP file IO exceptions
     */
    @Throws(IOException::class)
    fun publishZip(
            connectUrl: String,
            zipFile: File,
            readTimeoutMs: Long,
            pollingTimeoutMs: Long,
            pollingNumberOfTries: Int): Response {

        val client = RestClient.Builder()
                .withCredentials(credentials)
                .withBaseUrl(AzureEnvironment.AZURE, AzureEnvironment.Endpoint.RESOURCE_MANAGER)
                .withResponseBuilderFactory(AzureResponseBuilder.Factory())
                .withSerializerAdapter(AzureJacksonAdapter())
                .withReadTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .build()
                .httpClient()

        val requestBody = RequestBody.create(null, zipFile)
        val request = Request.Builder()
                .url(connectUrl)
                .post(requestBody)
                .header("Content-type", "application/octet-steam")
                .build()

        client.newCall(request).execute().use { response ->
            // If the initial async deploy fails, there is no use in polling.
            if (!response.isSuccessful) return response

            // If it succeeds, poll for a result
            val pollingUrl = response.header("Location") ?: throw ExecutionException("The Azure KUDU Service did not return an URL to poll for deployment status.")
            val maximumPollingDurationSeconds = (pollingTimeoutMs * pollingNumberOfTries) / 1000

            return WaitForResponse(pollingUrl, timeoutInSeconds = readTimeoutMs, client)
                    .withRetries(pollingNumberOfTries,
                    { _, _ -> },
                    { pollingResponse ->
                        if (pollingResponse.code() == 202) {
                            SpinWait.spinUntil(pollingTimeoutMs) { false }
                            false
                        } else {
                            true
                        }
                    }) ?: throw ExecutionException("The Azure KUDU Service did not return a valid deployment status polling result within $maximumPollingDurationSeconds seconds.")
        }
    }
}