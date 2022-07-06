/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.Constants.FILE_NAME_AUTH_METHOD_DETAILS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.AZURE_ENVIRONMENT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.RESIGNIN;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNIN_METHOD;

@Slf4j
public class AuthMethodManager {
    public static final String AUTH_METHOD_DETAIL = "auth_method_detail";
    public static final String AUTH_TYPE = "authType";
    private AuthMethodDetails authMethodDetails;
    private final Set<Runnable> signInEventListeners = new HashSet<>();
    private final Set<Runnable> signOutEventListeners = new HashSet<>();
    private final CompletableFuture<Boolean> initFuture = new CompletableFuture<>();
    private final IdentityAzureManager identityAzureManager = IdentityAzureManager.getInstance();

    static {
        // fix the class load problem for intellij plugin
        disableLog("com.microsoft.aad.adal4j.AuthenticationContext", "com.microsoft.aad.msal4j.PublicClientApplication",
                "com.microsoft.aad.msal4j.ConfidentialClientApplication");
    }

    private static void disableLog(String... classes) {
        for (String className : classes) {
            try {
                final Logger logger = LoggerFactory.getLogger(className);
                final Field innerLoggerField = FieldUtils.getDeclaredField(logger.getClass(), "logger", true);
                final Object innerLogger = innerLoggerField.get(logger);
                if (innerLogger instanceof java.util.logging.Logger) {
                    ((java.util.logging.Logger) innerLogger).setLevel(Level.OFF);
                }
            } catch (Throwable e) {
                // swallow exceptions here
            }
        }
    }

    private static class LazyHolder {
        static final AuthMethodManager INSTANCE = new AuthMethodManager();
    }

    public static AuthMethodManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private AuthMethodManager() {
        Mono.fromCallable(() -> {
            try {
                initAuthMethodManagerFromSettings();
            } catch (final Throwable ex) {
                log.warn("Cannot restore login due to error: " + ex.getMessage());
            }
            return true;
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @NotNull
    @AzureOperation(
            name = "common.create_rest_client.sub",
            params = {"sid"},
            type = AzureOperation.Type.TASK
    )
    public Azure getAzureClient(String sid) {
        final AzureManager manager = getAzureManager();
        if (manager != null) {
            final Azure azure = manager.getAzure(sid);
            if (azure != null) {
                return azure;
            }
        }
        final String error = "Failed to connect Azure service with current account";
        final String action = "Confirm you have already signed in with subscription: " + sid;
        throw new AzureToolkitRuntimeException(error, null, action);
    }

    public void addSignInEventListener(Runnable l) {
        signInEventListeners.add(l);
    }

    public void removeSignInEventListener(Runnable l) {
        signInEventListeners.remove(l);
    }

    public void addSignOutEventListener(Runnable l) {
        signOutEventListeners.add(l);
    }

    public void removeSignOutEventListener(Runnable l) {
        signOutEventListeners.remove(l);
    }

    public void notifySignInEventListener() {
        for (final Runnable l : signInEventListeners) {
            l.run();
        }
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.SIGNIN, null));
        }
    }

