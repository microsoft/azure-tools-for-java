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
package com.microsoft.azuretools.azureexplorer.actions.docker;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;

import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;

@Name("Publish")
public class PublishDockerContainerAction extends NodeActionListener {
    private static final Logger log = Logger.getLogger(PublishDockerContainerAction.class.getName());
    IProject project;
    DockerHostModule dockerHostModule;

    public PublishDockerContainerAction(DockerHostModule dockerHostModule) {
        this.dockerHostModule = dockerHostModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent event) {
        try {
            if (!SignInCommandHandler.doSignIn(PluginUtil.getParentShell()))
                return;

            IProject project;
            try {
                project = AzureDockerUIResources.getCurrentSelectedProject();
            } catch (Exception Ignored) {
                project = null;
            }
            if (project == null) {
                project = (IProject) dockerHostModule.getProject();
            }

            AzureDockerUIResources.publish2DockerHostContainer(PluginUtil.getParentShell(), project, null);
        } catch (Exception ex1) {
            log.log(Level.SEVERE, "actionPerformed", ex1);
            ex1.printStackTrace();
        }
    }
}
