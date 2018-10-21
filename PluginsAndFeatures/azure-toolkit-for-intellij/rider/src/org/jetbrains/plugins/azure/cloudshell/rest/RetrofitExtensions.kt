package org.jetbrains.plugins.azure.cloudshell.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.microsoft.azure.AzureResponseBuilder
import com.microsoft.azure.management.resources.fluentcore.utils.ResourceManagerThrottlingInterceptor
import com.microsoft.azure.serializer.AzureJacksonAdapter
import com.microsoft.azuretools.authmanage.AdAuthManager
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials
import com.microsoft.azuretools.sdkmanage.AzureManager
import com.microsoft.rest.RestClient
import com.microsoft.rest.credentials.ServiceClientCredentials
import com.microsoft.rest.protocol.Environment
import com.microsoft.rest.protocol.SerializerAdapter
import com.microsoft.rest.serializer.JacksonAdapter
import java.util.concurrent.TimeUnit

fun <T> AzureManager.getRetrofitClient(environment: Environment, endpoint: Environment.Endpoint, className: Class<T>, tenantId: String): T =
        getRetrofitClient(environment, endpoint, className, RefreshableTokenCredentials(AdAuthManager.getInstance(), tenantId))

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
