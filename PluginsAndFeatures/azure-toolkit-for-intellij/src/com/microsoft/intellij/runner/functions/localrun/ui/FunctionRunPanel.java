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

package com.microsoft.intellij.runner.functions.localrun.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTable;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTableUtils;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.runner.functions.localrun.FunctionRunConfiguration;
import com.microsoft.intellij.ui.util.UIUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class FunctionRunPanel extends AzureSettingPanel<FunctionRunConfiguration> {

    private JPanel settings;
    private JPanel pnlMain;
    private TextFieldWithBrowseButton txtFunc;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;
    private AppSettingsTable appSettingsTable;

    private FunctionRunConfiguration functionRunConfiguration;

    public FunctionRunPanel(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration) {
        super(project);
        this.functionRunConfiguration = functionRunConfiguration;

        cbFunctionModule.setRenderer(new ListCellRendererWrapper<Module>() {
            @Override
            public void customize(JList list, Module module, int i, boolean b, boolean b1) {
                if (module != null) {
                    setText(module.getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });

        txtFunc.addActionListener(
                UIUtils.createFileChooserListenerWithTextPath(txtFunc, project,
                        FileChooserDescriptorFactory.createSingleFileDescriptor()));

        try {
            txtFunc.setText(FunctionUtils.getFuncPath());
        } catch (IOException | InterruptedException e) {
            // swallow as leave blank
        }
        fillModules();
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Run Function";
    }

    @Override
    public void disposeEditor() {
    }

    @Override
    protected void resetFromConfig(@NotNull FunctionRunConfiguration configuration) {
        if (MapUtils.isNotEmpty(configuration.getAppSettings())) {
            appSettingsTable.setAppSettings(configuration.getAppSettings());
        }
        // In case `FUNCTIONS_WORKER_RUNTIME` or `AZURE_WEB_JOB_STORAGE_KEY` was missed in configuration
        appSettingsTable.loadRequiredSettings();
        if (StringUtils.isNotEmpty(configuration.getFuncPath())) {
            txtFunc.setText(configuration.getFuncPath());
        }
        for (int i = 0; i < cbFunctionModule.getItemCount(); i++) {
            final Module module = cbFunctionModule.getItemAt(i);
            if (StringUtils.equals(configuration.getModuleName(), module.getName())) {
                cbFunctionModule.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    protected void apply(@NotNull FunctionRunConfiguration configuration) {
        configuration.setFuncPath(txtFunc.getText());
        configuration.setAppSettings(appSettingsTable.getAppSettings());
        configuration.saveModule((Module) cbFunctionModule.getSelectedItem());
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlMain;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return new JComboBox<Artifact>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new JComboBox<MavenProject>();
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    private void createUIComponents() {
        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new AppSettingsTable(localSettingPath);
        pnlAppSettings = AppSettingsTableUtils.createAppSettingPanel(appSettingsTable);
        appSettingsTable.loadLocalSetting();
    }

    private void fillModules() {
        Arrays.stream(FunctionUtils.listFunctionModules(project)).forEach(module -> cbFunctionModule.addItem(module));
    }
}
