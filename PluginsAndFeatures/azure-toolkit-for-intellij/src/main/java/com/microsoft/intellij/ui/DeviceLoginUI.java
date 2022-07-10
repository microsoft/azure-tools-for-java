/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.azure.identity.DeviceCodeInfo;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;


@RequiredArgsConstructor
public class DeviceLoginUI implements IDeviceLoginUI {
    @Nonnull
    private final Runnable onCancel;
    private DeviceLoginWindow deviceLoginWindow;

    public void promptDeviceCode(DeviceCodeInfo challenge) {
        deviceLoginWindow = new DeviceLoginWindow(challenge, this);
        deviceLoginWindow.show();
    }

    @Override
    public void closePrompt() {
        if (deviceLoginWindow != null) {
            deviceLoginWindow.closeDialog();
            deviceLoginWindow = null;
        }
    }

    @Override
    public void cancel() {
        onCancel.run();
    }
}
