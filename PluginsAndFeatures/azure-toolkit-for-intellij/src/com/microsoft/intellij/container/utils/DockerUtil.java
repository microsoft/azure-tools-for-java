
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

package com.microsoft.intellij.container.utils;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.container.ConsoleLogger;
import com.microsoft.intellij.container.Constant;
import com.microsoft.intellij.container.DockerRuntime;
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
import com.spotify.docker.client.messages.RegistryAuth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


public class DockerUtil {
    /**
     * create a docker file in specified folder.
     */
    public static void createDockerFile(Project project, String foldername, String filename, String content)
            throws IOException {
        String basePath = project.getBasePath();
        Paths.get(basePath, foldername).toFile().mkdirs();
        Path dockerFilePath = Paths.get(basePath, foldername, filename);
        if(!dockerFilePath.toFile().exists()){
            byte[] bytes = String.format(content, project.getName()).getBytes();
            Files.write(dockerFilePath, bytes);
        }
    }

    /**
     * createContainer.
     *
     * @param docker
     * @param project
     * @param imageName
     * @return
     * @throws DockerException
     * @throws InterruptedException
     */
    public static String createContainer(DockerClient docker, Project project, String imageName)
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

    /**
     * runContainer.
     *
     * @param docker
     * @param containerId
     * @return
     * @throws Exception
     */
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

    /**
     * buildImage.
     *
     * @param docker
     * @param project
     * @param dockerDirectory
     * @return
     * @throws DockerCertificateException
     * @throws DockerException
     * @throws InterruptedException
     * @throws IOException
     */
    public static String buildImage(DockerClient docker, Project project, Path dockerDirectory)
            throws DockerCertificateException, DockerException, InterruptedException, IOException {
        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();
        final String imageName = String.format("%s-%s:%tY%<tm%<td%<tH%<tM%<tS", Constant.IMAGE_PREFIX,
                project.getName().toLowerCase(), new java.util.Date());
        docker.build(dockerDirectory, imageName, new ProgressHandler() {
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

    public static void pushImage(DockerClient dockerClient, String registryUrl, String registryUsername,
                     String registryPassword, String latestImageName, String targetImageName, ProgressHandler handler)
            throws DockerException, InterruptedException {
        final RegistryAuth registryAuth = RegistryAuth.builder().username(registryUsername).password(registryPassword)
                .build();
        dockerClient.tag(latestImageName, targetImageName);
        dockerClient.push(targetImageName, handler, registryAuth);
    }
}
