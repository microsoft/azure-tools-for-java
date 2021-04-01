/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
