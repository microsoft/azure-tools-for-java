/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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
    private JRadioButton useJava17;
    private JTextField txtJvmOptions;
    private EnvironmentVariablesTextFieldWithBrowseButton envTable;
    private ComboBox<Double> numCpu;
    private ComboBox<Double> numMemory;
    private AzureSlider numInstance;
    private JBLabel statusEndpoint;
    private JBLabel statusStorage;
    private JLabel lblTestEndpoint;
    private JLabel lblRuntime;

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
        this.useJava17.addActionListener((e) -> debouncer.debounce());
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

        this.txtTestEndpoint.setVisible(false);
        this.lblTestEndpoint.setVisible(false);
        this.txtTestEndpoint.setHyperlinkTarget(null);
        final DefaultComboBoxModel<Double> numCpuModel = new DefaultComboBoxModel<>(new Double[]{0.5, 1.0});
        final DefaultComboBoxModel<Double> numMemoryModel = new DefaultComboBoxModel<>(new Double[]{0.5, 1.0, 2.0});
        numCpuModel.setSelectedItem(1.0);
        numMemoryModel.setSelectedItem(1.0);
        this.numCpu.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@NotNull JList<? extends Double> list, Double value, int index, boolean selected, boolean hasFocus) {
                setText(value < 1 ? value + "" : value.intValue() + "");
            }
        });
        this.numMemory.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@NotNull JList<? extends Double> list, Double value, int index, boolean selected, boolean hasFocus) {
                setText(value < 1 ? Double.valueOf(value * 1024).intValue() + "Mi" : value.intValue() + "Gi");
            }
        });
        this.numCpu.setModel(numCpuModel);
        this.numMemory.setModel(numMemoryModel);

    }

    public void reset() {
        AzureTaskManager.getInstance().runLater(() -> Optional.ofNullable(this.originalConfig).ifPresent(this::setValue));
    }

    public void setDataChangedListener(Consumer<? super SpringCloudAppConfig> listener) {
        this.listener = listener;
    }

    private void onDataChanged() {
        if (Objects.nonNull(this.originalConfig) && Objects.nonNull(this.listener)) {
            final SpringCloudAppConfig newConfig = this.getValue();
            this.listener.accept(newConfig);
        }
    }

    public synchronized void updateForm(@Nonnull SpringCloudApp app) {
        AzureTaskManager.getInstance().runInBackground(AzureString.format("load properties of app(%s)", app.name()), () -> {
            final String testUrl = app.getTestUrl();
            final SpringCloudPersistentDisk disk = app.getPersistentDisk();
            final String url = app.getApplicationUrl();
            AzureTaskManager.getInstance().runLater(() -> {
                if (testUrl != null) {
                    this.txtTestEndpoint.setHyperlinkText(testUrl.length() > 60 ? testUrl.substring(0, 60) + "..." : testUrl);
                    this.txtTestEndpoint.setHyperlinkTarget(testUrl.endsWith("/") ? testUrl.substring(0, testUrl.length() - 1) : testUrl);
                    this.txtTestEndpoint.setVisible(true);
                    this.lblTestEndpoint.setVisible(true);
                } else {
                    this.txtTestEndpoint.setVisible(false);
                    this.lblTestEndpoint.setVisible(false);
                    this.txtTestEndpoint.setHyperlinkTarget(null);
                }
                this.txtStorage.setText(Objects.nonNull(disk) ? disk.toString() : "---");
                this.txtEndpoint.setHyperlinkTarget(url);
                this.txtEndpoint.setEnabled(Objects.nonNull(url));
                if (Objects.nonNull(url)) {
                    this.txtEndpoint.setHyperlinkText(url);
                } else {
                    this.txtEndpoint.setIcon(null);
                    this.txtEndpoint.setText("---");
                }
            }, AzureTask.Modality.ANY);
        });
        final String sku = app.getParent().getSku();
        final boolean enterprise = sku.toLowerCase().startsWith("e");
        this.useJava8.setVisible(!enterprise);
        this.useJava11.setVisible(!enterprise);
        this.useJava17.setVisible(!enterprise);
        this.lblRuntime.setVisible(!enterprise);
        final boolean basic = sku.toLowerCase().startsWith("b");
        final Double cpu = this.numCpu.getItem();
        final Double mem = this.numMemory.getItem();
        final Double[] cpus = basic ? new Double[]{0.5, 1.0} : new Double[]{0.5, 1.0, 2.0, 3.0, 4.0};
        final Double[] mems = basic ? new Double[]{0.5, 1.0, 2.0} : new Double[]{0.5, 1.0, 2.0, 3.0, 4.0, 5., 6.0, 7.0, 8.0};
        final DefaultComboBoxModel<Double> numCpuModel = new DefaultComboBoxModel<>(cpus);
        final DefaultComboBoxModel<Double> numMemoryModel = new DefaultComboBoxModel<>(mems);
        numCpuModel.setSelectedItem(Objects.isNull(cpu) ? 1.0 : (cpu > (basic ? 1 : 4)) ? null : cpu);
        numMemoryModel.setSelectedItem(Objects.isNull(mem) ? 1.0 : mem > (basic ? 2 : 8) ? null : mem);
        this.numCpu.setModel(numCpuModel);
        this.numMemory.setModel(numMemoryModel);
        this.numInstance.setMaximum(basic ? 25 : 500);
        this.numInstance.setMajorTickSpacing(basic ? 5 : 50);
        this.numInstance.setMinorTickSpacing(basic ? 1 : 10);
        this.numInstance.setMinimum(0);
        this.numInstance.updateLabels();
    }

    @Contract("_->_")
    public SpringCloudAppConfig getValue(@Nonnull SpringCloudAppConfig appConfig) { // get config from form
        final SpringCloudDeploymentConfig deploymentConfig = Optional.ofNullable(appConfig.getDeployment())
            .orElse(SpringCloudDeploymentConfig.builder().build());
        if (this.useJava17.isVisible()) {
            final String javaVersion = this.useJava17.isSelected() ? RuntimeVersion.JAVA_17.toString() :
                this.useJava11.isSelected() ? RuntimeVersion.JAVA_11.toString() : RuntimeVersion.JAVA_8.toString();
            deploymentConfig.setRuntimeVersion(javaVersion);
        } else {
            deploymentConfig.setRuntimeVersion(null);
        }
        appConfig.setIsPublic("disable".equals(this.toggleEndpoint.getActionCommand()));
        deploymentConfig.setEnablePersistentStorage("disable".equals(this.toggleStorage.getActionCommand()));
        deploymentConfig.setCpu(numCpu.getItem());
        deploymentConfig.setMemoryInGB(numMemory.getItem());
        deploymentConfig.setInstanceCount(numInstance.getValue());
        deploymentConfig.setJvmOptions(Optional.ofNullable(this.txtJvmOptions.getText()).map(String::trim).orElse(""));
        deploymentConfig.setEnvironment(Optional.ofNullable(envTable.getEnvironmentVariables()).orElse(new HashMap<>()));
        appConfig.setDeployment(deploymentConfig);
        return appConfig;
    }

    @Override
    public synchronized void setValue(SpringCloudAppConfig config) {
        this.originalConfig = config;
        final SpringCloudDeploymentConfig deployment = config.getDeployment();
        this.toggleStorage(deployment.getEnablePersistentStorage());
        this.toggleEndpoint(config.getIsPublic());
        this.useJava17.setSelected(StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(), RuntimeVersion.JAVA_17.toString()));
        this.useJava11.setSelected(StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(), RuntimeVersion.JAVA_11.toString()));
        this.useJava8.setSelected(StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(), RuntimeVersion.JAVA_8.toString()));

        this.txtJvmOptions.setText(deployment.getJvmOptions());
        final Map<String, String> env = deployment.getEnvironment();
        this.envTable.setEnvironmentVariables(ObjectUtils.firstNonNull(env, Collections.emptyMap()));

        Optional.ofNullable(deployment.getCpu()).ifPresent(c -> this.numCpu.setItem(c));
        Optional.ofNullable(deployment.getMemoryInGB()).ifPresent(c -> this.numMemory.setItem(c));
        this.numInstance.setValue(deployment.getInstanceCount());
    }

    @Nonnull
    @Override
    public SpringCloudAppConfig getValue() {
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder()
                .deployment(SpringCloudDeploymentConfig.builder().build())
                .build();
        this.getValue(appConfig);
        return appConfig;
    }

    public void setEnabled(boolean enable) {
        this.useJava8.setEnabled(enable);
        this.useJava11.setEnabled(enable);
        this.useJava17.setEnabled(enable);
        this.toggleEndpoint.setEnabled(enable);
        this.toggleStorage.setEnabled(enable);
        numCpu.setEnabled(enable);
        numMemory.setEnabled(enable);
        numInstance.setEnabled(enable);
        envTable.setEnabled(enable);
        txtJvmOptions.setEnabled(enable);
    }

    private void toggleStorage(Boolean e) {
        if (Objects.isNull(this.originalConfig)) { // prevent action before data is loaded.
            return;
        }
        final boolean enabled = BooleanUtils.isTrue(e);
        this.toggleStorage.setActionCommand(enabled ? "disable" : "enable");
        this.toggleStorage.setText(enabled ? "Disable" : "Enable");
        this.statusStorage.setText("");
        if (this.originalConfig.getDeployment().isEnablePersistentStorage() != enabled) {
            this.statusStorage.setForeground(UIUtil.getContextHelpForeground());
            this.statusStorage.setText(enabled ? "<to be enabled>" : "<to be disabled>");
        }
    }

    private void toggleEndpoint(Boolean e) {
        if (Objects.isNull(this.originalConfig)) { // prevent action before data is loaded.
            return;
        }
        final boolean enabled = BooleanUtils.isTrue(e);
        this.toggleEndpoint.setActionCommand(enabled ? "disable" : "enable");
        this.toggleEndpoint.setText(enabled ? "Disable" : "Enable");
        this.statusEndpoint.setText("");
        if (this.originalConfig.isPublic() != enabled) {
            this.statusEndpoint.setForeground(UIUtil.getContextHelpForeground());
            this.statusEndpoint.setText(enabled ? "<to be enabled>" : "<to be disabled>");
        }
    }
}
