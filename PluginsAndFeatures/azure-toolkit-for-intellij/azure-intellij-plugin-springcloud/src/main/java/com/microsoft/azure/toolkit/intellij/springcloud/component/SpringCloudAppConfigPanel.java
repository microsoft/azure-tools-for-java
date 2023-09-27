/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudAppConfigPanel extends JPanel implements AzureFormPanel<SpringCloudAppDraft> {
    @Getter
    private JPanel contentPanel;
    private HyperlinkLabel txtEndpoint;
    private AzureActionButton<Void> toggleEndpoint;
    private HyperlinkLabel txtTestEndpoint;
    private JBLabel txtStorage;
    private AzureActionButton<Void> toggleStorage;
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
    private JLabel lblDisk;
    private JPanel pnlDisk;
    private JLabel lblInstance;
    @Setter
    private String defaultRuntimeVersion = SpringCloudDeploymentDraft.DEFAULT_RUNTIME_VERSION.toString();

    @Setter
    private Runnable dataChangedListener = () -> {
    };

    public SpringCloudAppConfigPanel() {
        super();
        this.init();
    }

    private void init() {
        final TailingDebouncer debouncer = new TailingDebouncer(this::onDataChanged, 300);
        final Action<Void> toggleStorageAction = new Action<Void>(Action.Id.of("user/springcloud.toggle_storage"))
                .withAuthRequired(false)
                .withHandler((Void ignore, AnActionEvent event) -> {
                    final ActionEvent e = event.getData(AzureActionButton.ACTION_EVENT_KEY);
                    final String actionCommand = Optional.ofNullable(e).map(ActionEvent::getActionCommand).orElse(StringUtils.EMPTY);
                    toggleStorage("enable".equals(actionCommand));
                    debouncer.debounce();
                });
        this.toggleStorage.setAction(toggleStorageAction);

        final Action<Void> toggleEndpointAction = new Action<Void>(Action.Id.of("user/springcloud.toggle_endpoint"))
                .withAuthRequired(false)
                .withHandler((Void ignore, AnActionEvent event) -> {
                    final ActionEvent e = event.getData(AzureActionButton.ACTION_EVENT_KEY);
                    final String actionCommand = Optional.ofNullable(e).map(ActionEvent::getActionCommand).orElse(StringUtils.EMPTY);
                    toggleEndpoint("enable".equals(actionCommand));
                    debouncer.debounce();
                });
        this.toggleEndpoint.setAction(toggleEndpointAction);

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
        numMemoryModel.setSelectedItem(2.0);
        this.numMemory.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@Nonnull JList<? extends Double> list, Double value, int index, boolean selected, boolean hasFocus) {
                Optional.ofNullable(value)
                    .map(v -> v < 1 ? Double.valueOf(v * 1024).intValue() + "Mi" : v + "Gi")
                    .ifPresentOrElse(this::setText, () -> setText(""));
            }
        });
        this.numCpu.setModel(numCpuModel);
        this.numMemory.setModel(numMemoryModel);
    }

    private void onDataChanged() {
        Optional.ofNullable(this.dataChangedListener).ifPresent(Runnable::run);
    }

    public synchronized void updateForm(@Nonnull SpringCloudApp app) {
        AzureTaskManager.getInstance().runInBackground(AzureString.format("load properties of app(%s)", app.getName()), () -> {
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
                    this.txtEndpoint.setHyperlinkText(url.length() > 60 ? url.substring(0, 60) + "..." : url);
                } else {
                    this.txtEndpoint.setIcon(null);
                    this.txtEndpoint.setText("---");
                }
            }, AzureTask.Modality.ANY);
        });
        final SpringCloudCluster service = app.getParent();
        final boolean enterprise = service.isEnterpriseTier();
        final boolean consumption = service.isConsumptionTier();
        final boolean standard = service.isStandardTier();
        final boolean basic = !enterprise && !consumption && !standard;
        this.useJava8.setVisible(!enterprise);
        this.useJava11.setVisible(!enterprise);
        this.useJava17.setVisible(!enterprise);
        this.lblRuntime.setVisible(!enterprise);
        this.lblDisk.setVisible(!enterprise && !consumption);
        this.pnlDisk.setVisible(!enterprise && !consumption);
        this.lblInstance.setText("Instances:");
        final Double cpu = this.numCpu.getItem();
        final Double mem = this.numMemory.getItem();
        final Double[] cpus = basic ? new Double[]{0.5, 1.0} : consumption ? new Double[]{0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0} : new Double[]{0.5, 1.0, 2.0, 3.0, 4.0};
        final Double[] mems = basic ? new Double[]{0.5, 1.0, 2.0} : consumption ? new Double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0} : new Double[]{0.5, 1.0, 2.0, 3.0, 4.0, 5., 6.0, 7.0, 8.0};
        final DefaultComboBoxModel<Double> numCpuModel = new DefaultComboBoxModel<>(cpus);
        final DefaultComboBoxModel<Double> numMemoryModel = new DefaultComboBoxModel<>(mems);
        numCpuModel.setSelectedItem(Objects.isNull(cpu) ? Double.valueOf(1.0) : (cpu > cpus[cpus.length - 1]) ? null : cpu);
        numMemoryModel.setSelectedItem(Objects.isNull(mem) ? Double.valueOf(2.0) : mem > mems[mems.length - 1] ? null : mem);
        if (consumption) {
            this.numMemory.addActionListener(e -> Optional.ofNullable((Double) numMemoryModel.getSelectedItem()).ifPresent(m -> numCpuModel.setSelectedItem(m / 2)));
            this.numCpu.addActionListener(e -> Optional.ofNullable((Double) numCpuModel.getSelectedItem()).ifPresent(c -> numMemoryModel.setSelectedItem(c * 2)));
            this.lblInstance.setText("Max replicas:");
        }
        this.numCpu.setModel(numCpuModel);
        this.numMemory.setModel(numMemoryModel);
        this.numInstance.setMaximum(basic ? 25 : consumption ? 30 : 500);
        this.numInstance.setMajorTickSpacing(basic || consumption ? 5 : 50);
        this.numInstance.setMinorTickSpacing(basic || consumption ? 1 : 10);
        this.numInstance.setMinimum(0);
        this.numInstance.updateLabels();
    }

    public void applyTo(@Nonnull SpringCloudAppDraft app) {
        final SpringCloudDeploymentDraft deployment = app.updateOrCreateActiveDeployment();
        final boolean isEnterpriseTier = this.useJava17.isVisible();
        if (isEnterpriseTier) {
            final String javaVersion = this.useJava17.isSelected() ? RuntimeVersion.JAVA_17.toString() :
                this.useJava11.isSelected() ? RuntimeVersion.JAVA_11.toString() : RuntimeVersion.JAVA_8.toString();
            deployment.setRuntimeVersion(javaVersion);
            app.setPersistentDiskEnabled("disable".equals(this.toggleStorage.getActionCommand()));
        } else {
            deployment.setRuntimeVersion(null);
            app.setPersistentDiskEnabled(false);
        }
        app.setPublicEndpointEnabled("disable".equals(this.toggleEndpoint.getActionCommand()));
        deployment.setCpu(numCpu.getItem());
        deployment.setMemoryInGB(numMemory.getItem());
        deployment.setCapacity(numInstance.getValue());
        deployment.setJvmOptions(Optional.ofNullable(this.txtJvmOptions.getText()).map(String::trim).orElse(""));
        deployment.setEnvironmentVariables(Optional.ofNullable(envTable.getEnvironmentVariables()).orElse(new HashMap<>()));
    }

    @Override
    public synchronized void setValue(@Nullable SpringCloudAppDraft app) {
        if (Objects.isNull(app)) {
            return;
        }
        final SpringCloudDeploymentDraft deployment = app.updateOrCreateActiveDeployment();
        this.statusStorage.putClientProperty("origin.enabled", app.isPersistentDiskEnabled());
        this.statusEndpoint.putClientProperty("origin.enabled", app.isPublicEndpointEnabled());
        this.toggleStorage(app.isPersistentDiskEnabled());
        this.toggleEndpoint(app.isPublicEndpointEnabled());
        final String runtime = Optional.ofNullable(deployment.getRuntimeVersion())
            .or(() -> Optional.ofNullable(this.defaultRuntimeVersion))
            .orElse(SpringCloudDeploymentDraft.DEFAULT_RUNTIME_VERSION.toString());
        this.useJava17.setSelected(StringUtils.equalsIgnoreCase(runtime, RuntimeVersion.JAVA_17.toString()));
        this.useJava11.setSelected(StringUtils.equalsIgnoreCase(runtime, RuntimeVersion.JAVA_11.toString()));
        this.useJava8.setSelected(StringUtils.equalsIgnoreCase(runtime, RuntimeVersion.JAVA_8.toString()));

        this.txtJvmOptions.setText(deployment.getJvmOptions());
        final Map<String, String> env = deployment.getEnvironmentVariables();
        this.envTable.setEnvironmentVariables(ObjectUtils.firstNonNull(env, Collections.emptyMap()));

        if (deployment.isDraftForCreating()) {
            this.numCpu.setItem(Optional.ofNullable(deployment.getCpu()).orElse(SpringCloudDeploymentDraft.DEFAULT_CPU));
            this.numMemory.setItem(Optional.ofNullable(deployment.getMemoryInGB()).orElse(SpringCloudDeploymentDraft.DEFAULT_MEMORY));
            this.numInstance.setValue(Optional.ofNullable(deployment.getCapacity()).orElse(SpringCloudDeploymentDraft.DEFAULT_CAPACITY));
        } else {
            this.numCpu.setItem(Optional.ofNullable(deployment.getCpu()).orElse(0d));
            this.numMemory.setItem(Optional.ofNullable(deployment.getMemoryInGB()).orElse(0d));
            this.numInstance.setValue(Optional.ofNullable(deployment.getCapacity()).orElse(0));
        }
    }

    @Nonnull
    @Override
    public SpringCloudAppDraft getValue() {
        throw new AzureToolkitRuntimeException("Not supported, use `applyTo` instead.");
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
        final boolean enabled = BooleanUtils.isTrue(e);
        this.toggleStorage.setActionCommand(enabled ? "disable" : "enable");
        this.toggleStorage.setText(enabled ? "Disable" : "Enable");
        this.statusStorage.setText("");
        if (BooleanUtils.isTrue((Boolean) this.statusStorage.getClientProperty("origin.enabled")) != enabled) {
            this.statusStorage.setForeground(UIUtil.getContextHelpForeground());
            this.statusStorage.setText(enabled ? "<to be enabled>" : "<to be disabled>");
        }
    }

    private void toggleEndpoint(Boolean e) {
        final boolean enabled = BooleanUtils.isTrue(e);
        this.toggleEndpoint.setActionCommand(enabled ? "disable" : "enable");
        this.toggleEndpoint.setText(enabled ? "Disable" : "Enable");
        this.statusEndpoint.setText("");
        if (BooleanUtils.isTrue((Boolean) this.statusEndpoint.getClientProperty("origin.enabled")) != enabled) {
            this.statusEndpoint.setForeground(UIUtil.getContextHelpForeground());
            this.statusEndpoint.setText(enabled ? "<to be enabled>" : "<to be disabled>");
        }
    }
}
