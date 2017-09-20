package com.microsoft.azuretools.adauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;


final public class AdTokenCache {
    private final Map<TokenCacheKey, AdTokenCacheEntry> tokenCacheDictionary;
    
    public static AdTokenCache getInstance() {
        return SingletonHandler.INSTANCE;
    }
    
    public void add(@NotNull AdTokenCacheEntry entry) {
        TokenCacheKey key = createKeyFromEntry(entry);
        if (key != null) {
            tokenCacheDictionary.put(key, entry);
        }
    }
    
    public void addMultiple(@NotNull final List<AdTokenCacheEntry> entries) {
        for (AdTokenCacheEntry entry : entries) {
            if (entry != null) {
                add(entry);
            }
        }
    }

    public TokenCacheKey createKey(final String authority,
           final String clientId, @NotNull final AuthResult result) {
        String userId = result.getUserId();
        String resource = result.getResource();
        return new TokenCacheKey(authority, clientId, userId, resource);
    }

    public void remove(@NotNull final AdTokenCacheEntry entry) {
        TokenCacheKey key = createKeyFromEntry(entry);
        tokenCacheDictionary.remove(key);
    }
    
    public void clear() {
        this.tokenCacheDictionary.clear();
    }
    
    public void removeMultiple(@NotNull final List<AdTokenCacheEntry> entries) {
        for (AdTokenCacheEntry entry : entries) {
            if (entry != null) {
                remove(entry);
            }
        }
    }
    
   public List<AdTokenCacheEntry> query(@NotNull final TokenCacheKey key, final Boolean isMRRT) {
        List<AdTokenCacheEntry> entryList = new ArrayList<AdTokenCacheEntry>();
        tokenCacheDictionary.forEach((k, v) -> {
            if (null != v && v.getAuthResult() != null) {
                if ((null == isMRRT || v.getAuthResult().isMultipleResourceRefreshToken() == isMRRT.booleanValue())
                    && (key.getClientId().isEmpty() || k.getClientId().equalsIgnoreCase(key.getClientId()))
                    && (key.getUserId().isEmpty() || k.getUserId().equalsIgnoreCase(key.getUserId()))
                    && (key.getAuthority().isEmpty() || k.getAuthority().equalsIgnoreCase(key.getAuthority()))) {
                    entryList.add(v);
                }
            }
        });
        return entryList;
    }
   
    private AdTokenCache() {
        tokenCacheDictionary = new ConcurrentHashMap<TokenCacheKey, AdTokenCacheEntry>();
    }

    private TokenCacheKey createKeyFromEntry(@NotNull final AdTokenCacheEntry entry) {
        if (entry.getAuthResult() == null) {
            return null;
        }
        
        return createKey(entry.getAuthority(), entry.getClientId(), entry.getAuthResult());
    }
    
    private static final class SingletonHandler {
        private static final AdTokenCache INSTANCE = new AdTokenCache();
    }
    
    public static class TokenCacheKey{
        private final String authority;
        private final String resource;
        private final String clientId;
        private final String userId;
        TokenCacheKey(final String authority, final String clientId, final String userId, final String resource) {
            this.authority = authority != null ? authority : "";
            this.resource = resource != null ? resource : "";
            this.clientId = clientId != null ? clientId : "";
            this.userId = userId != null ? userId : "";
        }
        
        public String getAuthority() {
            return authority;
        }
        
        public String getResource() {
            return resource;
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            
            if (obj instanceof TokenCacheKey) {
                TokenCacheKey other = (TokenCacheKey)obj;
                return (other.hashCode() == this.hashCode());
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            final String Delimiter = ":::";
            String key = (this.authority.toLowerCase() + Delimiter 
                    + this.resource.toLowerCase() + Delimiter
                    + this.clientId.toLowerCase() + Delimiter
                    + this.userId.toLowerCase());
   //                 + ((this.uniqueId != null) ? this.uniqueId.toLowerCase() : null) + Delimiter
   //                 + ((this.displayableId != null) ? this.displayableId.toLowerCase() : null));
            int hc = key.hashCode();
            return hc;
        }
    }
}
