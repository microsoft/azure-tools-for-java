/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class DeployWebAppAction extends AnAction {

    private static final WebAppConfigurationType configType = WebAppConfigurationType.getInstance();

    @Override
    @AzureOperation(name = "user/webapp.deploy_app")
    public void actionPerformed(@Nonnull AnActionEvent event) {
        AzureTaskManager.getInstance().runLater(() -> {
            OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
            final Module module = LangDataKeys.MODULE.getData(event.getDataContext());
            final Project project = Objects.requireNonNull(event.getProject());
            if (Objects.nonNull(module)) {
                AzureLoginHelper.requireSignedIn(module.getProject(), (a) -> deploy(module));
            } else {
                AzureLoginHelper.requireSignedIn(project, (a) -> deploy(project));
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public static void deploy(@Nonnull final WebApp webApp, @Nonnull final Project project) {
        final RunnerAndConfigurationSettings settings = getOrCreateRunConfigurationSettings(project, webApp, null);
        runConfiguration(project, settings);
    }

    public static void deploy(@Nonnull final Module module) {
        final RunnerAndConfigurationSettings settings = getOrCreateRunConfigurationSettings(module.getProject(), null, module);
        runConfiguration(module.getProject(), settings);
    }

    public static void deploy(@Nonnull final Project project) {
        final RunnerAndConfigurationSettings settings = getOrCreateRunConfigurationSettings(project, null, null);
        runConfiguration(project, settings);
    }

    @AzureOperation(name = "boundary/webapp.run_deploy_configuration")
    private static void runConfiguration(@Nonnull Project project, RunnerAndConfigurationSettings settings) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        AzureTaskManager.getInstance().runLater(() -> {
            if (RunDialog.editConfiguration(project, settings, message("webapp.deploy.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
                settings.storeInLocalWorkspace();
                manager.addConfiguration(settings);
                manager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }

    private static RunnerAndConfigurationSettings getOrCreateRunConfigurationSettings(@Nonnull Project project, @Nullable WebApp webApp, @Nullable Module module) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getWebAppConfigurationFactory();
        final String name = Optional.ofNullable(module).map(Module::getName)
            .or(() -> Optional.ofNullable(webApp).map(WebApp::getName))
            .map(n -> ":" + n)
            .orElse("");
        final String runConfigurationName = String.format("%s: %s%s", factory.getName(), project.getName(), name);
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(runConfigurationName);
        if (settings == null) {
            settings = manager.createConfiguration(runConfigurationName, factory);
        }
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof WebAppConfiguration configuration && Objects.nonNull(webApp)) {
            configuration.setWebApp(webApp);
            AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts()
                    .stream().filter(azureArtifact -> Objects.equals(azureArtifact.getModule(), module))
                    .findFirst()
                    .ifPresent(configuration::setArtifact);
        }
        return settings;
    }
}
