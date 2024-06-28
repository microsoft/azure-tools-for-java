/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.task;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.task.BaseDeployTask;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeployWebAppTask extends BaseDeployTask {

    public DeployWebAppTask(@Nonnull ComponentContext context) {
        super(context);
    }

    protected RunnerAndConfigurationSettings getRunConfigurationSettings(@Nonnull ComponentContext context, RunManagerEx manager) {
        final String appId = (String) context.getParameter(CreateWebAppTask.WEBAPP_ID);
        final ConfigurationFactory factory = WebAppConfigurationType.getInstance().getWebAppConfigurationFactory();
        final String runConfigurationName = String.format("Azure Sample: %s-%s", guidance.getName(), Utils.getTimestamp());
        final RunnerAndConfigurationSettings settings = manager.createConfiguration(runConfigurationName, factory);
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof WebAppConfiguration) {
            ((WebAppConfiguration) runConfiguration).setWebApp(Objects.requireNonNull(Azure.az(AzureWebApp.class).webApp(appId)));
            final List<AzureArtifact> allSupportedAzureArtifacts = AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
            // todo: change to use artifact build by maven in last step if not exist
            if (allSupportedAzureArtifacts.size() < 1) {
                throw new AzureToolkitRuntimeException("No supported artifact found, please confirm if project is loaded correctly.");
            }
            final AzureArtifact azureArtifact = allSupportedAzureArtifacts.get(0);
            ((WebAppConfiguration) runConfiguration).setArtifact(azureArtifact);
            final List<BeforeRunTask> beforeRunTasks = new ArrayList<>();
            beforeRunTasks.add(BuildArtifactBeforeRunTaskUtils.createBuildTask(azureArtifact, runConfiguration, true));
            beforeRunTasks.addAll(runConfiguration.getBeforeRunTasks());
            manager.setBeforeRunTasks(runConfiguration, beforeRunTasks);
            ((WebAppConfiguration) runConfiguration).setOpenBrowserAfterDeployment(false);
        }
        return settings;
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.webapp.deploy";
    }
}
