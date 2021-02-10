/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.interact;

import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.adauth.IWebUi;
import com.microsoft.azuretools.utils.IProgressTaskImpl;

/**
 * Created by shch on 10/4/2016.
 */
public interface IUIFactory {
//    ISelectAuthMethod getAuthMethodDialog();
    INotification getNotificationWindow();
    IWebUi getWebUi();
    IDeviceLoginUI getDeviceLoginUI();
    IProgressTaskImpl getProgressTaskImpl();
}
