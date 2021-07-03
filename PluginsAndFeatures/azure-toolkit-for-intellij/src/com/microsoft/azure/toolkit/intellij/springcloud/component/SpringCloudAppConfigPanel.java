/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.component.AzureSlider;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentInstanceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudSku;
import lombok.Getter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpringCloudAppConfigPanel extends JPanel implements AzureFormPanel<SpringCloudAppConfig> {
    @Getter
    private JPanel contentPanel;
    private HyperlinkLabel txtEndpoint;
    private JButton toggleEndpoint;
    private HyperlinkLabel txtTestEndpoint;
    private JBLabel txtStorage;
    private JButton toggleStorage;
    private JRadioButton useJava8;
    private JRadioButton useJava11;
    private JTextField txtJvmOptions;
    private EnvironmentVariablesTextFieldWithBrowseButton envTable;
    private ComboBox<Integer> numCpu;
    private ComboBox<Integer> numMemory;
    private AzureSlider numInstance;
    private JBLabel statusEndpoint;
    private JBLabel statusStorage;
    private JLabel lblTestEndpoint;

    private Consumer<? super SpringCloudAppConfig> listener = (config) -> {
    };
    private SpringCloudAppConfig originalConfig;

    public SpringCloudAppConfigPanel() {
        super();
        this.init();
    }

    private void init() {
        final TailingDebouncer debouncer = new TailingDebouncer(this::onDataChanged, 300);
        this.toggleStorage.addActionListener(e -> {
            toggleStorage("enable".equals(e.getActionCommand()));
            debouncer.debounce();
        });
        this.toggleEndpoint.addActionListener(e -> {
            toggleEndpoint("enable".equals(e.getActionCommand()));
            debouncer.debounce();
        });

        this.txtStorage.setBorder(JBUI.Borders.empty(0, 2));
        this.useJava8.addActionListener((e) -> debouncer.debounce());
        this.useJava11.addActionListener((e) -> debouncer.debounce());
        this.txtJvmOptions.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                debouncer.debounce();
            }
        });
        this.envTable.addChangeListener((e) -> debouncer.debounce());
        this.numCpu.addActionListener((e) -> debouncer.debounce());
        this.numMemory.addActionListener((e) -> debouncer.debounce());
        this.numInstance.addChangeListener((e) -> debouncer.debounce());
    }

    public void reset() {
        AzureTaskManager.getInstance().runLater(() -> Optional.ofNullable(this.originalConfig).ifPresent(this::setData));
    }

    public void setDataChangedListener(Consumer<? super SpringCloudAppConfig> listener) {
        this.listener = listener;
    }

    private void onDataChanged() {
        if (Objects.nonNull(this.originalConfig) && Objects.nonNull(this.listener)) {
            final SpringCloudAppConfig newConfig = this.getData();
            this.listener.accept(newConfig);
        }
    }

    public void updateForm(@Nonnull SpringCloudApp app) {
        final String testUrl = app.entity().getTestUrl();
        if (testUrl != null) {
            this.txtTestEndpoint.setHyperlinkText(testUrl.length() > 60 ? testUrl.substring(0, 60) + "..." : testUrl);
        } else {
            this.txtTestEndpoint.setVisible(false);
            this.lblTestEndpoint.setVisible(false);
        }
        this.txtTestEndpoint.setHyperlinkTarget(testUrl);
        final SpringCloudPersistentDisk disk = app.entity().getPersistentDisk();
        this.txtStorage.setText(Objects.nonNull(disk) ? disk.toString() : "---");
        final String url = app.entity().getApplicationUrl();
        this.txtEndpoint.setHyperlinkTarget(url);
        this.txtEndpoint.setEnabled(Objects.nonNull(url));
        if (Objects.nonNull(url)) {
            this.txtEndpoint.setHyperlinkText(url);
        } else {
            this.txtEndpoint.setIcon(null);
            this.txtEndpoint.setText("---");
        }
        final SpringCloudSku sku = app.getCluster().entity().getSku();
        final boolean basic = sku.getTier().toLowerCase().startsWith("b");
        final DefaultComboBoxModel<Integer> numCpuModel = (DefaultComboBoxModel<Integer>) this.numCpu.getModel();
        final DefaultComboBoxModel<Integer> numMemoryModel = (DefaultComboBoxModel<Integer>) this.numMemory.getModel();
        final Integer cpu = this.numCpu.getItem();
        final Integer mem = this.numMemory.getItem();
        final int maxCpu = basic ? 1 : 4;
        final int maxMem = basic ? 2 : 8;
        numCpuModel.removeAllElements();
        numCpuModel.addAll(IntStream.range(1, 1 + maxCpu).boxed().collect(Collectors.toList()));
        numCpuModel.setSelectedItem(Objects.nonNull(cpu) && cpu > maxCpu ? null : cpu);
        numMemoryModel.removeAllElements();
        numMemoryModel.addAll(IntStream.range(1, 1 + maxMem).boxed().collect(Collectors.toList()));
        numMemoryModel.setSelectedItem(Objects.nonNull(mem) && mem > maxMem ? null : mem);
        this.numInstance.setMaximum(basic ? 25 : 500);
        this.numInstance.setMajorTickSpacing(basic ? 5 : 50);
        this.numInstance.setMinorTickSpacing(basic ? 1 : 10);
        this.numInstance.setMinimum(0);
        this.numInstance.updateLabels();
        final SpringCloudDeploymentEntity deploymentEntity = Optional.ofNullable(app.activeDeployment())
                .map(SpringCloudDeployment::entity)
                .orElse(new SpringCloudDeploymentEntity("default", app.entity()));
        final List<SpringCloudDeploymentInstanceEntity> instances = deploymentEntity.getInstances();
        this.numInstance.setRealMin(Math.min(instances.size(), 1));
    }

    @Contract("_->_")
    public SpringCloudAppConfig getData(@Nonnull SpringCloudAppConfig appConfig) { // get config from form
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final String javaVersion = this.useJava11.isSelected() ? SpringCloudJavaVersion.JAVA_11 : SpringCloudJavaVersion.JAVA_8;
        appConfig.setIsPublic("disable".equals(this.toggleEndpoint.getActionCommand()));
        deploymentConfig.setRuntimeVersion(javaVersion);
        deploymentConfig.setEnablePersistentStorage("disable".equals(this.toggleStorage.getActionCommand()));
        deploymentConfig.setCpu(numCpu.getItem());
        deploymentConfig.setMemoryInGB(numMemory.getItem());
        deploymentConfig.setInstanceCount(numInstance.getValue());
        deploymentConfig.setJvmOptions(Optional.ofNullable(this.txtJvmOptions.getText()).map(String::trim).orElse(""));
        deploymentConfig.setEnvironment(Optional.ofNullable(envTable.getEnvironmentVariables()).orElse(new HashMap<>()));
        return appConfig;
    }

    @Override
    public void setData(SpringCloudAppConfig config) {
        this.originalConfig = config;
        final SpringCloudDeploymentConfig deployment = config.getDeployment();
        this.toggleStorage(deployment.getEnablePersistentStorage());
        this.toggleEndpoint(config.getIsPublic());
        final boolean useJava11 = StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(), SpringCloudJavaVersion.JAVA_11);
        this.useJava11.setSelected(useJava11);
        this.useJava8.setSelected(!useJava11);

        this.txtJvmOptions.setText(deployment.getJvmOptions());
        if (MapUtils.isNotEmpty(deployment.getEnvironment())) {
            this.envTable.setEnvironmentVariables(deployment.getEnvironment());
        }

        this.numCpu.setItem(Optional.ofNullable(deployment.getCpu()).orElse(1));
        this.numMemory.setItem(Optional.ofNullable(deployment.getMemoryInGB()).orElse(1));
        this.numInstance.setValue(Optional.ofNullable(deployment.getInstanceCount()).orElse(1));
    }

    @Nonnull
    public SpringCloudAppConfig getData() {
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder()
                .deployment(SpringCloudDeploymentConfig.builder().build())
                .build();
        this.getData(appConfig);
        return appConfig;
    }

    public void setEnabled(boolean enable) {
        this.useJava8.setEnabled(enable);
        this.useJava11.setEnabled(enable);
        this.toggleEndpoint.setEnabled(enable);
        this.toggleStorage.setEnabled(enable);
        numCpu.setEnabled(enable);
        numMemory.setEnabled(enable);
        numInstance.setEnabled(enable);
        envTable.setEnabled(enable);
        txtJvmOptions.setEnabled(enable);
    }

    private void toggleStorage(boolean enabled) {
        this.toggleStorage.setActionCommand(enabled ? "disable" : "enable");
        this.toggleStorage.setText(enabled ? "Disable" : "Enable");
        this.statusStorage.setText("");
        if (this.originalConfig.getDeployment().isEnablePersistentStorage() != enabled) {
            this.statusStorage.setForeground(UIUtil.getContextHelpForeground());
            this.statusStorage.setText(enabled ? "<to be enabled>" : "<to be disabled>");
        }
    }

    private void toggleEndpoint(boolean enabled) {
        this.toggleEndpoint.setActionCommand(enabled ? "disable" : "enable");
        this.toggleEndpoint.setText(enabled ? "Disable" : "Enable");
        this.statusEndpoint.setText("");
        if (this.originalConfig.isPublic() != enabled) {
            this.statusEndpoint.setForeground(UIUtil.getContextHelpForeground());
            this.statusEndpoint.setText(enabled ? "<to be enabled>" : "<to be disabled>");
        }
    }
}
