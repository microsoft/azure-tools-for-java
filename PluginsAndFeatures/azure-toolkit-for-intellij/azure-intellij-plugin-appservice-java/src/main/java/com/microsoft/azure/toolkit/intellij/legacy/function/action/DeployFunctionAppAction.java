/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.action;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.AzureFunctionSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeploymentConfigurationFactory;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class DeployFunctionAppAction extends AnAction {

    private static final AzureFunctionSupportConfigurationType functionType = AzureFunctionSupportConfigurationType.getInstance();

    @Override
    @AzureOperation(name = "user/function.deploy_app")
    public void actionPerformed(@Nonnull AnActionEvent event) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
        AzureTaskManager.getInstance().runLater(() -> {
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
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(FunctionUtils.isFunctionProject(event.getProject()));
    }

    public static void deploy(@Nonnull final FunctionApp app, @Nonnull final Project project) {
        final RunnerAndConfigurationSettings settings = getOrCreateRunConfigurationSettings(project, app, null);
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

    @AzureOperation(name = "boundary/function.run_deploy_configuration")
    private static void runConfiguration(@Nonnull Project project, RunnerAndConfigurationSettings settings) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        AzureTaskManager.getInstance().runLater(() -> {
            if (RunDialog.editConfiguration(project, settings, message("function.deploy.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
                settings.storeInLocalWorkspace();
                manager.addConfiguration(settings);
                manager.setBeforeRunTasks(settings.getConfiguration(), new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration())));
                manager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }

    private static RunnerAndConfigurationSettings getOrCreateRunConfigurationSettings(@Nonnull Project project, @Nullable FunctionApp app, @Nullable Module module) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = new FunctionDeploymentConfigurationFactory(functionType);
        final String name = Optional.ofNullable(module).map(Module::getName)
            .or(() -> Optional.ofNullable(app).map(FunctionApp::getName))
            .map(n -> ":" + n)
            .orElse("");
        final String runConfigurationName = String.format("%s: %s%s", factory.getName(), project.getName(), name);
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(runConfigurationName);
        if (settings == null) {
            settings = manager.createConfiguration(runConfigurationName, factory);
        }
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof FunctionDeployConfiguration) {
            if (Objects.nonNull(app) && app.getFormalStatus().isConnected()) {
                final FunctionAppConfig config = FunctionAppService.getInstance().getFunctionAppConfigFromExistingFunction(app);
                ((FunctionDeployConfiguration) runConfiguration).saveConfig(config);
            }
            if (Objects.nonNull(module)) {
                ((FunctionDeployConfiguration) runConfiguration).saveTargetModule(module);
            }
        }
        return settings;
    }
}
