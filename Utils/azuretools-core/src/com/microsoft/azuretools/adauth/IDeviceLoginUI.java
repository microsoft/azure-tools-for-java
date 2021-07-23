/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import com.azure.identity.DeviceCodeInfo;

import java.util.concurrent.Future;

public interface IDeviceLoginUI {
    default void promptDeviceCode(DeviceCodeInfo info) {

    }

    default void closePrompt() {

    }

    default void setFuture(Future future) {

    }

    default void cancel() {
    }
}
