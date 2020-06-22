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

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.authmanage.AzureManagerFactory;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import com.microsoft.azuretools.utils.AzureRegisterProviderNamespaces;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azuretools.Constants.FILE_NAME_SUBSCRIPTIONS_DETAILS_AZ;
import static com.microsoft.azuretools.authmanage.Environment.ENVIRONMENT_LIST;

public class AzureCliAzureManager extends AzureManagerBase {
    private static final String FAILED_TO_AUTH_WITH_AZURE_CLI = "Failed to auth with Azure CLI";
    private static final String UNABLE_TO_GET_AZURE_CLI_CREDENTIALS = "Unable to get Azure CLI credentials, " +
            "please ensure you have installed Azure CLI and signed in.";

    private static Settings settings;
    private Azure.Authenticated authenticated;
    private AzureCliCredentials azureCliCredentials;
    private SubscriptionManager subscriptionManager;

    static {
        settings = new Settings();
        settings.setSubscriptionsDetailsFileName(FILE_NAME_SUBSCRIPTIONS_DETAILS_AZ);
    }

    private static class LazyLoader {
        static final AzureCliAzureManager INSTANCE = new AzureCliAzureManager();
    }

    public static class AzureCliAzureManagerFactory implements AzureManagerFactory {

        @Override
        public @Nullable AzureManager factory(AuthMethodDetails authMethodDetails) {
            return getInstance().isSignedIn() ? getInstance() : null;
        }

        @Override
        public AuthMethodDetails restore(final AuthMethodDetails authMethodDetails) {
            try {
                getInstance().signIn();
            } catch (AzureExecutionException e) {
                // Catch the exception when restore
            } finally {
                return authMethodDetails;
            }
        }
    }

    public static AzureCliAzureManager getInstance() {
        return LazyLoader.INSTANCE;
    }

    @Override
    public Azure getAzure(String sid) throws IOException {
        if (sidToAzureMap.containsKey(sid)) {
            return sidToAzureMap.get(sid);
        }
        final Azure azure = auth().withSubscription(sid);
        // TODO: remove this call after Azure SDK properly implements handling of unregistered provider namespaces
        AzureRegisterProviderNamespaces.registerAzureNamespaces(azure);
        sidToAzureMap.put(sid, azure);
        return azure;
    }

    @Override
    public AppPlatformManager getAzureSpringCloudClient(String sid) {
        return sidToAzureSpringCloudManagerMap.computeIfAbsent(sid, s ->
                buildAzureManager(AppPlatformManager.configure()).authenticate(azureCliCredentials, s));
    }

    @Override
    public InsightsManager getInsightsManager(String sid) {
        return sidToInsightsManagerMap.computeIfAbsent(sid, s ->
                buildAzureManager(InsightsManager.configure()).authenticate(azureCliCredentials, s));
    }

    @Override
    public List<Subscription> getSubscriptions() {
        return authenticated.subscriptions().list();
    }

    @Override
    public List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() {
        final Tenant subscriptionTenant = authenticated.tenants().list().stream()
                .filter(tenant -> StringUtils.equals(tenant.tenantId(), authenticated.tenantId()))
                .findFirst().orElse(null);
        final List<Pair<Subscription, Tenant>> result = new ArrayList<>();
        for (Subscription subscription : getSubscriptions()) {
            result.add(new Pair<>(subscription, subscriptionTenant));
        }
        return result;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    @Override
    public void drop() throws IOException {
        authenticated = null;
        azureCliCredentials = null;
        subscriptionManager.cleanSubscriptions();
    }

    @Override
    public KeyVaultClient getKeyVaultClient(String tid) {
        final ServiceClientCredentials credentials = new KeyVaultCredentials() {
            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
                try {
                    return azureCliCredentials.getToken(resource);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        return new KeyVaultClient(credentials);
    }

    @Override
    public String getCurrentUserId() {
        return azureCliCredentials.clientId();
    }

    @Override
    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        return azureCliCredentials.getToken(resource);
    }

    @Override
    public String getManagementURI() {
        return azureCliCredentials.environment().managementEndpoint();
    }

    @Override
    public String getStorageEndpointSuffix() {
        return getEnvironment().getAzureEnvironment().storageEndpointSuffix();
    }

    @Override
    public Environment getEnvironment() {
        final AzureEnvironment azureEnvironment = azureCliCredentials.environment();
        return ENVIRONMENT_LIST.stream()
                .filter(environment -> azureEnvironment == environment.getAzureEnvironment())
                .findAny()
                .orElse(Environment.GLOBAL);
    }

    public boolean isSignedIn() {
        return authenticated != null && azureCliCredentials != null;
    }

    public AuthMethodDetails signIn() throws AzureExecutionException {
        try {
            AzureTokenWrapper azureTokenWrapper = AzureAuthHelper.getAzureCLICredential();
            if (azureTokenWrapper == null) {
                throw new AzureExecutionException(UNABLE_TO_GET_AZURE_CLI_CREDENTIALS);
            }
            azureCliCredentials = (AzureCliCredentials) azureTokenWrapper.getAzureTokenCredentials();
            authenticated = Azure.configure().authenticate(azureCliCredentials);
            subscriptionManager = new SubscriptionManager(this);

            final AuthMethodDetails authResult = new AuthMethodDetails();
            authResult.setAuthMethod(AuthMethod.AZ);
            authResult.setAzureEnv(azureCliCredentials.environment().toString());
            return authResult;
        } catch (IOException e) {
            try {
                drop();
            } catch (IOException ex) {
                // swallow exception while clean up
            }
            throw new AzureExecutionException(FAILED_TO_AUTH_WITH_AZURE_CLI, e);
        }
    }

    private Azure.Authenticated auth() throws IOException {
        return Azure.configure()
                .withInterceptor(new TelemetryInterceptor())
                .withUserAgent(CommonSettings.USER_AGENT)
                .authenticate(AzureCliCredentials.create());
    }
}
