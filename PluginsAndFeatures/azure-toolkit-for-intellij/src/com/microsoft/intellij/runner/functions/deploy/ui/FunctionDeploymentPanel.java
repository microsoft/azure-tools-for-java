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

package com.microsoft.intellij.runner.functions.deploy.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBoxModel;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBox;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTable;
import com.microsoft.intellij.runner.functions.component.table.AppSettingsTableUtils;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployConfiguration;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.microsoft.intellij.common.CommonConst.EMPTY_TEXT;
import static com.microsoft.intellij.common.CommonConst.LOADING_TEXT;


public class FunctionDeploymentPanel extends AzureSettingPanel<FunctionDeployConfiguration> implements FunctionDeployMvpView {

    private static final String CREATE_NEW_FUNCTION_APP = "Create Function App...";
    private static final String REFRESHING_FUNCTION_APP = "Refreshing...";
    private static final String CREATE_NEW_FUNCTION = "No available function, click to create a new one";

    private FunctionDeployViewPresenter presenter = null;

    private JPanel pnlRoot;
    private HyperlinkLabel lblCreateFunctionApp;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;
    private FunctionAppComboBox functionAppComboBox;
    private AppSettingsTable appSettingsTable;
    private FunctionAppComboBoxModel appSettingsFunctionApp;


    public FunctionDeploymentPanel(@NotNull Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.presenter = new FunctionDeployViewPresenter();
        this.presenter.onAttachView(this);

        cbFunctionModule.setRenderer(new ListCellRendererWrapper<Module>() {
            @Override
            public void customize(JList list, Module module, int i, boolean b, boolean b1) {
                if (module != null) {
                    setText(module.getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });

        fillModules();
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Deploy Azure Functions";
    }

    @Override
    public void disposeEditor() {
        presenter.onDetachView();
    }

    @Override
    public void beforeFillAppSettings() {
        appSettingsTable.getEmptyText().setText(LOADING_TEXT);
        appSettingsTable.clear();
    }

    @Override
    public void fillAppSettings(Map<String, String> appSettings) {
        appSettingsTable.getEmptyText().setText(EMPTY_TEXT);
        appSettingsTable.setAppSettings(appSettings);
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
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

    @Override
    protected void resetFromConfig(@NotNull FunctionDeployConfiguration configuration) {
        if (MapUtils.isNotEmpty(configuration.getAppSettings())) {
            appSettingsTable.setAppSettings(configuration.getAppSettings());
        }
        if (StringUtils.isAllEmpty(configuration.getFunctionId(), configuration.getAppName())) {
            functionAppComboBox.refreshItems();
        } else {
            final FunctionAppComboBoxModel functionAppComboBoxModel =
                    new FunctionAppComboBoxModel(configuration.getModel());
            appSettingsFunctionApp = functionAppComboBoxModel;
            functionAppComboBox.refreshItemsWithDefaultValue(functionAppComboBoxModel);
        }
        final Module previousModule = configuration.getModule();
        if (previousModule != null) {
            for (int i = 0; i < cbFunctionModule.getItemCount(); i++) {
                final Module module = cbFunctionModule.getItemAt(i);
                if (Paths.get(module.getModuleFilePath()).equals(Paths.get(previousModule.getModuleFilePath()))) {
                    cbFunctionModule.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void apply(@NotNull FunctionDeployConfiguration configuration) {
        configuration.saveTargetModule((Module) cbFunctionModule.getSelectedItem());
        configuration.setAppSettings(appSettingsTable.getAppSettings());
        final FunctionAppComboBoxModel functionModel = functionAppComboBox.getValue();
        if (functionModel != null) {
            configuration.saveModel(functionModel);
        }
    }

    private void createUIComponents() {
        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new AppSettingsTable(localSettingPath);
        pnlAppSettings = AppSettingsTableUtils.createAppSettingPanel(appSettingsTable);

        functionAppComboBox = new FunctionAppComboBox(project);
        functionAppComboBox.addActionListener(event -> onSelectFunctionApp());
    }

    private void onSelectFunctionApp() {
        final FunctionAppComboBoxModel model = getSelectedFunctionApp();
        if (model == null) {
            return;
        } else if (model.getResource() == null) { // For new create function or template model from configuration
            if (model.isNewCreateResource() && appSettingsFunctionApp != null && !appSettingsFunctionApp.isNewCreateResource()) {
                // Clear app settings table when user first choose create new function app
                this.fillAppSettings(Collections.EMPTY_MAP);
            }
        } else { // For existing Functions
            if (!AppServiceComboBoxModel.isSameApp(model, appSettingsFunctionApp) || appSettingsTable.isEmpty()) {
                // Do not refresh if selected function app is not changed except create run configuration from azure explorer
                presenter.loadAppSettings(model.getResource());
            }
        }
        appSettingsFunctionApp = model;
    }

    private FunctionAppComboBoxModel getSelectedFunctionApp() {
        return functionAppComboBox.getValue();
    }

    private void fillModules() {
        Arrays.stream(FunctionUtils.listFunctionModules(project)).forEach(module -> cbFunctionModule.addItem(module));
    }
}
