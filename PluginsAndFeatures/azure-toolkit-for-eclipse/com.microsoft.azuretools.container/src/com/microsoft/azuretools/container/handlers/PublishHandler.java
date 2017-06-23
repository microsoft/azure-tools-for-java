package com.microsoft.azuretools.container.handlers;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.ui.wizard.publish.PublishWizard;
import com.microsoft.azuretools.container.ui.wizard.publish.PublishWizardDialog;
import com.microsoft.azuretools.container.utils.DockerUtil;
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

        DockerClient dockerClient = DockerRuntime.getInstance().getDockerBuilder().build();
        try {
            ConsoleLogger.info(Constant.MESSAGE_BUILDING_IMAGE);
            DockerUtil.buildImage(dockerClient, project, project.getLocation() + Constant.DOCKER_CONTEXT_FOLDER);
        } catch (DockerCertificateException | DockerException | InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }

        PublishWizard pw = new PublishWizard();
        WizardDialog pwd = new PublishWizardDialog(window.getShell(), pw);
        pwd.open();
        return null;
    }

}
