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

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.AdAuthManagerBuilder;
import com.microsoft.azuretools.authmanage.AzureManagerFactory;
import com.microsoft.azuretools.authmanage.BaseADAuthManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;

import java.io.IOException;
import java.util.Objects;

import static com.microsoft.azuretools.Constants.FILE_NAME_SUBSCRIPTIONS_DETAILS_AT;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AccessTokenAzureManager extends AzureManagerBase {
    private final BaseADAuthManager delegateADAuthManager;

    static {
        settings.setSubscriptionsDetailsFileName(FILE_NAME_SUBSCRIPTIONS_DETAILS_AT);
    }

    public AccessTokenAzureManager(final BaseADAuthManager delegateADAuthManager) {
        this.delegateADAuthManager = delegateADAuthManager;
    }

    @Override
    public String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        return delegateADAuthManager.getAccessToken(tid, resource, promptBehavior);
    }

    @Override
    public String getCurrentUserId() {
        return delegateADAuthManager.getAccountEmail();
    }

    @Override
    protected String getCurrentTenantId() {
        return delegateADAuthManager.getCommonTenantId();
    }

    protected boolean isSignedIn() {
        return Objects.nonNull(this.delegateADAuthManager);
    }

    @Override
    public void drop() {
        super.drop();
        delegateADAuthManager.signOut();
    }

    public static class AccessTokenAzureManagerFactory implements AzureManagerFactory, AdAuthManagerBuilder {
        private final BaseADAuthManager adAuthManager;

        public AccessTokenAzureManagerFactory(final BaseADAuthManager adAuthManager) {
            this.adAuthManager = adAuthManager;
        }

        @Override
        public AzureManager factory(final AuthMethodDetails authMethodDetails) {
            if (isBlank(authMethodDetails.getAccountEmail())) {
                throw new IllegalArgumentException(
                        "No account email provided to create Azure manager for access token based authentication");
            }

            adAuthManager.applyAuthMethodDetails(authMethodDetails);
            return new AccessTokenAzureManager(adAuthManager);
        }

        @Override
        public AuthMethodDetails restore(AuthMethodDetails authMethodDetails) {
            if (!StringUtils.isNullOrEmpty(authMethodDetails.getAccountEmail())
                    && !adAuthManager.tryRestoreSignIn(authMethodDetails)) {
                return new AuthMethodDetails();
            }

            return authMethodDetails;
        }

        @Override
        public BaseADAuthManager getInstance() {
            return adAuthManager;
        }
    }
}
