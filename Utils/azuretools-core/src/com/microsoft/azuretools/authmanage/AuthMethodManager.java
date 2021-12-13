/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.Constants.FILE_NAME_AUTH_METHOD_DETAILS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

@Slf4j
public class AuthMethodManager {
    public static final String AUTH_METHOD_DETAIL = "auth_method_detail";
    private AuthMethodDetails authMethodDetails;
    private final Set<Runnable> signInEventListeners = new HashSet<>();
    private final Set<Runnable> signOutEventListeners = new HashSet<>();
    private final CompletableFuture<Boolean> initFuture = new CompletableFuture();
    private final IdentityAzureManager identityAzureManager = IdentityAzureManager.getInstance();

    static {
        // fix the class load problem for intellij plugin
        disableLogLevelFor("com.microsoft.aad.adal4j.AuthenticationContext", "com.microsoft.aad.msal4j.PublicClientApplication",
                "com.microsoft.aad.msal4j.ConfidentialClientApplication");
    }

    private static void disableLogLevelFor(String...classes) {
        try {
            Class<?> loggerClz = Class.forName("org.apache.log4j.Logger");
            Class<?> loggerLevelClz = Class.forName("org.apache.log4j.Level");
            Object offLevel = FieldUtils.readDeclaredStaticField(loggerLevelClz, "OFF");
            Method getLoggerMethod = ClassUtils.getPublicMethod(loggerClz, "getLogger", String.class);
            if (getLoggerMethod != null) {
                for (String className : classes) {
                    Object logger2 = getLoggerMethod.invoke(loggerClz, className);
                    FieldUtils.writeField(logger2, "level", offLevel, true);
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // ignore
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
        final String error = "Cannot connect to Azure with current account";
        throw new AzureToolkitRuntimeException(error);
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
        System.out.println("saving authMethodDetails...");
        final String sd = JsonHelper.serialize(authMethodDetails);
        AzureStoreManager.getInstance().getIdeStore().setProperty(ACCOUNT, AUTH_METHOD_DETAIL, sd);
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
                List<SubscriptionDetail> persistSubscriptions = SubscriptionManager.loadSubscriptions();
                if (CollectionUtils.isNotEmpty(persistSubscriptions)) {
                    List<String> savedSubscriptionList = persistSubscriptions.stream()
                            .filter(SubscriptionDetail::isSelected).map(SubscriptionDetail::getSubscriptionId).distinct().collect(Collectors.toList());
                    identityAzureManager.selectSubscriptionByIds(savedSubscriptionList);
                }
                initFuture.complete(true);
                // pre-load regions
                AzureAccount az = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class);
                Optional.ofNullable(identityAzureManager.getSelectedSubscriptionIds()).ifPresent(sids -> sids.stream().limit(5).forEach(az::listRegions));

                final String authMethod = authMethodDetails.getAuthMethod() == null ? "Empty" : authMethodDetails.getAuthMethod().name();
                final Map<String, String> telemetryProperties = new HashMap<String, String>() {
                    {
                        put(SIGNIN_METHOD, authMethod);
                        put(AZURE_ENVIRONMENT, CommonSettings.getEnvironment().getName());
                    }
                };
                EventUtil.logEvent(EventType.info, operation, telemetryProperties);
                notifySignInEventListener();
            } catch (RuntimeException exception) {
                initFuture.complete(true);
                EventUtil.logError(operation, ErrorType.systemError, exception, null, null);
                this.authMethodDetails = new AuthMethodDetails();
                this.authMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
                notifySignOutEventListener();
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
                FileStorage fs = new FileStorage(FILE_NAME_AUTH_METHOD_DETAILS, CommonSettings.getSettingsBaseDir());
                byte[] data = fs.read();
                json = new String(data);
                AzureStoreManager.getInstance().getIdeStore().setProperty(ACCOUNT, AUTH_METHOD_DETAIL, json);
                fs.removeFile();
            }
            if (StringUtils.isBlank(json)) {
                System.out.println("No auth method details are saved.");
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
