/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.ProxyResource;
import com.microsoft.azure.management.appplatform.v2020_07_01.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.springcloud.AzureResourceWrapper;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariableTable;
import com.microsoft.azure.toolkit.intellij.springcloud.SpringCloudAppCreationDialog;
import com.microsoft.azure.toolkit.intellij.springcloud.SpringCloudModel;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.intellij.CommonConst;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.nimbusds.oauth2.sdk.util.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import rx.Observable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class SpringCloudDeploymentConfigurationPanel extends AzureSettingPanel<SpringCloudDeploymentConfiguration> {
    private final SpringCloudDeploymentConfiguration configuration;
    private static final String CREATE_APP = "Create app ...";
    private JPanel panelRoot;
    private JComboBox<AzureArtifact> cbMavenProject;
    private JLabel lblMavenProject;
    private JComboBox cbSpringApps;
    private JRadioButton java8RadioButton;
    private JComboBox cbCPU;
    private JComboBox cbMemory;
    private JTextField textJvmOptions;
    private JComboBox cbInstanceCount;
    private JRadioButton java11RadioButton;
    private JRadioButton radioEnablePersistent;
    private JRadioButton radioDisablePersistent;
    private JLabel lblCpu;
    private JLabel lblMemory;
    private JLabel lblInstanceCount;
    private JLabel lblJvmOptions;
    private JLabel lblPersistent;
    private JComboBox cbSubscription;
    private JComboBox cbClusters;
    private JRadioButton radioPublic;
    private JRadioButton radioNonPublic;
    private JLabel lblEnvVar;
    private JPanel pnlEnvironmentTable;
    private JComboBox cbArtifact;
    private JLabel lblArtifact;
    private EnvironmentVariableTable environmentVariableTable;

    private String lastSelectedSubsId;
    private String lastSelectedClusterId;

    private boolean isAppInitialized = false;
    private boolean isClusterInitialized = false;

    public SpringCloudDeploymentConfigurationPanel(@NotNull Project project, @NotNull SpringCloudDeploymentConfiguration configuration) {
        super(project, false);
        this.configuration = configuration;
        cbSubscription.setRenderer(new SimpleListCellRenderer<Object>() {
            @Override
            public void customize(JList list, Object subscription, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (subscription instanceof Subscription) {
                    setText(((Subscription) subscription).displayName());
                } else {
                    setText(Objects.toString(subscription, ""));
                }
            }
        });
        cbClusters.setRenderer(new SimpleListCellRenderer<Object>() {
            @Override
            public void customize(JList list, Object cluster, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (cluster instanceof ServiceResourceInner) {
                    setText(((ServiceResourceInner) cluster).name());
                } else {
                    setText(Objects.toString(cluster, ""));
                }
            }
        });

        cbSpringApps.setRenderer(new SimpleListCellRenderer<Object>() {
            @Override
            public void customize(JList list, Object app, int
                index, boolean isSelected, boolean cellHasFocus) {
                setText(Objects.toString(app, ""));
            }
        });

        cbSubscription.addActionListener(e -> {
            if (!(cbSubscription.getSelectedItem() instanceof Subscription)) {
                return;
            }
            final Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
            if (subscription == null) {
                return;
            }
            final String selectedSid = subscription.subscriptionId();
            if (!Comparing.equal(lastSelectedSubsId, selectedSid)) {
                initCluster(SpringCloudIdHelper.getClusterName(configuration.getModel().getClusterId()));
                cbClusters.setEnabled(false);
                final SchedulerProvider scheduler = SchedulerProviderFactory.getInstance().getSchedulerProvider();
                Observable.fromCallable(() -> AzureSpringCloudMvpModel.listAllSpringCloudClustersBySubscription(selectedSid))
                    .subscribeOn(scheduler.io())
                    .subscribe(clusters -> DefaultLoader.getIdeHelper().invokeLater(() -> this.fillClusters(clusters)));
                lastSelectedSubsId = selectedSid;
            }
        });

        cbClusters.addActionListener(e -> {
            if (cbClusters.getSelectedItem() instanceof String) {
                return;
            }
            final ServiceResourceInner cluster = (ServiceResourceInner) cbClusters.getSelectedItem();
            if (cluster == null) {
                return;
            }
            final String selectedCid = cluster.id();
            if (!Comparing.equal(lastSelectedClusterId, selectedCid)) {
                initApp(configuration.getModel().getAppName(), configuration.getModel().isCreateNewApp());
                cbSpringApps.setEnabled(false);
                final SchedulerProvider scheduler = SchedulerProviderFactory.getInstance().getSchedulerProvider();
                Observable.fromCallable(() -> AzureSpringCloudMvpModel.listAppsByClusterId(selectedCid))
                    .subscribeOn(scheduler.io())
                    .subscribe(apps -> DefaultLoader.getIdeHelper().invokeLater(() -> this.fillApps(apps)));
                lastSelectedClusterId = selectedCid;
            }
        });
        final ButtonGroup javaButtonGroup = new ButtonGroup();
        javaButtonGroup.add(java8RadioButton);
        javaButtonGroup.add(java11RadioButton);

        final ButtonGroup persistentButtonGroup = new ButtonGroup();
        persistentButtonGroup.add(radioEnablePersistent);
        persistentButtonGroup.add(radioDisablePersistent);

        final ButtonGroup publicButtonGroup = new ButtonGroup();
        publicButtonGroup.add(radioPublic);
        publicButtonGroup.add(radioNonPublic);

        init();
    }

    private void init() {
        final SchedulerProvider scheduler = SchedulerProviderFactory.getInstance().getSchedulerProvider();
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getSelectedSubscriptions())
            .subscribeOn(scheduler.io())
            .subscribe(subscriptions -> DefaultLoader.getIdeHelper().invokeLater(() -> this.fillSubscription(subscriptions)));
        cbSubscription.addItem(CommonConst.REFRESH_TEXT);
    }

    private synchronized void initCluster(String initialValue) {
        if (!isClusterInitialized) {
            cbClusters.removeAllItems();
            if (StringUtils.isNotEmpty(initialValue)) {
                cbClusters.addItem(initialValue + " ...");
            } else {
                cbClusters.addItem(CommonConst.REFRESH_TEXT);
                cbClusters.setSelectedItem(CommonConst.REFRESH_TEXT);
            }
            cbClusters.setEnabled(false);
            isClusterInitialized = true;
        }
    }

    private synchronized void initApp(String initialValue, boolean isCreate) {
        if (!isAppInitialized) {
            cbSpringApps.removeAllItems();
            cbSpringApps.addItem(new AzureResourceWrapper(CREATE_APP, true));

            if (StringUtils.isNotEmpty(initialValue)) {
                final AzureResourceWrapper cacheOne = new AzureResourceWrapper(initialValue, false, isCreate);
                cbSpringApps.addItem(cacheOne);
                cbSpringApps.setSelectedItem(cacheOne);
            } else {
                cbSpringApps.setSelectedIndex(-1);
            }
            cbSpringApps.addPopupMenuListener(new PopupMenuListenerAdapter() {
                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    onSelectApp();
                }
            });
            cbSpringApps.setEnabled(false);
            isAppInitialized = true;
        }
    }

    private void onSelectApp() {
        final AzureResourceWrapper selectedObject = (AzureResourceWrapper) cbSpringApps.getSelectedItem();
        if (selectedObject != null && selectedObject.isFixedOption() && selectedObject.getName().equals(CREATE_APP)) {
            AzureTaskManager.getInstance().runLater(() -> createNewAppWizard());
        }
    }

    private void createNewAppWizard() {
        cbSpringApps.setSelectedItem(null);
        final SpringCloudAppCreationDialog dialog = new SpringCloudAppCreationDialog(null,
            cbSpringApps,
            "Create Azure Spring Cloud app");
        dialog.pack();
        if (dialog.showAndGet()) {
            AzureResourceWrapper newApp = dialog.getNewAppWrapper();
            if (newApp != null) {
                int count = cbSpringApps.getItemCount();
                boolean existed = false;
                for (int i = 0; i < count; i++) {
                    final AzureResourceWrapper option = (AzureResourceWrapper) cbSpringApps.getItemAt(i);
                    if (!option.isFixedOption() && StringUtils.equals(option.getName(), newApp.getName())) {
                        existed = true;
                        newApp = option;
                        break;
                    }
                }
                if (!existed) {
                    cbSpringApps.addItem(newApp);
                    count = cbSpringApps.getItemCount();
                    final List<Object> toRemove = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        final AzureResourceWrapper resource = (AzureResourceWrapper) cbSpringApps.getItemAt(i);
                        if (resource.isFixedOption() && !resource.getName().equals(CREATE_APP)) {
                            toRemove.add(resource);
                        }
                    }
                    for (final Object rem : toRemove) {
                        cbSpringApps.removeItem(rem);
                    }
                }
                cbSpringApps.setSelectedItem(newApp);
            }
        } else {
            final int count = cbSpringApps.getItemCount();
            for (int i = 0; i < count; i++) {
                final AzureResourceWrapper resource = (AzureResourceWrapper) cbSpringApps.getItemAt(i);
                if (resource.isFixedOption() && resource.getName().equals(CREATE_APP)) {
                    continue;
                }
                cbSpringApps.setSelectedItem(resource);
                break;
            }
        }
    }

    public void fillSubscription(@NotNull List<Subscription> subscriptions) {
        final boolean isPreviousConfigurationExisted = subscriptions.stream()
            .anyMatch(subscription -> Comparing.equal(subscription.subscriptionId(), configuration.getSubscriptionId()));
        fillComboBoxSilently(cbSubscription, () -> {
            cbSubscription.removeAllItems();
            for (final Subscription subscription : subscriptions) {
                cbSubscription.addItem(subscription);
                if (Comparing.equal(subscription.subscriptionId(), configuration.getSubscriptionId())) {
                    cbSubscription.setSelectedItem(subscription);
                }
            }
        }, isPreviousConfigurationExisted);
    }

    public void fillClusters(@NotNull List<? extends ServiceResourceInner> clusters) {
        final String clusterId = this.configuration.getModel().getClusterId();
        final boolean isPreviousConfigurationExisted = clusters.stream().anyMatch(cluster -> Comparing.equal(cluster.id(), clusterId));
        fillComboBoxSilently(cbClusters, () -> {
            final boolean selected = cbClusters.getSelectedItem() != null;
            cbClusters.removeAllItems();
            clusters.sort(Comparator.comparing(ProxyResource::name));
            boolean first = true;

            for (final ServiceResourceInner cluster : clusters) {
                // https://stackoverflow.com/questions/15549568/jcombobox-selects-the-first-item-by-iteself
                if (first && selected) {
                    cbClusters.insertItemAt(cluster, 0);
                } else {
                    cbClusters.addItem(cluster);
                }
                first = false;
                if (Comparing.equal(cluster.id(), clusterId)) {
                    cbClusters.setSelectedItem(cluster);
                }
            }
            // select first if there are clusters
            if (cbClusters.getSelectedItem() == null && !clusters.isEmpty()) {
                cbClusters.setSelectedItem(clusters.get(0));
            }
            cbClusters.setEnabled(true);
        }, isPreviousConfigurationExisted);
    }

    public void fillApps(@NotNull List<? extends AppResourceInner> apps) {
        final SpringCloudModel model = this.configuration.getModel();
        final boolean isSameSpringAppExist = apps.stream().anyMatch(app -> StringUtils.equals(model.getAppName(), app.name()));
        // We need to update configuration if target spring app does not exists or new create spring app succeed
        final boolean isPreviousConfigurationExisted = model.isCreateNewApp() ^ isSameSpringAppExist;
        fillComboBoxSilently(cbSpringApps, () -> {
            AzureResourceWrapper currentSel = (AzureResourceWrapper) cbSpringApps.getSelectedItem();
            cbSpringApps.removeAllItems();
            cbSpringApps.addItem(new AzureResourceWrapper(CREATE_APP, true));
            apps.sort(Comparator.comparing(ProxyResource::name));
            AzureResourceWrapper firstItem = null;
            for (final AppResourceInner app : apps) {
                final AzureResourceWrapper wrapper = new AzureResourceWrapper(app);
                cbSpringApps.addItem(wrapper);
                if (firstItem == null) {
                    firstItem = wrapper;
                }
                if (currentSel != null && StringUtils.equals(currentSel.getName(), app.name())) {
                    cbSpringApps.setSelectedItem(wrapper);
                    currentSel = null;
                }
            }
            if (currentSel != null && currentSel.isFixedOption()) {
                cbSpringApps.setSelectedItem(null);
            }

            if (currentSel != null && currentSel.isNewCreate()) {
                cbSpringApps.addItem(currentSel);
                cbSpringApps.setSelectedItem(currentSel);
            } else {
                // avoid select createApp by default
                if (apps.isEmpty()) {
                    final AzureResourceWrapper emptyOption = new AzureResourceWrapper("<Empty>", true);
                    cbSpringApps.addItem(emptyOption);
                    cbSpringApps.setSelectedItem(emptyOption);
                } else {
                    cbSpringApps.setSelectedItem(firstItem);
                }
            }
            cbSpringApps.setEnabled(true);
        }, isPreviousConfigurationExisted);
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Deploy to Azure Spring Cloud";
    }

    @Override
    public void disposeEditor() {
    }

    @Override
    protected void resetFromConfig(@NotNull SpringCloudDeploymentConfiguration configuration) {
        final SpringCloudModel model = configuration.getModel();
        this.cbCPU.setSelectedItem(intToString(model.getCpu(), 1));
        this.cbMemory.setSelectedItem(intToString(model.getMemoryInGB(), 1));
        this.cbInstanceCount.setSelectedItem(intToString(model.getInstanceCount(), 1));
        this.textJvmOptions.setText(model.getJvmOptions());

        if (StringUtils.equalsIgnoreCase(model.getRuntimeVersion(), RuntimeVersion.JAVA_11.toString())) {
            this.java11RadioButton.setSelected(true);
        } else {
            this.java8RadioButton.setSelected(true);
        }

        if (model.isEnablePersistentStorage()) {
            this.radioEnablePersistent.setSelected(true);
        } else {
            this.radioDisablePersistent.setSelected(true);
        }

        final boolean isPublic = model.isPublic();
        this.radioPublic.setSelected(isPublic);
        this.radioNonPublic.setSelected(!isPublic);
        if (MapUtils.isNotEmpty(model.getEnvironment())) {
            environmentVariableTable.setEnv(model.getEnvironment());
        }
    }

    private void fillComboBoxSilently(JComboBox cbArtifact, Runnable runnable, boolean shouldBeSilent) {
        if (!shouldBeSilent) {
            runnable.run();
            return;
        }
        final ItemListener[] listeners = cbArtifact.getItemListeners();
        Arrays.stream(listeners).forEach(listener -> cbArtifact.removeItemListener(listener));
        runnable.run();
        Arrays.stream(listeners).forEach(listener -> cbArtifact.addItemListener(listener));
    }

    private static String intToString(Integer i, int defaultValue) {
        if (i != null) {
            return i.toString();
        }
        return Integer.toString(defaultValue);
    }

    @NotNull
    protected JLabel getLblAzureArtifact() {
        return lblMavenProject;
    }

    @NotNull
    protected JComboBox<AzureArtifact> getCbAzureArtifact() {
        return cbMavenProject;
    }

    @Override
    protected void apply(@NotNull SpringCloudDeploymentConfiguration configuration) {
        final SpringCloudModel model = configuration.getModel();
        final SpringCloudModel oldModel = this.configuration.getModel();
        model.setSubscriptionId(getValueFromComboBox(this.cbSubscription, Subscription::subscriptionId, Subscription.class));

        if (cbClusters.getSelectedItem() instanceof ServiceResourceInner) {
            model.setClusterId(getValueFromComboBox(this.cbClusters, ProxyResource::id, ServiceResourceInner.class));
        } else {
            // Set cluster and app value from old configuration in case refreshing when edit existing configuration
            model.setClusterId(oldModel.getClusterId());
        }

        final AzureResourceWrapper ar = getValueFromComboBox(this.cbSpringApps, t -> t, AzureResourceWrapper.class);
        if (ar != null && !ar.isFixedOption()) {
            model.setAppName(ar.getName());
            model.setCreateNewApp(ar.isNewCreate());
        } else {
            model.setAppName(oldModel.getAppName());
            model.setCreateNewApp(oldModel.isCreateNewApp());
        }

        model.setCpu(getValueFromComboBox(this.cbCPU, Integer::parseInt, String.class));

        model.setMemoryInGB(getValueFromComboBox(this.cbMemory, Integer::parseInt, String.class));
        model.setInstanceCount(getValueFromComboBox(this.cbInstanceCount, Integer::parseInt, String.class));
        if (this.java8RadioButton.isSelected()) {
            model.setRuntimeVersion(RuntimeVersion.JAVA_8.toString());
        } else if (this.java11RadioButton.isSelected()) {
            model.setRuntimeVersion(RuntimeVersion.JAVA_11.toString());
        }
        if (StringUtils.isNotEmpty(this.textJvmOptions.getText())) {
            model.setJvmOptions(this.textJvmOptions.getText());
        } else {
            model.setJvmOptions("");
        }
        model.setEnablePersistentStorage(this.radioEnablePersistent.isSelected());
        model.setPublic(radioPublic.isSelected());
        model.setEnvironment(environmentVariableTable.getEnv());
        final AzureArtifact artifact = (AzureArtifact) getCbAzureArtifact().getSelectedItem();
        if (Objects.nonNull(artifact)) {
            model.setArtifactIdentifier(
                AzureArtifactManager.getInstance(project).getArtifactIdentifier(artifact));
        } else {
            model.setArtifactIdentifier("");
        }
        syncBeforeRunTasks(artifact, configuration);
    }

    private static <T, Q> T getValueFromComboBox(JComboBox comboBox, Function<Q, T> selectFunc, @NotNull Class<Q> clz) {
        if (comboBox == null || comboBox.getSelectedItem() == null) {
            return null;
        }
        final Object s = comboBox.getSelectedItem();
        if (clz.isAssignableFrom(s.getClass())) {
            return selectFunc.apply((Q) s);
        }
        return null;
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return this.panelRoot;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return new JComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new JComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    private void createUIComponents() {
        pnlEnvironmentTable = new JPanel();
        pnlEnvironmentTable.setLayout(new GridLayoutManager(1, 1));
        environmentVariableTable = new EnvironmentVariableTable();
        pnlEnvironmentTable.add(environmentVariableTable.getComponent(),
            new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 7, 7, null, null, null));
        pnlEnvironmentTable.setFocusable(false);
    }
}
