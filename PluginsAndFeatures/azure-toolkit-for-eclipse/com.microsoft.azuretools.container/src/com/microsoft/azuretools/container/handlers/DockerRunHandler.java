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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;
import com.microsoft.azuretools.container.Constant;

public class DockerRunHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        IProject project = PluginUtil.getSelectedProject();
        
        if (project == null) {
            MessageDialog.openError(window.getShell(), this.getClass().toString(), "Can't detect an active project");
            return null;
        }
        
        if (!SignInCommandHandler.doSignIn(window.getShell())){
            return null;
        }

        String destinationPath = project.getLocation() + Constant.DOCKER_CONTEXT_FOLDER + project.getName() + ".war";
        DockerClient docker;
        try {
            docker = DefaultDockerClient.fromEnv().build();
            export(project, destinationPath);
            String imageName = build(docker, project, project.getLocation() + Constant.DOCKER_CONTEXT_FOLDER );
            String webappUrl = run(docker, project, imageName);
            MessageDialog.openInformation(window.getShell(), "Docker Run", webappUrl);
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.openError(window.getShell(), this.getClass().toString(), e.getMessage());
            MessageDialog.openInformation(window.getShell(), this.getClass().toString(), Constant.MESSAGE_INSTRUCTION);
        }
        return null;
    }
    
    
    private String run(DockerClient docker, IProject project, String imageName) throws Exception{
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        List<PortBinding> randomPort = new ArrayList<>();
        PortBinding randomBinding = PortBinding.randomPort("0.0.0.0");
        randomPort.add(randomBinding);
        portBindings.put(Constant.TOMCAT_SERVICE_PORT, randomPort);
        
        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();
        
        final ContainerConfig config = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(imageName)
                .exposedPorts(Constant.TOMCAT_SERVICE_PORT)
                .build();
        final ContainerCreation container = docker.createContainer(config);
        docker.startContainer(container.id());
        final List<Container> containers = docker.listContainers();
        final Optional<Container> res = containers.stream().filter(item -> item.id().equals(container.id())).findFirst();
        
        if(res.isPresent()){
            return String.format(
                    "Image Name:\n%s\n\n"
                    + "Container ID:\n%s\n\n"
                    + "URL: http://%s:%s/%s/\n",
                    imageName,
                    res.get().id(),
                    docker.getHost(),
                    res.get().ports().stream().filter(item->item.privatePort().equals(8080)).findFirst().get().publicPort(), 
                    project.getName());
        }else{
            throw new Exception(String.format("Fail to start Container #id=%s", container.id()));
        }
    }
    
    private String build(DockerClient docker, IProject project, String dockerDirectory) throws DockerCertificateException, DockerException, InterruptedException, IOException{
        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();
        final String imageName = String.format("%s-%s:%tY%<tm%<td%<tH%<tM%<tS", Constant.IMAGE_PREFIX, project.getName(), new java.util.Date());
        final String returnedImageId = docker.build(
            Paths.get(dockerDirectory), imageName, new ProgressHandler() {
              @Override
              public void progress(ProgressMessage message) throws DockerException {
                final String imageId = message.buildImageId();
                if (imageId != null) {
                  imageIdFromMessage.set(imageId);
                }
              }
            });
        return imageName;
    }

    private void export(IProject project, String destinationPath) throws Exception {
        String projectName = project.getName();
        System.out.println("Building project '" + projectName + "'...");
        project.build(IncrementalProjectBuilder.FULL_BUILD, null);
    
        System.out.println("Exporting to WAR...");
        IDataModel dataModel = DataModelFactory.createDataModel(new WebComponentExportDataModelProvider());
        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, projectName);
        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, destinationPath);
    
        dataModel.getDefaultOperation().execute(null, null);
        System.out.println("Done.");
    }
}
