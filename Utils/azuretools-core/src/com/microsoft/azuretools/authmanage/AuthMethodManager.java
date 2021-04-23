/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.azure.core.implementation.http.HttpClientProviders;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.MySQLManager;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.Constants.FILE_NAME_AUTH_METHOD_DETAILS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class AuthMethodManager {
    private static final Logger LOGGER = Logger.getLogger(AuthMethodManager.class.getName());
    private AuthMethodDetails authMethodDetails;
    private final Set<Runnable> signInEventListeners = new HashSet<>();
    private final Set<Runnable> signOutEventListeners = new HashSet<>();
    private final CompletableFuture<Boolean> initFuture = new CompletableFuture();
    private final IdentityAzureManager identityAzureManager = IdentityAzureManager.getInstance();

    static {
        // fix the class load problem for intellij plugin
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(AuthMethodManager.class.getClassLoader());
            HttpClientProviders.createInstance();
            com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
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
            } catch (Throwable ex) {
                LOGGER.warning("Cannot restore login due to error: " + ex.getMessage());
            }
            return true;
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @NotNull
    @AzureOperation(
            name = "common|rest_client.create",
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
        final String errorCode = "001";
        throw new AzureToolkitRuntimeException(error, null, action, errorCode);
    }

    @AzureOperation(
        name = "common|rest_client.create_asc",
        params = {"sid"},
        type = AzureOperation.Type.TASK
    )
    public AppPlatformManager getAzureSpringCloudClient(String sid) {
        final AzureManager manager = getAzureManager();
        if (manager != null) {
            return getAzureManager().getAzureSpringCloudClient(sid);
        }
        final String error = "Failed to connect Azure service with current account";
        final String action = "Confirm you have already signed in with subscription: " + sid;
        throw new AzureToolkitRuntimeException(error, action);
    }

    @AzureOperation(
        name = "common|rest_client.create_mysql",
        params = {"sid"},
        type = AzureOperation.Type.TASK
    )
    public MySQLManager getMySQLManager(String sid) {
        final AzureManager manager = getAzureManager();
        if (manager != null) {
            return manager.getMySQLManager(sid);
        }
        final String error = "Failed to get manager of Azure Database for MySQL with current account";
        final String action = "Confirm you have already signed in with subscription: " + sid;
        throw new AzureToolkitRuntimeException(error, action);
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
        for (Runnable l : signInEventListeners) {
            l.run();
        }
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.SIGNIN, null));
        }
    }

    private void notifySignOutEventListener() {
        for (Runnable l : signOutEventListeners) {
            l.run();
        }
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.SIGNOUT, null));
        }
    }

    @Nullable
    public AzureManager getAzureManager() {
        waitInitFinish();
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

    public boolean isSignedIn() {
        waitInitFinish();
        return identityAzureManager != null && identityAzureManager.isSignedIn();
    }

    public AuthMethod getAuthMethod() {
        return authMethodDetails == null ? null : authMethodDetails.getAuthMethod();
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return this.authMethodDetails;
    }

    @AzureOperation(name = "account|auth_setting.update", type = AzureOperation.Type.TASK)
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

    @AzureOperation(name = "account|auth_setting.persist", type = AzureOperation.Type.TASK)
    public void persistAuthMethodDetails() {
        waitInitFinish();
        try {
            System.out.println("saving authMethodDetails...");
            String sd = JsonHelper.serialize(authMethodDetails);
            FileStorage fs = new FileStorage(FILE_NAME_AUTH_METHOD_DETAILS, CommonSettings.getSettingsBaseDir());
            fs.write(sd.getBytes(StandardCharsets.UTF_8));
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
                    targetAuthMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
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
                            LOGGER.warning("The AD auth method is not supported now, ignore the credential.");
                            break;
                        case SP:
                            targetAuthMethodDetails.setAuthType(AuthType.SERVICE_PRINCIPAL);
                            break;
                        default:
                            break;
                    }
                }
                authMethodDetails = this.identityAzureManager.restoreSignIn(targetAuthMethodDetails).block();
                List<String> allSubscriptionIds = identityAzureManager.getSubscriptionDetails().stream()
                        .map(SubscriptionDetail::getSubscriptionId).collect(Collectors.toList());
                identityAzureManager.selectSubscriptionByIds(allSubscriptionIds);
                final String authMethod = authMethodDetails.getAuthMethod() == null ? "Empty" : authMethodDetails.getAuthMethod().name();
                final Map<String, String> telemetryProperties = new HashMap<String, String>() {
                    {
                        put(SIGNIN_METHOD, authMethod);
                        put(AZURE_ENVIRONMENT, CommonSettings.getEnvironment().getName());
                    }
                };
                initFuture.complete(true);
                EventUtil.logEvent(EventType.info, operation, telemetryProperties);
            } catch (RuntimeException exception) {
                initFuture.complete(true);
                EventUtil.logError(operation, ErrorType.systemError, exception, null, null);
                this.authMethodDetails = new AuthMethodDetails();
                this.authMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
            }
            return this;
        });
    }

    @AzureOperation(name = "account|auth_setting.load", type = AzureOperation.Type.TASK)
    private static AuthMethodDetails loadSettings() {
        System.out.println("loading authMethodDetails...");
        try {
            FileStorage fs = new FileStorage(FILE_NAME_AUTH_METHOD_DETAILS, CommonSettings.getSettingsBaseDir());
            byte[] data = fs.read();
            String json = new String(data);
            if (json.isEmpty()) {
                System.out.println(FILE_NAME_AUTH_METHOD_DETAILS + " is empty");
                return new AuthMethodDetails();
            }
            return JsonHelper.deserialize(AuthMethodDetails.class, json);
        } catch (IOException ignored) {
            System.out.println("Failed to loading authMethodDetails settings. Use defaults.");
            return new AuthMethodDetails();
        }
    }

    private void waitInitFinish() {
        try {
            this.initFuture.get();
        } catch (InterruptedException | ExecutionException e) {
        }
    }
}
