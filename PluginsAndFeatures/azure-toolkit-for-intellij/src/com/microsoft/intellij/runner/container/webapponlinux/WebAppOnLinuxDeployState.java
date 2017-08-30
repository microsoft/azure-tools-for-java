/*
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

package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.container.utils.Constant;
import com.microsoft.intellij.runner.container.utils.DockerProgressHandler;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;

public class WebAppOnLinuxDeployState implements RunProfileState {
    private final WebAppOnLinuxDeployModel deployModel;
    private final Project project;


    public WebAppOnLinuxDeployState(Project project, WebAppOnLinuxDeployModel webAppOnLinuxDeployModel) {
        this.deployModel = webAppOnLinuxDeployModel;
        this.project = project;
    }

    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        final RunProcessHandler processHandler = new RunProcessHandler();
        processHandler.addDefaultListener();
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);

        Observable.fromCallable(
                () -> {
                    processHandler.setText("Starting job ...  ");
                    String basePath = project.getBasePath();
                    if (basePath == null) {
                        processHandler.println("Project base path is null.", ProcessOutputTypes.STDERR);
                        throw new FileNotFoundException("Project base path is null.");
                    }
                    // locate artifact to specified location
                    String targetFilePath = deployModel.getTargetPath();
                    processHandler.setText(String.format("Locating artifact ... [%s]", targetFilePath));
                    String targetFileName = deployModel.getTargetName();

                    // validate dockerfile
                    Path targetDockerfile = Paths.get(basePath, Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME);
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
                    String imageNameWithTag = deployModel.getPrivateRegistryImageSetting().getImageNameWithTag();
                    processHandler.setText(String.format("Building image ...  [%s]", imageNameWithTag));
                    DockerClient docker = DefaultDockerClient.fromEnv().build();
                    String latestImageName = DockerUtil.buildImage(docker,
                            imageNameWithTag,
                            Paths.get(basePath, Constant.DOCKERFILE_FOLDER),
                            new DockerProgressHandler(processHandler)
                    );

                    // push to ACR
                    PrivateRegistryImageSetting acrInfo = deployModel.getPrivateRegistryImageSetting();
                    processHandler.setText(String.format("Pushing to ACR ... [%s] ", acrInfo.getServerUrl()));
                    DockerUtil.pushImage(docker, acrInfo.getServerUrl(), acrInfo.getUsername(), acrInfo.getPassword(),
                            acrInfo.getImageNameWithTag(),
                            new DockerProgressHandler(processHandler)
                    );

                    // deploy
                    if (deployModel.isCreatingNewWebAppOnLinux()) {
                        // create new WebApp
                        processHandler.setText(String.format("Creating new WebApp ... [%s]",
                                deployModel.getWebAppName()));
                        WebApp app = AzureWebAppMvpModel.getInstance()
                                .createWebAppOnLinux(deployModel);

                        if (app != null && app.name() != null) {
                            processHandler.setText(String.format("URL:  http://%s.azurewebsites.net/", app.name()));
                            updateConfigurationDataModel(app);

                            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH,
                                    null));
                        }
                    } else {
                        // update WebApp
                        processHandler.setText(String.format("Updating WebApp ... [%s]",
                                deployModel.getWebAppName()));
                        WebApp app = AzureWebAppMvpModel.getInstance()
                                .updateWebAppOnLinux(deployModel.getSubscriptionId(), deployModel.getWebAppId(),
                                        acrInfo);
                        if (app != null && app.name() != null) {
                            processHandler.setText(String.format("URL:  http://%s.azurewebsites.net/", app.name()));
                        }
                    }
                    return null;
                }
        ).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
                (res) -> {
                    processHandler.setText("Updating cache ... ");
                    AzureWebAppMvpModel.getInstance().listAllWebAppsOnLinux(true);
                    processHandler.setText("Job done");
                    processHandler.notifyProcessTerminated(0);
                    sendTelemetry(true, null);
                },
                (err) -> {
                    err.printStackTrace();
                    processHandler.println(err.getMessage(), ProcessOutputTypes.STDERR);
                    processHandler.notifyProcessTerminated(0);
                    sendTelemetry(false, err.getMessage());
                }
        );
        return new DefaultExecutionResult(consoleView, processHandler);
    }

    // TODO: refactor later
    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("SubscriptionId", deployModel.getSubscriptionId());
        map.put("CreateNewApp", String.valueOf(deployModel.isCreatingNewWebAppOnLinux()));
        map.put("CreateNewSP", String.valueOf(deployModel.isCreatingNewAppServicePlan()));
        map.put("CreateNewRGP", String.valueOf(deployModel.isCreatingNewResourceGroup()));
        map.put("Success", String.valueOf(success));
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Webapp (Linux)", "Deploy", map);
    }

    private void updateConfigurationDataModel(WebApp app) {
        deployModel.setCreatingNewWebAppOnLinux(false);
        deployModel.setWebAppId(app.id());
        deployModel.setResourceGroupName(app.resourceGroupName());
    }
}
