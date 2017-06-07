package com.microsoft.azuretools.container.testers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class DockerizedTester extends PropertyTester {

	@Override
	public boolean test(Object arg0, String arg1, Object[] arg2, Object arg3) {
		IProject project = PluginUtil.getSelectedProject();
		if(!project.exists()){ return false; }
		IFolder folder = project.getFolder(Constant.DOCKER_CONTEXT_FOLDER);
		if(!folder.exists()) { return false; }
		IFile file = folder.getFile(Constant.DOCKERFILE_NAME);
		return file.exists();
	}
}
