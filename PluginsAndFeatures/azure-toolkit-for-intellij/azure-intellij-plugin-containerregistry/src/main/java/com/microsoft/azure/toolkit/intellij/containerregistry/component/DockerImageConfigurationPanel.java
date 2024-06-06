/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.google.common.hash.Hashing;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistryDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.model.Sku;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration.*;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class DockerImageConfigurationPanel implements AzureForm<DockerPushConfiguration> {
    @Getter
    private JPanel pnlRoot;
    private AzureDockerHostComboBox cbDockerHost;
    private AzureDockerImageComboBox cbDockerImage;
    private AzureTextInput txtRepositoryName;
    private JLabel lblRepository;
    private JLabel lblTagName;
    private AzureTextInput txtTagName;
    private ACRRegistryComboBox cbContainerRegistry;
    private JLabel lblRepositoryPrefix;
    private JPanel pnlImageName;
    private JPanel pnlContainerRegistry;
    private ActionLink linkEnableAdminUser;
    private JLabel lblContainerRegistry;
    private JLabel lblDockerHost;
    private JLabel lblDockerfile;
    private final Project project;
    private final AtomicReference<String> repositoryReference = new AtomicReference<>();
    @Getter
    @Setter
    private boolean enableCustomizedImageName = true;
    private DockerImage image;
    private ContainerApp containerApp;

    public DockerImageConfigurationPanel(final Project project) {
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.cbDockerHost.setRequired(true);
        this.cbDockerImage.setRequired(true);
        this.cbContainerRegistry.addItemListener(this::onSelectContainerRegistry);
        this.cbContainerRegistry.addValidator(this::validateAdminUserEnableStatus);
        this.cbDockerHost.addItemListener(this::onDockerHostChanged);
        this.cbDockerImage.addItemListener(this::onSelectNewDockerImage);
        this.linkEnableAdminUser.addActionListener(this::onEnableAdminUser);
        this.txtRepositoryName.addValidator(this::validateRepositoryName);
        this.txtTagName.addValidator(this::validateTagName);
        lblRepository.setLabelFor(txtRepositoryName);
        lblTagName.setLabelFor(txtTagName);
        lblContainerRegistry.setLabelFor(cbContainerRegistry);
        lblDockerfile.setLabelFor(cbDockerImage);
        lblDockerHost.setLabelFor(cbDockerHost);
    }

    private AzureValidationInfo validateTagName() {
        final String tagName = txtTagName.getValue();
        if (StringUtils.isBlank(tagName) || tagName.length() > TAG_LENGTH) {
            return AzureValidationInfo.error(TAG_LENGTH_INVALID, txtTagName);
        }
        if (!tagName.matches(TAG_REGEX)) {
            return AzureValidationInfo.error(String.format(TAG_INVALID, tagName, TAG_REGEX), txtTagName);
        }
        return AzureValidationInfo.success(txtTagName);
    }

    @Nonnull
    private AzureValidationInfo validateRepositoryName() {
        final String repositoryName = txtRepositoryName.getValue();
        if (StringUtils.isBlank(repositoryName) || repositoryName.length() < 1 || repositoryName.length() > REPO_LENGTH) {
            return AzureValidationInfo.error(REPO_LENGTH_INVALID, txtRepositoryName);
        }
        if (repositoryName.endsWith("/")) {
            return AzureValidationInfo.error(CANNOT_END_WITH_SLASH, txtRepositoryName);
        }
        final String[] repoComponents = repositoryName.split("/");
        for (final String component : repoComponents) {
            if (!component.matches(REPO_COMPONENTS_REGEX)) {
                final String message = String.format(REPO_COMPONENT_INVALID, component, REPO_COMPONENTS_REGEX);
                return AzureValidationInfo.error(message, txtRepositoryName);
            }
        }
        return AzureValidationInfo.success(txtRepositoryName);
    }

    private void onEnableAdminUser(ActionEvent actionEvent) {
        final ContainerRegistry value = cbContainerRegistry.getValue();
        if (Objects.nonNull(value)) {
            AzureTaskManager.getInstance().runInBackground("Enable Admin User", () -> {
                value.enableAdminUser(); // call method instead of action directly as we need to invoke callback actions
                AzureTaskManager.getInstance().runLater(() -> {
                    cbContainerRegistry.validateValueAsync();
                    cbContainerRegistry.reloadItems();
                }, AzureTask.Modality.ANY);
            });
        }
    }

    private AzureValidationInfo validateAdminUserEnableStatus() {
        final ContainerRegistry value = cbContainerRegistry.getValue();
        if (Objects.isNull(value)) {
            return AzureValidationInfo.success(cbContainerRegistry);
        }
        return value.isAdminUserEnabled() ? AzureValidationInfo.success(cbContainerRegistry) :
                AzureValidationInfo.error(String.format("Admin user is not enabled for registry (%s)", value.getName()), cbContainerRegistry);
    }

    public void enableContainerRegistryPanel() {
        pnlContainerRegistry.setVisible(true);
        cbContainerRegistry.setRequired(true);
        cbContainerRegistry.validateValueAsync();
        cbContainerRegistry.reloadItems();
    }

    private void onSelectContainerRegistry(ItemEvent itemEvent) {
        final ContainerRegistry value = cbContainerRegistry.getValue();
        Mono.fromCallable(() -> Optional.ofNullable(value).map(ContainerRegistry::getLoginServerUrl).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(loginServerUrl -> AzureTaskManager.getInstance().runLater(() -> {
                    lblRepositoryPrefix.setText(" " + loginServerUrl + "/"); // add space to keep align with combo box
                    lblRepositoryPrefix.setVisible(StringUtils.isNotBlank(loginServerUrl));
                    repositoryReference.set(loginServerUrl);
                    final String repositoryName = txtRepositoryName.getValue();
                    if (StringUtils.startsWith(repositoryName, loginServerUrl)) {
                        txtRepositoryName.setValue(StringUtils.removeStart(repositoryName, loginServerUrl + "/"));
                    }
                    final boolean adminUserEnabled = Optional.ofNullable(value).map(ContainerRegistry::isAdminUserEnabled).orElse(true);
                    linkEnableAdminUser.setVisible(!adminUserEnabled);
                }, AzureTask.Modality.ANY));
    }

    private void onSelectNewDockerImage(ItemEvent itemEvent) {
        final DockerImage image = cbDockerImage.getValue();
        // workaround for reload issue
        if (Objects.isNull(image) || Objects.equals(image, this.image)) {
            return;
        }
        this.image = image;
        final boolean isDraftImage = image.isDraft();
        final String loginServerUrl = repositoryReference.get();
        final String repositoryName = image.getRepositoryName();
        final String fixedRepositoryName = StringUtils.isNotEmpty(loginServerUrl) && StringUtils.startsWith(repositoryName, loginServerUrl) ?
                StringUtils.removeStart(repositoryName, loginServerUrl + "/") : repositoryName;
        txtRepositoryName.setValue(fixedRepositoryName);
        txtTagName.setValue(image.getTagName());
        pnlImageName.setVisible(enableCustomizedImageName || isDraftImage);
        txtRepositoryName.setRequired(enableCustomizedImageName || isDraftImage);
        txtRepositoryName.validateValueAsync();
        txtTagName.setRequired(enableCustomizedImageName || isDraftImage);
        txtTagName.validateValueAsync();
    }

    private void onDockerHostChanged(@Nullable final ItemEvent event) {
        this.cbDockerImage.setDockerHost(cbDockerHost.getValue());
    }

    @Override
    public DockerPushConfiguration getValue() {
        final DockerPushConfiguration result = new DockerPushConfiguration();
        final DockerImage image = Optional.ofNullable(this.cbDockerImage.getValue()).map(DockerImage::new).orElse(null);
        final ContainerRegistry registry = this.cbContainerRegistry.getValue();
        result.setDockerHost(this.cbDockerHost.getValue());
        result.setDockerImage(image);
        if (Optional.ofNullable(image).map(DockerImage::isDraft).orElse(false)) {
            image.setRepositoryName(this.txtRepositoryName.getValue());
            image.setTagName(this.txtTagName.getValue());
        }
        Optional.ofNullable(registry).ifPresent(cr -> {
            result.setContainerRegistryId(cr.getId());
            result.setFinalRepositoryName(cr.getLoginServerUrl() + "/" + this.txtRepositoryName.getValue());
            result.setFinalTagName(this.txtTagName.getValue());
        });
        return result;
    }

    @Override
    public void setValue(@Nonnull final DockerPushConfiguration data) {
        Optional.ofNullable(data.getDockerHost()).ifPresent(host -> {
            cbDockerHost.getDrafts().add(host);
            cbDockerHost.setValue(h -> StringUtils.equalsIgnoreCase(host.getDockerHost(), h.getDockerHost()));
        });
        Optional.ofNullable(data.getDockerImage()).ifPresent(i -> {
            this.image = i;
            this.cbDockerImage.setValue(i);
        });
        Optional.ofNullable(data.getContainerRegistryId())
                .map(id -> (ContainerRegistry) Azure.az(AzureContainerRegistry.class).getById(id))
                .ifPresent(cbContainerRegistry::setValue);
        final String repositoryName = Optional.ofNullable(data.getFinalRepositoryName())
                .orElseGet(() -> Optional.ofNullable(data.getDockerImage()).map(DockerImage::getRepositoryName).orElse(null));
        final String tagName = Optional.ofNullable(data.getFinalTagName())
                .orElseGet(() -> Optional.ofNullable(data.getDockerImage()).map(DockerImage::getTagName).orElse(null));
        Optional.ofNullable(repositoryName).ifPresent(txtRepositoryName::setValue);
        Optional.ofNullable(tagName).ifPresent(txtTagName::setValue);
        final boolean isDraftImage = Optional.ofNullable(data.getDockerImage()).map(DockerImage::isDraft).orElse(false);
        pnlImageName.setVisible(isDraftImage || enableCustomizedImageName);
    }

    // todo: remove duplicate with ImageForm
    public void setContainerApp(final ContainerApp containerApp) {
        this.containerApp = containerApp;
        if (Objects.nonNull(containerApp)) {
            // update the draft value
            this.cbContainerRegistry.setSubscription(containerApp.getSubscription());
            final Object rawValue = this.cbContainerRegistry.getRawValue();
            final ContainerRegistryDraft draft = getDraftRegistry();
            final List<ContainerRegistry> draftItems = cbContainerRegistry.getDraftItems();
            draftItems.clear();
            draftItems.add(draft);
            if (Objects.isNull(rawValue)) {
                cbContainerRegistry.setValue(val -> val.isAdminUserEnabled() &&
                        StringUtils.equalsIgnoreCase(val.getResourceGroupName(), containerApp.getResourceGroupName()));
            } else if (rawValue instanceof ContainerRegistryDraft) {
                cbContainerRegistry.setValue(draft);
            }
            cbContainerRegistry.reloadItems();
        }
    }

    // todo: remove duplicate with ImageForm
    @Nullable
    private ContainerRegistryDraft getDraftRegistry() {
        if (Objects.isNull(this.containerApp)) {
            return null;
        }
        final String originalString = containerApp.getSubscriptionId() + "/" + containerApp.getResourceGroupName()
                + "/" + Optional.ofNullable(containerApp.getManagedEnvironment()).map(AbstractAzResource::getName).orElse(containerApp.getName());
        final String acrRegistryName = "ca" + Hashing.sha256().hashString(originalString, StandardCharsets.UTF_8).toString().substring(0, 10) + "acr";  // ACR names must start + end in a letter
        final ContainerRegistryDraft draft = az(AzureContainerRegistry.class)
                .registry(containerApp.getSubscriptionId()).create(acrRegistryName, containerApp.getResourceGroupName());
        draft.setSku(Sku.Standard);
        draft.setAdminUserEnabled(true);
        draft.setRegion(Optional.ofNullable(containerApp.getRegion()).orElse(Region.US_EAST));
        return draft;
    }

    public void addImageListener(@Nonnull final AzureValueChangeListener<DockerImage> listener) {
        this.cbDockerImage.addValueChangedListener(listener);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbDockerHost, cbDockerImage);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbDockerHost = new AzureDockerHostComboBox(project);
        this.cbDockerImage = new AzureDockerImageComboBox(project);
        this.cbContainerRegistry = new ACRRegistryComboBox();
    }

    private void $$$setupUI$$$() {
    }
}
