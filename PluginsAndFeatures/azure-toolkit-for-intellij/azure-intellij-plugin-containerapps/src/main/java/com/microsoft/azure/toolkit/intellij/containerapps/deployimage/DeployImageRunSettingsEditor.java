/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.intellij.execution.impl.CheckableRunConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.ui.DockerDeploymentSettingPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public class DeployImageRunSettingsEditor extends SettingsEditor<DeployImageRunConfiguration> implements CheckableRunConfigurationEditor<DeployImageRunConfiguration> {
    private final DockerDeploymentSettingPanel panel;
    private final DeployImageRunConfiguration configuration;

    public DeployImageRunSettingsEditor(Project project, DeployImageRunConfiguration configuration) {
        super();
        this.panel = new DockerDeploymentSettingPanel(project, configuration);
        this.configuration = configuration;
    }

    @Override
    public void checkEditorData(DeployImageRunConfiguration s) {

    }

    @Override
    protected void resetEditorFrom(@NotNull DeployImageRunConfiguration config) {
        this.panel.setValue(config.getDataModel());
        AzureTaskManager.getInstance().runOnPooledThread(() ->
                AzureTaskManager.getInstance().runLater(() -> this.panel.setValue(config.getDataModel()), AzureTask.Modality.ANY));
    }

    @Override
    protected void applyEditorTo(@NotNull DeployImageRunConfiguration s) throws ConfigurationException {
        final List<AzureValidationInfo> infos = this.panel.getAllValidationInfos(true);
        final AzureValidationInfo error = infos.stream()
                .filter(i -> !i.isValid())
                .findAny().orElse(null);
        if (Objects.nonNull(error)) {
            final String message = error.getType() == AzureValidationInfo.Type.PENDING ? "Please try later after validation" : error.getMessage();
            throw new ConfigurationException(message);
        }
        s.setDataModel(this.panel.getValue());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return this.panel.getPnlRoot();
    }
}
