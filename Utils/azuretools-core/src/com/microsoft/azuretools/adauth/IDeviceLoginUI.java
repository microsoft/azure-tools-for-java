/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import com.azure.identity.DeviceCodeInfo;
import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import reactor.core.Disposable;

public interface IDeviceLoginUI {
    @Nullable
    AuthenticationResult authenticate(final AuthenticationContext ctx, final DeviceCode deviceCode,
                                      final AuthenticationCallback<AuthenticationResult> callback);

    default void promptDeviceCode(DeviceCodeInfo info) {

    }

    default void closePrompt() {

    }

    default void setDisposable(Disposable disposable) {

    }

    default void cancel() {
    }
}