    private void notifySignOutEventListener() {
        for (final Runnable l : signOutEventListeners) {
            l.run();
        }
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.SIGNOUT, null));
        }
    }

    @Nullable
    public AzureManager getAzureManager() {
        if (!this.initFuture.isDone()) {
            return null;
        }
        if (!this.isSignedIn()) {
            return null;
        }
        return identityAzureManager;
    }

    @AzureOperation(name = "account.sign_out", type = AzureOperation.Type.TASK)
    @CacheEvict(CacheEvict.ALL) // evict all caches on signing out
    public void signOut() {
        waitInitFinish();
        identityAzureManager.drop();
        cleanAll();
        notifySignOutEventListener();
    }

    public boolean isRestoringSignIn() {
        return !initFuture.isDone();
    }

    public boolean isSignedIn() {
        if (!initFuture.isDone()) {
            return false;
        }
        return identityAzureManager != null && identityAzureManager.isSignedIn();
    }

    public AuthMethod getAuthMethod() {
        return authMethodDetails == null ? null : authMethodDetails.getAuthMethod();
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return this.authMethodDetails;
    }

    @AzureOperation(name = "account.update_auth_setting", type = AzureOperation.Type.TASK)
    public synchronized void setAuthMethodDetails(AuthMethodDetails authMethodDetails) {
        waitInitFinish();
        cleanAll();
        this.authMethodDetails = authMethodDetails;
        persistAuthMethodDetails();

    }

    private synchronized void cleanAll() {
        waitInitFinish();
        identityAzureManager.getSubscriptionManager().cleanSubscriptions();
        authMethodDetails = new AuthMethodDetails();
        persistAuthMethodDetails();
    }

    @AzureOperation(name = "account.persist_auth_setting", type = AzureOperation.Type.TASK)
    public void persistAuthMethodDetails() {
        waitInitFinish();
        try {
            System.out.println("saving authMethodDetails...");
            final String sd = JsonHelper.serialize(authMethodDetails);
            AzureStoreManager.getInstance().getIdeStore().setProperty(ACCOUNT, AUTH_METHOD_DETAIL, sd);
        } catch (final IOException e) {
            final String error = "Failed to persist auth method settings while updating";
            final String action = "Retry later";
            throw new AzureToolkitRuntimeException(error, e, action);
        }
    }

    private void initAuthMethodManagerFromSettings() {
        EventUtil.executeWithLog(ACCOUNT, RESIGNIN, operation -> {
            try {
                AuthMethodDetails targetAuthMethodDetails = loadSettings();
                if (targetAuthMethodDetails == null || targetAuthMethodDetails.getAuthMethod() == null) {
                    targetAuthMethodDetails = new AuthMethodDetails();
                    targetAuthMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
                } else {
                    // convert old auth method to new ones
                    switch (targetAuthMethodDetails.getAuthMethod()) {
                        case AZ: {
                            targetAuthMethodDetails.setAuthType(AuthType.AZURE_CLI);
                            break;
                        }
                        case DC: {
                            targetAuthMethodDetails.setAuthType(AuthType.DEVICE_CODE);
                            break;
                        }
                        case AD:
                            // we don't support it now
                            log.warn("The AD auth method is not supported now, ignore the credential.");
                            break;
                        case SP:
                            targetAuthMethodDetails.setAuthType(AuthType.SERVICE_PRINCIPAL);
                            break;
                        default:
                            break;
                    }
                    targetAuthMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
                }
                authMethodDetails = this.identityAzureManager.restoreSignIn(targetAuthMethodDetails).block();
                final List<Subscription> persistSubscriptions = SubscriptionManager.loadSubscriptions();
                if (CollectionUtils.isNotEmpty(persistSubscriptions)) {
                    final List<String> savedSubscriptionList = persistSubscriptions.stream()
                            .filter(Subscription::isSelected).map(Subscription::getId).distinct().collect(Collectors.toList());
                    identityAzureManager.selectSubscriptionByIds(savedSubscriptionList);
                }
                initFuture.complete(true);
                final String authMethod = authMethodDetails.getAuthMethod() == null ? "Empty" : authMethodDetails.getAuthMethod().name();
                final Map<String, String> telemetryProperties = new HashMap<String, String>() {
                    {
                        put(SIGNIN_METHOD, authMethod);
                        put(AUTH_TYPE, Optional.ofNullable(authMethodDetails.getAuthType()).map(AuthType::name).orElse("Empty"));
                        put(AZURE_ENVIRONMENT, CommonSettings.getEnvironment().getName());
                    }
                };
                EventUtil.logEvent(EventType.info, operation, telemetryProperties);
                notifySignInEventListener();
            } catch (final RuntimeException exception) {
                initFuture.complete(true);
                EventUtil.logError(operation, ErrorType.systemError, exception, null, null);
                this.authMethodDetails = new AuthMethodDetails();
                this.authMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
                notifySignOutEventListener();
            } finally {
                AzureEventBus.emit("account.restore_sign_in");
            }
            return this;
        });
    }

    @AzureOperation(name = "account.load_auth_setting", type = AzureOperation.Type.TASK)
    private static AuthMethodDetails loadSettings() {
        System.out.println("loading authMethodDetails...");
        try {
            String json = AzureStoreManager.getInstance().getIdeStore().getProperty(ACCOUNT, AUTH_METHOD_DETAIL, "");
            if (StringUtils.isBlank(json)) {
                final FileStorage fs = new FileStorage(FILE_NAME_AUTH_METHOD_DETAILS, CommonSettings.getSettingsBaseDir());
                final byte[] data = fs.read();
                json = new String(data);
                AzureStoreManager.getInstance().getIdeStore().setProperty(ACCOUNT, AUTH_METHOD_DETAIL, json);
                fs.removeFile();
                OperationContext.action().setTelemetryProperty("isLoadAuthMethodDetailsFromDisk", String.valueOf(true));
            }
            if (StringUtils.isBlank(json)) {
                System.out.println("No auth method details are saved.");
                return new AuthMethodDetails();
            }
            return JsonHelper.deserialize(AuthMethodDetails.class, json);
        } catch (final IOException ignored) {
            System.out.println("Failed to loading authMethodDetails settings. Use defaults.");
            return new AuthMethodDetails();
        }
    }

    private void waitInitFinish() {
        try {
            this.initFuture.get();
        } catch (final InterruptedException | ExecutionException ignored) {
        }
    }
}
