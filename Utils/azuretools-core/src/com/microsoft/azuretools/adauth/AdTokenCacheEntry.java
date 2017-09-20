package com.microsoft.azuretools.adauth;

final class AdTokenCacheEntry {
    private final AuthResult authResult;
    private final String authority;
    private final String clientId;

    public AdTokenCacheEntry(final AuthResult authResult,
            final String authority, final String clientId) {
        this.authResult = authResult;
        this.authority = authority;
        this.clientId = clientId;
    }
    
    public AuthResult getAuthResult() {
        return authResult;
    }

    public String getAuthority() {
        return authority;
    }
    
    public String getClientId() {
        return clientId;
    }
}
