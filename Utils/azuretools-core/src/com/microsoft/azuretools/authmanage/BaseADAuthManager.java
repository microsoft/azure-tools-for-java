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

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.adauth.AuthContext;
import com.microsoft.azuretools.adauth.AuthResult;
import com.microsoft.azuretools.adauth.IWebUi;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.models.AdAuthDetails;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.securestore.SecureStore;
import com.microsoft.azuretools.service.ServiceManager;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class BaseADAuthManager {
    protected AzureEnvironment env;
    protected AdAuthDetails adAuthDetails;
    protected static final String COMMON_TID = "common";// Common Tenant ID
    protected static final String SECURE_STORE_SERVICE = "ADAuthManager";
    protected static final String SECURE_STORE_KEY = "cachedAuthResult";
    protected static final Logger LOGGER = Logger.getLogger(AdAuthManager.class.getName());

    @NotNull
    protected String commonTenantId = COMMON_TID;
    @Nullable
    final protected SecureStore secureStore;

    public BaseADAuthManager() {
        adAuthDetails = new AdAuthDetails();
        env = CommonSettings.getAdEnvironment();
        if (env == null) {
            throw new ExceptionInInitializerError("Azure environment is not setup");
        }

        secureStore = ServiceManager.getServiceProvider(SecureStore.class);
    }

    public abstract boolean tryRestoreSignIn(AuthMethodDetails authMethodDetails);

    public void setCommonTenantId(@NotNull String commonTenantId) {
        this.commonTenantId = commonTenantId;
    }

    @NotNull
    public String getCommonTenantId() {
        return commonTenantId;
    }

    // logout
    protected void cleanCache() {
        AuthContext.cleanTokenCache();
        adAuthDetails = new AdAuthDetails();

        // clear saved auth result
        setCommonTenantId(COMMON_TID);
        saveToSecureStore(null);
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

    protected void saveToSecureStore(@Nullable AuthResult authResult) {
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

    @Nullable
    protected AuthResult loadFromSecureStore() {
        if (secureStore == null) {
            return null;
        }

        final String authJson = secureStore.loadPassword(SECURE_STORE_SERVICE, SECURE_STORE_KEY);
        if (authJson != null) {
            try {
                final AuthResult savedAuth = JsonHelper.deserialize(AuthResult.class, authJson);
                if (!savedAuth.getUserId().equals(adAuthDetails.getAccountEmail())) {
                    return null;
                }

                final String tenantId = StringUtils.isNullOrWhiteSpace(savedAuth.getUserInfo().getTenantId()) ?
                        COMMON_TID : savedAuth.getUserInfo().getTenantId();
                final AuthContext ac = createContext(tenantId, null);
                final AuthResult updatedAuth = ac.acquireToken(savedAuth);
                saveToSecureStore(updatedAuth);
                return updatedAuth;
            } catch (IOException e) {
                LOGGER.warning("Can't restore the authentication cache: " + e.getMessage());
            }
        }

        return null;
    }

    protected AuthContext createContext(@NotNull final String tid, final UUID corrId) throws IOException {
        String authority = null;
        final String endpoint = env.activeDirectoryEndpoint();
        if (StringUtils.isNullOrEmpty(endpoint)) {
            throw new IOException("Azure authority endpoint is empty");
        }
        if (endpoint.endsWith("/")) {
            authority = endpoint + tid;
        } else {
            authority = endpoint + "/" + tid;
        }
        return new AuthContext(authority, Constants.clientId, true, corrId);
    }
}
