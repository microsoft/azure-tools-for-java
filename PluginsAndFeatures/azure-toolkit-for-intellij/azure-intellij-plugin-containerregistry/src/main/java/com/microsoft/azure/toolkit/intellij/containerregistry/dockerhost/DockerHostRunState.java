/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.Constant;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class DockerHostRunState extends AzureRunProfileState<String> {
    private static final String DEFAULT_PORT = Constant.TOMCAT_SERVICE_PORT;
    private static final Pattern PORT_PATTERN = Pattern.compile("EXPOSE\\s+(\\d+).*");
    private final DockerHostRunSetting dataModel;
    private String containerId;

    public DockerHostRunState(Project project, DockerHostRunSetting dataModel) {
        super(project);
        this.dataModel = dataModel;
    }

    @Override
    @AzureOperation(
        name = "platform/docker.run_image.repo|tag|host",
        params = {"this.dataModel.getImageName()", "this.dataModel.getTagName()", "this.dataModel.getDockerHost()"}
    )
    public String executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        final AzureDockerClient docker = AzureDockerClient.from(dataModel.getDockerHost(), dataModel.isTlsEnabled(), dataModel.getDockerCertPath());
        final File file = Optional.ofNullable(dataModel.getDockerFilePath()).map(File::new).filter(File::exists).orElse(null);
        Optional.ofNullable(containerId).filter(StringUtils::isNoneBlank).ifPresent(docker::stopContainer);
        containerId = docker.createContainer(String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName()), dataModel.getPort());

        final Container container = docker.runContainer(containerId);
        // props
        final String hostname = Optional.ofNullable(new URI(dataModel.getDockerHost()).getHost()).orElse("localhost");
        final ContainerPort[] ports = container.getPorts();
        final String message = ArrayUtils.getLength(ports) != 1 ? String.format("Container is running on %s now!", hostname) :
            String.format(Constant.MESSAGE_CONTAINER_STARTED, hostname + ":" + ports[0].getPublicPort());
        AzureMessager.getMessager().info(message);
        if (ArrayUtils.getLength(ports) > 1) {
            Arrays.stream(ports).forEach(port -> AzureMessager.getMessager().info(String.format("\tImage port %s has been redirected to %s", port.getPrivatePort(), port.getPublicPort())));
        }
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@Nonnull ProcessEvent event, boolean willBeDestroyed) {
                AzureTaskManager.getInstance().runInBackground(String.format("Stop Container %s", containerId), () -> {
                    try {
                        docker.stopContainer(containerId);
                    } catch (final Exception e) {
                        AzureMessager.getMessager().warning(String.format("Failed to stop container %s", containerId), e);
                    }
                });
            }
        });
        return hostname;
    }

    @Nonnull
    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP_DOCKERLOCAL);
    }

    @Override
    protected void onSuccess(String result, @Nonnull RunProcessHandler processHandler) {
        processHandler.setText("Container started.");
    }

    protected Map<String, String> getTelemetryMap() {
        final String fileType = dataModel.getTargetName() == null ? StringUtils.EMPTY : MavenRunTaskUtil.getFileType(dataModel.getTargetName());
        return Collections.singletonMap(TelemetryConstants.FILETYPE, fileType);
    }
}
