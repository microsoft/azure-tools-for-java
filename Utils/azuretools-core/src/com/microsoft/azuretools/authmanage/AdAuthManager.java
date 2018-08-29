/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.adauth.*;
import com.microsoft.azuretools.authmanage.models.AdAuthDetails;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.securestore.SecureStore;
import com.microsoft.azuretools.service.ServiceManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class AdAuthManager {
    private static final Logger LOGGER = Logger.getLogger(AdAuthManager.class.getName());
    private IWebUi webUi;
    private AzureEnvironment env;
    private AdAuthDetails adAuthDetails;

    @Nullable
    final private SecureStore secureStore;

    private static AdAuthManager instance = null;
    private static final String COMMON_TID = "common";  // Common Tenant ID
    private static final String SECURE_STORE_SERVICE = "ADAuthManager";
    private static final String SECURE_STORE_KEY = "cachedAuthResult";

    private static final String AUTHORIZATIONREQUIRED = "Authorization is required, please sign out and sign in again";
    @NotNull
    private String commonTenantId = COMMON_TID;

    /**
     * Get the AdAuthManager singleton instance.
     * @return AdAuthManager singleton instance.
     * @throws IOException thrown when there is exception.
     */
    public static AdAuthManager getInstance() throws IOException {
        if (instance == null) {
            instance = new AdAuthManager();
        }
        return instance;
    }

    public String getAccessToken(String tid) throws IOException {
        return getAccessToken(tid, env.resourceManagerEndpoint(), PromptBehavior.Auto);
    }

    /**
     * Get access token.
     * @param tid String, tenant id.
     * @param resource String, resource url.
     * @param promptBehavior PromptBehavior, prompt enum.
     * @return String access token.
     * @throws IOException thrown when fail to get access token.
     */
    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        AuthContext ac = createContext(tid, null);
        AuthResult result = null;
        try {
            result = ac.acquireToken(resource, false, adAuthDetails.getAccountEmail(), false);
        } catch (AuthException e) {
            if (AuthError.InvalidGrant.equalsIgnoreCase(e.getError())
                    || AuthError.InteractionRequired.equalsIgnoreCase(e.getError())) {
                throw new IOException(AUTHORIZATIONREQUIRED, e);
            } else {
                throw e;
            }
        }
        return result.getAccessToken();
    }

    /**
     * Try to sign in with persisted authentication result.
     *
     * @param authMethodDetails The authentication method detail for helping
     * @return true for success
     */
    public synchronized boolean tryRestoreSignIn(@NotNull AuthMethodDetails authMethodDetails) {
        if (secureStore == null || authMethodDetails.getAzureEnv() == null ||
                // Restore only for the same saved Azure environment with current
                !CommonSettings.getEnvironment().getName().equals(authMethodDetails.getAzureEnv())) {
            return false;
        }

        AdAuthDetails originAdAuthDetails = adAuthDetails;

        adAuthDetails = new AdAuthDetails();
        adAuthDetails.setAccountEmail(authMethodDetails.getAccountEmail());

        try {
            // Try to restore
            AuthResult savedAuth = loadFromSecureStore();

            if (savedAuth != null) {
                signIn(savedAuth);

                return true;
            }
        } catch (Exception ignored) {
            LOGGER.info("The cached token is expired, can't restore it.");
            cleanCache();
        }

        adAuthDetails = originAdAuthDetails;
        return false;
    }

    /**
     * Sign in azure account.
     * @return AuthResult, auth result.
     * @throws IOException thrown when failed to get auth result.
     */
    public AuthResult signIn() throws IOException {
        return signIn(null);
    }

    /**
     * Sign in azure account with saved authentication result.
     *
     * @param savedAuth saved authentication result, null for signing in from scratch
     * @return AuthResult, auth result.
     * @throws IOException thrown when failed to get auth result.
     */
    public AuthResult signIn(@Nullable AuthResult savedAuth) throws IOException {

        // build token cache for azure and graph api
        // using azure sdk directly

        AuthResult result;

        if (savedAuth == null) {
            cleanCache();
            AuthContext ac = createContext(getCommonTenantId(), null);
            result = ac.acquireToken(env.managementEndpoint(), true, null, false);
        } else {
            result = savedAuth;
        }

        String userId = result.getUserId();
        boolean isDisplayable = result.isUserIdDisplayble();

        Map<String, List<String>> tidToSidsMap = new HashMap<>();

        List<Tenant> tenants = AccessTokenAzureManager.getTenants(getCommonTenantId());
        for (Tenant t : tenants) {
            String tid = t.tenantId();
            AuthContext ac1 = createContext(tid, null);
            // put tokens into the cache
            try {
                ac1.acquireToken(env.managementEndpoint(), false, userId, isDisplayable);
            } catch (AuthException e) {
                //TODO: should narrow to AuthError.InteractionRequired
                ac1.acquireToken(env.managementEndpoint(), true, userId, isDisplayable);
            }

            // FIXME!!! Some environments and subscriptions can't get the resource manager token
            // Let the log in process passed, and throwing the errors when to access those resources
            try {
                ac1.acquireToken(env.resourceManagerEndpoint(), false, userId, isDisplayable);
            } catch (AuthException e) {
                if (CommonSettings.getEnvironment() instanceof ProvidedEnvironment) {
                    // Swallow the exception since some provided environments are not full featured
                    LOGGER.warning("Can't get " + env.resourceManagerEndpoint() + " access token from environment " +
                            CommonSettings.getEnvironment().getName());
                } else {
                    throw e;
                }
            }

            try {
                ac1.acquireToken(env.graphEndpoint(), false, userId, isDisplayable);
            } catch (AuthException e) {
                if (CommonSettings.getEnvironment() instanceof ProvidedEnvironment) {
                    // Swallow the exception since some provided environments are not full featured
                    LOGGER.warning("Can't get " + env.graphEndpoint() + " access token from environment " +
                            CommonSettings.getEnvironment().getName());
                } else {
                    throw e;
                }
            }

            // ADL account access token
            try {
                ac1.acquireToken(env.dataLakeEndpointResourceId(), false, userId, isDisplayable);
            } catch (AuthException e) {
                LOGGER.warning("Can't get " + env.dataLakeEndpointResourceId() + " access token from environment " +
                        CommonSettings.getEnvironment().getName() + "for user " + userId);
            }

            // TODO: remove later
            // ac1.acquireToken(Constants.resourceVault, false, userId, isDisplayable);
            List<String> sids = new LinkedList<>();
            for (Subscription s : AccessTokenAzureManager.getSubscriptions(tid)) {
                sids.add(s.subscriptionId());
            }
            tidToSidsMap.put(t.tenantId(), sids);
        }

        if (!isDisplayable) {
            throw new IllegalArgumentException("accountEmail is null");
        }

        adAuthDetails.setAccountEmail(userId);
        adAuthDetails.setTidToSidsMap(tidToSidsMap);

        saveToSecureStore(result);

        return result;
    }

    public Map<String, List<String>> getAccountTenantsAndSubscriptions() {
        return adAuthDetails.getTidToSidsMap();
    }

    /**
     * Sign out azure account.
     */
    public void signOut() {
        cleanCache();
        adAuthDetails.setAccountEmail(null);
        adAuthDetails.setTidToSidsMap(null);
    }

    public boolean isSignedIn() {
        return adAuthDetails.getAccountEmail() != null;
    }

    public String getAccountEmail() {
        return adAuthDetails.getAccountEmail();
    }

    @Nullable
    private AuthResult loadFromSecureStore() {
        if (secureStore == null) {
            return null;
        }

        String authJson = secureStore.loadPassword(SECURE_STORE_SERVICE, SECURE_STORE_KEY);

        if (authJson != null) {
            try {
                AuthResult savedAuth = JsonHelper.deserialize(AuthResult.class, authJson);

                if (!savedAuth.getUserId().equals(adAuthDetails.getAccountEmail())) {
                    return null;
                }

                String tenantId = StringUtils.isNullOrWhiteSpace(savedAuth.getUserInfo().getTenantId()) ? COMMON_TID :
                        savedAuth.getUserInfo().getTenantId();

                AuthContext ac = createContext(tenantId, null);
                AuthResult updatedAuth = ac.acquireToken(savedAuth);

                saveToSecureStore(updatedAuth);

                return updatedAuth;
            } catch (IOException e) {
                LOGGER.warning("Can't restore the authentication cache: " + e.getMessage());
            }
        }

        return null;
    }

    private void saveToSecureStore(@Nullable AuthResult authResult) {
        if (secureStore == null) {
            return;
        }

        try {
            @Nullable
            String authJson = JsonHelper.serialize(authResult);

            String tenantId = (authResult == null || StringUtils.isNullOrWhiteSpace(authResult.getUserInfo().getTenantId())) ?
                    COMMON_TID :
                    authResult.getUserInfo().getTenantId();
            // Update common tenantId after token acquired successfully
            setCommonTenantId(tenantId);

            secureStore.savePassword(SECURE_STORE_SERVICE, SECURE_STORE_KEY, authJson);
        } catch (IOException e) {
            LOGGER.warning("Can't persistent the authentication cache: " + e.getMessage());
        }
    }

    private AuthContext createContext(@NotNull final String tid, final UUID corrId) throws IOException {
        String authority = null;
        String endpoint = env.activeDirectoryEndpoint();
        if (StringUtils.isNullOrEmpty(endpoint)) {
            throw new IOException("Azure authority endpoint is empty");
        }
        if (endpoint.endsWith("/")) {
            authority = endpoint + tid;
        } else {
            authority = endpoint + "/" + tid;
        }
        return new AuthContext(authority, Constants.clientId, Constants.redirectUri, this.webUi, true, corrId);
    }

    // logout
    private void cleanCache() {
        AuthContext.cleanTokenCache();
        adAuthDetails = new AdAuthDetails();

        // clear saved auth result
        setCommonTenantId(COMMON_TID);
        saveToSecureStore(null);
    }

    private AdAuthManager() throws IOException {
        adAuthDetails = new AdAuthDetails();
        webUi = CommonSettings.getUiFactory().getWebUi();
        env = CommonSettings.getAdEnvironment();
        if (env == null) {
            throw new IOException("Azure environment is not setup");
        }

        secureStore = ServiceManager.getServiceProvider(SecureStore.class);
    }

    public void setCommonTenantId(@NotNull String commonTenantId) {
        this.commonTenantId = commonTenantId;
    }

    @NotNull
    public String getCommonTenantId() {
        return commonTenantId;
    }
}
