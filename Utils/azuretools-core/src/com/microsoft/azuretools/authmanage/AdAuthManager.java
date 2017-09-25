/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.adauth.*;
import com.microsoft.azuretools.authmanage.models.AdAuthDetails;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class AdAuthManager {
    private final static Logger LOGGER = Logger.getLogger(AdAuthManager.class.getName());
    private IWebUi webUi;
    private AzureEnvironment env;
    private AdAuthDetails adAuthDetails;
    private static AdAuthManager instance = null;

    public static AdAuthManager getInstance() throws IOException {
        if( instance == null) {
            instance = new AdAuthManager();
        }
        return instance;
    }

    public String getAccessToken(String tid) throws IOException {
        return getAccessToken(tid, env.resourceManagerEndpoint(), PromptBehavior.Auto);
    }

    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        AuthContext ac = createContext(tid, null);
        AuthResult result = ac.acquireToken(resource, false, adAuthDetails.getAccountEmail(), false);
        return result.getAccessToken();
    }

    public AuthResult signIn() throws IOException {

        // build token cache for azure and graph api
        // using azure sdk directly

        cleanCache();
        String commonTid = "common";
        AuthContext ac = createContext(commonTid, null); 
        AuthResult result = ac.acquireToken(env.managementEndpoint(), true, null, false);
        String userId = result.getUserId();
        boolean isDisplayable = result.isUserIdDisplayble();

        Map<String, List<String>> tidToSidsMap = new HashMap<>();
//        List<Tenant> tenants = AccessTokenAzureManager.authTid(commonTid).tenants().list();
        List<Tenant> tenants = AccessTokenAzureManager.getTenants(commonTid);
        for (Tenant t : tenants) {
            String tid = t.tenantId();
            AuthContext ac1 = createContext(tid, null);
            // put tokens into the cache
            ac1.acquireToken(env.managementEndpoint(), false, userId, isDisplayable);
            ac1.acquireToken(env.resourceManagerEndpoint(), false, userId, isDisplayable);
            ac1.acquireToken(env.graphEndpoint(), false, userId, isDisplayable);
            // TODO: remove later
            ac1.acquireToken(Constants.resourceVault, false, userId, isDisplayable);
            List<String> sids = new LinkedList<>();
            for (Subscription s : AccessTokenAzureManager.getSubscriptions(tid)) {
                sids.add(s.subscriptionId());
            }
            tidToSidsMap.put(t.tenantId(), sids);
        }

        if (!isDisplayable) {
            throw new IllegalArgumentException("accountEmail is null");
        }

        adAuthDetails.setAccountEmail(userId);
        adAuthDetails.setTidToSidsMap(tidToSidsMap);

        return result;
    }

    public Map<String, List<String>>  getAccountTenantsAndSubscriptions() {
        return adAuthDetails.getTidToSidsMap();
    }

    public void signOut() {
        cleanCache();
        adAuthDetails.setAccountEmail(null);
        adAuthDetails.setTidToSidsMap(null);
//        saveSettings();
    }

    public boolean isSignedIn() {
        return adAuthDetails.getAccountEmail() != null;
    }

    public String getAccountEmail() { return adAuthDetails.getAccountEmail(); }

    private AuthContext createContext(@NotNull final String tid, final UUID corrId) throws IOException {
    	String authority = null;
    	String endpoint = env.activeDirectoryEndpoint();
    	if (StringUtils.isNullOrEmpty(endpoint)) {
    		throw new IOException("Azure authority endpoint is empty");
    	}
    	
    	if (endpoint.endsWith("/")) {
    		authority = endpoint + tid;
    	} else {
    		authority = endpoint + "/" + tid;
    	}
        return new AuthContext(authority, Constants.clientId, Constants.redirectUri,
                this.webUi, true, corrId);
    }
    
    // logout
    private void cleanCache() {
        AdTokenCache.getInstance().clear();
    }

    private AdAuthManager() throws IOException {
        adAuthDetails = new AdAuthDetails();
        webUi = CommonSettings.getUiFactory().getWebUi();
        env = CommonSettings.getAdEnvironment();
        if (env == null) {
        	throw new IOException("Azure environment is not setup");
        }
    }
}
