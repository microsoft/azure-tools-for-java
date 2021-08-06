/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.adauth.IWebUi;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.interact.IUIFactory;
import com.microsoft.intellij.ProgressTaskModal;
import com.microsoft.azuretools.utils.IProgressTaskImpl;

/**
 * Created by shch on 10/4/2016.
 */
public class UIFactory implements IUIFactory{

    @Override
    public INotification getNotificationWindow() {
        return new NotificationWindow();
    }

    @Override
    public IDeviceLoginUI getDeviceLoginUI() { return new DeviceLoginUI(); }

    @Override
    public IProgressTaskImpl getProgressTaskImpl() {
        return new ProgressTaskModal(getProject());
    }

    private Project getProject() {
        return null;
    }
}
