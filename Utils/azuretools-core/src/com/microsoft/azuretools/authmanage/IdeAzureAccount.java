/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.resources.fluentcore.arm.AzureConfigurable;
import com.microsoft.azure.management.resources.fluentcore.arm.implementation.ManagerBase;
import com.microsoft.azure.management.resources.implementation.ResourceManager;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.IIdeStore;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.TelemetryInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.microsoft.azure.toolkit.lib.Azure.az;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;

@Slf4j
public class IdeAzureAccount {
    private static final String AUTH_CONFIG_CACHE = "auth_configuration_cache";
    private static final String SERVICE_PRINCIPAL_STORE_SERVICE = "Service Principal";
    private static final String[] namespaces = new String[]{"Microsoft.Resources", "Microsoft.Network", "Microsoft.Compute", "Microsoft.KeyVault",
        "Microsoft.Storage", "Microsoft.Web", "Microsoft.Authorization", "Microsoft.HDInsight", "Microsoft.DBforMySQL"};
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // fix the class load problem for intellij plugin
        disableLogLevelFor("com.microsoft.aad.adal4j.AuthenticationContext", "com.microsoft.aad.msal4j.PublicClientApplication",
            "com.microsoft.aad.msal4j.ConfidentialClientApplication");
    }

    private static void disableLogLevelFor(String... classes) {
        try {
            final Class<?> loggerClz = Class.forName("org.apache.log4j.Logger");
            final Class<?> loggerLevelClz = Class.forName("org.apache.log4j.Level");
            final Object offLevel = FieldUtils.readDeclaredStaticField(loggerLevelClz, "OFF");
            final Method getLoggerMethod = ClassUtils.getPublicMethod(loggerClz, "getLogger", String.class);
            if (getLoggerMethod != null) {
                for (final String className : classes) {
                    final Object logger2 = getLoggerMethod.invoke(loggerClz, className);
                    FieldUtils.writeField(logger2, "level", offLevel, true);
                }
            }
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // ignore
        }
    }

    private IdeAzureAccount() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            try {
                loginWithCachedConfiguration();
            } catch (final Throwable ex) {
                log.warn("Cannot restore login due to error: " + ex.getMessage());
            }
        });
        AzureEventBus.on("account.subscription_changed.account", new AzureEventBus.EventListener((a) -> {
            if (this.isLoggedIn()) {
                final Account account = az(AzureAccount.class).account();
                this.cacheAuthConfiguration(account.getConfig());
            }
        }));
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener((a) -> this.invalidateCache()));
    }

    private void invalidateCache() {
        final AzureStoreManager manager = AzureStoreManager.getInstance();
        final Optional<ISecureStore> secureStore = Optional.ofNullable(manager.getSecureStore());
        final Optional<IIdeStore> ideStore = Optional.ofNullable(manager.getIdeStore());
        final AzureAccount az = az(AzureAccount.class);
        secureStore.ifPresent(s -> s.forgetPassword(SERVICE_PRINCIPAL_STORE_SERVICE, az.account().getClientId(), null));
        ideStore.ifPresent(s -> s.setProperty(ACCOUNT, AUTH_CONFIG_CACHE, null));
    }

    @AzureOperation(name = "account.restore_signin", type = AzureOperation.Type.SERVICE)
    public void loginWithCachedConfiguration() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            try {
                final AuthConfiguration cache = loadAuthConfigurationCache();
                if (cache != null && CollectionUtils.isNotEmpty(cache.getSelectedSubscriptions())) {
                    AzureEventBus.emit("account.restoring_auth");
                    final Account account = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).login(cache);
                    AzureEventBus.emit("account.restored_auth");
                }
            } catch (final Throwable ex) {
                log.warn("Cannot restore login due to error: " + ex.getMessage());
            }
        });
    }

    @AzureOperation(name = "account.cache_auth_config", type = AzureOperation.Type.TASK)
    private void cacheAuthConfiguration(@Nonnull final AuthConfiguration config) {
        try {
            log.debug("cache auth configuration...");
            final String serialized = mapper.writeValueAsString(config);
            final AzureStoreManager manager = AzureStoreManager.getInstance();
            final Optional<ISecureStore> secureStore = Optional.ofNullable(manager.getSecureStore());
            final IIdeStore ideStore = manager.getIdeStore();
            ideStore.setProperty(ACCOUNT, AUTH_CONFIG_CACHE, serialized);
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
    @AzureOperation(name = "account.load_auth_config_cache", type = AzureOperation.Type.TASK)
    private static AuthConfiguration loadAuthConfigurationCache() {
        log.debug("loading auth configuration cache...");
        try {
            final AzureStoreManager manager = AzureStoreManager.getInstance();
            final Optional<ISecureStore> secureStore = Optional.ofNullable(manager.getSecureStore());
            final Optional<IIdeStore> ideStore = Optional.ofNullable(manager.getIdeStore());
            final String cache = ideStore.map(s -> s.getProperty(ACCOUNT, AUTH_CONFIG_CACHE, "")).orElse(null);
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

    public <T extends AzureConfigurable<T>, M extends ManagerBase> M authenticateForTrack1(String sid, T configurable, BiFunction<AzureTokenCredentials, T, M> auth) {
        final Subscription subscription = az(AzureAccount.class).account().getSubscription(sid);
        final AzureTokenCredentials credentials = getCredentialForTrack1(subscription.getTenantId());
        configurable
            .withInterceptor(new TelemetryInterceptor())
            .withUserAgent(CommonSettings.USER_AGENT);
        Optional.ofNullable(createProxyFromConfig()).ifPresent(proxy -> {
            configurable.withProxy(proxy);
            Optional.ofNullable(createProxyAuthenticatorFromConfig()).ifPresent(configurable::withProxyAuthenticator);
        });
        final M m = auth.apply(credentials, configurable);
        registerAzureNamespaces(m.resourceManager());
        return m;
    }

    @Nullable
    private static Proxy createProxyFromConfig() {
        final AzureConfiguration config = az().config();
        if (StringUtils.isNotBlank(config.getProxySource())) {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getHttpProxyHost(), config.getHttpProxyPort()));
        }
        return null;
    }

    @Nullable
    private static Authenticator createProxyAuthenticatorFromConfig() {
        final AzureConfiguration az = az().config();
        if (StringUtils.isNoneBlank(az.getProxySource(), az.getProxyUsername(), az.getProxyPassword())) {
            return (route, response) -> {
                final String credential = Credentials.basic(az.getProxyUsername(), az.getProxyPassword());
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            };
        }
        return null;
    }

    public AzureTokenCredentials getCredentialForTrack1(String tid) {
        final com.azure.core.management.AzureEnvironment env = az(AzureCloud.class).getOrDefault();
        final AzureEnvironment azureEnvironment = Arrays.stream(AzureEnvironment.knownEnvironments())
            .filter(e -> StringUtils.equalsIgnoreCase(env.getManagementEndpoint(), e.managementEndpoint()))
            .findFirst().orElse(AzureEnvironment.AZURE);
        return new AzureTokenCredentials(azureEnvironment, tid) {
            @Override
            public String getToken(String s) {
                final TokenRequestContext context = new TokenRequestContext().addScopes(ScopeUtil.resourceToScopes(s));
                final AccessToken token = az(AzureAccount.class).account().getTenantTokenCredential(tid).getToken(context).block();
                return Objects.requireNonNull(token, "failed to get access token for track1 lib").getToken();
            }
        };
    }

    public String getAccessTokenForTrack1(String tid) throws IOException {
        final com.azure.core.management.AzureEnvironment env = az(AzureCloud.class).getOrDefault();
        return getCredentialForTrack1(tid).getToken(env.getManagementEndpoint());
    }

    private static void registerAzureNamespaces(ResourceManager resourceManager) {
        try {
            Arrays.stream(namespaces).parallel()
                .map(resourceManager.providers()::getByName)
                .filter(provider -> !StringUtils.equalsIgnoreCase("Registered", provider.registrationState()))
                .forEach(provider -> resourceManager.providers().register(provider.namespace()));
        } catch (final Exception ignored) {
            // No need to handle this for now since this functionality will be eventually removed once the Azure SDK
            //  something similar
        }
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
