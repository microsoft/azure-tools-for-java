/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.container.model.DockerHost.DEFAULT_WINDOWS_HOST;


public class DockerUtil {
    public static DockerClient getDefaultDockerClient() {
        final DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        final DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
        return DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
    }

    public static DockerClient getDockerClient(String dockerHost, boolean tlsEnabled, String certPath) {
        final DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(dockerHost);
        if (tlsEnabled) {
            builder.withDockerCertPath(certPath).build();
        }
        final DefaultDockerClientConfig config = builder.build();
        final ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
        return DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();
    }

    public static void createDockerFile(String baseDir, String filename, String content) throws IOException {
        final File dockerFile = Paths.get(baseDir, filename).toFile();
        if (!dockerFile.exists()) {
            FileUtil.writeToFile(dockerFile, content);
        }
    }

    public static String createContainer(@Nonnull String imageNameWithTag, String port) throws DockerException {
        return createContainer(getDefaultDockerClient(), imageNameWithTag, port);
    }

    public static String createContainer(@Nonnull DockerClient docker, @Nonnull String imageNameWithTag, String port) throws DockerException {
        final CreateContainerCmd cmd = docker.createContainerCmd(imageNameWithTag)
            .withPortBindings(new PortBinding(Ports.Binding.bindPort(findFreePort()), ExposedPort.parse(port)));
        final CreateContainerResponse container = cmd.exec();
        return container.getId();
    }

    public static Container runContainer(@Nonnull String containerId) throws DockerException {
        return runContainer(getDefaultDockerClient(), containerId);
    }

    public static Container runContainer(@Nonnull DockerClient docker, @Nonnull String containerId) throws DockerException {
        docker.startContainerCmd(containerId).exec();
        final List<Container> containers = docker.listContainersCmd().exec();
        return containers.stream().filter(item -> item.getId().equals(containerId)).findFirst()
            .orElseThrow(() -> new DockerException("Error in starting container.", 404));
    }

    public static String buildImage(String imageNameWithTag, @Nonnull File dockerFile, File baseDir) throws DockerException {
        return buildImage(getDefaultDockerClient(), imageNameWithTag, dockerFile, baseDir);
    }

    @Nullable
    @AzureOperation(name = "boundary/docker.build_image.image|dir", params = {"imageNameWithTag", "baseDir"})
    public static String buildImage(@Nonnull DockerClient docker, @Nonnull DockerImage image, @Nullable BuildImageResultCallback callback)
            throws DockerException {
        return buildImage(docker, image.getImageName(), image.getDockerFile(), image.getBaseDirectory(), callback);
    }

    public static String buildImage(@Nonnull DockerClient docker, String imageNameWithTag, @Nonnull File dockerFile, File baseDir)
        throws DockerException {
        return buildImage(docker, imageNameWithTag, dockerFile, baseDir, null);
    }

    @AzureOperation(name = "boundary/docker.build_image.image|dir", params = {"imageNameWithTag", "baseDir"})
    public static String buildImage(@Nonnull DockerClient docker, String imageNameWithTag, @Nonnull File dockerFile, @Nullable File baseDir, @Nullable BuildImageResultCallback callback)
            throws DockerException {
        baseDir = Optional.ofNullable(baseDir).orElseGet(dockerFile::getParentFile);
        final String imageId = docker.buildImageCmd()
            .withDockerfile(dockerFile)
            .withBaseDirectory(baseDir)
            .withTags(Set.of(imageNameWithTag))
            .exec(Optional.ofNullable(callback).orElseGet(BuildImageResultCallback::new)).awaitImageId();
        return imageId == null ? null : imageNameWithTag;
    }

    public static void tagImage(@Nonnull DockerClient docker, final String sourceImageWithTag, final String repository, final String tag) throws DockerException {
        docker.tagImageCmd(sourceImageWithTag, repository, tag).exec();
    }

    public static void pushImage(@Nonnull String registryUrl, String username, String password, @Nonnull String targetImageName)
        throws DockerException, InterruptedException {
        pushImage(getDefaultDockerClient(), registryUrl, username, password, targetImageName);
    }

    @AzureOperation(name = "boundary/docker.push_image.image|registry", params = {"targetImageName", "registryUrl"})
    public static void pushImage(@Nonnull DockerClient dockerClient, @Nonnull String registryUrl, String username, String password,
                                 @Nonnull String targetImageName)
        throws DockerException, InterruptedException {
        pushImage(dockerClient, registryUrl, username, password, targetImageName, null);
    }

