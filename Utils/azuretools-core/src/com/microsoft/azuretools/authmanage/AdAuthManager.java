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
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AdAuthManager {
    private final static Logger LOGGER = Logger.getLogger(AdAuthManager.class.getName());
    private final TokenCache cache;
    private TokenFileStorage tokenFileStorage;
    private static AdAuthManager instance = null;
    //private static String adAuthSettingsFileName = "AdAuthDetails.json";
    private static AdAuthDetails adAuthDetails = new AdAuthDetails();
    //private static Map<String, List<String>> tidToSidsMap = new HashMap<>();

    public static AdAuthManager getInstance() throws IOException {
        if( instance == null) {
            AuthContext.setUserDefinedWebUi(CommonSettings.getUiFactory().getWebUi());
            instance = new AdAuthManager(false);
            // load accountEmail
//            instance.loadSettings();
        }
        return instance;
    }

    public String getAccessToken(String tid) throws IOException {
        return getAccessToken(tid, Constants.resourceARM, PromptBehavior.Auto);
    }

    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        AuthContext ac = new AuthContext(String.format("%s/%s", Constants.authority, tid), cache);
        AuthenticationResult result = ac.acquireToken(resource, Constants.clientId, Constants.redirectUri, promptBehavior, null);
        return result.getAccessToken();
    }

    public AuthenticationResult signIn() throws IOException {

        // build token cache for azure and graph api
        // using azure sdk directly

        cleanCache();
        String commonTid = "common";
        AuthContext ac = new AuthContext(String.format("%s/%s", Constants.authority, commonTid), cache);

        AuthenticationResult result = ac.acquireToken(AzureEnvironment.AZURE.resourceManagerEndpoint(), Constants.clientId, Constants.redirectUri, PromptBehavior.Always, null);
        String uniqueId = result.getUserInfo().getUniqueId();
        String displayableId = result.getUserInfo().getDisplayableId();
        UserIdentifier uid;
        if (uniqueId != null) {
            uid = new UserIdentifier(uniqueId, UserIdentifierType.UniqueId);
        } else {
            uid = new UserIdentifier(displayableId, UserIdentifierType.RequiredDisplayableId);
        }

        Map<String, List<String>> tidToSidsMap = new HashMap<>();
//        List<Tenant> tenants = AccessTokenAzureManager.authTid(commonTid).tenants().list();
        List<Tenant> tenants = AccessTokenAzureManager.getTenants(commonTid);
        for (Tenant t : tenants) {
            String tid = t.tenantId();
            AuthContext ac1 = new AuthContext(String.format("%s/%s", Constants.authority, tid), cache);
            // put tokens into the cache
            ac1.acquireToken(AzureEnvironment.AZURE.resourceManagerEndpoint(), Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, uid);
            ac1.acquireToken(AzureEnvironment.AZURE.graphEndpoint(), Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, uid);
            ac1.acquireToken(Constants.resourceVault, Constants.clientId, Constants.redirectUri, PromptBehavior.Auto, uid);
            List<String> sids = new LinkedList<>();
            for (Subscription s : AccessTokenAzureManager.getSubscriptions(tid)) {
                sids.add(s.subscriptionId());
            }
            tidToSidsMap.put(t.tenantId(), sids);
        }

        // save account email
        if (displayableId == null) {
            throw new IllegalArgumentException("accountEmail is null");
        }

        adAuthDetails.setAccountEmail(displayableId);
        adAuthDetails.setTidToSidsMap(tidToSidsMap);
//        saveSettings();

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

    // logout
    public void cleanCache() {
        cache.clear();
    }

    private AdAuthManager(boolean useFileCache) throws IOException {
        cache = new TokenCache();
        if (useFileCache) {
            tokenFileStorage = new TokenFileStorage(CommonSettings.settingsBaseDir);
            byte[] data = tokenFileStorage.read();
            cache.deserialize(data);

            cache.setOnAfterAccessCallback(new Runnable() {
                public void run() {
                    try {
                        if(cache.getHasStateChanged()) {
                            tokenFileStorage.write(cache.serialize());
                            cache.setHasStateChanged(false);
                        }
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            });
        }
    }

//    private void loadSettings() {
//        System.out.println("loadSettings()");
//        FileStorage fs = new FileStorage(adAuthSettingsFileName, settingsBaseDir);
//        byte[] data = fs.read();
//        String json = new String(data);
//        if (json.isEmpty()) {
//            adAuthDetails = new AdAuthDetails();
//            System.out.println(adAuthSettingsFileName + "file is empty");
//            return;
//        }
//        adAuthDetails = JsonHelper.deserialize(AdAuthDetails.class, json);
//    }

//    private void saveSettings() {
//        System.out.println("saveSettings()");
//        String sd = JsonHelper.serialize(adAuthDetails);
//        FileStorage fs = new FileStorage(adAuthSettingsFileName, settingsBaseDir);
//        fs.write(sd.getBytes(Charset.forName("utf-8")));
//    }
}
