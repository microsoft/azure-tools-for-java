/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui.views;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;

public class AzureDeploymentProgressNotification {
    private static final Logger log =  Logger.getLogger(AzureDeploymentProgressNotification.class.getName());

    public static void createAzureDeploymentProgressNotification(String key, String desc, String url, String urltext, String status) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    WindowsAzureActivityLogView waView = (WindowsAzureActivityLogView) PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage().showView("com.microsoft.azuretools.core.ui.views.WindowsAzureActivityLogView");
                    waView.addDeployment(key, desc, new Date(), url, urltext, status);
                } catch (Exception e) {
                    log.log(Level.WARNING, "createAzureDeploymentProgressNotification: can't open Azure Activity Window", e);
                }
            }
        });
    }

    public static void createAzureDeploymentProgressNotification(String deploymentName, String deploymentDescription) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    WindowsAzureActivityLogView waView = (WindowsAzureActivityLogView) PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage().showView("com.microsoft.azuretools.core.ui.views.WindowsAzureActivityLogView");
                    waView.addDeployment(deploymentName, deploymentDescription, new Date());
                } catch (Exception e) {
                    log.log(Level.WARNING, "createAzureDeploymentProgressNotification: can't open Azure Activity Window", e);
                }
            }
        });
    }

    public static void notifyProgress(Object parent, String deploymentId, String deploymentURL, int progress, String message, Object... args) {
        DeploymentEventArgs arg = new DeploymentEventArgs(parent);
        arg.setId(deploymentId);
        arg.setDeploymentURL(deploymentURL);
        arg.setDeployMessage(String.format(message, args));
        arg.setDeployCompleteness(progress);
        arg.setStartTime(new Date());
        com.microsoft.azuretools.core.Activator.getDefault().fireDeploymentEvent(arg);
    }
}
