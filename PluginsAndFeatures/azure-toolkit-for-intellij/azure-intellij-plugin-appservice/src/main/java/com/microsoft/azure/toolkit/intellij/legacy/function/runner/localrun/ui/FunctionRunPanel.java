/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTable;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.FunctionRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionCoreToolsCombobox;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionRunPanel extends AzureSettingPanel<FunctionRunConfiguration> {
    private static final int DEFAULT_FUNC_PORT = 7071;
    private JPanel settings;
    private JPanel pnlMain;
    private FunctionCoreToolsCombobox txtFunc;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;
    private JTextField txtPort;
    private JCheckBox chkAuto;
    private JLabel lblModule;
    private JLabel lblFunctionCli;
    private JLabel lblPort;
    private JLabel lblAppSettings;
    private FunctionAppSettingsTable appSettingsTable;
    private String appSettingsKey = UUID.randomUUID().toString();

    private final FunctionRunConfiguration functionRunConfiguration;
    private Module previousModule = null;

    public FunctionRunPanel(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration) {
        super(project);
        this.functionRunConfiguration = functionRunConfiguration;
        $$$setupUI$$$();
        cbFunctionModule.setRenderer(new ListCellRendererWrapper<>() {
            @Override
            public void customize(JList list, Module module, int i, boolean b, boolean b1) {
                if (module != null) {
                    setText(module.getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });
        chkAuto.addItemListener(e -> txtPort.setEnabled(!chkAuto.isSelected()));
        lblModule.setLabelFor(cbFunctionModule);
        lblFunctionCli.setLabelFor(txtFunc);
        lblPort.setLabelFor(txtPort);
        lblAppSettings.setLabelFor(appSettingsTable);
        fillModules();
    }

    @NotNull
    @Override
    public String getPanelName() {
        return message("function.run.title");
    }

    @Override
    public void disposeEditor() {
    }

    @Override
    protected void resetFromConfig(@NotNull FunctionRunConfiguration configuration) {
        if (MapUtils.isNotEmpty(configuration.getAppSettings())) {
            appSettingsTable.setAppSettings(configuration.getAppSettings());
        }
        if (StringUtils.isNotEmpty(configuration.getAppSettingsKey())) {
            this.appSettingsKey = configuration.getAppSettingsKey();
            appSettingsTable.setAppSettings(FunctionUtils.loadAppSettingsFromSecurityStorage(appSettingsKey));
        }
        // In case `FUNCTIONS_WORKER_RUNTIME` or `AZURE_WEB_JOB_STORAGE_KEY` was missed in configuration
        appSettingsTable.loadRequiredSettings();
        if (StringUtils.isNotEmpty(configuration.getFuncPath())) {
            txtFunc.setValue(configuration.getFuncPath());
        }
        this.previousModule = configuration.getModule();
        selectModule(previousModule);

        final int port = functionRunConfiguration.getFuncPort() <= 0 ? FunctionUtils.findFreePort(DEFAULT_FUNC_PORT) : functionRunConfiguration.getFuncPort();
        txtPort.setText(String.valueOf(port));
        chkAuto.setSelected(configuration.isAutoPort());
    }

    @Override
    protected void apply(@NotNull FunctionRunConfiguration configuration) {
        configuration.setFuncPath(txtFunc.getItem());
        Optional.ofNullable((Module) cbFunctionModule.getSelectedItem()).ifPresent(configuration::saveModule);
        FunctionUtils.saveAppSettingsToSecurityStorage(appSettingsKey, appSettingsTable.getAppSettings());
        // save app settings storage key instead of real value
        configuration.setAppSettings(Collections.emptyMap());
        configuration.setAppSettingsKey(appSettingsKey);
        configuration.setAutoPort(chkAuto.isSelected());
        try {
            configuration.setFuncPort(Integer.parseInt(txtPort.getText()));
        } catch (final NumberFormatException e) {
            configuration.setFuncPort(-1);
        }
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlMain;
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

    private void createUIComponents() {
        txtFunc = new FunctionCoreToolsCombobox(project, true);
        final String localSettingPath = Paths.get(Objects.requireNonNull(project.getBasePath()), "local.settings.json").toString();
        appSettingsTable = new FunctionAppSettingsTable(localSettingPath);
        pnlAppSettings = FunctionAppSettingsTableUtils.createAppSettingPanel(appSettingsTable);
        appSettingsTable.loadLocalSetting();
    }

    private void fillModules() {
        AzureTaskManager.getInstance()
                .runOnPooledThreadAsObservable(new AzureTask<>(() -> FunctionUtils.listFunctionModules(project)))
                .subscribe(modules -> AzureTaskManager.getInstance().runLater(() -> {
                    Arrays.stream(modules).forEach(cbFunctionModule::addItem);
                    selectModule(previousModule);
                }, AzureTask.Modality.ANY));
    }

    // todo: @hanli migrate to use AzureComboBox<Module>
    private void selectModule(final Module target) {
        if (target == null) {
            return;
        }
        for (int i = 0; i < cbFunctionModule.getItemCount(); i++) {
            final Module module = cbFunctionModule.getItemAt(i);
            if (Paths.get(module.getModuleFilePath()).equals(Paths.get(target.getModuleFilePath()))) {
                cbFunctionModule.setSelectedIndex(i);
                break;
            }
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
