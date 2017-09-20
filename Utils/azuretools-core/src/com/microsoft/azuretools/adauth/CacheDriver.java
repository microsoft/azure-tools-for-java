package com.microsoft.azuretools.adauth;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.azuretools.adauth.AdTokenCache.TokenCacheKey;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public abstract class CacheDriver {
    private static final Logger log = Logger.getLogger(CacheDriver.class.getName());
    private static final long EXPIREBUFFER = 300000; //in milliseconds
    private static final String DUPLICATETOKEN = "More than one token matches the criteria. The result is ambiguous.";

    private final String authority;
    private final String clientId;

    public CacheDriver(@NotNull final String authority, @NotNull final String clientId) {
        this.authority = authority;
        this.clientId = clientId;
    }
    
    public AuthResult find(final String resource, final String userId) throws AuthException{
        TokenCacheKey key = new TokenCacheKey(this.authority, this.clientId, userId, resource);
        SingleEntryResult singleRes = loadSingleEntry(key);
        if(singleRes != null) {
            AdTokenCacheEntry entry = refreshEntryIfNecessary(singleRes, key);
            if (null != entry) {
                return entry.getAuthResult();
            }
        }
        return null;
    }
    
    public AdTokenCacheEntry createAddEntry(@NotNull final AuthResult result, final String resource) {
        String newResource = result.getResource();
        if (StringUtils.isNullOrEmpty(newResource)) {
            result.setResource(resource);
        } 
        
        AdTokenCacheEntry entry = new AdTokenCacheEntry(result, this.authority, this.clientId);
        updateRefreshTokens(result);
        AdTokenCache.getInstance().add(entry);
        return entry;
    }
    
    abstract protected AuthResult getTokenWithRefreshToken(@NotNull final String refreshToken, final String resource) throws AuthException;
    
    private AdTokenCacheEntry refreshEntryIfNecessary(@NotNull final SingleEntryResult result, @NotNull final TokenCacheKey key) throws AuthException {
        AdTokenCacheEntry entry = result.cacheEntry;
        if (entry == null || entry.getAuthResult() == null) {
            throw new AuthException("Null auth result in cache entry");
        }
      
        AuthResult authResult = entry.getAuthResult();
        if (StringUtils.isNullOrEmpty(authResult.getRefreshToken())) {
            AdTokenCache.getInstance().remove(entry);
            throw new AuthException("Null refresh token in auth result");
        }
        
        String refreshToken = authResult.getRefreshToken();
      
        long expireTimeStamp = authResult.getExpiresOnDate() != null ? authResult.getExpiresOnDate().getTime() : 0;
        long nowTimeStamp = (new Date()).getTime();
        long nowPlusBuffer = nowTimeStamp + EXPIREBUFFER;
      
        if (result.isResourceSpecific && nowPlusBuffer > expireTimeStamp) {
            AdTokenCache.getInstance().remove(entry);
            return refreshExpireEntry(refreshToken, key.getResource());
        } else if (!result.isResourceSpecific && authResult.isMultipleResourceRefreshToken()) {
            return acquireTokenFromMRRT(refreshToken, key.getResource());
        } else {
            return entry;
        }
    }

    private AdTokenCacheEntry acquireTokenFromMRRT(@NotNull final String refreshToken, final String resource) throws AuthException{
        AuthResult result = getTokenWithRefreshToken(refreshToken, resource);

        if (null == result) {
            throw new AuthException("Fail to refresh the token");
        }
        
        return createAddEntry(result, resource);
    }

    private AdTokenCacheEntry refreshExpireEntry(@NotNull final String refreshToken, final String resource) throws AuthException{
        AuthResult result = getTokenWithRefreshToken(refreshToken, null);

        if (null == result) {
            throw new AuthException("Fail to refresh the token");
        }
        
        return createAddEntry(result, resource);
    }

    private void updateRefreshTokens(@NotNull final AuthResult result) {
        if (result.isMultipleResourceRefreshToken() && !StringUtils.isNullOrEmpty(result.getRefreshToken())) {
            String userId = result.getUserId();
            String refreshToken = result.getRefreshToken();
            
            if (!StringUtils.isNullOrEmpty(userId)) {
                List<AdTokenCacheEntry> mrrtEntries = getMRRTEntriesForUser(userId);
                AdTokenCache.getInstance().removeMultiple(mrrtEntries);
                
                for (AdTokenCacheEntry entry : mrrtEntries) {
                    if (entry.getAuthResult() != null) {
                        entry.getAuthResult().setRefreshToken(refreshToken);
                    }
                }
                
                AdTokenCache.getInstance().addMultiple(mrrtEntries);
            }
        }
    }
    
    
    private List<AdTokenCacheEntry> getMRRTEntriesForUser(@NotNull final String userId) {
        TokenCacheKey queryKey = new TokenCacheKey(null, this.clientId, userId, null);
        return AdTokenCache.getInstance().query(queryKey, true);
    }

    private List<AdTokenCacheEntry> getPotentialEntries(@NotNull final TokenCacheKey key) {
        TokenCacheKey potentialKey = new TokenCacheKey(null, key.getClientId(), key.getUserId(), null);
        return AdTokenCache.getInstance().query(potentialKey, null);
    }
    
    private SingleEntryResult loadSingleEntry(@NotNull final TokenCacheKey key) throws AuthException{
        List<AdTokenCacheEntry> entries = getPotentialEntries(key);
        List<AdTokenCacheEntry> resSpecificEntries = new ArrayList<AdTokenCacheEntry>();
        AdTokenCacheEntry mrrtToken = null;
        
        for (AdTokenCacheEntry entry : entries) {
            if (entry.getAuthResult() != null) {
                AuthResult result = entry.getAuthResult();
                if (key.getResource().equalsIgnoreCase(result.getResource())
                        && key.getAuthority().equalsIgnoreCase(entry.getAuthority())) {
                    resSpecificEntries.add(entry);
                } else if(null == mrrtToken
                        && result.isMultipleResourceRefreshToken()) {
                    mrrtToken = entry;
                }
            }
        }
        
        if (resSpecificEntries.isEmpty()) {
            return mrrtToken == null ? null : new SingleEntryResult(mrrtToken, false);
        } else if (resSpecificEntries.size() == 1) {
            return new SingleEntryResult(resSpecificEntries.get(0), true);
        } else {
            log.log(Level.SEVERE, DUPLICATETOKEN);
            throw new AuthException(DUPLICATETOKEN);
        }
    }
    
    private class SingleEntryResult{
        private final AdTokenCacheEntry cacheEntry;
        private final boolean isResourceSpecific;
        public SingleEntryResult(final AdTokenCacheEntry cacheEntry, boolean isResourceSpecific) {
            this.cacheEntry = cacheEntry;
            this.isResourceSpecific = isResourceSpecific;
        }
    }
}
