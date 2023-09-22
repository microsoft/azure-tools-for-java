/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import static com.microsoft.azure.toolkit.lib.Azure.az;

@Slf4j
public class IdeAzureAccount {
    private static final String AUTH_CONFIG_CACHE = "auth_configuration_cache";
    private static final String SERVICE_PRINCIPAL_STORE_SERVICE = "Service Principal";
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // fix the class load problem for intellij plugin
        disableLog("com.microsoft.aad.adal4j.AuthenticationContext", "com.microsoft.aad.msal4j.PublicClientApplication",
            "com.microsoft.aad.msal4j.ConfidentialClientApplication");
    }

    private static void disableLog(String... classes) {
        for (final String className : classes) {
            try {
                final Logger logger = LoggerFactory.getLogger(className);
                final Field innerLoggerField = FieldUtils.getDeclaredField(logger.getClass(), "logger", true);
                final Object innerLogger = innerLoggerField.get(logger);
                if (innerLogger instanceof java.util.logging.Logger) {
                    ((java.util.logging.Logger) innerLogger).setLevel(Level.OFF);
                }
            } catch (final Throwable e) {
                // swallow exceptions here
            }
        }
    }

    private IdeAzureAccount() {
        AzureEventBus.on("account.subscription_changed.account", new AzureEventBus.EventListener((a) -> {
            if (this.isLoggedIn()) {
                final Account account = az(AzureAccount.class).account();
                this.cacheAuthConfiguration(account.getConfig());
            }
        }));
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener((a) -> this.invalidateCache((Account) a.getSource())));
    }

    private void invalidateCache(@Nonnull final Account account) {
        final AzureStoreManager manager = AzureStoreManager.getInstance();
        final Optional<ISecureStore> secureStore = Optional.ofNullable(manager.getSecureStore());
        secureStore.ifPresent(s -> s.forgetPassword(SERVICE_PRINCIPAL_STORE_SERVICE, account.getClientId(), null));
        Azure.az().config().set("account.auth_configuration_cache", null);
    }

    @AzureOperation(name = "auto/account.restore_signin")
    public void restoreSignin() {
        this.loginWithCachedConfiguration();
    }

    @AzureOperation(name = "internal/account.restore_signin")
    private void loginWithCachedConfiguration() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            try {
                final AuthConfiguration cache = loadAuthConfigurationCache();
                if (cache != null && CollectionUtils.isNotEmpty(cache.getSelectedSubscriptions())) {
                    AzureEventBus.emit("account.restoring_auth");
                    final Account account = Azure.az(AzureAccount.class).login(cache);
                    AzureEventBus.emit("account.restored_auth");
                }
            } catch (final Throwable ex) {
                log.warn("Cannot restore login due to error: " + ex.getMessage());
            }
        });
    }

    @AzureOperation(name = "boundary/account.cache_auth_config")
    private void cacheAuthConfiguration(@Nonnull final AuthConfiguration config) {
        try {
            log.debug("cache auth configuration...");
            final String serialized = mapper.writeValueAsString(config);
            final AzureStoreManager manager = AzureStoreManager.getInstance();
            final Optional<ISecureStore> secureStore = Optional.ofNullable(manager.getSecureStore());
            Azure.az().config().set("account.auth_configuration_cache", serialized);
            if (config.getType() == AuthType.SERVICE_PRINCIPAL) {
                final String password = Objects.nonNull(config.getCertificate()) ? config.getCertificatePassword() : config.getKey();
                if (StringUtils.isBlank(password)) {
                    throw new AzureToolkitRuntimeException("invalid service principal.");
                }
                secureStore.ifPresent(s -> s.savePassword(SERVICE_PRINCIPAL_STORE_SERVICE, config.getClient(), null, password));
            }
        } catch (final IOException e) {
            final String error = "Failed to cache auth configuration.";
            log.warn(error);
        }
    }

    @Nullable
    @AzureOperation(name = "boundary/account.load_auth_config_cache")
    private static AuthConfiguration loadAuthConfigurationCache() {
        log.debug("loading auth configuration cache...");
        try {
            final AzureStoreManager manager = AzureStoreManager.getInstance();
            final Optional<ISecureStore> secureStore = Optional.ofNullable(manager.getSecureStore());
            final String cache = az().config().getString("account.auth_configuration_cache");
            if (StringUtils.isNotBlank(cache)) {
                final AuthConfiguration config = mapper.readValue(cache, AuthConfiguration.class);
                if (config.getType() == AuthType.SERVICE_PRINCIPAL) {
                    final String password = secureStore.map(s -> s.loadPassword(SERVICE_PRINCIPAL_STORE_SERVICE, config.getClient(), null)).orElse(null);
                    if (Objects.nonNull(config.getCertificate())) {
                        config.setCertificatePassword(password);
                    } else if (StringUtils.isNotBlank(password)) {
                        config.setKey(password);
                    } else {
                        throw new AzureToolkitRuntimeException("invalid service principal.");
                    }
                }
                return config;
            }
            log.debug("No auth configuration cache are found.");
        } catch (final IOException ignored) {
            log.debug("Failed to load auth configuration cache.");
        }
        return null;
    }

    public String getAccessTokenByTrack2(String tenantId, String resourceId) {
        final TokenRequestContext context = new TokenRequestContext().addScopes(ScopeUtil.resourceToScopes(resourceId));
        final AccessToken token = az(AzureAccount.class).account().getTenantTokenCredential(tenantId).getToken(context).block();
        return Objects.requireNonNull(token, "failed to get access token for track2 lib").getToken();
    }

    public boolean isLoggedIn() {
        return Azure.az(AzureAccount.class).isLoggedIn();
    }

    private static class LazyHolder {
        static final IdeAzureAccount INSTANCE = new IdeAzureAccount();
    }

    public static IdeAzureAccount getInstance() {
        return LazyHolder.INSTANCE;
    }
}
