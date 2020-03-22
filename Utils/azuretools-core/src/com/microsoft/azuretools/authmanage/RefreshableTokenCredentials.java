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
