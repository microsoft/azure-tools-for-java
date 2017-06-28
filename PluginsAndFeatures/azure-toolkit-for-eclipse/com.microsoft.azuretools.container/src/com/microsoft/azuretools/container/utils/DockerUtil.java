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

package com.microsoft.azuretools.container.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerRuntime;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;

public class DockerUtil {
    /**
     * create a docker file in specified folder.
     */
    public static void createDockerFile(IProject project, String foldername, String filename, String content)
            throws CoreException {
        // create file
        IFolder folder = project.getFolder(foldername);
        if (!folder.exists()) {
            folder.create(true, true, null);
        }
        IFile dockerfile = folder.getFile(filename);
        if (!dockerfile.exists()) {
            byte[] bytes = String.format(content, project.getName()).getBytes();
            dockerfile.create(new ByteArrayInputStream(bytes), false, null);
        }
    }

    public static String createContainer(DockerClient docker, IProject project, String imageName)
            throws DockerException, InterruptedException {
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        List<PortBinding> randomPort = new ArrayList<>();
        PortBinding randomBinding = PortBinding.randomPort("0.0.0.0");
        randomPort.add(randomBinding);
        portBindings.put(Constant.TOMCAT_SERVICE_PORT, randomPort);

        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        final ContainerConfig config = ContainerConfig.builder().hostConfig(hostConfig).image(imageName)
                .exposedPorts(Constant.TOMCAT_SERVICE_PORT).build();
        final ContainerCreation container = docker.createContainer(config);
        return container.id();
    }

    public static String runContainer(DockerClient docker, String containerId) throws Exception {
        docker.startContainer(containerId);
        final List<Container> containers = docker.listContainers();
        final Optional<Container> res = containers.stream().filter(item -> item.id().equals(containerId)).findFirst();

        if (res.isPresent()) {
            DockerRuntime.getInstance().setRunningContainerId(res.get().id());
            return String.format("http://%s:%s", docker.getHost(),
                    res.get().ports().stream()
                            .filter(item -> item.privatePort().toString().equals(Constant.TOMCAT_SERVICE_PORT))
                            .findFirst().get().publicPort());
        } else {
            String errorMsg = String.format(Constant.ERROR_STARTING_CONTAINER, containerId);
            ConsoleLogger.error(errorMsg);
            throw new Exception(errorMsg);
        }
    }

    public static String buildImage(DockerClient docker, IProject project, String dockerDirectory)
            throws DockerCertificateException, DockerException, InterruptedException, IOException {
        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();
        final String imageName = String.format("%s-%s:%tY%<tm%<td%<tH%<tM%<tS", Constant.IMAGE_PREFIX,
                project.getName().toLowerCase(), new java.util.Date());
        docker.build(Paths.get(dockerDirectory), imageName, new ProgressHandler() {
            @Override
            public void progress(ProgressMessage message) throws DockerException {
                final String imageId = message.buildImageId();
                if (imageId != null) {
                    imageIdFromMessage.set(imageId);
                }
            }
        });
        DockerRuntime.getInstance().setLatestImageName(imageName);
        return imageName;
    }

    public static boolean containerExists(DockerClient docker, String containerId)
            throws DockerException, InterruptedException {
        long count = docker.listContainers().stream().filter(item -> item.id().equals(containerId)).count();
        return (count > 0);
    }
}
