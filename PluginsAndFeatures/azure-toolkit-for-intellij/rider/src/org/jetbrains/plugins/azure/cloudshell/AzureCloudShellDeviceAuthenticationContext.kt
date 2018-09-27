package org.jetbrains.plugins.azure.cloudshell

import com.microsoft.aad.adal4j.AuthenticationContext
import com.microsoft.aad.adal4j.AuthenticationException
import com.microsoft.aad.adal4j.AuthenticationResult
import com.microsoft.aad.adal4j.DeviceCode
import com.microsoft.azuretools.sdkmanage.AzureManager
import com.microsoft.rest.credentials.ServiceClientCredentials
import com.microsoft.rest.credentials.TokenCredentials
import okhttp3.Request
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.concurrent.Executors

class AzureCloudShellDeviceAuthenticationContext private constructor(
        private val authenticationContext: AuthenticationContext,
        private val azureManager: AzureManager) {

    companion object {
        private val clientId = "aebc6443-996d-45c2-90f0-388ff96faa56" // VS Code

        fun create(azureManager: AzureManager, tenantIdentifier: String): AzureCloudShellDeviceAuthenticationContext {
            var endpoint = azureManager.environment.azureEnvironment.activeDirectoryEndpoint()
            val authority = if (endpoint.endsWith("/")) {
                endpoint + tenantIdentifier
            } else {
                endpoint + "/" + tenantIdentifier
            }

            val executor = Executors.newSingleThreadExecutor()
            val authenticationContext = AuthenticationContext(authority, false, executor)

            return AzureCloudShellDeviceAuthenticationContext(authenticationContext, azureManager)
        }
    }

    fun acquireDeviceCode(): DeviceCode = try {
        authenticationContext.acquireDeviceCode(
                clientId,
                azureManager.environment.azureEnvironment.managementEndpoint(), null).get()
    } catch (e: AuthenticationException) {
        // TODO: now what
        throw e
    }

    fun acquireTokenByDeviceCode(deviceCode: DeviceCode): AuthenticationResult = try {
        authenticationContext.acquireTokenByDeviceCode(deviceCode, null).get()
    } catch (e: AuthenticationException) {
        // TODO: now what
        throw e
    }

    fun tokenCredentialsFor(authenticationToken: AuthenticationResult): ServiceClientCredentials =
            RefreshableDeviceTokenCredentials(authenticationContext, authenticationToken)

    class RefreshableDeviceTokenCredentials(private val authenticationContext: AuthenticationContext,
            private var authenticationToken: AuthenticationResult)
        : TokenCredentials("Bearer", null) {
        override fun getToken(request: Request?): String {
            if (authenticationToken.expiresOnDate <= DateTime.now(DateTimeZone.UTC).minusMinutes(10).toDate()) {
                authenticationToken = authenticationContext.acquireTokenByRefreshToken(
                        authenticationToken.refreshToken, clientId, null).get()
            }

            return authenticationToken.accessToken
        }
    }
}