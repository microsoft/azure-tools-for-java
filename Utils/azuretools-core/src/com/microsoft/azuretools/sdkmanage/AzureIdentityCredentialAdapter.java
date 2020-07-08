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

package com.microsoft.azuretools.sdkmanage;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convert token credential in azure-identity to legacy AzureTokenCredentials
 * Refers https://github.com/jongio/azidext/blob/master/java/src/main/java/com/azure/identity/extensions/AzureIdentityCredentialAdapter.java
 */
public class AzureIdentityCredentialAdapter extends AzureTokenCredentials {
    private final TokenCredential tokenCredential;
    private final Map<String, AccessToken> accessTokenCache = new ConcurrentHashMap<>();
    private final String[] scopes;

    public AzureIdentityCredentialAdapter(String tenantId, TokenCredential tokenCredential) {
        this(AzureEnvironment.AZURE, tenantId, tokenCredential, new String[]{AzureEnvironment.AZURE.resourceManagerEndpoint()});
    }

    public AzureIdentityCredentialAdapter(AzureEnvironment environment, String tenantId, TokenCredential tokenCredential) {
        this(environment, tenantId, tokenCredential, new String[]{environment.resourceManagerEndpoint()});
    }

    public AzureIdentityCredentialAdapter(AzureEnvironment environment, String tenantId,
                                          TokenCredential tokenCredential, String[] scopes) {
        super(environment, tenantId);
        this.tokenCredential = tokenCredential;
        this.scopes = scopes;
    }

    @Override
    public String getToken(String endpoint) {
        if (!accessTokenCache.containsKey(endpoint) || accessTokenCache.get(endpoint).isExpired()) {
            accessTokenCache.put(endpoint,
                    this.tokenCredential.getToken(new TokenRequestContext().addScopes(scopes)).block());
        }
        return accessTokenCache.get(endpoint).getToken();
    }
}
