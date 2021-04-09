/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AzureManagerFactory;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;

@Deprecated
public class AzureCliAzureManager extends AzureManagerBase {
    @Override
    public @Nullable String getAccessToken(String tid, String resource, PromptBehavior promptBehavior) throws IOException {
        return null;
    }

    @Override
    public String getCurrentUserId() {
        return null;
    }

    @Override
    protected String getCurrentTenantId() {
        return null;
    }

    @Override
    public void drop() {

    }

    public boolean isSignedIn() {
        return false;
    }

    public AuthMethodDetails signIn() throws AzureExecutionException {
        return null;
    }

    public static AzureCliAzureManager getInstance() {
        return LazyLoader.INSTANCE;
    }

    public static class AzureCliAzureManagerFactory implements AzureManagerFactory {

        @Override
        public @Nullable AzureManager factory(AuthMethodDetails authMethodDetails) {
            return null;
        }

        @Override
        public AuthMethodDetails restore(final AuthMethodDetails authMethodDetails) {
            return authMethodDetails;
        }
    }

    private static class LazyLoader {
        static final AzureCliAzureManager INSTANCE = new AzureCliAzureManager();
    }
}
