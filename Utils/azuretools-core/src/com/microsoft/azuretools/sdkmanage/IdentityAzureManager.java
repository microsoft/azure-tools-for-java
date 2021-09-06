/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.lib.Azure;
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
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class IdentityAzureManager extends AzureManagerBase {

    private static final String SERVICE_PRINCIPAL_STORE_SERVICE = "Service Principal";
    private ISecureStore secureStore;

    private static final String LEGACY_SECURE_STORE_SERVICE = "ADAuthManager";
    private static final String LEGACY_SECURE_STORE_KEY = "cachedAuthResult";

    public IdentityAzureManager() {
        secureStore = AzureStoreManager.getInstance().getSecureStore();
        if (secureStore != null) {
            // forgot old password, since in new auth, refresh token will be stored through azure identity persistence layer
            secureStore.forgetPassword(LEGACY_SECURE_STORE_SERVICE, LEGACY_SECURE_STORE_KEY, null);
        }
    }

    protected AzureTokenCredentials getCredentials(String tenantId) {
        return Azure.az(AzureAccount.class).account().getTokenCredentialForTenantV1(tenantId);
    }

    private static class LazyLoader {
        static final IdentityAzureManager INSTANCE = new IdentityAzureManager();
    }

    public static IdentityAzureManager getInstance() {
        return IdentityAzureManager.LazyLoader.INSTANCE;
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

    public List<String> getSelectedSubscriptionIds() {
        if (!isSignedIn()) {
            return new ArrayList<>();
        }
        List<Subscription> selectedSubscriptions = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
        if (CollectionUtils.isNotEmpty(selectedSubscriptions)) {
            return selectedSubscriptions.stream().map(Subscription::getId).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<Subscription> getSelectedSubscriptions() {
        if (!isSignedIn()) {
            return new ArrayList<>();
        }
        return Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
    }

    @Override
    protected List<Tenant> getTenants(com.microsoft.azure.management.Azure.Authenticated authentication) {
        if (!isSignedIn()) {
            return new ArrayList<>();
        }
        final List<String> tenantIds = Azure.az(AzureAccount.class).account().getEntity().getTenantIds();
        // override the tenants from super
        return super.getTenants(authentication).stream().filter(tenant -> tenantIds.contains(tenant.tenantId())).collect(Collectors.toList());
    }

    public Mono<AuthMethodDetails> signInAzureCli() {
        AzureAccount az = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class);
        return az.loginAsync(AuthType.AZURE_CLI, false).flatMap(Account::continueLogin).map(account -> fromAccountEntity(account.getEntity()));
    }

    public Mono<AuthMethodDetails> signInOAuth() {
        AzureAccount az = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class);
        return az.loginAsync(AuthType.OAUTH2, true).flatMap(Account::continueLogin).map(account -> fromAccountEntity(account.getEntity()));
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
                auth.setTenant(authMethodDetails.getTenantId());
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
                if (StringUtils.isNotBlank(authMethodDetails.getClientId())) {
                    AccountEntity entity = new AccountEntity();
                    entity.setType(authType);
                    entity.setEnvironment(Azure.az(AzureCloud.class).get());
                    entity.setEmail(authMethodDetails.getAccountEmail());
                    entity.setClientId(authMethodDetails.getClientId());
                    entity.setTenantIds(StringUtils.isNotBlank(authMethodDetails.getTenantId()) ?
                                        Collections.singletonList(authMethodDetails.getTenantId()) : null);
                    Account account = Azure.az(AzureAccount.class).account(entity);
                    return Mono.just(fromAccountEntity(account.getEntity()));
                } else {
                    throw new AzureToolkitRuntimeException("Cannot restore credentials due to version change.");
                }
            }

        } catch (Throwable e) {
            if (StringUtils.isNotBlank(authMethodDetails.getClientId())
                && authMethodDetails.getAuthType() == AuthType.SERVICE_PRINCIPAL && secureStore != null) {
                secureStore.forgetPassword(SERVICE_PRINCIPAL_STORE_SERVICE, authMethodDetails.getClientId(), null);
            }
            return Mono.error(new AzureToolkitRuntimeException(String.format("Cannot restore credentials due to error: %s", e.getMessage())));
        }
    }

    private static String getSecureStoreKey(String clientId) {
        return StringUtils.joinWith("|", "account", clientId);
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
    public String getCurrentUserId() {
        if (!isSignedIn()) {
            return null;
        }
        return StringUtils.firstNonBlank(Azure.az(AzureAccount.class).account().getEntity().getEmail(), "unknown");
    }

    @Override
    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        return Azure.az(AzureAccount.class).account().getTokenCredentialForTenantV1(tid).getToken(resource);
    }

    @Override
    protected String getCurrentTenantId() {
        return "common";
    }

    @Override
    public void drop() {
        if (!isSignedIn()) {
            return;
        }
        final AzureAccount az = Azure.az(AzureAccount.class);
        final AccountEntity account = az.account().getEntity();
        if (StringUtils.isNotBlank(account.getClientId()) && account.getType() == AuthType.SERVICE_PRINCIPAL && secureStore != null) {
            secureStore.forgetPassword(SERVICE_PRINCIPAL_STORE_SERVICE, account.getClientId(), null);
        }
        az.logout();
        super.drop();
    }

    private static AuthMethodDetails fromAccountEntity(AccountEntity entity) {
        AuthMethodDetails authMethodDetails = new AuthMethodDetails();
        authMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
        authMethodDetails.setAuthType(entity.getType());
        authMethodDetails.setClientId(entity.getClientId());
        authMethodDetails.setTenantId(CollectionUtils.isEmpty(entity.getTenantIds()) ? "" : entity.getTenantIds().get(0));
        authMethodDetails.setAzureEnv(AzureEnvironmentUtils.getCloudNameForAzureCli(entity.getEnvironment()));
        authMethodDetails.setAccountEmail(entity.getEmail());
        return authMethodDetails;
    }
}
