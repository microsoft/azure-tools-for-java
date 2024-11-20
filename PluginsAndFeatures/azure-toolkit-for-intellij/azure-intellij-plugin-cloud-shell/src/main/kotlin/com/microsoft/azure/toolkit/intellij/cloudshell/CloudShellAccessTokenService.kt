/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell

import com.azure.core.credential.TokenRequestContext
import com.azure.identity.implementation.util.ScopeUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.Account
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import kotlinx.coroutines.reactor.awaitSingleOrNull

@Service
class CloudShellAccessTokenService {
    companion object {
        fun getInstance(): CloudShellAccessTokenService = service()
        private val LOG = logger<CloudShellAccessTokenService>()
    }

    suspend fun getAccessToken(tenantId: String, azAccount: Account? = null): String? {
        val account = azAccount ?: Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            LOG.warn("Account is not logged in")
            return null
        }

        val managementEndpoint = account.environment.managementEndpoint
        val scopes = ScopeUtil.resourceToScopes(managementEndpoint)
        val request = TokenRequestContext().apply { addScopes(*scopes) }
        val accessToken = account
            .getTenantTokenCredential(tenantId)
            .getToken(request)
            .awaitSingleOrNull()
            ?.token

        if (accessToken == null) {
            LOG.warn("Unable to get access token")
            return null
        }

        return accessToken
    }
}