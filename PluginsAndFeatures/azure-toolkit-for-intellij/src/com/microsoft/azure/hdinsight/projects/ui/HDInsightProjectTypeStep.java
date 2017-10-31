/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.projects.ui;

import com.intellij.ide.plugins.*;
import com.intellij.ide.projectWizard.ProjectTemplateList;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser;
import com.microsoft.azure.hdinsight.projects.HDInsightExternalSystem;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.azure.hdinsight.projects.HDInsightProjectTemplate;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public class HDInsightProjectTypeStep extends ModuleWizardStep implements Disposable {
    private HDInsightModuleBuilder moduleBuilder;

    private final boolean scalaPluginInstalled;

    private JPanel mainPanel;
    private ProjectTemplateList templateList;
    private JComboBox externalSystemsComboBox;
    private JLabel externalSystemsLabel;

    private static final String SCALA_PLUGIN_ID = "org.intellij.scala";

    public HDInsightProjectTypeStep(HDInsightModuleBuilder moduleBuilder) {
        this.moduleBuilder = moduleBuilder;

        this.scalaPluginInstalled = (null == PluginManager.getPlugin(PluginId.findId(SCALA_PLUGIN_ID))) ?
                false : true;

        this.templateList.setTemplates(moduleBuilder.getTemplates(), false);
        this.templateList.addListSelectionListener(e -> templateUpdated());
        checkScalaPlugin();

        this.externalSystemsComboBox.addItem(HDInsightExternalSystem.MAVEN);
        if (this.scalaPluginInstalled) {
            this.externalSystemsComboBox.addItem(HDInsightExternalSystem.SBT);
        }
        setExternalSystems();

        this.externalSystemsLabel.setText("Build tool:");
        this.externalSystemsLabel.setDisplayedMnemonic('u');
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
        switch (template.getTemplateType()) {
            case Java:
                return true;
            case Scala:
            case ScalaClusterSample:
                return this.scalaPluginInstalled;
            default:
                return false;
        }
    }

    private void templateUpdated() {
        setExternalSystems();
        checkScalaPlugin();
    }

    private void setExternalSystems() {
        this.externalSystemsComboBox.setSelectedItem(HDInsightExternalSystem.MAVEN);
        HDInsightProjectTemplate template = (HDInsightProjectTemplate) this.templateList.getSelectedTemplate();
        switch (template.getTemplateType()) {
            case Java:
                this.externalSystemsComboBox.setEnabled(false);
                break;
            default:
                this.externalSystemsComboBox.setEnabled(true);
        }
    }

    private void checkScalaPlugin() {
        HDInsightProjectTemplate template = (HDInsightProjectTemplate) this.templateList.getSelectedTemplate();
        switch (template.getTemplateType()) {
            case Scala:
            case ScalaClusterSample:
                if (!this.scalaPluginInstalled) {
                    showScalaPluginInstallDialog();
                }
                return;
            default:
                return;
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
            ApplicationManager.getApplication().invokeAndWait(() -> {
                PluginsAdvertiser.installAndEnablePlugins(pluginIds, new Runnable() {
                    @Override
                    public void run() {
                        PluginInstaller.addStateListener(new PluginStateListener() {
                            @Override
                            public void install(@NotNull IdeaPluginDescriptor descriptor) {
                                if (descriptor.getPluginId().toString().equals(SCALA_PLUGIN_ID)) {
                                    showRestartDialog();
                                }
                            }

                            @Override
                            public void uninstall(@NotNull IdeaPluginDescriptor descriptor) {
                            }
                        });
                    }
                });
            });
        }
    }

    private void showRestartDialog() {
        if (PluginManagerConfigurable.showRestartDialog() == Messages.YES) {
            ApplicationManagerEx.getApplicationEx().restart(true);
        }
    }
}
