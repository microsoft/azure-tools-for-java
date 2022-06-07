/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.sdkmanage;

import com.azure.identity.implementation.util.IdentityConstants;
import com.microsoft.aad.msal4jextensions.persistence.mac.ISecurityLibrary;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import com.microsoft.azuretools.utils.AzureRegisterProviderNamespaces;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;


public class IdentityAzureManager implements AzureManager {

    private static final String SERVICE_PRINCIPAL_STORE_SERVICE = "Service Principal";
    private static final String LEGACY_SECURE_STORE_SERVICE = "ADAuthManager";
    private static final String LEGACY_SECURE_STORE_KEY = "cachedAuthResult";
    private static final String CHINA_PORTAL = "https://portal.azure.cn";
    private static final String GLOBAL_PORTAL = "https://ms.portal.azure.com";

    private static final Logger LOGGER = Logger.getLogger(IdentityAzureManager.class.getName());

    private Map<String, com.microsoft.azure.management.Azure> sidToAzureMap = new ConcurrentHashMap<>();
    private final ISecureStore secureStore;
    private final SubscriptionManager subscriptionManager;

    public IdentityAzureManager() {
        this.subscriptionManager = new SubscriptionManager();
        secureStore = AzureStoreManager.getInstance().getSecureStore();
        if (secureStore != null) {
            // forgot old password, since in new auth, refresh token will be stored through azure identity persistence layer
            secureStore.forgetPassword(LEGACY_SECURE_STORE_SERVICE, LEGACY_SECURE_STORE_KEY, null);
        }
    }

    private static class LazyLoader {
        static final IdentityAzureManager INSTANCE = new IdentityAzureManager();
    }

    public static IdentityAzureManager getInstance() {
        return IdentityAzureManager.LazyLoader.INSTANCE;
    }

    @Override
    public String getPortalUrl() {
        final Environment azureEnvironment = getEnvironment();
        if (azureEnvironment == null || azureEnvironment == Environment.GLOBAL) {
            return GLOBAL_PORTAL;
        } else if (azureEnvironment == Environment.CHINA) {
            return CHINA_PORTAL;
        } else {
            return azureEnvironment.getAzureEnvironment().portal();
        }
    }

    @Override
    public @Nullable com.microsoft.azure.management.Azure getAzure(String sid) {
        if (!isSignedIn()) {
            return null;
        }
        if (sidToAzureMap.containsKey(sid)) {
            return sidToAzureMap.get(sid);
        }
        return sidToAzureMap.computeIfAbsent(sid, id -> {
            final AzureTokenCredentials credentials = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account().getTokenCredentialV1(sid);
            final com.microsoft.azure.management.Azure.Configurable configurable = com.microsoft.azure.management.Azure.configure()
                    .withInterceptor(new TelemetryInterceptor())
                    .withUserAgent(CommonSettings.USER_AGENT);
            Optional.ofNullable(createProxyFromConfig()).ifPresent(proxy -> {
                configurable.withProxy(proxy);
                Optional.ofNullable(createProxyAuthenticatorFromConfig()).ifPresent(configurable::withProxyAuthenticator);
            });
            final com.microsoft.azure.management.Azure azure = configurable.authenticate(credentials).withSubscription(sid);
            AzureRegisterProviderNamespaces.registerAzureNamespaces(azure);
            return azure;
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
        return getEnvironment().getAzureEnvironment().managementEndpoint();
    }

    @Override
    public String getStorageEndpointSuffix() {
        if (!isSignedIn()) {
            return null;
        }
        return getEnvironment().getAzureEnvironment().storageEndpointSuffix();
    }

    private static Proxy createProxyFromConfig() {
        final AzureConfiguration config = az().config();
        if (StringUtils.isNotBlank(config.getProxySource())) {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getHttpProxyHost(), config.getHttpProxyPort()));
        }
        return null;
    }

