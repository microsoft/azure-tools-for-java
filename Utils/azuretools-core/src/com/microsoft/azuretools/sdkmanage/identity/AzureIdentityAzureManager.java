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

package com.microsoft.azuretools.sdkmanage.identity;

import com.azure.core.credential.TokenCredential;
import com.azure.resourcemanager.resources.fluentcore.profile.AzureProfile;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.SubscriptionManagerPersist;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureIdentityCredentialAdapter;
import com.microsoft.azuretools.sdkmanage.AzureManagerBase;
import com.microsoft.azuretools.sdkmanage.IdentityUtils;
import com.microsoft.azuretools.sdkmanage.Settings;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import com.microsoft.azuretools.utils.AzureRegisterProviderNamespaces;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AzureIdentityAzureManager extends AzureManagerBase {

    protected Settings settings = new Settings();
    protected SubscriptionManager subscriptionManager;
    protected AzureTokenCredentials credentials;
    protected Azure.Authenticated authenticated;

    @Override
    public Azure getAzure(String sid) throws IOException {
        if (!isSignedIn()) {
            return null;
        }
        if (sidToAzureMap.containsKey(sid)) {
            return sidToAzureMap.get(sid);
        }
        final Azure azure = getAuthenticated().withSubscription(sid);
        // TODO: remove this call after Azure SDK properly implements handling of unregistered provider namespaces
        AzureRegisterProviderNamespaces.registerAzureNamespaces(azure);
        sidToAzureMap.put(sid, azure);
        return azure;
    }

    @Override
    public AppPlatformManager getAzureSpringCloudClient(String sid) throws IOException {
        return isSignedIn() ? sidToAzureSpringCloudManagerMap.computeIfAbsent(sid, s ->
                buildAzureManager(AppPlatformManager.configure()).authenticate(getAzureTokenCredential(), s)) : null;
    }

    @Override
    public InsightsManager getInsightsManager(String sid) {
        return isSignedIn() ? sidToInsightsManagerMap.computeIfAbsent(sid, s ->
                buildAzureManager(InsightsManager.configure()).authenticate(getAzureTokenCredential(), s)) : null;
    }

    @Override
    public List<Subscription> getSubscriptions() {
        return isSignedIn() ? getAuthenticated().subscriptions().list() : Collections.EMPTY_LIST;
    }

    @Override
    public List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() throws IOException {
        if (!isSignedIn()) {
            return Collections.EMPTY_LIST;
        }
        final List<Subscription> subscriptionList = getSubscriptions();
        final Map<String, String> subs2tenants = getSubs2TenantMap();
        final Map<String, Tenant> tenants = getAuthenticated().tenants().list()
                .stream()
                .collect(Collectors.toMap(Tenant::tenantId, tenant -> tenant));
        return CollectionUtils.isEmpty(subscriptionList) ? Collections.EMPTY_LIST :
                subscriptionList.stream()
                        .map(subscription -> new Pair<>(subscription, tenants.get(subs2tenants.get(subscription.subscriptionId()))))
                        .collect(Collectors.toList());
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public void drop() throws IOException {
        authenticated = null;
        credentials = null;
        getSubscriptionManager().cleanSubscriptions();
    }

    @Override
    public KeyVaultClient getKeyVaultClient(String tid) throws Exception {
        if (!isSignedIn()) {
            return null;
        }
        final ServiceClientCredentials serviceClientCredentials = new KeyVaultCredentials() {
            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
                try {
                    return AzureIdentityAzureManager.this.credentials.getToken(resource);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        return new KeyVaultClient(serviceClientCredentials);
    }

    @Override
    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        return getAzureTokenCredential().getToken(resource);
    }

    @Override
    public String getManagementURI() throws IOException {
        return getEnvironment().getAzureEnvironment().resourceManagerEndpoint();
    }

    @Override
    public String getStorageEndpointSuffix() {
        return getEnvironment().getAzureEnvironment().storageEndpointSuffix();
    }

    @Override
    public Environment getEnvironment() {
        return CommonSettings.getEnvironment();
    }

    @Override
    public com.azure.resourcemanager.Azure getTrack2AzureClient(String subscriptionId) {
        return getTrack2AzureAuthenticated().withSubscription(subscriptionId);
    }

    public boolean isSignedIn() {
        return getTokenCredential() != null;
    }

    public com.azure.resourcemanager.Azure.Authenticated getTrack2AzureAuthenticated() {
        return com.azure.resourcemanager.Azure.configure()
                .authenticate(getTokenCredential(), new AzureProfile(
                        IdentityUtils.parseAzureEnvironment(CommonSettings.getEnvironment().getName())
                ));
    }

    private Map<String, String> getSubs2TenantMap() {
        return getTrack2AzureAuthenticated().subscriptions().list().stream()
                .collect(Collectors.toMap(subs -> subs.subscriptionId(), subs -> subs.inner().tenantId()));
    }

    @Override
    public synchronized SubscriptionManager getSubscriptionManager() {
        if (subscriptionManager == null) {
            subscriptionManager = new SubscriptionManagerPersist(this);
        }
        return subscriptionManager;
    }

    @Override
    public abstract String getCurrentUserId() throws IOException;

    @Nullable
    protected synchronized AzureTokenCredentials getAzureTokenCredential() {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            if (credentials == null) {
                final TokenCredential tokenCredential = getTokenCredential();
                credentials = tokenCredential == null ? null :
                        new AzureIdentityCredentialAdapter(CommonSettings.getAdEnvironment(), getCredentialTenantId(), tokenCredential);
            }
            return credentials;
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Nullable
    protected synchronized Azure.Authenticated getAuthenticated() {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            if (authenticated == null) {
                authenticated = getAzureTokenCredential() == null ? null :
                        Azure.configure()
                                .withInterceptor(new TelemetryInterceptor())
                                .withUserAgent(CommonSettings.USER_AGENT)
                                .authenticate(getAzureTokenCredential());
            }
            return authenticated;
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Nullable
    protected abstract TokenCredential getTokenCredential();

    protected String getCredentialTenantId() {
        return "common";
    }
}
