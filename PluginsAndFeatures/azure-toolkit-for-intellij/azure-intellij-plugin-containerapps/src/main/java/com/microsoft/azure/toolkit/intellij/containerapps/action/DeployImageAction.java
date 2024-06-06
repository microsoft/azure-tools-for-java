package com.microsoft.azure.toolkit.intellij.containerapps.action;

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
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerapps.AzureContainerAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageModel;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageRunConfiguration;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeploymentType;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class DeployImageAction extends AnAction {
    private static final AzureContainerAppConfigurationType configType = AzureContainerAppConfigurationType.getInstance();

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        deployImageToAzureContainerApps(null, null, event);
    }

    @AzureOperation(name = "boundary/containerapps.run_deploy_configuration")
    public static void deployImageToAzureContainerApps(@Nullable final VirtualFile file, @Nullable final ContainerApp app,
                                                       @Nonnull final AnActionEvent event) {
        AzureTaskManager.getInstance().runLater(() -> {
            OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
            final Module module = LangDataKeys.MODULE.getData(event.getDataContext());
            final Project project = Objects.requireNonNull(event.getProject());
            AzureLoginHelper.requireSignedIn(project, (a) -> {
                final RunnerAndConfigurationSettings settings = getOrCreateRunConfigurationSettings(event.getProject(), app, module, file);
                final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
                AzureTaskManager.getInstance().runLater(() -> {
                    if (RunDialog.editConfiguration(project, settings, message("containerapps.deploy.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
                        settings.storeInLocalWorkspace();
                        manager.addConfiguration(settings);
                        manager.setSelectedConfiguration(settings);
                        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
                    }
                });
            });
        });
    }

    private static RunnerAndConfigurationSettings getOrCreateRunConfigurationSettings(
            @Nonnull Project project, @Nullable ContainerApp app, @Nullable Module module, @Nullable final VirtualFile dockerFile) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getDeployImageRunConfigurationFactory();
        final String name = Optional.ofNullable(module).map(Module::getName)
                .or(() -> Optional.ofNullable(app).map(ContainerApp::getName))
                .map(n -> ":" + n)
                .orElse("");
        final String runConfigurationName = String.format("%s: %s%s", factory.getName(), project.getName(), name);
        final RunnerAndConfigurationSettings settings = Optional.ofNullable(manager.findConfigurationByName(runConfigurationName))
                .orElseGet(()-> createDefaultRunConfiguration(manager, factory, runConfigurationName, dockerFile));
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof DeployImageRunConfiguration configuration) {
            Optional.ofNullable(app).map(ContainerApp::getId).ifPresent(id -> configuration.getDataModel().setContainerAppId(id));
            Optional.ofNullable(module).map(Module::getName).ifPresent(configuration.getDataModel()::setModuleName);
        }
        return settings;
    }

    private static RunnerAndConfigurationSettings createDefaultRunConfiguration(RunManagerEx manager, ConfigurationFactory factory, String runConfigurationName, VirtualFile dockerFile) {
        final RunnerAndConfigurationSettings result = manager.createConfiguration(runConfigurationName, factory);
        final RunConfiguration runConfiguration = result.getConfiguration();
        if (runConfiguration instanceof DeployImageRunConfiguration configuration) {
            // by default, use code deployment, however, if user has specified dockerfile, use image deployment
            configuration.setDeploymentType(DeploymentType.Code);
            Optional.ofNullable(dockerFile).map(DockerImage::new).ifPresent(image -> {
                configuration.setDeploymentType(DeploymentType.Image);
                final DeployImageModel dataModel = Optional.ofNullable(configuration.getDataModel()).orElseGet(DeployImageModel::new);
                dataModel.setDockerImage(image);
                configuration.setDataModel(dataModel);
            });
        }
        return result;
    }
}
