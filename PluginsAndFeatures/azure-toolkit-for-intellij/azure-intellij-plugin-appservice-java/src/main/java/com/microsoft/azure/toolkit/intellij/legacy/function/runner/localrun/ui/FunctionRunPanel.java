/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.ui;

import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.function.components.ModuleFileComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTable;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.FunctionRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionCoreToolsCombobox;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionRunPanel extends AzureSettingPanel<FunctionRunConfiguration> {
    private static final int DEFAULT_FUNC_PORT = 7071;
    private JPanel settings;
    private JPanel pnlMain;
    private FunctionCoreToolsCombobox txtFunc;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;
    private JLabel lblModule;
    private JLabel lblFunctionCli;
    private JLabel lblAppSettings;
    private ModuleFileComboBox cbHostJson;
    private AzureTextInput txtFuncArguments;
    private FunctionAppSettingsTable appSettingsTable;
    private String appSettingsKey = UUID.randomUUID().toString();

    private final FunctionRunConfiguration functionRunConfiguration;
    private Module previousModule = null;

    public FunctionRunPanel(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration) {
        super(project, false);
        this.functionRunConfiguration = functionRunConfiguration;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        cbFunctionModule.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList list, Module module, int i, boolean b, boolean b1) {
                if (module != null) {
                    setText(module.getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });
        cbFunctionModule.addItemListener(this::onSelectModule);
        lblModule.setLabelFor(cbFunctionModule);
        lblFunctionCli.setLabelFor(txtFunc);
        lblAppSettings.setLabelFor(appSettingsTable);
        this.txtFuncArguments.setValue(FunctionUtils.getDefaultFuncArguments());
        fillModules();
    }

    private void onSelectModule(ItemEvent itemEvent) {
        final Object module = cbFunctionModule.getSelectedItem();
        if (module instanceof Module) {
            // sync connector tasks
            cbHostJson.setModule((Module) module);
            functionRunConfiguration.saveModule((Module) module);
            final DataContext context = DataManager.getInstance().getDataContext(pnlMain);
            final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context);
            if (Objects.isNull(editor)) {
                return;
            }
            BuildArtifactBeforeRunTaskUtils.updateConnectorBeforeRunTask(this.functionRunConfiguration, editor);
        } else {
            cbHostJson.setModule(null);
        }
    }

    @NotNull
    @Override
    public String getPanelName() {
        return message("function.run.title");
    }

    @Override
    public void disposeEditor() {
        Optional.ofNullable(txtFunc).ifPresent(AzureFormInputComponent::dispose);
    }

    @Override
    protected void resetFromConfig(@NotNull FunctionRunConfiguration configuration) {
        if (MapUtils.isNotEmpty(configuration.getAppSettings())) {
            appSettingsTable.setAppSettings(configuration.getAppSettings());
        }
        if (StringUtils.isNotEmpty(configuration.getAppSettingsKey())) {
            this.appSettingsKey = configuration.getAppSettingsKey();
            final Map<String, String> appSettings = FunctionUtils.loadAppSettingsFromSecurityStorage(appSettingsKey);
            if (MapUtils.isNotEmpty(appSettings)) {
                appSettingsTable.setAppSettings(appSettings);
            } else {
                final String localSettingsPath = configuration.getDefaultLocalSettingsJsonPath(configuration.getModule());

                if (StringUtils.isNotBlank(localSettingsPath) && FileUtil.exists(localSettingsPath)) {
                    appSettingsTable.setLocalSettingPath(localSettingsPath);
                    appSettingsTable.loadLocalSetting();
                }
            }
        }
        // In case `FUNCTIONS_WORKER_RUNTIME` or `AZURE_WEB_JOB_STORAGE_KEY` was missed in configuration
        appSettingsTable.loadRequiredSettings();
        if (StringUtils.isNotEmpty(configuration.getFuncPath())) {
            txtFunc.setValue(configuration.getFuncPath());
        }
        if (StringUtils.isNotEmpty(configuration.getFunctionHostArguments())) {
            txtFuncArguments.setValue(configuration.getFunctionHostArguments());
        }
        if (StringUtils.isNotEmpty(configuration.getHostJsonPath())) {
            cbHostJson.setValue(LocalFileSystem.getInstance().findFileByIoFile(new File(configuration.getHostJsonPath())));
        }
        this.previousModule = configuration.getModule();
        selectModule(previousModule);
    }

    @Override
    protected void apply(@NotNull FunctionRunConfiguration configuration) {
        configuration.setFuncPath(txtFunc.getItem());
        Optional.ofNullable((Module) cbFunctionModule.getSelectedItem()).ifPresent(module -> {
            configuration.saveModule(module);
            configuration.setLocalSettingsJsonPath(FunctionUtils.getDefaultLocalSettingsJsonPath(module));
        });
        FunctionUtils.saveAppSettingsToSecurityStorage(appSettingsKey, appSettingsTable.getAppSettings());
        // save app settings storage key instead of real value
        configuration.setAppSettings(Collections.emptyMap());
        configuration.setAppSettingsKey(appSettingsKey);
        configuration.setHostJsonPath(Optional.ofNullable(cbHostJson.getValue()).map(VirtualFile::getCanonicalPath).orElse(null));
        configuration.setFunctionHostArguments(txtFuncArguments.getValue());
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
        appSettingsTable.setProject(project);
        pnlAppSettings = FunctionAppSettingsTableUtils.createAppSettingPanel(appSettingsTable);
        // appSettingsTable.loadLocalSetting();

        cbHostJson = new ModuleFileComboBox(project, "host.json");
        cbHostJson.setRequired(true);
    }

    private void fillModules() {
        AzureTaskManager.getInstance()
                .runOnPooledThread(new AzureTask<>(() -> FunctionUtils.listFunctionModules(project)))
                .thenAccept(modules -> AzureTaskManager.getInstance().runLater(() -> {
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
            //noinspection UnstableApiUsage
            if (Paths.get(module.getModuleFilePath()).equals(Paths.get(target.getModuleFilePath()))) {
                cbFunctionModule.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    protected boolean shouldInitializeBeforeRunTasks() {
        return false;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
