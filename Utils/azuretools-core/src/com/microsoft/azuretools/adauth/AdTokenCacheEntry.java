/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

final class AdTokenCacheEntry {
    private final AuthResult authResult;
    private final String authority;
    private final String clientId;

    AdTokenCacheEntry(final AuthResult authResult,
            final String authority, final String clientId) {
        this.authResult = authResult;
        this.authority = authority;
        this.clientId = clientId;
    }

    AuthResult getAuthResult() {
        return authResult;
    }

    String getAuthority() {
        return authority;
    }

    String getClientId() {
        return clientId;
    }
}
