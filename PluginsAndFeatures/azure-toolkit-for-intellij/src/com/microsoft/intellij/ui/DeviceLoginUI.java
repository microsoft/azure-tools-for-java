/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.azure.identity.DeviceCodeInfo;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import lombok.Setter;

import java.util.concurrent.Future;


public class DeviceLoginUI implements IDeviceLoginUI {
    private DeviceLoginWindow deviceLoginWindow;

    @Setter
    private Future future;

    public void promptDeviceCode(DeviceCodeInfo challenge) {
        deviceLoginWindow = new DeviceLoginWindow(challenge, this);
        deviceLoginWindow.show();
    }

    @Override
    public void closePrompt() {
        if (deviceLoginWindow != null) {
            deviceLoginWindow.closeDialog();
        }
    }

    @Override
    public void cancel() {
        if (future != null) {
            this.future.cancel(true);
        }
    }
}
