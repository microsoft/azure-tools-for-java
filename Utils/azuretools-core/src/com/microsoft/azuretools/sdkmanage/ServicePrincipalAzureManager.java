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

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import org.apache.commons.lang3.StringUtils;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AzureManagerFactory;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.SubscriptionManagerPersist;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import com.microsoft.azuretools.utils.AzureRegisterProviderNamespaces;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.rest.credentials.ServiceClientCredentials;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static com.microsoft.azuretools.Constants.FILE_NAME_SUBSCRIPTIONS_DETAILS_SP;

public class ServicePrincipalAzureManager extends AzureManagerBase {
    public static class ServicePrincipalAzureManagerFactory implements AzureManagerFactory {
        @Override
        public AzureManager factory(final AuthMethodDetails authMethodDetails) {
            final String credFilePath = authMethodDetails.getCredFilePath();
            if (StringUtils.isBlank(credFilePath)) {
                throw new IllegalArgumentException("This credential file path is blank");
            }

            final Path filePath = Paths.get(credFilePath);
            if (!Files.exists(filePath)) {
                final INotification nw = CommonSettings.getUiFactory().getNotificationWindow();
                nw.deliver("Credential File Error", "File doesn't exist: " + filePath.toString());
                throw new IllegalArgumentException("This credential file doesn't exist: " + filePath.toString());
            }

            return new ServicePrincipalAzureManager(new File(credFilePath));
        }
    }

    private final static Logger LOGGER = Logger.getLogger(ServicePrincipalAzureManager.class.getName());
    private static Settings settings;
    private final SubscriptionManager subscriptionManager;
    private final File credFile;
    private ApplicationTokenCredentials atc;
    private Environment env = null;

    static {
        settings = new Settings();
        settings.setSubscriptionsDetailsFileName(FILE_NAME_SUBSCRIPTIONS_DETAILS_SP);
    }

    public static void cleanPersist() throws IOException {
        String subscriptionsDetailsFileName = settings.getSubscriptionsDetailsFileName();
        SubscriptionManagerPersist.deleteSubscriptions(subscriptionsDetailsFileName);
    }

    public ServicePrincipalAzureManager(String tid, String appId, String appKey) {
        this.credFile = null;
        this.atc = new ApplicationTokenCredentials(appId, tid, appKey, null);
        this.subscriptionManager = new SubscriptionManagerPersist(this);
    }

    public ServicePrincipalAzureManager(File credFile) {
        this.credFile = credFile;
        this.subscriptionManager = new SubscriptionManagerPersist(this);
    }

    private Azure.Authenticated auth() throws IOException {
        Azure.Configurable azureConfigurable = Azure.configure()
                    .withInterceptor(new TelemetryInterceptor())
                    .withUserAgent(CommonSettings.USER_AGENT);
        return (atc == null)
                ? azureConfigurable.authenticate(credFile)
                : azureConfigurable.authenticate(atc);
    }

    @Override
    public Azure getAzure(String sid) throws IOException {
        if (sidToAzureMap.containsKey(sid)) {
            return sidToAzureMap.get(sid);
        }
        Azure azure = auth().withSubscription(sid);
        // TODO: remove this call after Azure SDK properly implements handling of unregistered provider namespaces
        AzureRegisterProviderNamespaces.registerAzureNamespaces(azure);
        sidToAzureMap.put(sid, azure);
        return azure;
    }

    @Override
    public AppPlatformManager getAzureSpringCloudClient(String sid) throws IOException {
        return sidToAzureSpringCloudManagerMap.computeIfAbsent(sid, s -> {
            try {
                return authSpringCloud(sid);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<Subscription> getSubscriptions() throws IOException {
        List<Subscription> sl = auth().subscriptions().list();
        return sl;
    }

    @Override
    public List<Pair<Subscription, Tenant>> getSubscriptionsWithTenant() throws IOException {
        List<Pair<Subscription, Tenant>> stl = new LinkedList<>();
        for (Tenant t : getTenants()) {
            //String tid = t.tenantId();
            for (Subscription s : getSubscriptions()) {
                stl.add(new Pair<Subscription, Tenant>(s, t));
            }
//            try {
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//            }
        }
        return stl;
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
        System.out.println("ServicePrincipalAzureManager.drop()");
        subscriptionManager.cleanSubscriptions();
    }

    public List<Tenant> getTenants() throws IOException {
        List<Tenant> tl = auth().tenants().list();
        return tl;
    }

    @Override
    public KeyVaultClient getKeyVaultClient(String tid) {
        ServiceClientCredentials creds = new KeyVaultCredentials() {
            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
                try {
                    initATCIfNeeded();
                    return atc.getToken(resource);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        return new KeyVaultClient(creds);
    }

    @Override
    public String getCurrentUserId() throws IOException {
        initATCIfNeeded();
        return atc.clientId();
    }

    @Override
    public String getAccessToken(String tid, String ignored1, PromptBehavior ignored2) throws IOException {
        String uri = getManagementURI();
        return atc.getToken(uri);
    }

    @Override
    public String getManagementURI() throws IOException {
        initATCIfNeeded();
        // default to global cloud
        return atc.environment() == null ? AzureEnvironment.AZURE.resourceManagerEndpoint() : atc.environment().resourceManagerEndpoint();
    }

    @Override
    public Environment getEnvironment() {
        initEnv();
        return env;
    }

    @Override
    public String getStorageEndpointSuffix() {
        return getEnvironment().getAzureEnvironment().storageEndpointSuffix();
    }

    private void initATCIfNeeded() throws IOException {
        if (atc == null) {
            atc = ApplicationTokenCredentials.fromFile(credFile);
        }
    }

    private void initEnv() {
        if (env != null) {
            return;
        }
        try {
            String managementURI = getManagementURI().toLowerCase();
            if (managementURI.endsWith("/")) {
                managementURI = managementURI.substring(0, managementURI.length() - 1);
            }

            if (AzureEnvironment.AZURE.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                env = Environment.GLOBAL;
            } else if (AzureEnvironment.AZURE_CHINA.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                env = Environment.CHINA;
            } else if (AzureEnvironment.AZURE_GERMANY.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                env = Environment.GERMAN;
            } else if (AzureEnvironment.AZURE_US_GOVERNMENT.resourceManagerEndpoint().toLowerCase().startsWith(managementURI)) {
                env = Environment.US_GOVERNMENT;
            } else {
                env = Environment.GLOBAL;
            }
        } catch (Exception e) {
            env = Environment.GLOBAL;
        }
    }

    private AppPlatformManager authSpringCloud(String sid) throws IOException {
        if (credFile != null) {
            throw new IOException("Cannot use Auth file for spring cloud authentication.");
        }
        return AppPlatformManager.configure()
                .withInterceptor(new TelemetryInterceptor())
                .withUserAgent(CommonSettings.USER_AGENT)
                .authenticate(atc, sid);
    }

}
