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
 *
 */

package com.microsoft.azuretools.authmanage;


import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.sdkmanage.ServicePrincipalAzureManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static com.microsoft.azuretools.Constants.FILE_NAME_AUTH_METHOD_DETAILS;

public class AuthMethodManager {
    private final static Logger LOGGER = Logger.getLogger(AuthMethodManager.class.getName());
    private static final String CANNOT_GET_AZURE_MANAGER = "Cannot get Azure Manager. "
            + "Please check if you have already signed in.";
    private static final String CANNOT_GET_AZURE_BY_SID = "Cannot get Azure with Subscription ID: %s. "
            + "Please check if you have already signed in with this Subscription.";
    private AuthMethodDetails authMethodDetails = null;
    private volatile AzureManager azureManager;
    private Set<Runnable> signInEventListeners = new HashSet<>();
    private Set<Runnable> signOutEventListeners = new HashSet<>();

    private AuthMethodManager() {
        loadSettings();
    }

    private static class LazyHolder {
        static final AuthMethodManager INSTANCE = new AuthMethodManager();
    }

    public static AuthMethodManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    public Azure getAzureClient(String sid) throws IOException {
        if (getAzureManager() == null) {
            throw new IOException(CANNOT_GET_AZURE_MANAGER);
        }
        Azure azure = getAzureManager().getAzure(sid);
        if (azure == null) {
            throw new IOException(String.format(CANNOT_GET_AZURE_BY_SID, sid));
        }
        return azure;
    }

    public void addSignInEventListener(Runnable l) {
        if (!signInEventListeners.contains(l)) {
            signInEventListeners.add(l);
        }
    }

    public void removeSignInEventListener(Runnable l) {
        if (signInEventListeners.contains(l)) {
            signInEventListeners.remove(l);
        }
    }

    public void addSignOutEventListener(Runnable l) {
        if (!signOutEventListeners.contains(l)) {
            signOutEventListeners.add(l);
        }
    }

    public void removeSignOutEventListener(Runnable l) {
        if (signOutEventListeners.contains(l)) {
            signOutEventListeners.remove(l);
        }
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

    public AzureManager getAzureManager() throws IOException {
        return getAzureManager(getAuthMethod());
    }

    private synchronized AzureManager getAzureManager(final AuthMethod authMethod) throws IOException {
        AzureManager localAzureManagerRef = azureManager;

        if (localAzureManagerRef == null) {
            final String accountEmail = authMethodDetails.getAccountEmail();
            switch (authMethod) {
                case AD:
                    if (StringUtils.isNullOrEmpty(accountEmail) ||
                            (!AdAuthManager.getInstance().isSignedIn() &&
                                    !AdAuthManager.getInstance().tryRestoreSignIn(authMethodDetails))) {
                        return null;
                    }
                    localAzureManagerRef = new AccessTokenAzureManager();
                    break;
                case DC:
                    if (StringUtils.isNullOrEmpty(accountEmail) ||
                            (!DCAuthManager.getInstance().isSignedIn() &&
                                    !DCAuthManager.getInstance().tryRestoreSignIn(authMethodDetails))) {
                        return null;
                    }
                    localAzureManagerRef = new AccessTokenAzureManager();
                    break;
                case SP:
                    final String credFilePath = authMethodDetails.getCredFilePath();
                    if (StringUtils.isNullOrEmpty(credFilePath)) {
                        return null;
                    }
                    final Path filePath = Paths.get(credFilePath);
                    if (!Files.exists(filePath)) {
                        cleanAll();
                        final INotification nw = CommonSettings.getUiFactory().getNotificationWindow();
                        nw.deliver("Credential File Error", "File doesn't exist: " + filePath.toString());
                        return null;
                    }
                    localAzureManagerRef = new ServicePrincipalAzureManager(new File(credFilePath));
            }

            azureManager = localAzureManagerRef;
        }

        return localAzureManagerRef;
    }

    public void signOut() throws IOException {
        cleanAll();
        notifySignOutEventListener();
    }

    private synchronized void cleanAll() throws IOException {
        if (azureManager != null) {
            azureManager.getSubscriptionManager().cleanSubscriptions();
            azureManager = null;
        }
        ServicePrincipalAzureManager.cleanPersist();
        authMethodDetails.setAccountEmail(null);
        authMethodDetails.setAzureEnv(null);
        authMethodDetails.setCredFilePath(null);
        saveSettings();
    }

    public boolean isSignedIn() throws IOException {
        return getAzureManager() != null;
    }

    public AuthMethod getAuthMethod() {
        return authMethodDetails.getAuthMethod();
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return this.authMethodDetails;
    }

    public void setAuthMethodDetails(AuthMethodDetails authMethodDetails) throws IOException {
        cleanAll();
        this.authMethodDetails = authMethodDetails;
        saveSettings();
        //if (isSignedIn()) notifySignInEventListener();
    }

    private void loadSettings() {
        System.out.println("loading authMethodDetails...");
        try {
            FileStorage fs = new FileStorage(FILE_NAME_AUTH_METHOD_DETAILS, CommonSettings.getSettingsBaseDir());
            byte[] data = fs.read();
            String json = new String(data);
            if (json.isEmpty()) {
                System.out.println(FILE_NAME_AUTH_METHOD_DETAILS + " is empty");
                authMethodDetails = new AuthMethodDetails();
                return;
            }
            authMethodDetails = JsonHelper.deserialize(AuthMethodDetails.class, json);
        } catch (IOException ignored) {
            System.out.println("Failed to loading authMethodDetails settings. Use defaults.");
            authMethodDetails = new AuthMethodDetails();
        }
    }

    private void saveSettings() throws IOException {
        System.out.println("saving authMethodDetails...");
        String sd = JsonHelper.serialize(authMethodDetails);
        FileStorage fs = new FileStorage(FILE_NAME_AUTH_METHOD_DETAILS, CommonSettings.getSettingsBaseDir());
        fs.write(sd.getBytes(Charset.forName("utf-8")));
    }
}