    private static Authenticator createProxyAuthenticatorFromConfig() {
        final AzureConfiguration az = az().config();
        if (StringUtils.isNoneBlank(az.getProxySource(), az.getProxyUsername(), az.getProxyPassword())) {
            return (route, response) -> {
                String credential = Credentials.basic(az.getProxyUsername(), az.getProxyPassword());
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            };
        }
        return null;
    }

    /**
     * Override the getSubscriptionDetails since az account has already loaded the subscriptions
     */
    @Deprecated
    public List<SubscriptionDetail> getSubscriptionDetails() {
        return Azure.az(AzureAccount.class).account().getSubscriptions().stream().map(subscription -> new SubscriptionDetail(
                subscription.getId(),
                subscription.getName(),
                subscription.getTenantId(),
                subscription.isSelected())).collect(Collectors.toList());
    }

    @Deprecated
    public List<Subscription> getSubscriptions() {
        return Azure.az(AzureAccount.class).account().getSubscriptions();
    }

    public void selectSubscriptionByIds(List<String> subscriptionIds) {
        Azure.az(AzureAccount.class).account().selectSubscription(subscriptionIds);
    }

    @Override
    public Subscription getSubscriptionById(String sid) {
        return Azure.az(AzureAccount.class).account().getSubscription(sid);
    }

    @Override
    public List<Subscription> getSelectedSubscriptions() {
        if (!isSignedIn()) {
            return new ArrayList<>();
        }
        return Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
    }

