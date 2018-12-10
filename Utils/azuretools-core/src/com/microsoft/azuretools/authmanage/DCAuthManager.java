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

import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azuretools.adauth.AuthContext;
import com.microsoft.azuretools.adauth.AuthResult;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.models.AdAuthDetails;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;

public class DCAuthManager extends BaseADAuthManager {
    private static class LazyLoader {
        static final DCAuthManager INSTANCE = new DCAuthManager();
    }

    private DCAuthManager() {
        super();
    }

    public static DCAuthManager getInstance() {
        return LazyLoader.INSTANCE;
    }

    /**
     * Try to sign in with persisted authentication result.
     *
     * @param authMethodDetails The authentication method detail for helping
     * @return true for success
     */
    @Override
    public synchronized boolean tryRestoreSignIn(@NotNull final AuthMethodDetails authMethodDetails) {
        final String azureEnvironment = authMethodDetails.getAzureEnv();
        if (secureStore == null || StringUtils.isNullOrEmpty(azureEnvironment) ||
                // Restore only for the same saved Azure environment with current
                !CommonSettings.getEnvironment().getName().equals(azureEnvironment)) {
            return false;
        }

        final AdAuthDetails originAdAuthDetails = adAuthDetails;
        adAuthDetails = new AdAuthDetails();
        adAuthDetails.setAccountEmail(authMethodDetails.getAccountEmail());

        try {
            final AuthResult savedAuth = loadFromSecureStore();
            if (savedAuth != null) {
                saveToSecureStore(savedAuth);
                return true;
            }
        } catch (Exception ignored) {
            LOGGER.info("The cached token is expired, can't restore it.");
            cleanCache();
        }

        adAuthDetails = originAdAuthDetails;
        return false;
    }

    public AuthResult deviceLogin(final AuthenticationCallback<AuthenticationResult> callback) throws IOException {
        cleanCache();
        final AuthContext ac = createContext(getCommonTenantId(), null);
        final AuthResult result = ac.acquireToken(env.managementEndpoint(), true, null, callback);
        if (!result.isUserIdDisplayble()) {
            // todo refactor the words
            throw new IllegalArgumentException("User Info is null");
        }
        // todo: acquire token by device code for other resources
        adAuthDetails.setAccountEmail(result.getUserId());
        adAuthDetails.setTidToSidsMap(null);
        saveToSecureStore(result);
        return result;
    }
}
