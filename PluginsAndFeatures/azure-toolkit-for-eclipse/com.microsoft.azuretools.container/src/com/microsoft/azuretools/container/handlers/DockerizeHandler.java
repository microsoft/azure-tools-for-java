/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.container.handlers;

import java.io.ByteArrayInputStream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DefaultDockerClient.Builder;
import com.microsoft.azuretools.container.Runtime;
public class DockerizeHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        ConsoleLogger.info("Adding docker support ...");
        try {
            IProject project = PluginUtil.getSelectedProject();
            if (project == null) {
                throw new Exception("Can't detect an active project");
            }
            IFolder folder = project.getFolder(Constant.DOCKER_CONTEXT_FOLDER);
            if(!folder.exists()) folder.create(true, true, null);

            createDockerFile(project, folder, Constant.DOCKERFILE_NAME);
            ConsoleLogger.info(String.format(
                    "Docker file created at: %s", 
                    folder.getFile(Constant.DOCKERFILE_NAME).getFullPath()
                    ));
            Builder builder = DefaultDockerClient.fromEnv();
            ConsoleLogger.info(String.format(
                    "Current docker host: %s", 
                    builder.uri()
                    ));
            Runtime.setDocker(builder.build());
            ConsoleLogger.info("Successfully added docker support!");
            ConsoleLogger.info(Constant.MESSAGE_INSTRUCTION);
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLogger.error(Constant.ERROR_CREATING_DOCKERFILE);
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
