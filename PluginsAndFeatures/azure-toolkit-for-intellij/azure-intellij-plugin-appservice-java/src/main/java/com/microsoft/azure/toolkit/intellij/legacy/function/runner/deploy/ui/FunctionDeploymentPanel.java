/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui;

import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.function.components.ModuleFileComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTable;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeployModel;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components.DeploymentSlotComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.config.DeploymentSlotConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;


public class FunctionDeploymentPanel extends AzureSettingPanel<FunctionDeployConfiguration> implements AzureFormPanel<FunctionDeployModel> {

    private JPanel pnlRoot;
    private HyperlinkLabel lblCreateFunctionApp;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;
    private FunctionAppComboBox functionAppComboBox;
    private JLabel lblModule;
    private JLabel lblFunction;
    private JLabel lblAppSettings;
    private JCheckBox chkSlot;
    private DeploymentSlotComboBox cbDeploymentSlot;
    private ModuleFileComboBox cbHostJson;
    private FunctionAppSettingsTable appSettingsTable;
    private String appSettingsKey;
    private String appSettingsResourceId;
    private Module previousModule = null;
    private final FunctionDeployConfiguration configuration;
    private final Project project;

    public FunctionDeploymentPanel(@Nonnull Project project, @Nonnull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.project = project;
        this.configuration = functionDeployConfiguration;
        this.appSettingsKey = StringUtils.firstNonBlank(functionDeployConfiguration.getAppSettingsKey(), UUID.randomUUID().toString());
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
        functionAppComboBox.setRequired(true);
        chkSlot.addItemListener(e -> onSlotCheckBoxChanged());

        lblModule.setLabelFor(cbFunctionModule);
        lblFunction.setLabelFor(functionAppComboBox);
        lblAppSettings.setLabelFor(appSettingsTable);
        final JLabel lblDeploymentSlot = new JLabel("Deployment Slot:");
        lblDeploymentSlot.setLabelFor(cbDeploymentSlot);
        fillModules();
    }