    @AzureOperation(name = "boundary/docker.push_image.image|registry", params = {"targetImageName", "registryUrl"})
    public static void pushImage(@Nonnull DockerClient dockerClient, @Nonnull String registryUrl, String username, String password,
                                 @Nonnull String targetImageName, @Nullable PushImageResultCallback callback)
            throws DockerException, InterruptedException {
        final AuthConfig authConfig = new AuthConfig().withUsername(username).withPassword(password).withRegistryAddress(registryUrl);
        final PushImageCmd cmd = dockerClient.pushImageCmd(targetImageName).withAuthConfig(authConfig);
        final PushImageResultCallback resultCallback = Optional.ofNullable(callback).orElseGet(PushImageResultCallback::new);
        cmd.exec(resultCallback).awaitCompletion();
    }

    public static void pullImage(@Nonnull String registryUrl, String username, String password,
                                 @Nonnull String repository, @Nonnull String tag)
        throws DockerException, InterruptedException {
        pullImage(getDefaultDockerClient(), registryUrl, username, password, repository, tag);
    }

    @AzureOperation(name = "boundary/docker.pull_image.image|registry", params = {"repository", "registryUrl"})
    public static void pullImage(@Nonnull DockerClient dockerClient, @Nonnull String registryUrl, String username, String password,
                                 @Nonnull String repository, @Nonnull String tag)
        throws DockerException, InterruptedException {
        final AuthConfig authConfig = new AuthConfig().withUsername(username).withPassword(password).withRegistryAddress(registryUrl);
        final String fullRepositoryName = String.format("%s/%s", registryUrl, repository);
        final PullImageCmd cmd = dockerClient.pullImageCmd(fullRepositoryName).withRegistry(registryUrl).withTag(tag).withAuthConfig(authConfig);
        cmd.exec(new ResultCallback.Adapter<>()).awaitCompletion();
    }

    public static void stopContainer(@Nonnull String containerId) throws DockerException {
        stopContainer(getDefaultDockerClient(), containerId);
    }

    public static void stopContainer(@Nonnull DockerClient dockerClient, @Nonnull String containerId) throws DockerException {
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();
    }

    public static DockerClient getDockerClient(@Nonnull final DockerHost dockerHost) {
        return getDockerClient(dockerHost.getDockerHost(), dockerHost.isTlsEnabled(), dockerHost.getDockerCertPath());
    }

    // todo: add host configurations from docker extension
    public static List<DockerHost> getDockerHosts() {
        final List<DockerHost> result = new ArrayList<>();
        final DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        final String certPath = config.getSSLConfig() instanceof LocalDirectorySSLConfig ? ((LocalDirectorySSLConfig) config.getSSLConfig()).getDockerCertPath() : null;
        result.add(new DockerHost(config.getDockerHost().toString(), certPath));
        if (!SystemInfo.isWindows && result.contains(DEFAULT_WINDOWS_HOST)) {
            result.add(DEFAULT_WINDOWS_HOST);
        }
        return result;
    }

    /**
     * check if the default docker file exists.
     * If yes, return the path as a String.
     * Else return an empty String.
     */
    public static String getDefaultDockerFilePathIfExist(String basePath) {
        try {
            if (!StringUtils.isEmpty(basePath)) {
                final Path targetDockerfile = Paths.get(basePath, Constant.DOCKERFILE_NAME);
                if (targetDockerfile.toFile().exists()) {
                    return targetDockerfile.toString();
                }
            }
        } catch (final RuntimeException ignored) {
        }
        return "";
    }

    public static void ping() {
        ping(getDefaultDockerClient());
    }

    @AzureOperation(name = "boundary/docker.ping_host")
    public static void ping(DockerClient docker) {
        try {
            docker.pingCmd().exec();
        } catch (final DockerException e) {
            throw new AzureToolkitRuntimeException("Failed to connect docker server, \nIs Docker installed and running?");
        }
    }

    public static List<DockerImage> listLocalImages(@Nonnull final DockerClient dockerClient) {
        final List<Image> images = dockerClient.listImagesCmd().exec();
        return images.stream().map(DockerImage::new).collect(Collectors.toList());
    }

    public static DockerImage getImageWithName(@Nonnull final DockerClient dockerClient, @Nonnull final String imageName) {
        return dockerClient.listImagesCmd().exec().stream().filter(image -> {
            final String[] repoTags = image.getRepoTags();
            return repoTags != null && Arrays.asList(repoTags).contains(imageName);
        }).findFirst().map(DockerImage::new).orElse(null);
    }

    // todo: move to socket utils
    private static int findFreePort() {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (final IOException e) {
            return -1;
        }
    }
}
