/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.webapp.handlers;

import com.microsoft.azuretools.core.actions.MavenExecuteAction;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.webapp.ui.WebAppDeployDialog;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class DeployToAzureHandler extends AzureAbstractHandler {

    private static final String MAVEN_GOALS = "package";
    private static final String TITLE = "Deploy to Azure App Service";
    private static final String NO_PROJECT_ERR = "Can't detect an active project";

    @Override
    public Object onExecute(ExecutionEvent ee) throws ExecutionException {
        IProject project = PluginUtil.getSelectedProject();
        Shell shell = HandlerUtil.getActiveWorkbenchWindowChecked(ee).getShell();
        if (project != null) {
            if (!SignInCommandHandler.doSignIn(shell)) {
                return null;
            }
        } else {
            MessageDialog.openInformation(shell, TITLE, NO_PROJECT_ERR);
            return null;
        }
        try {
            if (MavenUtils.isMavenProject(project)) {
                MavenExecuteAction action = new MavenExecuteAction(MAVEN_GOALS);
                IContainer container;
                container = MavenUtils.getPomFile(project).getParent();
                action.launch(container, () -> {
                    DefaultLoader.getIdeHelper().invokeLater(() -> WebAppDeployDialog.go(shell, project));
                    return null;
                });
            } else {
                WebAppDeployDialog.go(shell, project);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.openInformation(shell, TITLE, e.getMessage());
        }
        return null;
    }
}
