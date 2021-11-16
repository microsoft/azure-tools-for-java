/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.appservice.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.appservice.ui.WebAppDeployDialog;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class DeployToAzureHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent ee) throws ExecutionException {
        IProject project = PluginUtil.getSelectedProject();
        Shell shell = HandlerUtil.getActiveWorkbenchWindowChecked(ee).getShell();
        String resourceId = ee.getParameter("resourceId");
        SignInCommandHandler.requireSignedIn(shell, () ->
                DefaultLoader.getIdeHelper().invokeLater(() -> WebAppDeployDialog.go(shell, project, resourceId)));
        return null;
    }
}
