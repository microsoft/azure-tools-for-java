/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.function.components.ModuleFileComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTable;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components.DeploymentSlotComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;


public class FunctionDeploymentPanel extends AzureSettingPanel<FunctionDeployConfiguration> implements AzureFormPanel<FunctionDeployConfiguration> {

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

    public FunctionDeploymentPanel(@NotNull Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.configuration = functionDeployConfiguration;
        this.appSettingsKey = StringUtils.firstNonBlank(functionDeployConfiguration.getAppSettingsKey(), UUID.randomUUID().toString());
        $$$setupUI$$$();
        init();
    }

    private void init() {
        cbFunctionModule.setRenderer(new ListCellRendererWrapper<>() {
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
        } else {
            cbHostJson.setModule(null);
        }
    }

    @NotNull
    @Override
    public String getPanelName() {
        return message("function.deploy.title");
    }

    @Override
    public void disposeEditor() {

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
            this.appSettingsTable.setAppSettings(configuration.getAppSettings());
        }
        if (StringUtils.isNotEmpty(configuration.getAppSettingsKey())) {
            this.appSettingsKey = configuration.getAppSettingsKey();
        }
        if (StringUtils.isNotEmpty(configuration.getHostJsonPath())) {
            cbHostJson.setValue(LocalFileSystem.getInstance().findFileByIoFile(new File(configuration.getHostJsonPath())));
        }
        Optional.ofNullable(configuration.getConfig())
                .filter(config -> !StringUtils.isAllEmpty(config.getResourceId(), config.getName()))
                .ifPresent(config -> {
                    this.functionAppComboBox.setValue(config);
                    this.functionAppComboBox.setConfigModel(config);
                    this.chkSlot.setSelected(config.getDeploymentSlot() != null);
                    this.toggleDeploymentSlot(config.getDeploymentSlot() != null);
                    this.appSettingsResourceId = StringUtils.isAllEmpty(config.getResourceId(), config.getName()) ? null :
                            getResourceId(config, config.getDeploymentSlot());
                    Optional.ofNullable(config.getDeploymentSlot()).ifPresent(cbDeploymentSlot::setValue);
                    Optional.ofNullable(config.getAppSettings()).ifPresent(appSettingsTable::setAppSettings);
                });
        this.previousModule = configuration.getModule();
        selectModule(previousModule);
    }

    @Override
    protected void apply(@NotNull FunctionDeployConfiguration configuration) {
        configuration.setAppSettingsKey(appSettingsKey);
        configuration.setAppSettings(appSettingsTable.getAppSettings());
        configuration.setHostJsonPath(Optional.ofNullable(cbHostJson.getValue()).map(VirtualFile::getCanonicalPath).orElse(null));
        Optional.ofNullable((Module) cbFunctionModule.getSelectedItem()).ifPresent(configuration::saveTargetModule);
        Optional.ofNullable(functionAppComboBox.getValue())
                .map(value -> value.toBuilder()
                        .deploymentSlot(chkSlot.isSelected() ? cbDeploymentSlot.getValue() : null)
                        .appSettings(appSettingsTable.getAppSettings()).build())
                .ifPresent(configuration::saveConfig);
    }

    private void createUIComponents() {
        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new FunctionAppSettingsTable(localSettingPath);
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
            loadAppSettings(getResourceId(Objects.requireNonNull(functionAppComboBox.getValue()), value), value.isNewCreate());
        }
    }

    private void onSelectFunctionApp(final FunctionAppConfig value) {
        if (value == null) {
            return;
        }
        // disable slot for draft function
        this.chkSlot.setEnabled(StringUtils.isNotEmpty(value.getResourceId()));
        if (StringUtils.isEmpty(value.getResourceId())) {
            this.chkSlot.setSelected(false);
        }
        toggleDeploymentSlot(chkSlot.isSelected());
        this.cbDeploymentSlot.setAppService(value.getResourceId());
        if (!this.chkSlot.isSelected()) {
            loadAppSettings(getResourceId(value, null), StringUtils.isEmpty(value.getResourceId()));
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

    private void onSlotCheckBoxChanged() {
        toggleDeploymentSlot(chkSlot.isSelected());
        final FunctionAppConfig function = functionAppComboBox.getValue();
        final DeploymentSlotConfig slot = cbDeploymentSlot.getValue();
        // reload app settings when switch slot configuration
        if (chkSlot.isSelected() && ObjectUtils.allNotNull(function, slot)) {
            loadAppSettings(getResourceId(functionAppComboBox.getValue(), slot), slot.isNewCreate());
        } else if (!chkSlot.isSelected() && Objects.nonNull(function)) {
            loadAppSettings(getResourceId(functionAppComboBox.getValue(), null), StringUtils.isEmpty(function.getResourceId()));
        }
    }

    private void toggleDeploymentSlot(boolean isDeployToSlot) {
        cbDeploymentSlot.setEnabled(isDeployToSlot);
        cbDeploymentSlot.setRequired(isDeployToSlot);
        cbDeploymentSlot.validateValueAsync();
    }

    @Override
    public void setValue(FunctionDeployConfiguration data) {
        resetFromConfig(data);
    }

    @Override
    public FunctionDeployConfiguration getValue() {
        final FunctionDeployConfiguration result = new FunctionDeployConfiguration(configuration.getProject(), configuration.getFactory(), configuration.getName());
        apply(result);
        return result;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(functionAppComboBox, cbDeploymentSlot);
    }

    @Nullable
    private String getResourceId(@Nonnull FunctionAppConfig config, @Nullable DeploymentSlotConfig slotConfig) {
        if (Objects.isNull(slotConfig)) {
            return StringUtils.isNoneBlank(config.getResourceId()) ? config.getResourceId() :
                    Azure.az(AzureFunctions.class).functionApps(config.getSubscriptionId()).getOrTemp(config.getName(), config.getResourceGroupName()).getId();
        } else {
            return Optional.ofNullable(Azure.az(AzureFunctions.class).functionApp(config.getResourceId()))
                    .map(func -> func.slots().getOrTemp(slotConfig.getName(), null).getId())
                    .orElse(null);
        }
    }
}
