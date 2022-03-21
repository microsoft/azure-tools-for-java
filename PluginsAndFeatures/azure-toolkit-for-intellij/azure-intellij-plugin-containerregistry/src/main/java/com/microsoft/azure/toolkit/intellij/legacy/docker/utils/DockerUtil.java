/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class DockerUtil {
    private static final String DOCKER_PING_ERROR = "Failed to connect docker host: %s\nIs Docker installed and running?";

    /**
     * create a docker file in specified folder.
     */
    public static void createDockerFile(String basePath, String folderName, String filename, String content)
            throws IOException {
        if (StringUtils.isEmpty(basePath)) {
            throw new FileNotFoundException("Project basePath is null.");
        }
        //noinspection ResultOfMethodCallIgnored
        Paths.get(basePath, folderName).toFile().mkdirs();
        Path dockerFilePath = Paths.get(basePath, folderName, filename);
        if (!dockerFilePath.toFile().exists()) {
            byte[] bytes = content.getBytes();
            Files.write(dockerFilePath, bytes);
        }
    }

    /**
     * create container with specified ImageName:TagName.
     */
    public static String createContainer(DockerClient docker, String imageNameWithTag, String containerServerPort)
            throws DockerException, InterruptedException {
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        List<PortBinding> randomPort = new ArrayList<>();
        PortBinding randomBinding = PortBinding.randomPort("0.0.0.0");
        randomPort.add(randomBinding);
        portBindings.put(containerServerPort, randomPort);

        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        final ContainerConfig config = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(imageNameWithTag)
                .exposedPorts(containerServerPort)
                .build();
        final ContainerCreation container = docker.createContainer(config);
        return container.id();
    }

    /**
     * runContainer.
     */
    public static Container runContainer(DockerClient docker, String containerId) throws DockerException,
            InterruptedException {
        docker.startContainer(containerId);
        List<Container> containers = docker.listContainers();
        Optional<Container> container = containers.stream().filter(item -> item.id().equals(containerId))
                .findFirst();
        if (container.isPresent()) {
            return container.get();
        } else {
            throw new DockerException("Error in starting container.");
        }
    }

    /**
     * build image.
     */
    @AzureOperation(
        name = "docker.build_image.image|dir|host",
        params = {"imageNameWithTag", "dockerDirectory", "docker.getHost()"},
        type = AzureOperation.Type.TASK
    )
    public static String buildImage(DockerClient docker, String imageNameWithTag, Path dockerDirectory,
                                    String dockerFile, ProgressHandler progressHandler)
            throws DockerException, InterruptedException, IOException {
        String imageId = docker.build(dockerDirectory, imageNameWithTag, dockerFile, progressHandler);
        return imageId == null ? null : imageNameWithTag;
    }

    /**
     * Push image to a private registry.
     */
    @AzureOperation(
        name = "docker.push_image.image|registry",
        params = {"targetImageName", "registryUrl"},
        type = AzureOperation.Type.TASK
    )
    public static void pushImage(DockerClient dockerClient, String registryUrl, String registryUsername,
                                 String registryPassword, String targetImageName,
                                 ProgressHandler handler)
            throws DockerException, InterruptedException {
        final RegistryAuth registryAuth = RegistryAuth.builder().username(registryUsername).password(registryPassword)
                .build();
        if (targetImageName.startsWith(registryUrl)) {
            dockerClient.push(targetImageName, handler, registryAuth);
        } else {
            throw new DockerException("serverUrl and imageName mismatch.");
        }
    }

    /**
     * Pull image from a private registry.
     */
    public static void pullImage(DockerClient dockerClient, String registryUrl, String registryUsername,
                                 String registryPassword, String targetImageName)
            throws DockerException, InterruptedException {
        final RegistryAuth registryAuth = RegistryAuth.builder().username(registryUsername).password(registryPassword)
                .build();
        if (targetImageName.startsWith(registryUrl)) {
            dockerClient.pull(targetImageName, registryAuth);
        } else {
            throw new DockerException("serverUrl and imageName mismatch.");
        }
    }

    /**
     * Stop a container by id.
     */
    public static void stopContainer(DockerClient dockerClient, String runningContainerId) throws DockerException,
            InterruptedException {
        if (runningContainerId != null) {
            dockerClient.stopContainer(runningContainerId, Constant.TIMEOUT_STOP_CONTAINER);
            dockerClient.removeContainer(runningContainerId);
        }
    }

    /**
     * Get DockerClient instance.
     */
    public static DockerClient getDockerClient(String dockerHost, boolean tlsEnabled, String certPath) throws
            DockerCertificateException {
        if (tlsEnabled) {
            return DefaultDockerClient.builder().uri(URI.create(dockerHost))
                    .dockerCertificates(new DockerCertificates(Paths.get(certPath)))
                    .build();
        } else {
            return DefaultDockerClient.builder().uri(URI.create(dockerHost)).build();
        }
    }

    /**
     * check if the default docker file exists.
     * If yes, return the path as a String.
     * Else return an empty String.
     */
    public static String getDefaultDockerFilePathIfExist(String basePath) {
        try {
            if (!StringUtils.isEmpty(basePath)) {
                Path targetDockerfile = Paths.get(basePath, Constant.DOCKERFILE_NAME);
                if (targetDockerfile != null && targetDockerfile.toFile().exists()) {
                    return targetDockerfile.toString();
                }
            }
        } catch (RuntimeException ignored) {
        }
        return "";
    }

    @AzureOperation(
        name = "docker.ping_host.host",
        params = {"docker.getHost()"},
        type = AzureOperation.Type.TASK
    )
    public static void ping(DockerClient docker) throws AzureExecutionException {
        try {
            docker.ping();
        } catch (DockerException | InterruptedException e) {
            final String msg = String.format(DOCKER_PING_ERROR, docker.getHost());
            AzureMessager.getMessager().error(msg, "Failed to connect docker host");
            throw new AzureExecutionException(String.format("Failed to connect docker host: %s", docker.getHost()));
        }
    }
}
