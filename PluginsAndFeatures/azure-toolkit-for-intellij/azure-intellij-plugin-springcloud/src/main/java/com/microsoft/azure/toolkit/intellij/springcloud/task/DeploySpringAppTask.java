/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.task;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.task.BaseDeployTask;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.springcloud.deplolyment.SpringCloudDeploymentConfiguration;
import com.microsoft.azure.toolkit.intellij.springcloud.deplolyment.SpringCloudDeploymentConfigurationType;
import com.microsoft.azure.toolkit.intellij.springcloud.deplolyment.WrappedAzureArtifact;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeploySpringAppTask extends BaseDeployTask {
    public static final String DEPLOY_MODULE = "deployModule";
    public static final String PACKAGE_MODULE = "packageModule";

    public DeploySpringAppTask(@Nonnull ComponentContext context) {
        super(context);
    }

    @Override
    protected RunnerAndConfigurationSettings getRunConfigurationSettings(@Nonnull ComponentContext context, RunManagerEx manager) {
        final SpringCloudApp app = Objects.requireNonNull((SpringCloudApp) context.getParameter(CreateSpringAppTask.SPRING_APP), "'springApp' is required to deploy spring cloud app.");
        final ConfigurationFactory factory = SpringCloudDeploymentConfigurationType.getInstance().getConfigurationFactories()[0];
        final String runConfigurationName = String.format("Azure Sample: %s-%s", guidance.getName(), Utils.getTimestamp());
        final RunnerAndConfigurationSettings settings = manager.createConfiguration(runConfigurationName, factory);
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof SpringCloudDeploymentConfiguration) {
            ((SpringCloudDeploymentConfiguration) runConfiguration).setApp(app);
            ((SpringCloudDeploymentConfiguration) runConfiguration).setAppConfig(getAppConfig(app));
            final List<BeforeRunTask> beforeRunTasks = getBeforeRunTasks(runConfiguration);
            beforeRunTasks.addAll(runConfiguration.getBeforeRunTasks());
            manager.setBeforeRunTasks(runConfiguration, beforeRunTasks);
        }
        return settings;
    }

    private SpringCloudAppConfig getAppConfig(@Nonnull final SpringCloudApp app) {
        final String packageModule = Objects.requireNonNull((String) context.getParameter(DEPLOY_MODULE),
                "'deployModule' is required to deploy spring cloud app.");
        final SpringCloudAppConfig config = app.isDraftForCreating() ?
                ((SpringCloudAppDraft) app).getConfig() : SpringCloudAppConfig.fromApp(app);
        final SpringCloudDeploymentConfig deploymentConfig = config.getDeployment();
        final AzureArtifact artifact = getArtifactById(packageModule);
        deploymentConfig.setArtifact(new WrappedAzureArtifact(artifact, this.project));
        return config;
    }

    private List<BeforeRunTask> getBeforeRunTasks(final RunConfiguration runConfiguration) {
        final String packageModule = Objects.requireNonNull((String) context.getParameter(PACKAGE_MODULE),
                "'packageModule' is required to deploy spring cloud app.");
        final List<BeforeRunTask> beforeRunTasks = new ArrayList<>();
        final List<AzureArtifact> allSupportedAzureArtifacts = AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
        final AzureArtifact azureArtifact = getArtifactById(packageModule);
        beforeRunTasks.add(BuildArtifactBeforeRunTaskUtils.createBuildTask(azureArtifact, runConfiguration));
        return beforeRunTasks;
    }

    @Nonnull
    private AzureArtifact getArtifactById(final String artifactId) {
        final AzureArtifactManager artifactManager = AzureArtifactManager.getInstance(project);
        return artifactManager.getAllSupportedAzureArtifacts()
                .stream()
                .filter(artifact -> StringUtils.equalsIgnoreCase(artifactId, artifactManager.getArtifactId(artifact)))
                .findFirst().orElseThrow(() -> new AzureToolkitRuntimeException(String.format("Cannot find artifact by id: %s", artifactId)));
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.springcloud.deploy";
    }
}
