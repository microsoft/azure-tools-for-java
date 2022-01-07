/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudDeploymentConfigurationPanel extends JPanel implements AzureFormPanel<SpringCloudAppConfig> {
    private final Project project;
    @Setter
    private SpringCloudDeploymentConfiguration configuration;

    @Getter
    private JPanel contentPanel;
    private AzureArtifactComboBox selectorArtifact;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private SpringCloudAppComboBox selectorApp;

    public SpringCloudDeploymentConfigurationPanel(SpringCloudDeploymentConfiguration config, @Nonnull final Project project) {
        super();
        this.project = project;
        this.configuration = config;
        this.init();
    }

    private void init() {
        this.selectorArtifact.setFileFilter(virtualFile -> StringUtils.equalsIgnoreCase("jar", FileNameUtils.getExtension(virtualFile.getPath())));
        this.selectorArtifact.addItemListener(this::onArtifactChanged);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorCluster.addItemListener(this::onClusterChanger);
        this.selectorSubscription.setRequired(true);
        this.selectorCluster.setRequired(true);
        this.selectorApp.setRequired(true);
        this.selectorArtifact.setRequired(true);
    }

    private void onArtifactChanged(final ItemEvent e) {
        final DataContext context = DataManager.getInstance().getDataContext(getContentPanel());
        final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context);
        final AzureArtifact artifact = (AzureArtifact) e.getItem();
        if (Objects.nonNull(editor) && Objects.nonNull(artifact)) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                BuildArtifactBeforeRunTaskUtils.removeBeforeRunTask(editor, artifact, this.configuration);
            }
            if (e.getStateChange() == ItemEvent.SELECTED) {
                BuildArtifactBeforeRunTaskUtils.addBeforeRunTask(editor, artifact, this.configuration);
            }
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = this.selectorSubscription.getValue();
            this.selectorCluster.setSubscription(subscription);
        }
    }

    private void onClusterChanger(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudCluster cluster = this.selectorCluster.getValue();
            this.selectorApp.setCluster(cluster);
        }
    }

    @Override
    public synchronized void setValue(SpringCloudAppConfig appConfig) {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final String resourceGroup = appConfig.getResourceGroup();
            final SpringCloudCluster cluster = Optional.of(Azure.az(AzureSpringCloud.class))
                .map(az -> az.clusters(appConfig.getSubscriptionId()))
                .map(cs -> cs.get(appConfig.getClusterName(), appConfig.getResourceGroup()))
                .orElse(null);
            final SpringCloudApp app = Optional.ofNullable(cluster)
                .map(c -> c.apps().get(appConfig.getAppName(), resourceGroup))
                .orElse(null);
            if (Objects.nonNull(cluster) && Objects.isNull(app)) {
                final SpringCloudAppDraft draft = cluster.apps().create(appConfig.getAppName(), appConfig.getResourceGroup());
                draft.setConfig(appConfig);
                this.selectorApp.addLocalItem(draft);
            }
        });
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
        Optional.ofNullable(deploymentConfig).map(SpringCloudDeploymentConfig::getArtifact).map(a -> ((WrappedAzureArtifact) a))
            .ifPresent((a -> this.selectorArtifact.setArtifact(a.getArtifact())));
        Optional.ofNullable(appConfig.getSubscriptionId())
            .ifPresent((id -> this.selectorSubscription.setValue(new ItemReference<>(id, Subscription::getId))));
        Optional.ofNullable(appConfig.getClusterName())
            .ifPresent((id -> this.selectorCluster.setValue(new ItemReference<>(id, SpringCloudCluster::name))));
        Optional.ofNullable(appConfig.getAppName())
            .ifPresent((id -> this.selectorApp.setValue(new ItemReference<>(id, SpringCloudApp::name))));
    }

    @Nullable
    @Override
    public SpringCloudAppConfig getValue() {
        SpringCloudAppConfig appConfig = Optional.ofNullable(this.selectorApp.getValue())
            .filter(a -> a instanceof SpringCloudAppDraft)
            .map(a -> ((SpringCloudAppDraft) a).getConfig())
            .orElse(null);
        if (Objects.isNull(appConfig)) {
            appConfig = SpringCloudAppConfig.builder()
                .deployment(SpringCloudDeploymentConfig.builder().build())
                .build();
        }
        this.getValue(appConfig);
        return appConfig;
    }

    public SpringCloudAppConfig getValue(SpringCloudAppConfig appConfig) {
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        appConfig.setSubscriptionId(Optional.ofNullable(this.selectorSubscription.getValue()).map(Subscription::getId).orElse(null));
        appConfig.setClusterName(Optional.ofNullable(this.selectorCluster.getValue()).map(AzResource::name).orElse(null));
        appConfig.setAppName(Optional.ofNullable(this.selectorApp.getValue()).map(AzResource::name).orElse(null));
        final AzureArtifact artifact = this.selectorArtifact.getValue();
        if (Objects.nonNull(artifact)) {
            deploymentConfig.setArtifact(new WrappedAzureArtifact(this.selectorArtifact.getValue(), this.project));
        }
        return appConfig;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.selectorArtifact,
            this.selectorSubscription,
            this.selectorCluster,
            this.selectorApp
        };
        return Arrays.asList(inputs);
    }

    private void createUIComponents() {
        this.selectorArtifact = new AzureArtifactComboBox(project);
        this.selectorArtifact.refreshItems();
    }
}
