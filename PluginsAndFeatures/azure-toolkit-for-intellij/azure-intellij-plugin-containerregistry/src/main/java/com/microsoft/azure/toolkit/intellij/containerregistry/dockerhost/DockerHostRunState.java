/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.container.Constant;
import com.microsoft.azure.toolkit.intellij.container.DockerUtil;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
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
    public String executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        final DockerClient docker = DockerUtil.getDockerClient(dataModel.getDockerHost(), dataModel.isTlsEnabled(), dataModel.getDockerCertPath());
        final File file = Optional.ofNullable(dataModel.getDockerFilePath()).map(File::new).filter(File::exists).orElse(null);
        final String dockerFileContent = file == null ? null : FileUtils.readFileToString(file, "UTF-8");
        final String containerServerPort = Optional.ofNullable(dockerFileContent).map(this::getPortFromDockerfile).orElse(DEFAULT_PORT);
        Optional.ofNullable(containerId).filter(StringUtils::isNoneBlank).ifPresent(id -> DockerUtil.stopContainer(docker, id));
        containerId = DockerUtil.createContainer(docker, String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName()), containerServerPort);

        final Container container = DockerUtil.runContainer(docker, containerId);
        // props
        final String hostname = new URI(dataModel.getDockerHost()).getHost();
        String publicPort = null;
        final ContainerPort[] ports = container.getPorts();
        if (ports != null) {
            for (ContainerPort portMapping : ports) {
                if (StringUtils.equals(containerServerPort, String.valueOf(portMapping.getPrivatePort()))) {
                    publicPort = String.valueOf(portMapping.getPublicPort());
                }
            }
        }
        processHandler.setText(String.format(Constant.MESSAGE_CONTAINER_STARTED,
            (hostname != null ? hostname : "localhost") + (publicPort != null ? ":" + publicPort : "")
        ));
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                DockerUtil.stopContainer(containerId);
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

    private String getPortFromDockerfile(@Nonnull String dockerFileContent) {
        final Matcher result = Arrays.stream(dockerFileContent.split("\\R+"))
            .map(PORT_PATTERN::matcher)
            .filter(Matcher::matches)
            .findFirst().orElse(null);
        return result == null ? DEFAULT_PORT : result.group(1);
    }

}
