/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AdTokenCache {
    private final Map<TokenCacheKey, AdTokenCacheEntry> tokenCacheDictionary;

    static AdTokenCache getInstance() {
        return SingletonHandler.INSTANCE;
    }

    /**
     * add an entry to cache.
     * @param entry AdTokenCacheEntry.
     */
    void add(@NotNull AdTokenCacheEntry entry) {
        TokenCacheKey key = createKeyFromEntry(entry);
        if (key != null) {
            tokenCacheDictionary.put(key, entry);
        }
    }

    /**
     * add multiple entries to cache.
     * @param entries List of AdTokenCacheEntry.
     */
    void addMultiple(@NotNull final List<AdTokenCacheEntry> entries) {
        for (AdTokenCacheEntry entry : entries) {
            if (entry != null) {
                add(entry);
            }
        }
    }

    /**
     * create a new key.
     * @param authority authority endpoint.
     * @param clientId String.
     * @param result AuthResult.
     * @return TokenCacheKey.
     */
    TokenCacheKey createKey(final String authority,
                                   final String clientId, @NotNull final AuthResult result) {
        String userId = result.getUserId();
        String resource = result.getResource();
        return new TokenCacheKey(authority, clientId, userId, resource);
    }

    /**
     * remove the entry from cache.
     * @param entry AdTokenCacheEntry to be removed.
     */
    void remove(@NotNull final AdTokenCacheEntry entry) {
        TokenCacheKey key = createKeyFromEntry(entry);
        if (null == key) {
            return;
        }
        tokenCacheDictionary.remove(key);
    }

    /**
     * clear the cache.
     */
    void clear() {
        this.tokenCacheDictionary.clear();
    }

    /**
     * remove multiple entries.
     * @param entries List of AdTokenCacheEntry to be removed.
     */
    void removeMultiple(@NotNull final List<AdTokenCacheEntry> entries) {
        for (AdTokenCacheEntry entry : entries) {
            if (entry != null) {
                remove(entry);
            }
        }
    }

    /**
     * query the cache.
     * @param key TokenCacheKey.
     * @param isMrrt Boolean.
     * @return List of AdTokenCacheEntry.
     */
    List<AdTokenCacheEntry> query(@NotNull final TokenCacheKey key, final Boolean isMrrt) {
        List<AdTokenCacheEntry> entryList = new ArrayList<AdTokenCacheEntry>();
        tokenCacheDictionary.forEach((k, v) -> {
            if (null != v && v.getAuthResult() != null) {
                if ((null == isMrrt || v.getAuthResult().isMultipleResourceRefreshToken() == isMrrt.booleanValue())
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

    static class TokenCacheKey {
        private final String authority;
        private final String resource;
        private final String clientId;
        private final String userId;

        /**
         * TokenCacheKey.
         * @param authority String.
         * @param clientId String.
         * @param userId String.
         * @param resource String.
         */
        TokenCacheKey(final String authority, final String clientId,
                             final String userId, final String resource) {
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
                TokenCacheKey other = (TokenCacheKey) obj;
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
