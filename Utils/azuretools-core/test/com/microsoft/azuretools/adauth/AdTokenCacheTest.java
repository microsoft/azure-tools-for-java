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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdTokenCacheTest {
    @Before
    public void setUp() throws Exception {
        AdTokenCache.getInstance().clear();
    }

    @Test
    public void testAdd() {
        String authority = "testEndpoint";
        String clientId = "clientId";
        AdTokenCacheEntry entry = new AdTokenCacheEntry(null, authority, clientId);
        AdTokenCache.getInstance().add(entry);
        Map<AdTokenCache.TokenCacheKey, AdTokenCacheEntry> dict = Whitebox.<Map<AdTokenCache.TokenCacheKey, AdTokenCacheEntry> > getInternalState(AdTokenCache.getInstance(), "tokenCacheDictionary");
        Assert.assertEquals(0, dict.size());

        AuthResult authResult = new AuthResult("tokenType", "accessToken",
                "refreshToken", 100, null, "resource");

        Assert.assertEquals(null, authResult.getUserInfo());
        Assert.assertEquals("", authResult.getUserId());
        Assert.assertEquals(false, authResult.isUserIdDisplayble());
        Assert.assertEquals(true, authResult.isMultipleResourceRefreshToken());

        entry = new AdTokenCacheEntry(authResult, authority, clientId);
        AdTokenCache.getInstance().add(entry);
        dict = Whitebox.<Map<AdTokenCache.TokenCacheKey, AdTokenCacheEntry> > getInternalState(AdTokenCache.getInstance(), "tokenCacheDictionary");
        Assert.assertEquals(1, dict.size());

        entry = new AdTokenCacheEntry(authResult, authority, clientId);
        AdTokenCache.getInstance().add(entry);
        Assert.assertEquals(1, dict.size());
    }

    @Test
    public void testAddRemoveMultiple() {
        String authority = "testEndpoint";
        String clientId = "clientId";
        List<AdTokenCacheEntry> list = new ArrayList<AdTokenCacheEntry>();
        AdTokenCacheEntry entry = new AdTokenCacheEntry(null, authority, clientId);
        list.add(entry);
        AuthResult authResult = new AuthResult("tokenType", "accessToken",
                "refreshToken", 100, null, "resource");
        entry = new AdTokenCacheEntry(authResult, authority, clientId);
        list.add(entry);

        AuthResult authResult1 = new AuthResult("tokenType", "accessToken",
                "refreshToken", 100, null, null);
        entry = new AdTokenCacheEntry(authResult1, authority, clientId);
        list.add(entry);

        entry = new AdTokenCacheEntry(authResult, authority, clientId);
        list.add(entry);
        list.add(null);

        AdTokenCache.getInstance().addMultiple(list);

        Map<AdTokenCache.TokenCacheKey, AdTokenCacheEntry> dict = Whitebox.<Map<AdTokenCache.TokenCacheKey, AdTokenCacheEntry> > getInternalState(AdTokenCache.getInstance(), "tokenCacheDictionary");
        Assert.assertEquals(2, dict.size());

        AdTokenCache.getInstance().removeMultiple(list);
        Assert.assertEquals(0, dict.size());

    }

    @Test
    public void testRemove() {
        String authority = "testEndpoint";
        String clientId = "clientId";
        AuthResult authResult = new AuthResult("tokenType", "accessToken",
                "refreshToken", 100, null, "resource");
        AdTokenCacheEntry entry = new AdTokenCacheEntry(authResult, authority, clientId);

        AdTokenCache.getInstance().add(entry);
        AuthResult authResult1 = new AuthResult("tokenType", "accessToken",
                "refreshToken", 100, null, null);
        entry = new AdTokenCacheEntry(authResult1, authority, clientId);
        AdTokenCache.getInstance().add(entry);

        AdTokenCache.getInstance().remove(new AdTokenCacheEntry(null, authority, clientId));
        Map<AdTokenCache.TokenCacheKey, AdTokenCacheEntry> dict = Whitebox.<Map<AdTokenCache.TokenCacheKey, AdTokenCacheEntry> > getInternalState(AdTokenCache.getInstance(), "tokenCacheDictionary");
        Assert.assertEquals(2, dict.size());

        AdTokenCache.getInstance().remove(entry);
        Assert.assertEquals(1, dict.size());
    }

    @Test
    public void testQuery() {
        String authority = "testEndpoint";
        String clientId = "clientId";
        AuthResult authResult1 = new AuthResult("tokenType", "accessToken",
                "refreshToken", 100, null, "resource");
        AuthResult authResult2 = new AuthResult("tokenType", "accessToken",
                "refreshToken", 100, null, null);

        AdTokenCache.getInstance().add(new AdTokenCacheEntry(authResult1, authority, clientId));
        AdTokenCache.getInstance().add(new AdTokenCacheEntry(authResult2, authority, clientId));

        AdTokenCache.TokenCacheKey q1 = new AdTokenCache.TokenCacheKey(authority, clientId, null, null);
        List<AdTokenCacheEntry> qResult = AdTokenCache.getInstance().query(q1, null);
        Assert.assertEquals(2, qResult.size());

        qResult = AdTokenCache.getInstance().query(q1, false);
        Assert.assertEquals(1, qResult.size());

        qResult = AdTokenCache.getInstance().query(q1, true);
        Assert.assertEquals(1, qResult.size());

        AdTokenCache.TokenCacheKey q2 = new AdTokenCache.TokenCacheKey(authority, clientId, null, "resource");
        qResult = AdTokenCache.getInstance().query(q2, true);
        Assert.assertEquals(1, qResult.size());
    }
}
