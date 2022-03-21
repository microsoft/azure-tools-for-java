/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.dockerhost;

import com.google.common.collect.ImmutableList;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.intellij.legacy.docker.utils.Constant;
import com.microsoft.azure.toolkit.intellij.legacy.docker.utils.DockerProgressHandler;
import com.microsoft.azure.toolkit.intellij.legacy.docker.utils.DockerUtil;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.RunProcessHandler;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerHostRunState extends AzureRunProfileState<String> {
    private static final String DEFAULT_PORT = Constant.TOMCAT_SERVICE_PORT;
    private static final Pattern PORT_PATTERN = Pattern.compile("EXPOSE\\s+(\\d+).*");
    private static final String DOCKER_PING_ERROR = "Failed to connect docker host: %s\nIs Docker installed and running?";
    private final DockerHostRunSetting dataModel;

    public DockerHostRunState(Project project, DockerHostRunSetting dataModel) {
        super(project);
        this.dataModel = dataModel;
    }

    @Override
    public String executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception {
        final String[] runningContainerId = {null};

        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(ProcessEvent processEvent) {

            }

            @Override
            public void processTerminated(ProcessEvent processEvent) {
            }

            @Override
            public void processWillTerminate(ProcessEvent processEvent, boolean b) {
                try {
                    DockerClient docker = DockerUtil.getDockerClient(
                            dataModel.getDockerHost(),
                            dataModel.isTlsEnabled(),
                            dataModel.getDockerCertPath()
                    );
                    DockerUtil.stopContainer(docker, runningContainerId[0]);
                } catch (Exception e) {
                    // ignore
                }
            }

            @Override
            public void onTextAvailable(ProcessEvent processEvent, Key key) {
            }
        });

        processHandler.setText("Starting job ...  ");
        String basePath = project.getBasePath();
        if (basePath == null) {
            processHandler.println("Project base path is null.", ProcessOutputTypes.STDERR);
            throw new FileNotFoundException("Project base path is null.");
        }
        // locate artifact to specified location
        String targetFilePath = dataModel.getTargetPath();
        processHandler.setText(String.format("Locating artifact ... [%s]", targetFilePath));
        // validate dockerfile
        Path targetDockerfile = Paths.get(dataModel.getDockerFilePath());
        processHandler.setText(String.format("Validating dockerfile ... [%s]", targetDockerfile));
        if (!targetDockerfile.toFile().exists()) {
            throw new FileNotFoundException("Dockerfile not found.");
        }
        // replace placeholder if exists
        String content = new String(Files.readAllBytes(targetDockerfile));
        content = content.replaceAll(Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER,
                Paths.get(basePath).toUri().relativize(Paths.get(targetFilePath).toUri()).getPath()
        );
        Files.write(targetDockerfile, content.getBytes());
        // build image
        String imageNameWithTag = String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName());
        processHandler.setText(String.format("Building image ...  [%s]", imageNameWithTag));
        DockerClient docker = DockerUtil.getDockerClient(
                dataModel.getDockerHost(),
                dataModel.isTlsEnabled(),
                dataModel.getDockerCertPath()
        );
        DockerUtil.ping(docker);
        DockerUtil.buildImage(docker,
                imageNameWithTag,
                targetDockerfile.getParent(),
                targetDockerfile.getFileName().toString(),
                new DockerProgressHandler(processHandler)
        );
        // docker run
        String containerServerPort = getPortFromDockerfile(content);
        if (StringUtils.isBlank(containerServerPort)) {
            if (StringUtils.endsWith(targetFilePath, MavenConstants.TYPE_WAR)) {
                containerServerPort = "80";
            } else {
                containerServerPort = "8080";
            }
        }
        String containerId = DockerUtil.createContainer(docker, String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName()), containerServerPort);
        runningContainerId[0] = containerId;
        Container container = DockerUtil.runContainer(docker, containerId);
        // props
        String hostname = new URI(dataModel.getDockerHost()).getHost();
        String publicPort = null;
        ImmutableList<Container.PortMapping> ports = container.ports();
        if (ports != null) {
            for (Container.PortMapping portMapping : ports) {
                if (StringUtils.equals(containerServerPort, String.valueOf(portMapping.privatePort()))) {
                    publicPort = String.valueOf(portMapping.publicPort());
                }
            }
        }
        processHandler.setText(String.format(Constant.MESSAGE_CONTAINER_STARTED,
                (hostname != null ? hostname : "localhost") + (publicPort != null ? ":" + publicPort : "")
        ));
        return null;
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP_DOCKERLOCAL);
    }

    @Override
    protected void onSuccess(String result, @NotNull RunProcessHandler processHandler) {
        processHandler.setText("Container started.");
    }

    protected Map<String, String> getTelemetryMap() {
        final String fileType = dataModel.getTargetName() == null ? StringUtils.EMPTY : MavenRunTaskUtil.getFileType(dataModel.getTargetName());
        return Collections.singletonMap(TelemetryConstants.FILETYPE, fileType);
    }

    private String getPortFromDockerfile(@NotNull String dockerFileContent) {
        final Matcher result = Arrays.stream(dockerFileContent.split("\\R+"))
                                     .map(value -> PORT_PATTERN.matcher(value))
                                     .filter(Matcher::matches)
                                     .findFirst().orElse(null);
        return result == null ? DEFAULT_PORT : result.group(1);
    }
}
