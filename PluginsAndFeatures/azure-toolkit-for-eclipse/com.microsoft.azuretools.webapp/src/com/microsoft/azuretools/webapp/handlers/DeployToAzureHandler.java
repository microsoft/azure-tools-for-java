/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
