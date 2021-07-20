/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.arm.resources.AzureConfigurable;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.RestExceptionHandlerInterceptor;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import com.microsoft.azuretools.utils.AzureRegisterProviderNamespaces;
import com.microsoft.azuretools.utils.Pair;
import okhttp3.internal.http2.Settings;
import org.apache.commons.lang3.StringUtils;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;
import static com.microsoft.azuretools.authmanage.Environment.CHINA;
import static com.microsoft.azuretools.authmanage.Environment.GERMAN;
import static com.microsoft.azuretools.authmanage.Environment.GLOBAL;
import static com.microsoft.azuretools.authmanage.Environment.US_GOVERNMENT;

/**
 * Created by vlashch on 1/27/17.
 */
public abstract class AzureManagerBase implements AzureManager {
    private static final String CHINA_PORTAL = "https://portal.azure.cn";
    private static final String GLOBAL_PORTAL = "https://ms.portal.azure.com";

    private static final String CHINA_SCM_SUFFIX = ".scm.chinacloudsites.cn";
    private static final String GLOBAL_SCM_SUFFIX = ".scm.azurewebsites.net";

    private static final Logger LOGGER = Logger.getLogger(AzureManagerBase.class.getName());
    private static final String MICROSOFT_INSIGHTS_NAMESPACE = "microsoft.insights";

    protected Map<String, Azure> sidToAzureMap = new ConcurrentHashMap<>();
    protected Map<String, InsightsManager> sidToInsightsManagerMap = new ConcurrentHashMap<>();
    protected final SubscriptionManager subscriptionManager;
    protected static final Settings settings = new Settings();

    protected AzureManagerBase() {
        this.subscriptionManager = new SubscriptionManager();
    }

    @Override
    public String getPortalUrl() {
        Environment env = getEnvironment();
        if (GLOBAL.equals(env)) {
            return GLOBAL_PORTAL;
        } else if (CHINA.equals(env)) {
            return CHINA_PORTAL;
        } else if (GERMAN.equals(env)) {
            return AzureEnvironment.AZURE_GERMANY.portal();
        } else if (US_GOVERNMENT.equals(env)) {
            return AzureEnvironment.AZURE_US_GOVERNMENT.portal();
        } else {
            return env.getAzureEnvironment().portal();
        }
    }

    @Override
    public String getScmSuffix() {
        Environment env = getEnvironment();
        if (GLOBAL.equals(env)) {
            return GLOBAL_SCM_SUFFIX;
        } else if (CHINA.equals(env)) {
            return CHINA_SCM_SUFFIX;
        } else {
            return GLOBAL_SCM_SUFFIX;
        }
    }

    protected <T extends AzureConfigurable<T>> T buildAzureManager(AzureConfigurable<T> configurable) {
        return configurable.withInterceptor(new TelemetryInterceptor())
                .withUserAgent(CommonSettings.USER_AGENT);
    }

    @Override
    public List<Subscription> getSubscriptions() {
        return getSubscriptionsWithTenant().stream().map(Pair::first).collect(Collectors.toList());
    }

    @Override
    @AzureOperation(name = "account|subscription.list.tenant|authorized", type = AzureOperation.Type.SERVICE)
    public List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() {
        final List<Pair<Subscription, Tenant>> subscriptions = new LinkedList<>();
        final Azure.Authenticated authentication = authTenant(getCurrentTenantId());
        // could be multi tenant - return all subscriptions for the current account
        final List<Tenant> tenants = getTenants(authentication);
        final List<String> failedTenantIds = new ArrayList<>();
        for (final Tenant tenant : tenants) {
            try {
                final Azure.Authenticated tenantAuthentication = authTenant(tenant.tenantId());
                final List<Subscription> tenantSubscriptions = getSubscriptions(tenantAuthentication);
                for (final Subscription subscription : tenantSubscriptions) {
                    subscriptions.add(new Pair<>(subscription, tenant));
                }
            } catch (final Exception e) {
                // just skip for cases user failing to get subscriptions of tenants he/she has no permission to get access token.
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                failedTenantIds.add(tenant.tenantId());

            }
        }
        if (!failedTenantIds.isEmpty()) {
            final INotification nw = CommonSettings.getUiFactory().getNotificationWindow();
            nw.deliver("Lack permission for some tenants", "You don't have permission on the tenant(s): " + StringUtils.join(failedTenantIds, ","));
        }

        return subscriptions;
    }

