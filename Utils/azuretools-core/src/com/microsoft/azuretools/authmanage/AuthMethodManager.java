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

package com.microsoft.azuretools.authmanage;


import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import com.microsoft.azure.management.mysql.v2017_12_01.implementation.MySQLManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.sdkmanage.ServicePrincipalAzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static com.microsoft.azuretools.Constants.FILE_NAME_AUTH_METHOD_DETAILS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class AuthMethodManager {
    private static final Logger LOGGER = Logger.getLogger(AuthMethodManager.class.getName());
    private static final String CANNOT_GET_AZURE_MANAGER = "Cannot get Azure Manager. " +
            "Please check if you have already signed in.";
    private static final String CANNOT_GET_AZURE_BY_SID = "Cannot get Azure with Subscription ID: %s. " +
            "Please check if you have already signed in with this Subscription.";
    private static final String FAILED_TO_GET_AZURE_MANAGER_INSTANCE = "Failed to get an AzureManager instance " +
            "for AuthMethodDetails: %s with error %s";

    private AuthMethodDetails authMethodDetails;
    private volatile AzureManager azureManager;
    private final Set<Runnable> signInEventListeners = new HashSet<>();
    private final Set<Runnable> signOutEventListeners = new HashSet<>();

    private AuthMethodManager(AuthMethodDetails authMethodDetails) {
        this.authMethodDetails = authMethodDetails;
        final AzureManager manager = getAzureManager();
        // initialize subscription manager when restore authentication
        if (this.authMethodDetails.getAuthMethod() != null && Objects.nonNull(manager)) {
            manager.getSubscriptionManager().updateSubscriptionDetailsIfNull();
        }
    }

    public static AuthMethodManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    @NotNull
    public Azure getAzureClient(String sid) {
        final AzureManager manager = getAzureManager();
        if (manager != null) {
            final Azure azure = manager.getAzure(sid);
            if (azure != null) {
                return azure;
            }
        }
        final String error = "Failed to initialize request";
        final String action = "Confirm you have already signed in with subscription: " + sid;
        throw new AzureToolkitRuntimeException(error, action);
    }

    public AppPlatformManager getAzureSpringCloudClient(String sid) {
        final AzureManager manager = getAzureManager();
        if (manager != null) {
            return getAzureManager().getAzureSpringCloudClient(sid);
        }
        final String error = "Failed to initialize request";
        final String action = "Confirm you have already signed in with subscription: " + sid;
        throw new AzureToolkitRuntimeException(error, action);
    }

    public MySQLManager getMySQLClient(String sid) throws IOException {
        if (getAzureManager() == null) {
            throw new IOException(CANNOT_GET_AZURE_MANAGER);
        }
        return getAzureManager().getMySQLClient(sid);
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
        return getAzureManager(getAuthMethod());
    }

    public void signOut() {
        cleanAll();
        notifySignOutEventListener();
    }

    public boolean isSignedIn() {
        return azureManager != null;
    }

    public AuthMethod getAuthMethod() {
        return authMethodDetails == null ? null : authMethodDetails.getAuthMethod();
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return this.authMethodDetails;
    }

    public synchronized void setAuthMethodDetails(AuthMethodDetails authMethodDetails) {
        cleanAll();
        this.authMethodDetails = authMethodDetails;
        try {
            persistAuthMethodDetails();
        } catch (final IOException e) {
            final String error = "Failed to persist auth method settings while updating";
            final String action = "Retry later";
            throw new AzureToolkitRuntimeException(error, e, action);
        }
    }

    private synchronized @Nullable AzureManager getAzureManager(final AuthMethod authMethod) {
        if (authMethod == null) {
            return null;
        }
        if (azureManager == null) {
            try {
                azureManager = authMethod.createAzureManager(getAuthMethodDetails());
            } catch (RuntimeException ex) {
                LOGGER.info(String.format(FAILED_TO_GET_AZURE_MANAGER_INSTANCE, getAuthMethodDetails(), ex.getMessage()));
                cleanAll();
            }
        }
        return azureManager;
    }

    private synchronized void cleanAll() {
        if (azureManager != null) {
            azureManager.drop();
            azureManager.getSubscriptionManager().cleanSubscriptions();
            azureManager = null;
        }
        ServicePrincipalAzureManager.cleanPersist();
        authMethodDetails.setAccountEmail(null);
        authMethodDetails.setAzureEnv(null);
        authMethodDetails.setAuthMethod(null);
        authMethodDetails.setCredFilePath(null);
        try {
            persistAuthMethodDetails();
        } catch (final IOException e) {
            final String error = "Failed to persist local auth method settings while cleaning";
            final String action = "Retry later";
            throw new AzureToolkitRuntimeException(error, e, action);
        }
    }

    private void persistAuthMethodDetails() throws IOException {
        System.out.println("saving authMethodDetails...");
        String sd = JsonHelper.serialize(authMethodDetails);
        FileStorage fs = new FileStorage(FILE_NAME_AUTH_METHOD_DETAILS, CommonSettings.getSettingsBaseDir());
        fs.write(sd.getBytes(StandardCharsets.UTF_8));
    }

    private static class LazyHolder {
        static final AuthMethodManager INSTANCE = initAuthMethodManagerFromSettings();
    }

    private static AuthMethodManager initAuthMethodManagerFromSettings() {
        return EventUtil.executeWithLog(ACCOUNT, RESIGNIN, operation -> {
            try {
                final AuthMethodDetails savedAuthMethodDetails = loadSettings();
                final AuthMethodDetails authMethodDetails = savedAuthMethodDetails.getAuthMethod() == null ?
                        new AuthMethodDetails() : savedAuthMethodDetails.getAuthMethod().restoreAuth(savedAuthMethodDetails);
                final String authMethod = authMethodDetails.getAuthMethod() == null ? "Empty" : authMethodDetails.getAuthMethod().name();
                final Map<String, String> telemetryProperties = new HashMap<String, String>() {{
                        put(SIGNIN_METHOD, authMethod);
                        put(AZURE_ENVIRONMENT, CommonSettings.getEnvironment().getName());
                    }};
                EventUtil.logEvent(EventType.info, operation, telemetryProperties);
                return new AuthMethodManager(authMethodDetails);
            } catch (RuntimeException ignore) {
                EventUtil.logError(operation, ErrorType.systemError, ignore, null, null);
                return new AuthMethodManager(new AuthMethodDetails());
            }
        });
    }

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
}
