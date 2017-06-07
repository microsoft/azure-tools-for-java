package com.microsoft.azuretools.container.handlers;

import java.io.ByteArrayInputStream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class DockerizeHandler extends AzureAbstractHandler {

	@Override
	public Object onExecute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IProject project = PluginUtil.getSelectedProject();
	    IFolder folder = project.getFolder(Constant.DOCKER_CONTEXT_FOLDER);
		try {
		    if(!folder.exists()) folder.create(true, true, null);
			createDockerFile(project, folder, Constant.DOCKERFILE_NAME);
			MessageDialog.openInformation(window.getShell(), "Add Docker Support", String.format("%s\n\n%s", Constant.MESSAGE_DOCKERFILE_CREATED, Constant.MESSAGE_INSTRUCTION));
		} catch (CoreException e) {
			e.printStackTrace();
			MessageDialog.openError(window.getShell(), this.getClass().toString(), Constant.ERROR_CREATING_DOCKERFILE);
		}
		return null;
	}
	
	public void createDockerFile(IProject project, IFolder folder, String filename) throws CoreException{
		//create file
		IFile dockerfile = folder.getFile(filename);
		if (!dockerfile.exists()) {
		    byte[] bytes = String.format(Constant.DOCKERFILE_CONTENT_TOMCAT, project.getName()).getBytes();
		    dockerfile.create(new ByteArrayInputStream(bytes), false, null);
		}
		
	}

}
