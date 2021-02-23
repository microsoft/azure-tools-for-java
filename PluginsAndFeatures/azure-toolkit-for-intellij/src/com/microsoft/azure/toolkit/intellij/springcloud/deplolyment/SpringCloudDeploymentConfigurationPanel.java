/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.RuntimeVersion;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariableTable;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SpringCloudDeploymentConfigurationPanel extends JPanel implements AzureFormPanel<SpringCloudAppConfig> {
    private final Project project;

    @Getter
    private JPanel contentPanel;
    private AzureArtifactComboBox selectorArtifact;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private SpringCloudAppComboBox selectorApp;
    private JComboBox<String> cbCPU;
    private JComboBox<String> cbMemory;
    private JComboBox<String> cbInstanceCount;
    private JTextField textJvmOptions;
    private JRadioButton useJava8;
    private JRadioButton useJava11;
    private JRadioButton enablePersistent;
    private JRadioButton disablePersistent;
    private JRadioButton enablePublic;
    private JRadioButton disablePublic;
    private JPanel pnlEnvironmentTable;
    private EnvironmentVariableTable environmentVariableTable;

    public SpringCloudDeploymentConfigurationPanel(@NotNull final Project project) {
        super();
        this.project = project;
        this.init();
    }

    private void init() {
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorCluster.addItemListener(this::onClusterChanger);
        this.selectorSubscription.setRequired(true);
        this.selectorCluster.setRequired(true);
        this.selectorApp.setRequired(true);
        this.selectorArtifact.setRequired(true);
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.selectorCluster.setSubscription(subscription);
        }
    }

    private void onClusterChanger(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudCluster cluster = (SpringCloudCluster) e.getItem();
            this.selectorApp.setCluster(cluster);
        }
    }

    public void setData(SpringCloudAppConfig appConfig) {
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        this.cbCPU.setSelectedItem(Optional.ofNullable(deploymentConfig.getCpu()).map(String::valueOf).orElse("1"));
        this.cbMemory.setSelectedItem(Optional.ofNullable(deploymentConfig.getMemoryInGB()).map(String::valueOf).orElse("1"));
        this.cbInstanceCount.setSelectedItem(Optional.ofNullable(deploymentConfig.getInstanceCount()).map(String::valueOf).orElse("1"));
        this.textJvmOptions.setText(deploymentConfig.getJvmOptions());
        this.selectorSubscription.setValue(new ItemReference<>(Subscription.class, s -> appConfig.getSubscriptionId().equals(s.subscriptionId())));
        this.selectorCluster.setValue(new ItemReference<>(SpringCloudCluster.class, c -> appConfig.getClusterName().equals(c.name())));
        this.selectorApp.setValue(new ItemReference<>(SpringCloudApp.class, a -> appConfig.getAppName().equals(a.name())));
        final boolean useJava11 = StringUtils.equalsIgnoreCase(appConfig.getRuntimeVersion(), RuntimeVersion.JAVA_11.toString());
        this.useJava11.setSelected(useJava11);
        this.useJava8.setSelected(!useJava11);
        final boolean enableStorage = deploymentConfig.isEnablePersistentStorage();
        this.enablePersistent.setSelected(enableStorage);
        this.disablePersistent.setSelected(!enableStorage);
        final boolean isPublic = appConfig.isPublic();
        this.enablePublic.setSelected(isPublic);
        this.disablePublic.setSelected(!isPublic);

        if (MapUtils.isNotEmpty(deploymentConfig.getEnvironment())) {
            environmentVariableTable.setEnv(deploymentConfig.getEnvironment());
        }
    }

    @Nullable
    public SpringCloudAppConfig getData() {
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder()
            .deployment(SpringCloudDeploymentConfig.builder().build())
            .build();
        this.getData(appConfig);
        return appConfig;
    }

    public SpringCloudAppConfig getData(SpringCloudAppConfig appConfig) {
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final RuntimeVersion javaVersion = this.useJava11.isSelected() ? RuntimeVersion.JAVA_11 : RuntimeVersion.JAVA_8;
        appConfig.setSubscriptionId(this.selectorSubscription.getValue().subscriptionId());
        appConfig.setClusterName(this.selectorCluster.getValue().name());
        appConfig.setAppName(this.selectorApp.getValue().name());
        appConfig.setIsPublic(enablePublic.isSelected());
        appConfig.setRuntimeVersion(javaVersion.toString());
        deploymentConfig.setCpu(Optional.ofNullable(this.cbCPU.getSelectedItem()).map(o -> Integer.parseInt((String) o)).orElse(1));
        deploymentConfig.setMemoryInGB(Optional.ofNullable(this.cbMemory.getSelectedItem()).map(o -> Integer.parseInt((String) o)).orElse(1));
        deploymentConfig.setInstanceCount(Optional.ofNullable(this.cbInstanceCount.getSelectedItem()).map(o -> Integer.parseInt((String) o)).orElse(1));
        deploymentConfig.setJvmOptions(Optional.ofNullable(this.textJvmOptions.getText()).map(String::trim).orElse(""));
        deploymentConfig.setEnablePersistentStorage(this.enablePersistent.isSelected());
        deploymentConfig.setEnvironment(environmentVariableTable.getEnv());
        final AzureArtifact artifact = this.selectorArtifact.getValue();
        deploymentConfig.setArtifact(IArtifact.fromId(AzureArtifactManager.getInstance(this.project).getArtifactIdentifier(artifact)));
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
        pnlEnvironmentTable = new JPanel();
        pnlEnvironmentTable.setLayout(new GridLayoutManager(1, 1));
        environmentVariableTable = new EnvironmentVariableTable();
        pnlEnvironmentTable.add(environmentVariableTable.getComponent(),
            new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 7, 7, null, null, null));
        pnlEnvironmentTable.setFocusable(false);
    }
}
