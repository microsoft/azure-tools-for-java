package com.microsoft.intellij.util

import com.microsoft.azure.AzureEnvironment
import com.microsoft.azure.AzureResponseBuilder
import com.microsoft.azure.serializer.AzureJacksonAdapter
import com.microsoft.rest.RestClient
import com.microsoft.rest.credentials.BasicAuthenticationCredentials
import com.microsoft.rest.credentials.ServiceClientCredentials
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object WebAppDeploySession {

    private var credentials: ServiceClientCredentials? = null

    /**
     * Set http request credential using username and password
     *
     * @param username - user name to make a REST request
     * @param password - password to make a REST request
     */
    fun setCredentials(username: String, password: String) {
        credentials = BasicAuthenticationCredentials(username, password)
    }

    /**
     * Publish a ZIP file using Azure KUDU Service through REST API
     *
     * @param connectUrl - URL for REST request
     * @param zipFile    - file to publish on Azure
     *
     * @return {Response} object with publish response status
     * @throws IOException is throws for ZIP file IO exceptions
     */
    @Throws(IOException::class)
    fun publishZip(connectUrl: String, zipFile: File, readTimeoutMs: Long): Response {

        if (this.credentials == null)
            throw RuntimeException("User not signed in")

        val client = RestClient.Builder()
                .withCredentials(this.credentials!!)
                .withBaseUrl(AzureEnvironment.AZURE, AzureEnvironment.Endpoint.RESOURCE_MANAGER)
                .withResponseBuilderFactory(AzureResponseBuilder.Factory())
                .withSerializerAdapter(AzureJacksonAdapter())
                .withReadTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .build()
                .httpClient()

        val requestBody = RequestBody.create(MultipartBody.FORM, zipFile)
        val request = Request.Builder()
                .addHeader("Content-Type", "multipart/form-data")
                .url(connectUrl)
                .post(requestBody)
                .build()

        client.newCall(request).execute().use { response -> return response }
    }
}