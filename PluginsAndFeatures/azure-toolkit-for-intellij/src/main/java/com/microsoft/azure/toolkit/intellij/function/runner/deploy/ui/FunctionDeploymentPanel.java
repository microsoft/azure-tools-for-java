/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBoxModel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBox;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.azure.toolkit.intellij.function.runner.component.table.AppSettingsTable;
import com.microsoft.azure.toolkit.intellij.function.runner.component.table.AppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeployConfiguration;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.intellij.CommonConst.EMPTY_TEXT;
import static com.microsoft.intellij.CommonConst.LOADING_TEXT;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class FunctionDeploymentPanel extends AzureSettingPanel<FunctionDeployConfiguration> implements FunctionDeployMvpView {

    private final FunctionDeployViewPresenter<FunctionDeploymentPanel> presenter;

    private JPanel pnlRoot;
    private HyperlinkLabel lblCreateFunctionApp;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;
    private FunctionAppComboBox functionAppComboBox;
    private AppSettingsTable appSettingsTable;
    private FunctionAppComboBoxModel appSettingsFunctionApp;
    private String appSettingsKey = UUID.randomUUID().toString();


    public FunctionDeploymentPanel(@NotNull Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.presenter = new FunctionDeployViewPresenter<>();
        this.presenter.onAttachView(this);

        cbFunctionModule.setRenderer(new ListCellRendererWrapper<>() {
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
        return message("function.deploy.title");
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
        return new ComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new ComboBox<>();
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
        if (StringUtils.isNotEmpty(configuration.getAppSettingsKey())) {
            this.appSettingsKey = configuration.getAppSettingsKey();
            appSettingsTable.setAppSettings(FunctionUtils.loadAppSettingsFromSecurityStorage(appSettingsKey));
        }
        if (!StringUtils.isAllEmpty(configuration.getFunctionId(), configuration.getAppName())) {
            final FunctionAppComboBoxModel configModel = new FunctionAppComboBoxModel(configuration.getConfig());
            appSettingsFunctionApp = configModel;
            functionAppComboBox.setConfigModel(configModel);
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
        FunctionUtils.saveAppSettingsToSecurityStorage(appSettingsKey, appSettingsTable.getAppSettings());
        // save app settings storage key instead of real value
        configuration.setAppSettingsKey(appSettingsKey);
        final FunctionAppComboBoxModel functionModel = functionAppComboBox.getValue();
        if (functionModel != null) {
            configuration.saveConfig(functionModel.getConfig());
        }
    }

    private void createUIComponents() {
        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new AppSettingsTable(localSettingPath);
        pnlAppSettings = AppSettingsTableUtils.createAppSettingPanel(appSettingsTable);

        functionAppComboBox = new FunctionAppComboBox(project);
        functionAppComboBox.addActionListener(event -> onSelectFunctionApp());
        functionAppComboBox.refreshItems();
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
                this.beforeFillAppSettings();
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
