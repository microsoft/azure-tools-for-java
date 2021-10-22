/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects.ui;

import com.intellij.ide.plugins.*;
import com.intellij.ide.projectWizard.ProjectTemplateList;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.projects.HDInsightExternalSystem;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.azure.hdinsight.projects.HDInsightProjectTemplate;
import com.microsoft.azure.hdinsight.projects.HDInsightTemplatesType;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.ui.AccessibleListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public class HDInsightProjectTypeStep extends ModuleWizardStep implements Disposable {
    private HDInsightModuleBuilder moduleBuilder;

    private final boolean scalaPluginInstalled;

    private JPanel mainPanel;
    private ProjectTemplateList templateList;
    private JComboBox<HDInsightExternalSystem> externalSystemsComboBox;
    private JLabel externalSystemsLabel;

    private static final String SCALA_PLUGIN_ID = "org.intellij.scala";

    public HDInsightProjectTypeStep(HDInsightModuleBuilder moduleBuilder) {
        this.moduleBuilder = moduleBuilder;
        this.scalaPluginInstalled = PluginManagerCore.getPlugin(PluginId.findId(SCALA_PLUGIN_ID)) != null;

        this.templateList.addListSelectionListener(e -> onTemplateSelected());
        this.templateList.setTemplates(moduleBuilder.getTemplates(), false);

        var templateListView = this.templateList.getList();
        templateListView.setCellRenderer(new AccessibleListCellRenderer<>() {
            @NotNull
            @Override
            public String getText(ProjectTemplate template) {
                return template.getName();
            }
        });

        checkScalaPlugin();

        this.externalSystemsLabel.setText("Build tool:");
        this.externalSystemsLabel.setDisplayedMnemonic('u');
        this.externalSystemsLabel.setLabelFor(externalSystemsComboBox);
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void updateDataModel() {
        this.moduleBuilder.setSelectedTemplate((HDInsightProjectTemplate) this.templateList.getSelectedTemplate());
        this.moduleBuilder.setSelectedExternalSystem((HDInsightExternalSystem) this.externalSystemsComboBox.getSelectedItem());
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return (this.isScalaPluginStatusValid()) &&
                super.validate();
    }

    @Override
    public void dispose() {
        moduleBuilder = null;
    }

    private boolean isScalaPluginStatusValid() {
        HDInsightProjectTemplate template = (HDInsightProjectTemplate) this.templateList.getSelectedTemplate();

        if (template == null) {
            return false;
        }

        switch (template.getTemplateType()) {
            case Java:
                return true;
            case Scala:
            case ScalaClusterSample:
            case ScalaFailureTaskDebugSample:
                return this.scalaPluginInstalled;
            default:
                return false;
        }
    }

    private void onTemplateSelected() {
        setExternalSystems();
        checkScalaPlugin();
    }

    private void setExternalSystems() {
        HDInsightProjectTemplate selectedTemplate = (HDInsightProjectTemplate) this.templateList.getSelectedTemplate();
        this.externalSystemsComboBox.removeAllItems();

        if (selectedTemplate == null) {
            return;
        }

        this.externalSystemsComboBox.addItem(HDInsightExternalSystem.MAVEN);
        if (this.scalaPluginInstalled && selectedTemplate.getTemplateType() != HDInsightTemplatesType.ScalaFailureTaskDebugSample) {
            this.externalSystemsComboBox.addItem(HDInsightExternalSystem.SBT);
        }

        this.externalSystemsComboBox.setSelectedItem(HDInsightExternalSystem.MAVEN);
        switch (selectedTemplate.getTemplateType()) {
            case Java:
                this.externalSystemsComboBox.setEnabled(false);
                break;
            default:
                this.externalSystemsComboBox.setEnabled(true);
        }
    }

    private void checkScalaPlugin() {
        HDInsightProjectTemplate template = (HDInsightProjectTemplate) this.templateList.getSelectedTemplate();

        if (template == null) {
            return;
        }

        switch (template.getTemplateType()) {
            case Scala:
            case ScalaClusterSample:
            case ScalaFailureTaskDebugSample:
                if (!this.scalaPluginInstalled) {
                    showScalaPluginInstallDialog();
                }
                return;
            default:
        }
    }

    private void showScalaPluginInstallDialog() {
        if (Messages.showYesNoDialog("No Scala plugin found. Do you want to install Scala plugin?",
                "Scala Plugin Check",
                "Install",
                "Postpone",
                Messages.getQuestionIcon()) == Messages.YES) {

            Set<String> pluginIds = new HashSet<>();
            pluginIds.add(SCALA_PLUGIN_ID);
            AzureTaskManager.getInstance().runAndWait(() -> {
                PluginsAdvertiser.installAndEnablePlugins(pluginIds, () -> PluginInstaller.addStateListener(
                    new PluginStateListener() {
                        @Override
                        public void install(@NotNull IdeaPluginDescriptor descriptor) {
                            if (descriptor.getPluginId().toString().equals(SCALA_PLUGIN_ID)) {
                                showRestartDialog();
                            }
                        }

                        @Override
                        public void uninstall(@NotNull IdeaPluginDescriptor descriptor) {
                        }
                    }));
            });
        }
    }

    private void showRestartDialog() {
        if (PluginManagerConfigurable.showRestartDialog() == Messages.YES) {
            ApplicationManagerEx.getApplicationEx().restart(true);
        }
    }
}
