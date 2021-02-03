/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.logging.Logger;

public class RefreshableTokenCredentials extends AzureTokenCredentials {
    private final static Logger LOGGER = Logger.getLogger(RefreshableTokenCredentials.class.getName());
    private AzureManager azureAuthManager;

    /**
     * Initializes a new instance of the TokenCredentials.
     *
     * @param azureAuthManager authz/auth manager
     * @param tid  tenant ID
     */
    public RefreshableTokenCredentials(final AzureManager azureAuthManager, final String tid) {
        super(null, tid);
        this.azureAuthManager = azureAuthManager;
    }

//    @Override
//    public String getToken() {
//        try {
//            System.out.println("RefreshableTokenCredentials: getToken()");
//            return authManager.getAccessToken(tid);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        return null;
//    }

    @Override
    public String getToken(String s) throws IOException {
        return azureAuthManager.getAccessToken(domain(), s, PromptBehavior.Auto);
    }

    @Override
    public AzureEnvironment environment() {
        return CommonSettings.getAdEnvironment();
    }

    @Override
    public void applyCredentialsFilter(OkHttpClient.Builder clientBuilder) {
        try {
            super.applyCredentialsFilter(clientBuilder);
        } catch (Exception ex) {
            LOGGER.warning(ex.getMessage());
        }
    }
}
