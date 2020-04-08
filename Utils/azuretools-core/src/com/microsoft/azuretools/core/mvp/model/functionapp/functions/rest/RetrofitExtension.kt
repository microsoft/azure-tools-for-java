/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.azuretools.core.mvp.model.functionapp.functions.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.microsoft.azure.AzureResponseBuilder
import com.microsoft.azure.management.resources.fluentcore.utils.ResourceManagerThrottlingInterceptor
import com.microsoft.azure.serializer.AzureJacksonAdapter
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials
import com.microsoft.azuretools.sdkmanage.AzureManager
import com.microsoft.rest.RestClient
import com.microsoft.rest.credentials.ServiceClientCredentials
import com.microsoft.rest.protocol.Environment
import com.microsoft.rest.protocol.SerializerAdapter
import com.microsoft.rest.serializer.JacksonAdapter
import java.util.concurrent.TimeUnit

fun <T> AzureManager.getRetrofitClient(environment: Environment, endpoint: Environment.Endpoint, className: Class<T>, tenantId: String): T =
        getRetrofitClient(environment, endpoint, className, RefreshableTokenCredentials(this, tenantId))

fun <T> AzureManager.getRetrofitClient(environment: Environment, endpoint: Environment.Endpoint, className: Class<T>, credentials: ServiceClientCredentials): T =
        RestClient.Builder()
                .withCredentials(credentials)
                .withBaseUrl(environment, endpoint)
                .withResponseBuilderFactory(AzureResponseBuilder.Factory())
                .withSerializerAdapter(KotlinAzureJacksonAdapter())
                .withInterceptor(ResourceManagerThrottlingInterceptor())
                .withReadTimeout(180000L, TimeUnit.MILLISECONDS)
                .build()
                .retrofit()
                .create(className)

/**
 * A serialization helper class overriding [AzureJacksonAdapter] with support for Kotlin
 */
class KotlinAzureJacksonAdapter : JacksonAdapter(), SerializerAdapter<ObjectMapper> {
    init {
        serializer().registerModule(KotlinModule())
    }
}
