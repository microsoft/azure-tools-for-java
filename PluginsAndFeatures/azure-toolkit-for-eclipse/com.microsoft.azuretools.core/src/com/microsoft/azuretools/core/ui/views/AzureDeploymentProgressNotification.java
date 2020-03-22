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
