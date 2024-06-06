
/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.task.BaseDeployTask;
import com.microsoft.azure.toolkit.intellij.containerapps.AzureContainerAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.containerapps.component.DeploymentSourceForm;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageModel;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageRunConfiguration;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeploymentType;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class DeployContainerAppTask extends BaseDeployTask {
    public static final String DEPLOY_MODULE = "deployModule";
    public static final String TARGET_PORT = "targetPort";

    public DeployContainerAppTask(@Nonnull ComponentContext context) {
        super(context);
    }

    @Override
    protected RunnerAndConfigurationSettings getRunConfigurationSettings(@Nonnull ComponentContext context, RunManagerEx manager) {
        final ContainerApp app = Objects.requireNonNull((ContainerApp) context.getParameter(CreateContainerAppTask.CONTAINER_APP), "'containerApp' is required.");
        final ContainerRegistry registry = (ContainerRegistry) context.getParameter(CreateContainerRegistryTask.CONTAINER_REGISTRY);
        final String moduleName = (String) context.getParameter(DEPLOY_MODULE);
        final Module module = Arrays.stream(ModuleManager.getInstance(project).getModules()).filter(m -> m.getName().equals(moduleName)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Module '%s' not found.", moduleName)));
        final VirtualFile modulePath = ProjectUtil.guessModuleDir(module);
        final ConfigurationFactory factory = AzureContainerAppConfigurationType.getInstance().getConfigurationFactories()[0];
        final String runConfigurationName = String.format("Azure Sample: %s-%s", guidance.getName(), Utils.getTimestamp());
        final RunnerAndConfigurationSettings settings = manager.createConfiguration(runConfigurationName, factory);
        if (settings.getConfiguration() instanceof DeployImageRunConfiguration runConfiguration) {
            final DeployImageModel model = DeployImageModel.builder()
                .deploymentType(DeploymentType.Code)
                .moduleName(moduleName)
                .containerAppId(app.getId())
                .ingressConfig(IngressConfig.builder()
                    .enableIngress(true)
                    .external(true)
                    .targetPort(Optional.ofNullable((Integer) context.getParameter(TARGET_PORT)).orElse(80))
                    .build())
                .build();

            final ContainerAppDraft.ImageConfig imageConfig = new ContainerAppDraft.ImageConfig(DeploymentSourceForm.generateDefaultFullImageNameFromApp(app));
            final ContainerAppDraft.BuildImageConfig buildConfig = new ContainerAppDraft.BuildImageConfig();
            buildConfig.setSource(modulePath.toNioPath());
            imageConfig.setBuildImageConfig(buildConfig);
            imageConfig.setContainerRegistry(registry);
            model.setImageConfig(imageConfig);
            runConfiguration.setDataModel(model);
        }
        return settings;
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.containerapp.deploy";
    }
}
