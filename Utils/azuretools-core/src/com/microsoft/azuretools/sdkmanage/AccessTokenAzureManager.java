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

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.SubscriptionManagerPersist;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import com.microsoft.azuretools.utils.AzureRegisterProviderNamespaces;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import rx.Observable;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class AccessTokenAzureManager extends AzureManagerBase {
    private final static Logger LOGGER = Logger.getLogger(AccessTokenAzureManager.class.getName());
    private final SubscriptionManager subscriptionManager;

    public AccessTokenAzureManager() {
        this.subscriptionManager = new SubscriptionManagerPersist(this);
    }

    @Override
    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    @Override
    public void drop() throws IOException {
        subscriptionManager.cleanSubscriptions();
        AdAuthManager.getInstance().signOut();
    }

    private static Settings settings;

    static {
        settings = new Settings();
        settings.setSubscriptionsDetailsFileName("subscriptionsDetails-at.json");
    }

    @Override
    public Azure getAzure(String sid) throws IOException {
        if (sidToAzureMap.containsKey(sid)) {
            return sidToAzureMap.get(sid);
        }
        String tid = subscriptionManager.getSubscriptionTenant(sid);
        Azure azure = authTid(tid).withSubscription(sid);
        // TODO: remove this call after Azure SDK properly implements handling of unregistered provider namespaces
        AzureRegisterProviderNamespaces.registerAzureNamespaces(azure);
        sidToAzureMap.put(sid, azure);
        return azure;
    }

    @Override
    public List<Subscription> getSubscriptions() throws IOException {
        List<Subscription> sl = new LinkedList<Subscription>();
        // could be multi tenant - return all subscriptions for the current account
        List<Tenant> tl = getTenants(AdAuthManager.getInstance().getCommonTenantId());
        for (Tenant t : tl) {
            sl.addAll(getSubscriptions(t.tenantId()));
        }
        return sl;
    }

    @Override
    public List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() throws IOException {
        List<Pair<Subscription, Tenant>> stl = new LinkedList<>();
        for (Tenant t : getTenants(AdAuthManager.getInstance().getCommonTenantId())) {
            String tid = t.tenantId();
            for (Subscription s : getSubscriptions(tid)) {
                stl.add(new Pair<Subscription, Tenant>(s, t));
            }
        }
        return stl;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    public static List<Subscription> getSubscriptions(String tid) throws IOException {
        List<Subscription> sl = authTid(tid).subscriptions().listAsync()
                .onErrorResumeNext(err -> {
                    LOGGER.warning(err.getMessage());

                    return Observable.empty();
                })
                .toList()
                .toBlocking()
                .singleOrDefault(Collections.emptyList());

        return sl;
    }

    public static List<Tenant> getTenants(String tid) throws IOException {
        List<Tenant> tl = authTid(tid).tenants().listAsync()
                .onErrorResumeNext(err -> {
                    LOGGER.warning(err.getMessage());

                    return Observable.empty();
                })
                .toList()
                .toBlocking()
                .singleOrDefault(Collections.emptyList());

        return tl;
    }

//    public static Azure.Authenticated auth(String accessToken) throws Exception {
//        return Azure.configure().authenticate(getTokenCredentials(accessToken));
//    }

//    private static TokenCredentials getTokenCredentials(String token) throws Exception {
//        return null;
//    }

    private static Azure.Authenticated authTid(String tid) throws IOException {
//        String token = AdAuthManager.getInstance().getAccessToken(tid);
//        return auth(token);
        return Azure.configure()
                .withInterceptor(new TelemetryInterceptor())
                .withUserAgent(CommonSettings.USER_AGENT).authenticate(new RefreshableTokenCredentials(AdAuthManager.getInstance(), tid));
    }

    @Override
    public KeyVaultClient getKeyVaultClient(String tid) {
        ServiceClientCredentials creds = new KeyVaultCredentials() {
            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
            try {
            	// TODO: check usage
                return AdAuthManager.getInstance().getAccessToken(tid, resource, PromptBehavior.Auto);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            }
        };

        return new KeyVaultClient(creds);
    }

    @Override
    public String getCurrentUserId() throws IOException {
        return AdAuthManager.getInstance().getAccountEmail();
    }

    @Override
    public String getAccessToken(String tid) throws IOException {
        return AdAuthManager.getInstance().getAccessToken(tid, CommonSettings.getAdEnvironment().resourceManagerEndpoint(), PromptBehavior.Auto);
    }

    @Override
    public String getManagementURI() throws IOException {
        // environments other than global cloud are not supported for interactive login for now
        return CommonSettings.getAdEnvironment().resourceManagerEndpoint();
    }

    public String getStorageEndpointSuffix() {
        return CommonSettings.getAdEnvironment().storageEndpointSuffix();
    }
    
    @Override
    public Environment getEnvironment() {
        return CommonSettings.getEnvironment();
    }
}