    @Override
    public @Nullable Azure getAzure(String sid) {
        if (!isSignedIn()) {
            return null;
        }
        if (sidToAzureMap.containsKey(sid)) {
            return sidToAzureMap.get(sid);
        }
        final String tid = this.subscriptionManager.getSubscriptionTenant(sid);
        final Azure azure = authTenant(tid).withSubscription(sid);
        // TODO: remove this call after Azure SDK properly implements handling of unregistered provider namespaces
        AzureRegisterProviderNamespaces.registerAzureNamespaces(azure);
        sidToAzureMap.put(sid, azure);
        return azure;
    }

    public @Nullable InsightsManager getInsightsManager(String sid) {
        if (!isSignedIn()) {
            return null;
        }
        return sidToInsightsManagerMap.computeIfAbsent(sid, s -> {
            // Register insights namespace first
            final Azure azure = getAzure(sid);
            azure.providers().register(MICROSOFT_INSIGHTS_NAMESPACE);
            final String tid = this.subscriptionManager.getSubscriptionTenant(sid);
            return authApplicationInsights(sid, tid);
        });
    }

    @Override
    public SubscriptionManager getSubscriptionManager() {
        return this.subscriptionManager;
    }

    @Override
    public Environment getEnvironment() {
        if (!isSignedIn()) {
            return null;
        }
        return CommonSettings.getEnvironment();
    }

    @Override
    public @Nullable String getManagementURI() {
        if (!isSignedIn()) {
            return null;
        }
        // environments other than global cloud are not supported for interactive login for now
        return getEnvironment().getAzureEnvironment().resourceManagerEndpoint();
    }

    @Override
    public String getStorageEndpointSuffix() {
        if (!isSignedIn()) {
            return null;
        }
        return getEnvironment().getAzureEnvironment().storageEndpointSuffix();
    }

    @Override
    public void drop() {
        LOGGER.log(Level.INFO, "ServicePrincipalAzureManager.drop()");
        this.subscriptionManager.cleanSubscriptions();
    }

    protected abstract String getCurrentTenantId();

    protected boolean isSignedIn() {
        return false;
    }

    protected AzureTokenCredentials getCredentials(String tenantId) {
        return new RefreshableTokenCredentials(this, tenantId);
    }

    public List<Tenant> getTenants(String tenantId) {
        return getTenants(authTenant(tenantId));
    }

    public List<Subscription> getSubscriptions(String tenantId) {
        return getSubscriptions(authTenant(tenantId));
    }

    @AzureOperation(name = "account|subscription.list.tenant", params = {"authentication.tenantId()"}, type = AzureOperation.Type.TASK)
    private List<Subscription> getSubscriptions(Azure.Authenticated authentication) {
        return az(AzureAccount.class).account().getSubscriptions();
    }

    @AzureOperation(name = "account|tenant.list.authorized", type = AzureOperation.Type.TASK)
    protected List<Tenant> getTenants(Azure.Authenticated authentication) {
        return authentication.tenants().listAsync()
                .toList()
                .toBlocking()
                .singleOrDefault(Collections.emptyList());
    }

    @AzureOperation(name = "account|tenant.auth", params = {"tenantId"}, type = AzureOperation.Type.TASK)
    protected Azure.Authenticated authTenant(String tenantId) {
        final AzureTokenCredentials credentials = getCredentials(tenantId);
        return Azure.configure()
            .withInterceptor(new TelemetryInterceptor())
            .withInterceptor(new RestExceptionHandlerInterceptor())
            .withUserAgent(CommonSettings.USER_AGENT)
            .withProxy(createProxyFromConfig())
            .authenticate(credentials);
    }

    protected InsightsManager authApplicationInsights(String subscriptionId, String tenantId) {
        final AzureTokenCredentials credentials = getCredentials(tenantId);
        return buildAzureManager(InsightsManager.configure())
                .withInterceptor(new TelemetryInterceptor())
                .withInterceptor(new RestExceptionHandlerInterceptor())
                .withProxy(createProxyFromConfig())
                .authenticate(credentials, subscriptionId);
    }

    private static Proxy createProxyFromConfig() {
        return Optional.of(com.microsoft.azure.toolkit.lib.Azure.az().config().getHttpProxy()).map(proxy -> new Proxy(Proxy.Type.HTTP, proxy)).orElse(null);
    }
}
