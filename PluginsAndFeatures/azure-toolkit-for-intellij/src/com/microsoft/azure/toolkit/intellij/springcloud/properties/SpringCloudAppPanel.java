/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentInstanceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudSku;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import lombok.Getter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpringCloudAppPanel extends JPanel implements AzureFormPanel<SpringCloudAppConfig> {
    @Nullable
    private SpringCloudApp app;

    @Getter
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private SpringCloudAppComboBox selectorApp;
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
    private JBTable tableInstances;

    private BiConsumer<? super Boolean, ? super SpringCloudAppConfig> listener = (aBoolean, springCloudAppConfig) -> System.out.println(aBoolean);
    private SpringCloudAppConfig originalConfig;

    public SpringCloudAppPanel(@Nullable SpringCloudApp app, @Nonnull final Project project) {
        super();
        this.init(app);
    }

    private void init(SpringCloudApp origin) {
        this.selectorSubscription.addValueListener(s -> this.selectorCluster.setSubscription(s));
        this.selectorSubscription.setRequired(true);
        this.selectorSubscription.setLabel("Subscription");
        this.selectorCluster.addValueListener(c -> this.selectorApp.setCluster(c));
        this.selectorCluster.setRequired(true);
        this.selectorCluster.setLabel("Spring Cloud");
        this.selectorApp.addValueListener(this::setApp);
        this.selectorApp.setRequired(true);
        this.selectorApp.setLabel("App");

        final TailingDebouncer debouncer = new TailingDebouncer(this::onDataChanged, 300);

        this.toggleStorage.addActionListener(e -> {
            toggleStorage("enable".equals(e.getActionCommand()),
                Optional.ofNullable(this.app).map(a -> a.entity().getPersistentDisk()).orElse(null));
            debouncer.debounce();
        });
        this.toggleEndpoint.addActionListener(e -> {
            toggleEndpoint("enable".equals(e.getActionCommand()),
                Optional.ofNullable(this.app).map(a -> a.entity().getApplicationUrl()).orElse(null));
            debouncer.debounce();
        });

        final DefaultTableModel model = new DefaultTableModel() {
            public boolean isCellEditable(int var1, int var2) {
                return false;
            }
        };
        model.addColumn("App Instances Name");
        model.addColumn("Status");
        model.addColumn("Discover Status");
        tableInstances.setModel(model);
        tableInstances.setRowSelectionAllowed(true);
        tableInstances.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableInstances.getEmptyText().setText("Loading instances");

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

        final boolean appFixed = Objects.nonNull(origin);
        this.selectorSubscription.setValueFixed(appFixed);
        this.selectorCluster.setValueFixed(appFixed);
        this.selectorApp.setValueFixed(appFixed);

        if (appFixed) { // app is specified on construction(show properties)
            this.selectorCluster.setItemsLoader(() -> Collections.singletonList(origin.getCluster()));
            this.selectorApp.setItemsLoader(() -> Collections.singletonList(origin));
            this.setApp(origin);
        }
    }

    public void setValueChangedListener(BiConsumer<? super Boolean, ? super SpringCloudAppConfig> listener) {
        this.listener = listener;
    }

    public void refresh() {
        Objects.requireNonNull(this.app).refresh();
        AzureTaskManager.getInstance().runLater(this::updateForm);
    }

    public void reset() {
        AzureTaskManager.getInstance().runLater(() -> Optional.ofNullable(this.originalConfig).ifPresent(this::setData));
    }

    private void setApp(@Nonnull SpringCloudApp app) {
        if (Objects.equals(app, this.app)) {
            return;
        }
        this.app = app;
        AzureTaskManager.getInstance().runLater(this::updateForm);
    }

    private void updateForm() {
        assert Objects.nonNull(app): "app is not specified";
        final String testUrl = app.entity().getTestUrl();
        this.txtTestEndpoint.setHyperlinkText(testUrl.length() > 60 ? testUrl.substring(0, 60) + "..." : testUrl);
        this.txtTestEndpoint.setHyperlinkTarget(testUrl);

        final SpringCloudSku sku = app.getCluster().entity().getSku();
        final boolean basic = sku.getTier().toLowerCase().startsWith("b");
        final DefaultComboBoxModel<Integer> numCpuModel = (DefaultComboBoxModel<Integer>) this.numCpu.getModel();
        final DefaultComboBoxModel<Integer> numMemoryModel = (DefaultComboBoxModel<Integer>) this.numMemory.getModel();
        numCpuModel.removeAllElements();
        numCpuModel.addAll(IntStream.range(1, 1 + (basic ? 1 : 4)).boxed().collect(Collectors.toList()));
        numMemoryModel.removeAllElements();
        numMemoryModel.addAll(IntStream.range(1, 1 + (basic ? 2 : 8)).boxed().collect(Collectors.toList()));
        this.numInstance.setMaximum(basic ? 25 : 500);
        this.numInstance.setMajorTickSpacing(basic ? 5 : 50);
        this.numInstance.setMinorTickSpacing(basic ? 1 : 10);
        this.numInstance.setMinimum(0);
        this.numInstance.updateLabels();
        final SpringCloudDeploymentEntity deploymentEntity = Optional.ofNullable(app.activeDeployment()).stream().findAny()
            .or(() -> app.deployments().stream().findAny())
            .map(SpringCloudDeployment::entity)
            .orElse(new SpringCloudDeploymentEntity("default", app.entity()));
        final List<SpringCloudDeploymentInstanceEntity> instances = deploymentEntity.getInstances();
        this.numInstance.setRealMin(Math.min(instances.size(), 1));

        final DefaultTableModel model = (DefaultTableModel) this.tableInstances.getModel();
        model.setRowCount(0);
        instances.forEach(i -> model.addRow(new Object[]{i.getName(), i.status(), i.discoveryStatus()}));
        final int rows = model.getRowCount() < 5 ? 5 : instances.size();
        model.setRowCount(rows);
        this.tableInstances.setVisibleRowCount(rows);

        this.originalConfig = toConfig(app);
        this.setData(this.originalConfig);
    }

    private void onDataChanged() {
        if (Objects.nonNull(this.originalConfig) && Objects.nonNull(this.listener) && Objects.nonNull(this.app)) {
            final SpringCloudAppConfig newData = this.getData();
            this.listener.accept(!this.originalConfig.equals(newData), newData);
        }
    }

    private SpringCloudAppConfig toConfig(@NotNull SpringCloudApp app) { // get config from app
        final SpringCloudAppEntity appEntity = app.entity();
        final SpringCloudPersistentDisk disk = appEntity.getPersistentDisk();
        final SpringCloudDeploymentEntity deploymentEntity = Optional.ofNullable(app.activeDeployment()).stream().findAny()
            .or(() -> app.deployments().stream().findAny())
            .map(SpringCloudDeployment::entity)
            .orElse(new SpringCloudDeploymentEntity("default", app.entity()));
        final List<SpringCloudDeploymentInstanceEntity> instances = deploymentEntity.getInstances();

        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.builder().build();
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder().deployment(deploymentConfig).build();
        appConfig.setSubscriptionId(app.entity().getSubscriptionId());
        appConfig.setClusterName(app.getCluster().name());
        appConfig.setAppName(app.name());
        appConfig.setIsPublic(Objects.equals(app.entity().isPublic(), true));
        deploymentConfig.setRuntimeVersion(deploymentEntity.getRuntimeVersion());
        deploymentConfig.setEnablePersistentStorage(Objects.nonNull(disk) && disk.getSizeInGB() > 0);
        deploymentConfig.setCpu(deploymentEntity.getCpu());
        deploymentConfig.setMemoryInGB(deploymentEntity.getMemoryInGB());
        deploymentConfig.setInstanceCount(instances.size());
        deploymentConfig.setJvmOptions(Optional.ofNullable(deploymentEntity.getJvmOptions()).map(String::trim).orElse(""));
        deploymentConfig.setEnvironment(Optional.ofNullable(deploymentEntity.getEnvironmentVariables()).orElse(new HashMap<>()));
        return appConfig;
    }

    @Contract("_->_")
    public SpringCloudAppConfig getData(@Nonnull SpringCloudAppConfig appConfig) { // get config from form
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final String javaVersion = this.useJava11.isSelected() ? SpringCloudJavaVersion.JAVA_11 : SpringCloudJavaVersion.JAVA_8;
        appConfig.setSubscriptionId(Optional.ofNullable(this.selectorSubscription.getValue()).map(Subscription::getId).orElse(null));
        appConfig.setClusterName(Optional.ofNullable(this.selectorCluster.getValue()).map(IAzureEntityManager::name).orElse(null));
        appConfig.setAppName(Optional.ofNullable(this.selectorApp.getValue()).map(IAzureEntityManager::name).orElse(null));
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
    public void setData(SpringCloudAppConfig app) {
        final SpringCloudDeploymentConfig deployment = app.getDeployment();
        Optional.ofNullable(app.getSubscriptionId())
            .ifPresent((id -> this.selectorSubscription.setValue(new ItemReference<>(id, Subscription::getId))));
        Optional.ofNullable(app.getClusterName())
            .ifPresent((name -> this.selectorCluster.setValue(new ItemReference<>(name, SpringCloudCluster::name))));
        Optional.ofNullable(app.getAppName())
            .ifPresent((name -> this.selectorApp.setValue(new ItemReference<>(name, SpringCloudApp::name))));
        this.toggleStorage(deployment.getEnablePersistentStorage(), Optional.ofNullable(this.app).map(a -> a.entity().getPersistentDisk()).orElse(null));
        this.toggleEndpoint(app.getIsPublic(), Optional.ofNullable(this.app).map(a -> a.entity().getApplicationUrl()).orElse(null));
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
        tableInstances.setEnabled(enable);
    }

    private void toggleStorage(boolean enabled, @Nullable SpringCloudPersistentDisk disk) {
        if (enabled) {
            this.toggleStorage.setActionCommand("disable");
            this.toggleStorage.setText("Disable");
            this.txtStorage.setText(Optional.ofNullable(disk).map(Object::toString).orElse("<save to enable>"));
        } else {
            this.toggleStorage.setActionCommand("enable");
            this.toggleStorage.setText("Enable");
            this.txtStorage.setText("---");
        }
    }

    private void toggleEndpoint(boolean enabled, @Nullable String url) {
        if (enabled) {
            this.toggleEndpoint.setActionCommand("disable");
            this.toggleEndpoint.setText("Disable");
            if (Objects.nonNull(url)) {
                this.txtEndpoint.setHyperlinkTarget(url);
                this.txtEndpoint.setHyperlinkText(url);
                this.txtEndpoint.setEnabled(true);
            } else {
                this.txtEndpoint.setEnabled(false);
                this.txtEndpoint.setText("<save to enable>");
            }
        } else {
            this.toggleEndpoint.setActionCommand("enable");
            this.toggleEndpoint.setText("Enable");
            this.txtEndpoint.setHyperlinkTarget(null);
            this.txtEndpoint.setIcon(null);
            this.txtEndpoint.setText("---");
            this.txtEndpoint.setEnabled(false);
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.selectorSubscription,
            this.selectorCluster,
            this.selectorApp
        };
        return Arrays.asList(inputs);
    }

    private void createUIComponents() {
    }
}
