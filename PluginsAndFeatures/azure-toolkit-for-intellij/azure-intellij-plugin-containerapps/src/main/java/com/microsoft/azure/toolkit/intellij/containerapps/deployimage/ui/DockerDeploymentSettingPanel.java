/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage.ui;

import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.*;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerapps.component.*;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageModel;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageRunConfiguration;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeploymentType;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.DockerBuildTaskUtils;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.DockerImageConfigurationPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DockerDeploymentSettingPanel implements AzureFormPanel<DeployImageModel> {
    private final Project project;
    private final DeployImageRunConfiguration configuration;
    private JLabel lblArtifact;
    private ModuleComboBox cbModule;
    private JRadioButton rdoContainerImage;
    private JRadioButton rdoSourceCode;
    private JRadioButton rdoArtifact;
    private ContainerAppComboBox cbContainerApp;
    private JPanel pnlDeployment;
    private IngressConfigurationPanel pnlIngressConfiguration;
    private JLabel lblEnv;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private AzureHideableTitledSeparator titleEnv;
    private AzureHideableTitledSeparator titleDeployment;
    private AzureHideableTitledSeparator titleIngress;
    @Getter
    private JPanel pnlRoot;
    private JPanel pnlDeploymentHolder;
    private JPanel pnlEnvHolder;
    private JPanel pnlIngressHolder;

    private final DockerImageConfigurationPanel pnlDockerConfiguration;
    private final CodeForm codeSourceForm;
    private final ArtifactForm artifactSourceForm;
    private ContainerApp containerApp;

    public DockerDeploymentSettingPanel(@Nonnull Project project, DeployImageRunConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
        $$$setupUI$$$();

        this.pnlDockerConfiguration = new DockerImageConfigurationPanel(project);
        this.codeSourceForm = new CodeForm(project);
        this.artifactSourceForm = new ArtifactForm(project);
        this.init();
    }

    private void init() {
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(rdoContainerImage);
        buttonGroup.add(rdoSourceCode);
        buttonGroup.add(rdoArtifact);
        rdoContainerImage.addItemListener(ignore -> this.onSelectDeploymentType());
        rdoSourceCode.addItemListener(ignore -> this.onSelectDeploymentType());
        rdoArtifact.addItemListener(ignore -> this.onSelectDeploymentType());
        cbModule.addItemListener(this::onSelectModule);

        titleEnv.addContentComponent(pnlEnvHolder);
        titleDeployment.addContentComponent(pnlDeploymentHolder);
        titleIngress.addContentComponent(pnlIngressHolder);

        this.pnlDockerConfiguration.enableContainerRegistryPanel();
        this.cbContainerApp.setRequired(true);
        this.cbContainerApp.addItemListener(this::onSelectContainerApp);
        this.pnlDockerConfiguration.addImageListener(this::onSelectImage);
        this.artifactSourceForm.addArtifactListener(this::onArtifactChanged);

        this.onSelectDeploymentType();
    }

    private synchronized void onArtifactChanged(final ItemEvent e) {
        if (!rdoArtifact.isSelected()) {
            return;
        }
        final DataContext context = DataManager.getInstance().getDataContext(this.pnlRoot);
        final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context);
        final AzureArtifact artifact = (AzureArtifact) e.getItem();
        if (Objects.nonNull(editor) && Objects.nonNull(artifact)) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                System.out.println("remove before run task - artifact changed");
                BuildArtifactBeforeRunTaskUtils.removeBeforeRunTask(editor, artifact, this.configuration);
            }
            if (e.getStateChange() == ItemEvent.SELECTED) {
                System.out.println("add before run task - artifact changed");
                BuildArtifactBeforeRunTaskUtils.addBeforeRunTask(editor, artifact, this.configuration);
            }
        }
    }

    private void onSelectImage(final DockerImage image) {
        if (rdoContainerImage.isSelected()) {
            System.out.println("update docker - on select image");
            updateDockerBuildBeforeRunTasks();
        }
    }

    private void onSelectModule(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            Optional.ofNullable(cbModule.getValue()).ifPresent(this::applyModuleValue);
        }
    }

    private void applyModuleValue(@Nonnull final Module module) {
        artifactSourceForm.setModule(module);
        codeSourceForm.setCodeSourceByModule(module);
    }

    private void onSelectContainerApp(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof ContainerApp containerApp) {
            AzureTaskManager.getInstance().runInBackground("Loading container app", () -> {
                codeSourceForm.setContainerApp(containerApp);
                artifactSourceForm.setContainerApp(containerApp);
                pnlDockerConfiguration.setContainerApp(containerApp);
            });
            this.pnlIngressConfiguration.setValueFromContainerApp(containerApp);
        }
    }

    private void onSelectSubscription(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.cbContainerApp.setSubscription(subscription);
        }
    }

    private synchronized void onSelectDeploymentType() {
        final JPanel panel = rdoContainerImage.isSelected() ? pnlDockerConfiguration.getPnlRoot() :
                rdoSourceCode.isSelected() ? codeSourceForm.getContentPanel() : artifactSourceForm.getContentPanel();
        pnlDeployment.removeAll();
        pnlDeployment.add(panel, new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 3, 3, null, null, null, 0));
        AzureTaskManager.getInstance().runLater(() -> {
            pnlDeployment.revalidate();
            pnlDeployment.repaint();
            pnlRoot.repaint();
        }, AzureTask.Modality.ANY);
        // clean up before run task if needed
        if (rdoArtifact.isSelected()) {
            System.out.println("on select deployment type: clean up docker + add artifact");
            updateBuildArtifactBeforeRunTasks();
            cleanupDockerBuildBeforeRunTasks();
        } else if (rdoContainerImage.isSelected()) {
            System.out.println("on select deployment type: add docker + clean up artifact");
            updateDockerBuildBeforeRunTasks();
            cleanupBuildArtifactBeforeRunTasks();
        } else if (rdoSourceCode.isSelected()) {
            System.out.println("on select deployment type: clean up docker + clean up artifact");
            cleanupDockerBuildBeforeRunTasks();
            cleanupBuildArtifactBeforeRunTasks();
        }
        Optional.ofNullable(cbModule.getValue()).ifPresent(this::applyModuleValue);
    }

    private synchronized void cleanupBuildArtifactBeforeRunTasks() {
        final DataContext context = DataManager.getInstance().getDataContext(pnlRoot);
        final Module module = cbModule.getValue();
        final AzureArtifact azureArtifact = artifactSourceForm.getArtifact();
        if (Objects.nonNull(azureArtifact)) {
            Optional.ofNullable(ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context))
                    .ifPresent(editor -> BuildArtifactBeforeRunTaskUtils.removeBeforeRunTask(editor, azureArtifact, this.configuration));
        }
    }

    private synchronized void updateBuildArtifactBeforeRunTasks() {
        final DataContext context = DataManager.getInstance().getDataContext(pnlRoot);
        final Module module = cbModule.getValue();
        final AzureArtifact azureArtifact = artifactSourceForm.getArtifact();
        if (Objects.nonNull(azureArtifact)) {
            Optional.ofNullable(ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context))
                    .ifPresent(editor -> BuildArtifactBeforeRunTaskUtils.addBeforeRunTask(editor, azureArtifact, this.configuration));
        }
    }

    private synchronized void updateDockerBuildBeforeRunTasks() {
        Optional.ofNullable(pnlDockerConfiguration.getValue())
                .map(DockerPushConfiguration::getDockerImage)
                .ifPresent(image -> AzureTaskManager.getInstance().runLater(() -> {
                    final DataContext dataContext = DataManager.getInstance().getDataContext(pnlRoot);
                    DockerBuildTaskUtils.updateDockerBuildBeforeRunTasks(dataContext, this.configuration, image);
                }, AzureTask.Modality.ANY));
    }

    private synchronized void cleanupDockerBuildBeforeRunTasks() {
        final DataContext context = DataManager.getInstance().getDataContext(pnlRoot);
        Optional.ofNullable(ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context))
                .ifPresent(editor -> DockerBuildTaskUtils.removeBeforeRunTask(editor, this.configuration));
    }

    @Override
    public void setValue(final DeployImageModel data) {
        final String path = data.getPath();
        final Module module = Arrays.stream(ModuleManager.getInstance(project).getModules())
                .filter(m -> StringUtils.equalsIgnoreCase(m.getName(), data.getModuleName()))
                .findFirst().orElse(null);
        Optional.ofNullable(module).ifPresent(cbModule::setValue);
        final DeploymentType deploymentType = Optional.ofNullable(data.getDeploymentType()).orElse(DeploymentType.Image);
        switch (deploymentType) {
            case Code -> {
                rdoSourceCode.setSelected(true);
                codeSourceForm.setValue(data.getImageConfig());
                Optional.ofNullable(module).ifPresent(codeSourceForm::setModule);
            }
            case Artifact -> {
                rdoArtifact.setSelected(true);
                artifactSourceForm.setValue(data.getImageConfig());
            }
            case Image -> {
                rdoContainerImage.setSelected(true);
                pnlDockerConfiguration.setValue(data);
            }
            default -> throw new AzureToolkitRuntimeException("Unsupported deployment type");
        }

        Optional.ofNullable(data.getIngressConfig()).ifPresent(pnlIngressConfiguration::setValue);
        Optional.ofNullable(data.getEnvironmentVariables()).ifPresent(inputEnv::setEnvironmentVariables);
        Optional.ofNullable(data.getContainerAppId())
                .map(id -> (ContainerApp) Azure.az(AzureContainerApps.class).getById(id))
                .ifPresent(app -> {
                    cbContainerApp.setValue(app);
                    pnlIngressConfiguration.setContainerApp(app);
                });
    }

    @Override
    public DeployImageModel getValue() {
        Optional.ofNullable(cbContainerApp.getValue()).ifPresent(app -> {
            codeSourceForm.setContainerApp(app);
            artifactSourceForm.setContainerApp(app);
        });

        final DeployImageModel model = new DeployImageModel();
        model.setDeploymentType(rdoContainerImage.isSelected() ? DeploymentType.Image :
                rdoSourceCode.isSelected() ? DeploymentType.Code : DeploymentType.Artifact);
        final Module value = cbModule.getValue();
        model.setModuleName(Objects.requireNonNull(value).getName());
        Optional.ofNullable(cbContainerApp.getValue()).map(ContainerApp::getId).ifPresent(model::setContainerAppId);
        if (rdoContainerImage.isSelected()) {
            Optional.ofNullable(pnlDockerConfiguration.getValue()).ifPresent(conf -> {
                model.setFinalRepositoryName(conf.getFinalRepositoryName());
                model.setFinalTagName(conf.getFinalTagName());
                model.setDockerHost(conf.getDockerHost());
                model.setDockerImage(conf.getDockerImage());
                model.setContainerRegistryId(conf.getContainerRegistryId());
            });
        } else {
            final DeploymentSourceForm imageForm = rdoSourceCode.isSelected() ? codeSourceForm : artifactSourceForm;
            model.setImageConfig(Objects.requireNonNull(imageForm.getValue()));
        }
        Optional.ofNullable(pnlIngressConfiguration.getValue()).ifPresent(model::setIngressConfig);
        Optional.ofNullable(inputEnv.getEnvironmentVariables()).ifPresent(model::setEnvironmentVariables);
        return model;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureForm<?> azureForm = rdoContainerImage.isSelected() ? pnlDockerConfiguration :
                rdoSourceCode.isSelected() ? codeSourceForm : artifactSourceForm;
        return Arrays.asList(cbContainerApp, azureForm);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbModule = new ModuleComboBox(project);
    }
}
