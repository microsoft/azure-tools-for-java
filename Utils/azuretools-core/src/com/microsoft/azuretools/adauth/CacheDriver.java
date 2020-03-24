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

package com.microsoft.azuretools.adauth;

import com.microsoft.azuretools.adauth.AdTokenCache.TokenCacheKey;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class CacheDriver {
    private static final Logger log = Logger.getLogger(CacheDriver.class.getName());
    private static final long EXPIREBUFFER = 300000; //in milliseconds
    private static final String DUPLICATETOKEN = "More than one token matches the criteria. The result is ambiguous.";
    private static final String FAILREFRSH = "Fail to refresh the token";
    private static final String NOAUTHRESULT = "Null auth result in cache entry";
    private static final String NOREFRESHTOKEN = "Null refresh token in auth result";

    private final String authority;
    private final String clientId;

    CacheDriver(@NotNull final String authority, @NotNull final String clientId) {
        this.authority = authority;
        this.clientId = clientId;
    }


    AuthResult find(@NotNull final String resource, final String userId) throws AuthException {
        TokenCacheKey key = new TokenCacheKey(this.authority, this.clientId, userId, resource);
        SingleEntryResult singleRes = loadSingleEntry(key);
        if (singleRes != null) {
            AdTokenCacheEntry entry = refreshEntryIfNecessary(singleRes, key);
            if (null != entry) {
                return entry.getAuthResult();
            }
        }
        return null;
    }

    AdTokenCacheEntry createAddEntry(@NotNull final AuthResult result, final String resource) {
        return createAddEntry(result, resource, null);
    }

    private AdTokenCacheEntry createAddEntry(@NotNull final AuthResult result,
                                             final String resource, final UserInfo info) {
        if (StringUtils.isNullOrEmpty(result.getUserId())) {
            result.setUserInfo(info);
        }

        if (StringUtils.isNullOrEmpty(result.getResource())) {
            result.setResource(resource);
        }

        AdTokenCacheEntry entry = new AdTokenCacheEntry(result, this.authority, this.clientId);
        updateRefreshTokens(result);
        AdTokenCache.getInstance().add(entry);
        return entry;
    }

    private AdTokenCacheEntry refreshEntryIfNecessary(@NotNull final SingleEntryResult result,
                                                      @NotNull final TokenCacheKey key) throws AuthException {
        AdTokenCacheEntry entry = result.cacheEntry;
        if (entry == null || entry.getAuthResult() == null) {
            if (null != entry) {
                AdTokenCache.getInstance().remove(entry);
            }
            throw new AuthException(NOAUTHRESULT);
        }

        AuthResult authResult = entry.getAuthResult();
        if (StringUtils.isNullOrEmpty(authResult.getRefreshToken())) {
            AdTokenCache.getInstance().remove(entry);
            throw new AuthException(NOREFRESHTOKEN);
        }

        String refreshToken = authResult.getRefreshToken();

        long expireTimeStamp = authResult.getExpiresOnDate() != null ? authResult.getExpiresOnDate().getTime() : 0;
        long nowTimeStamp = (new Date()).getTime();
        long nowPlusBuffer = nowTimeStamp + EXPIREBUFFER;

        if (result.isResourceSpecific && nowPlusBuffer > expireTimeStamp) {
            AdTokenCache.getInstance().remove(entry);
            return refreshExpireEntry(refreshToken, key.getResource(), authResult.getUserInfo());
        } else if (!result.isResourceSpecific && authResult.isMultipleResourceRefreshToken()) {
            return acquireTokenFromMrrt(refreshToken, key.getResource(), authResult.getUserInfo());
        } else {
            return entry;
        }
    }

    private AdTokenCacheEntry acquireTokenFromMrrt(@NotNull final String refreshToken,
                                                   final String resource, final UserInfo info) throws AuthException {
        AuthResult result = getTokenWithRefreshToken(refreshToken, resource);

        if (null == result) {
            throw new AuthException(FAILREFRSH);
        }

        return createAddEntry(result, resource, info);
    }

    private AdTokenCacheEntry refreshExpireEntry(@NotNull final String refreshToken,
                                                 final String resource, final UserInfo info) throws AuthException {
        AuthResult result = getTokenWithRefreshToken(refreshToken, null);

        if (null == result) {
            throw new AuthException(FAILREFRSH);
        }

        return createAddEntry(result, resource, info);
    }

    private void updateRefreshTokens(@NotNull final AuthResult result) {
        if (result.isMultipleResourceRefreshToken() && !StringUtils.isNullOrEmpty(result.getRefreshToken())) {
            String userId = result.getUserId();
            String refreshToken = result.getRefreshToken();

            if (!StringUtils.isNullOrEmpty(userId)) {
                List<AdTokenCacheEntry> mrrtEntries = getMrrtEntriesForUser(userId);
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


    private List<AdTokenCacheEntry> getMrrtEntriesForUser(@NotNull final String userId) {
        TokenCacheKey queryKey = new TokenCacheKey(null, this.clientId, userId, null);
        return AdTokenCache.getInstance().query(queryKey, true);
    }

    private List<AdTokenCacheEntry> getPotentialEntries(@NotNull final TokenCacheKey key) {
        TokenCacheKey potentialKey = new TokenCacheKey(null, key.getClientId(), key.getUserId(), null);
        return AdTokenCache.getInstance().query(potentialKey, null);
    }

    private SingleEntryResult loadSingleEntry(@NotNull final TokenCacheKey key) throws AuthException {
        List<AdTokenCacheEntry> entries = getPotentialEntries(key);
        List<AdTokenCacheEntry> resSpecificEntries = new ArrayList<AdTokenCacheEntry>();
        AdTokenCacheEntry mrrtToken = null;

        for (AdTokenCacheEntry entry : entries) {
            if (entry.getAuthResult() != null) {
                AuthResult result = entry.getAuthResult();
                if (key.getResource().equalsIgnoreCase(result.getResource())
                        && key.getAuthority().equalsIgnoreCase(entry.getAuthority())) {
                    resSpecificEntries.add(entry);
                } else if (null == mrrtToken
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

    protected abstract AuthResult getTokenWithRefreshToken(@NotNull final String refreshToken,
                                                           final String resource) throws AuthException;

    private class SingleEntryResult {
        private final AdTokenCacheEntry cacheEntry;
        private final boolean isResourceSpecific;

        SingleEntryResult(final AdTokenCacheEntry cacheEntry, boolean isResourceSpecific) {
            this.cacheEntry = cacheEntry;
            this.isResourceSpecific = isResourceSpecific;
        }
    }

}