    public Mono<AuthMethodDetails> signInAzureCli() {
        AzureAccount az = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class);
        return az.loginAsync(AuthType.AZURE_CLI, false).flatMap(Account::continueLogin).map(account -> fromAccountEntity(account.getEntity()));
    }

    public Mono<AuthMethodDetails> signInOAuth() {
        AzureAccount az = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class);
        return az.loginAsync(AuthType.OAUTH2, shallEnablePersistence()).flatMap(Account::continueLogin).map(account -> fromAccountEntity(account.getEntity()));
    }

    public static boolean shallEnablePersistence() {
        if (SystemUtils.IS_OS_MAC) {
            try {
                ISecurityLibrary.library.CFRelease(null);
            } catch (final Throwable ex) {
                return false;
            }
        }
        return true;
    }

    public Mono<AuthMethodDetails> restoreSignIn(AuthMethodDetails authMethodDetails) {
        if (authMethodDetails == null || authMethodDetails.getAuthMethod() == null || authMethodDetails.getAuthType() == null) {
            return Mono.just(new AuthMethodDetails());
        }
        if (StringUtils.isNotBlank(authMethodDetails.getAzureEnv())) {
            Azure.az(AzureCloud.class).setByName(authMethodDetails.getAzureEnv());
        }
        AuthType authType = authMethodDetails.getAuthType();
        try {
            if (authType == AuthType.SERVICE_PRINCIPAL) {
                AuthConfiguration auth = new AuthConfiguration();
                auth.setType(AuthType.SERVICE_PRINCIPAL);
                auth.setClient(authMethodDetails.getClientId());
                if (CollectionUtils.isNotEmpty(authMethodDetails.getTenantIds())) {
                    auth.setTenant(authMethodDetails.getTenantIds().get(0));
                } else {
                    auth.setTenant(authMethodDetails.getTenantId());
                }

                auth.setEnvironment(Azure.az(AzureCloud.class).get());
                if (StringUtils.isNotBlank(authMethodDetails.getCertificate())) {
                    auth.setCertificate(authMethodDetails.getCertificate());
                } else {
                    secureStore.migratePassword(
                        "account|" + auth.getClient(),
                        null,
                        SERVICE_PRINCIPAL_STORE_SERVICE,
                        auth.getClient(), null);
                    String key = secureStore == null ? null : secureStore.loadPassword(SERVICE_PRINCIPAL_STORE_SERVICE,
                        authMethodDetails.getClientId(), null);
                    if (StringUtils.isBlank(key)) {
                        throw new AzureToolkitRuntimeException(
                                String.format("Cannot find SP security key for '%s' in intellij key pools.", authMethodDetails.getClientId()));
                    }
                    auth.setKey(key);
                }
                return signInServicePrincipal(auth).map(ac -> authMethodDetails);
            } else {
                final AccountEntity entity = new AccountEntity();
                entity.setType(authType);
                entity.setEnvironment(Azure.az(AzureCloud.class).get());
                entity.setEmail(authMethodDetails.getAccountEmail());
                entity.setClientId(StringUtils.isBlank(authMethodDetails.getClientId()) ?
                        IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID : authMethodDetails.getClientId());
                entity.setTenantIds(authMethodDetails.getTenantIds());
                entity.setSubscriptions(authMethodDetails.getSubscriptions());
                Account account = Azure.az(AzureAccount.class).account(entity);
                return Mono.just(fromAccountEntity(account.getEntity()));
            }
        } catch (Throwable e) {
            if (StringUtils.isNotBlank(authMethodDetails.getClientId()) && authMethodDetails.getAuthType() == AuthType.SERVICE_PRINCIPAL &&
                    secureStore != null) {
                secureStore.forgetPassword(SERVICE_PRINCIPAL_STORE_SERVICE, authMethodDetails.getClientId(), null);
            }
            return Mono.error(new AzureToolkitRuntimeException(String.format("Cannot restore credentials due to error: %s", e.getMessage()), e));
        }
    }

    public Mono<AuthMethodDetails> signInServicePrincipal(AuthConfiguration auth) {
        return Azure.az(AzureAccount.class).loginAsync(auth, false).flatMap(Account::continueLogin).map(account -> {
            if (secureStore != null && StringUtils.isNotBlank(auth.getKey())) {
                secureStore.savePassword(SERVICE_PRINCIPAL_STORE_SERVICE, auth.getClient(), null, auth.getKey());
            }
            AuthMethodDetails authMethodDetails = fromAccountEntity(account.getEntity());
            // special handle for SP
            authMethodDetails.setCertificate(auth.getCertificate());
            return authMethodDetails;
        });
    }

    public boolean isSignedIn() {
        try {
            Azure.az(AzureAccount.class).account();
            return true;
        } catch (AzureToolkitAuthenticationException ex) {
            return false;
        }
    }

    @Override
    public String getAccessToken(String tid, String resource) throws IOException {
        return Azure.az(AzureAccount.class).account().getTokenCredentialForTenantV1(tid).getToken(resource);
    }

    @Override
    public void drop() {
        if (!isSignedIn()) {
            return;
        }
        LOGGER.log(Level.INFO, "IdentityAzureManager.drop()");
        final AzureAccount az = Azure.az(AzureAccount.class);
        final AccountEntity account = az.account().getEntity();
        if (StringUtils.isNotBlank(account.getClientId()) && account.getType() == AuthType.SERVICE_PRINCIPAL && secureStore != null) {
            secureStore.forgetPassword(SERVICE_PRINCIPAL_STORE_SERVICE, account.getClientId(), null);
        }
        az.logout();
        this.subscriptionManager.cleanSubscriptions();
    }

    private static AuthMethodDetails fromAccountEntity(AccountEntity entity) {
        AuthMethodDetails authMethodDetails = new AuthMethodDetails();
        authMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
        authMethodDetails.setAuthType(entity.getType());
        authMethodDetails.setClientId(entity.getClientId());
        authMethodDetails.setTenantIds(entity.getTenantIds());
        authMethodDetails.setSubscriptions(entity.getSubscriptions());
        authMethodDetails.setAzureEnv(AzureEnvironmentUtils.getCloudName(entity.getEnvironment()));
        authMethodDetails.setAccountEmail(entity.getEmail());
        if (StringUtils.isBlank(entity.getClientId())) {
            // check whether toolkit will receive empty client id during authentication
            OperationContext.action().setTelemetryProperty("isEmptyClientId", String.valueOf(true));
        }
        return authMethodDetails;
    }
}
