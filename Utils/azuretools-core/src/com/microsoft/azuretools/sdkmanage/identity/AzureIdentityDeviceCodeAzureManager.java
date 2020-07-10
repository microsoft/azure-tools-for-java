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

package com.microsoft.azuretools.sdkmanage.identity;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;
import com.azure.identity.SharedTokenCacheCredentialBuilder;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azuretools.authmanage.AzureManagerFactory;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Consumer;

import static com.microsoft.azuretools.Constants.FILE_NAME_SUBSCRIPTIONS_DETAILS_AI_DC;

public class AzureIdentityDeviceCodeAzureManager extends AzureIdentityAzureManager {

    public static final String CLIENT_ID = "777acee8-5286-4d6e-8b05-f7c851d8ed0a";
    public static final String FAILED_TO_AUTH_WITH_DEVICE_CODE_FLOW = "Failed to auth with device code flow";

    private TokenCredential tokenCredential;
    private String tenantId;
    private String userName;

    public static class AzureCliAzureManagerFactory implements AzureManagerFactory {

        @Override
        public @Nullable AzureManager factory(AuthMethodDetails authMethodDetails) {
            return getInstance().isSignedIn() ? getInstance() : null;
        }

        @Override
        public AuthMethodDetails restore(final AuthMethodDetails authMethodDetails) {
            final AzureIdentityDeviceCodeAzureManager instance = getInstance();
            return instance.restore(authMethodDetails) ? authMethodDetails : null;
        }
    }

    public static AzureIdentityDeviceCodeAzureManager getInstance() {
        return AzureIdentityDeviceCodeAzureManager.LazyLoader.INSTANCE;
    }

    public boolean restore(AuthMethodDetails authMethodDetails) {
        final String userAccount = authMethodDetails.getAccountEmail();
        try {
            tokenCredential = new SharedTokenCacheCredentialBuilder().username(userAccount)
                    .clientId(CLIENT_ID).build();
            userName = authMethodDetails.getAccountEmail();
            // Todo: Add tenant id to auth method details
            tenantId = "common";
            return true;
        } catch (Exception e) {
            tokenCredential = null;
            return false;
        }
    }

    @Override
    public String getCurrentUserId() {
        return userName;
    }

    @Override
    protected @Nullable TokenCredential getTokenCredential() {
        return tokenCredential;
    }

    @Override
    protected String getCredentialTenantId() {
        return tenantId;
    }

    private Disposable authDisposable;

    public void stopAuthentication() {
        if (authDisposable != null) {
            authDisposable.dispose();
            authDisposable = null;
        }
    }

    public Mono<String> pullAuthenticationAccount() {
        return Mono.fromCallable(() -> {
            while (StringUtils.isEmpty(userName) && !authDisposable.isDisposed()) {
                Thread.sleep(50);
            }
            return userName;
        });
    }

    public void auth(Consumer<DeviceCodeInfo> challengeConsumer) throws AzureExecutionException {
        stopAuthentication();
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            final DeviceCodeCredential deviceCodeCredential = new DeviceCodeCredentialBuilder()
                    .enablePersistentCache(true)
                    .clientId(CLIENT_ID)
                    .challengeConsumer(challengeConsumer)
                    .build();

            authDisposable = deviceCodeCredential
                    .authenticate()
                    .subscribe(authenticationRecord -> {
                        this.tenantId = authenticationRecord.getTenantId();
                        this.userName = authenticationRecord.getUsername();
                        this.tokenCredential = deviceCodeCredential;
                    });
        } catch (Exception e) {
            throw new AzureExecutionException(FAILED_TO_AUTH_WITH_DEVICE_CODE_FLOW, e);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    public void drop() throws IOException {
        stopAuthentication();
        this.credentials = null;
        this.userName = null;
        this.tenantId = null;
        super.drop();
    }

    private AzureIdentityDeviceCodeAzureManager() {
        settings.setSubscriptionsDetailsFileName(FILE_NAME_SUBSCRIPTIONS_DETAILS_AI_DC);
    }

    private static class LazyLoader {
        static final AzureIdentityDeviceCodeAzureManager INSTANCE = new AzureIdentityDeviceCodeAzureManager();
    }
}
