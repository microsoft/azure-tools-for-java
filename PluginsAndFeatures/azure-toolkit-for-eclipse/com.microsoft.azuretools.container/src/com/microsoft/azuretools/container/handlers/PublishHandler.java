/**
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

package com.microsoft.azuretools.container.handlers;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.ui.wizard.publish.PublishWizard;
import com.microsoft.azuretools.container.utils.ConfigFileUtil;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.core.components.AzureWizardDialog;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

public class PublishHandler extends AzureAbstractHandler {

	@Override
	public Object onExecute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IProject project = PluginUtil.getSelectedProject();
		Properties props = ConfigFileUtil.loadConfig(project);
		DockerRuntime.getInstance().loadFromProps(props);
		DockerClient dockerClient = DockerRuntime.getInstance().getDockerBuilder().build();
		try {
			DockerUtil.buildImage(dockerClient, project, project.getLocation() + Constant.DOCKER_CONTEXT_FOLDER);
		} catch (DockerCertificateException | DockerException | InterruptedException | IOException e) {
			e.printStackTrace();
			return null;
		}

		PublishWizard pw = new PublishWizard();
		WizardDialog pwd = new AzureWizardDialog(window.getShell(), pw);
		if (pwd.open() == Window.OK) {
			ConsoleLogger.info(String.format("URL: http://%s.azurewebsites.net/%s",
					DockerRuntime.getInstance().getLatestImageName(), project.getName()));
		}
		props = DockerRuntime.getInstance().saveToProps(props);
		ConfigFileUtil.saveConfig(project, props);
		return null;
	}

}