    private void onSelectModule(ItemEvent itemEvent) {
        final Object module = cbFunctionModule.getSelectedItem();
        if (module instanceof Module) {
            cbHostJson.setModule((Module) module);
            configuration.saveTargetModule((Module) module);
            // sync connector tasks
            final DataContext context = DataManager.getInstance().getDataContext(pnlRoot);
            final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context);
            if (Objects.nonNull(editor)) {
                BuildArtifactBeforeRunTaskUtils.updateConnectorBeforeRunTask(this.configuration, editor);
            }
        } else {
            cbHostJson.setModule(null);
        }
    }

    @Nonnull
    @Override
    public String getPanelName() {
        return message("function.deploy.title");
    }

    @Nonnull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @Override
    protected void resetFromConfig(@Nonnull FunctionDeployConfiguration configuration) {
        setValue(configuration.getModel());
    }

    @Override
    protected void apply(@Nonnull FunctionDeployConfiguration configuration) {
        Optional.ofNullable(getValue()).ifPresent(configuration::saveModel);
    }

    private void createUIComponents() {
        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new FunctionAppSettingsTable(localSettingPath);
        appSettingsTable.setProject(project);
        pnlAppSettings = FunctionAppSettingsTableUtils.createAppSettingPanel(appSettingsTable);

        functionAppComboBox = new FunctionAppComboBox(project);
        functionAppComboBox.addValueChangedListener(this::onSelectFunctionApp);
        functionAppComboBox.reloadItems();

        cbDeploymentSlot = new DeploymentSlotComboBox(project);
        cbDeploymentSlot.addValueChangedListener(this::onSelectFunctionSlot);
        cbDeploymentSlot.reloadItems();

        cbHostJson = new ModuleFileComboBox(project, "host.json");
        cbHostJson.setRequired(true);
    }

    private void onSelectFunctionSlot(final DeploymentSlotConfig value) {
        if (value == null) {
            return;
        }
        toggleDeploymentSlot(chkSlot.isSelected());
        if (chkSlot.isSelected()) {
            final FunctionAppBase<?, ?, ?> resource = getResource(Objects.requireNonNull(functionAppComboBox.getValue()), value.getName());
            if (Objects.nonNull(resource)) {
                loadAppSettings(resource.getId(), !resource.exists());
            }
        }
    }

    private void onSelectFunctionApp(final FunctionAppConfig value) {
        if (value == null) {
            return;
        }
        final FunctionAppBase<?, ?, ?> resource = getResource(value, null);
        // disable slot for draft function
        final boolean isDraftResource = Objects.isNull(resource) || !resource.exists();
        this.chkSlot.setEnabled(!isDraftResource);
        if (isDraftResource) {
            this.chkSlot.setSelected(false);
        }
        toggleDeploymentSlot(chkSlot.isSelected());
        Optional.ofNullable(resource).map(AbstractAzResource::getId).ifPresent(this.cbDeploymentSlot::setAppService);
        if (!this.chkSlot.isSelected()) {
            loadAppSettings(getResourceId(value, null), isDraftResource);
        }
    }

    private void loadAppSettings(@Nullable final String resourceId, final boolean isNewResource) {
        if (StringUtils.equalsIgnoreCase(resourceId, this.appSettingsResourceId) && MapUtils.isNotEmpty(this.appSettingsTable.getAppSettings())) {
            return;
        }
        this.appSettingsResourceId = resourceId;
        this.appSettingsTable.loadAppSettings(() -> {
            final AbstractAzResource<?, ?, ?> resource = StringUtils.isBlank(resourceId) || isNewResource ? null : Azure.az().getById(resourceId);
            return resource instanceof AppServiceAppBase<?, ?, ?> ? ((AppServiceAppBase<?, ?, ?>) resource).getAppSettings() : Collections.emptyMap();
        });
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
            if (Objects.equals(ModuleUtil.getModuleDirPath(module), (ModuleUtil.getModuleDirPath(target)))) {
                cbFunctionModule.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onSlotCheckBoxChanged() {
        toggleDeploymentSlot(chkSlot.isSelected());
        final FunctionAppConfig function = functionAppComboBox.getValue();
        final DeploymentSlotConfig slot = cbDeploymentSlot.getValue();
        // reload app settings when switch slot configuration
        if (chkSlot.isSelected() && Objects.nonNull(function) && Objects.nonNull(slot)) {
            final FunctionAppBase<?, ?, ?> resource = getResource(function, slot.getName());
            Optional.ofNullable(resource).ifPresent(r -> loadAppSettings(r.getId(), !r.exists()));
        } else if (!chkSlot.isSelected() && Objects.nonNull(function)) {
            final FunctionAppBase<?, ?, ?> resource = getResource(function, null);
            Optional.ofNullable(resource).ifPresent(r -> loadAppSettings(r.getId(), !r.exists()));
        }
    }

    private void toggleDeploymentSlot(boolean isDeployToSlot) {
        cbDeploymentSlot.setEnabled(isDeployToSlot);
        cbDeploymentSlot.setRequired(isDeployToSlot);
        cbDeploymentSlot.validateValueAsync();
    }

    @Override
    public void setValue(FunctionDeployModel configuration) {
        if (MapUtils.isNotEmpty(configuration.getConfig().appSettings())) {
            this.appSettingsTable.setAppSettings(configuration.getConfig().appSettings());
        }
        if (StringUtils.isNotEmpty(configuration.getAppSettingsKey())) {
            this.appSettingsKey = configuration.getAppSettingsKey();
        }
        if (StringUtils.isNotEmpty(configuration.getHostJsonPath())) {
            cbHostJson.setValue(LocalFileSystem.getInstance().findFileByIoFile(new File(configuration.getHostJsonPath())));
        }
        Optional.of(configuration.getConfig())
                .filter(config -> StringUtils.isNotBlank(config.appName()))
                .ifPresent(config -> {
                    this.functionAppComboBox.setValue(c -> AppServiceComboBox.isSameApp(c, config));
                    this.functionAppComboBox.setConfigModel(config);
                    this.chkSlot.setSelected(config.deploymentSlotName() != null);
                    this.toggleDeploymentSlot(config.deploymentSlotName() != null);
                    this.appSettingsResourceId = StringUtils.isBlank(config.appName()) ? null : getResourceId(config, config.deploymentSlotName());
                    Optional.ofNullable(config.slotConfig()).ifPresent(cbDeploymentSlot::setValue);
                    Optional.ofNullable(config.appSettings()).ifPresent(appSettingsTable::setAppSettings);
                });
        this.previousModule = Arrays.stream(ModuleManager.getInstance(project).getModules())
                                    .filter(m -> StringUtils.equals(configuration.getModuleName(), m.getName()))
                                    .findFirst().orElse(null);
        selectModule(previousModule);
    }

    @Override
    public FunctionDeployModel getValue() {
        final FunctionDeployModel model = new FunctionDeployModel();
        model.setAppSettingsKey(appSettingsKey);
        model.setHostJsonPath(Optional.ofNullable(cbHostJson.getValue()).map(VirtualFile::getCanonicalPath).orElse(null));
        Optional.ofNullable((Module) cbFunctionModule.getSelectedItem()).map(Module::getName).ifPresent(model::setModuleName);
        Optional.ofNullable(functionAppComboBox.getValue())
                .map(value -> value.toBuilder()
                                   .slotConfig(chkSlot.isSelected() ? cbDeploymentSlot.getValue() : null)
                                   .appSettings(appSettingsTable.getAppSettings()).build())
                .ifPresent(model::setConfig);
        return model;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(functionAppComboBox, cbDeploymentSlot);
    }

    @Nullable
    private FunctionAppBase<?, ?, ?> getResource(@Nonnull FunctionAppConfig config, String slot) {
        if (StringUtils.isBlank(config.appName())) {
            return null;
        }
        final FunctionApp functionApp = Azure.az(AzureFunctions.class).functionApps(config.subscriptionId()).get(config.appName(), config.resourceGroup());
        return StringUtils.isBlank(slot) || Objects.isNull(functionApp) ? functionApp : functionApp.slots().get(slot, config.resourceGroup());
    }

    @Nullable
    private String getResourceId(@Nonnull FunctionAppConfig config, String slot) {
        return Optional.ofNullable(getResource(config, slot)).map(AbstractAzResource::getId).orElse(null);
    }
}
