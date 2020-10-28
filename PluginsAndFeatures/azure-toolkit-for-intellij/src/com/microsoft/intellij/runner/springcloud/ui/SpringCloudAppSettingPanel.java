/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.springcloud.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.ProxyResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.intellij.common.AzureResourceWrapper;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.springcloud.deploy.SpringCloudDeployConfiguration;
import com.microsoft.intellij.runner.springcloud.deploy.SpringCloudDeploySettingMvpView;
import com.microsoft.intellij.runner.springcloud.deploy.SpringCloudDeploySettingPresenter;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
import com.nimbusds.oauth2.sdk.util.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.function.Function;

public class SpringCloudAppSettingPanel extends AzureSettingPanel<SpringCloudDeployConfiguration>
        implements SpringCloudDeploySettingMvpView {
    private final SpringCloudDeployConfiguration configuration;
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

    private SpringCloudDeploySettingPresenter presenter = null;

    private String lastSelectedSubsId;
    private String lastSelectedClusterId;

    private boolean isAppInitialized = false;
    private boolean isClusterInitialized = false;

    public SpringCloudAppSettingPanel(@NotNull Project project, @NotNull SpringCloudDeployConfiguration configuration) {
        super(project, false);
        this.configuration = configuration;
        this.presenter = new SpringCloudDeploySettingPresenter();
        this.presenter.onAttachView(this);
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
            Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
            if (subscription == null) {
                return;
            }
            String selectedSid = subscription.subscriptionId();
            if (!Comparing.equal(lastSelectedSubsId, selectedSid)) {
                initCluster(SpringCloudIdHelper.getClusterName(configuration.getClusterId()));
                cbClusters.setEnabled(false);
                presenter.onLoadClusters(selectedSid);
                lastSelectedSubsId = selectedSid;
            }
        });

        cbClusters.addActionListener(e -> {
            if (cbClusters.getSelectedItem() instanceof String) {
                return;
            }
            ServiceResourceInner cluster = (ServiceResourceInner) cbClusters.getSelectedItem();
            if (cluster == null) {
                return;
            }
            String selectedCid = cluster.id();
            if (!Comparing.equal(lastSelectedClusterId, selectedCid)) {
                initApp(configuration.getAppName(), configuration.isCreateNewApp());
                cbSpringApps.setEnabled(false);
                presenter.onLoadApps(selectedCid);
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
        presenter.onLoadSubscription();
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
                AzureResourceWrapper cacheOne = new AzureResourceWrapper(initialValue, false, isCreate);
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
            ApplicationManager.getApplication().invokeLater(() -> createNewAppWizard());
        }
    }

    private void createNewAppWizard() {
        cbSpringApps.setSelectedItem(null);
        final CreateSpringCloudAppDialog dialog = new CreateSpringCloudAppDialog(null,
                                                                                 cbSpringApps,
                                                                                 "Create Azure Spring Cloud app");
        dialog.pack();
        if (dialog.showAndGet()) {
            AzureResourceWrapper newApp = dialog.getNewAppWrapper();
            if (newApp != null) {
                int count = cbSpringApps.getItemCount();
                boolean existed = false;
                for (int i = 0; i < count; i++) {
                    AzureResourceWrapper option = (AzureResourceWrapper) cbSpringApps.getItemAt(i);
                    if (!option.isFixedOption() && StringUtils.equals(option.getName(), newApp.getName())) {
                        existed = true;
                        newApp = option;
                        break;
                    }
                }
                if (!existed) {
                    cbSpringApps.addItem(newApp);
                    count = cbSpringApps.getItemCount();
                    List<Object> toRemove = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        final AzureResourceWrapper resource = (AzureResourceWrapper) cbSpringApps.getItemAt(i);
                        if (resource.isFixedOption() && !resource.getName().equals(CREATE_APP)) {
                            toRemove.add(resource);
                        }
                    }
                    for (Object rem : toRemove) {
                        cbSpringApps.removeItem(rem);
                    }
                }
                cbSpringApps.setSelectedItem(newApp);
            }
        } else {
            int count = cbSpringApps.getItemCount();
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

    @Override
    public void fillSubscription(@NotNull List<Subscription> subscriptions) {
        final boolean isPreviousConfigurationExisted =
                subscriptions.stream()
                             .anyMatch(subscription -> Comparing.equal(subscription.subscriptionId(),
                                                                     configuration.getSubscriptionId()));
        fillComboBoxSilently(cbSubscription, () -> {
            cbSubscription.removeAllItems();
            for (Subscription subscription : subscriptions) {
                cbSubscription.addItem(subscription);
                if (Comparing.equal(subscription.subscriptionId(), configuration.getSubscriptionId())) {
                    cbSubscription.setSelectedItem(subscription);
                }
            }
        }, isPreviousConfigurationExisted);
    }

    @Override
    public void fillClusters(@NotNull List<ServiceResourceInner> clusters) {
        final boolean isPreviousConfigurationExisted =
                clusters.stream().anyMatch(cluster -> Comparing.equal(cluster.id(), configuration.getClusterId()));
        fillComboBoxSilently(cbClusters, () -> {
            boolean selected = cbClusters.getSelectedItem() != null;
            cbClusters.removeAllItems();
            clusters.sort(Comparator.comparing(ProxyResource::name));
            boolean first = true;

            for (ServiceResourceInner cluster : clusters) {
                // https://stackoverflow.com/questions/15549568/jcombobox-selects-the-first-item-by-iteself
                if (first && selected) {
                    cbClusters.insertItemAt(cluster, 0);
                } else {
                    cbClusters.addItem(cluster);
                }
                first = false;
                if (Comparing.equal(cluster.id(), configuration.getClusterId())) {
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

    @Override
    public void fillApps(@NotNull List<AppResourceInner> apps) {
        final boolean isSameSpringAppExist =
                apps.stream().anyMatch(app -> StringUtils.equals(configuration.getAppName(), app.name()));
        // We need to update configuration if target spring app does not exists or new create spring app succeed
        final boolean isPreviousConfigurationExisted = configuration.isCreateNewApp() ^ isSameSpringAppExist;
        fillComboBoxSilently(cbSpringApps, () -> {
            AzureResourceWrapper currentSel = (AzureResourceWrapper) cbSpringApps.getSelectedItem();
            cbSpringApps.removeAllItems();
            cbSpringApps.addItem(new AzureResourceWrapper(CREATE_APP, true));
            apps.sort(Comparator.comparing(ProxyResource::name));
            AzureResourceWrapper firstItem = null;
            for (AppResourceInner app : apps) {
                AzureResourceWrapper wrapper = new AzureResourceWrapper(app);
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
                    AzureResourceWrapper emptyOption = new AzureResourceWrapper("<Empty>", true);
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
        this.presenter.onDetachView();
    }

    @Override
    protected void resetFromConfig(@NotNull SpringCloudDeployConfiguration configuration) {
        this.cbCPU.setSelectedItem(intToString(configuration.getCpu(), 1));
        this.cbMemory.setSelectedItem(intToString(configuration.getMemoryInGB(), 1));
        this.cbInstanceCount.setSelectedItem(intToString(configuration.getInstanceCount(), 1));
        this.textJvmOptions.setText(configuration.getJvmOptions());

        RuntimeVersion javaVer = configuration.getRuntimeVersion();
        if (javaVer == RuntimeVersion.JAVA_11) {
            this.java11RadioButton.setSelected(true);
        } else {
            this.java8RadioButton.setSelected(true);
        }

        boolean persistence = configuration.isEnablePersistentStorage();
        if (persistence) {
            this.radioEnablePersistent.setSelected(true);
        } else {
            this.radioDisablePersistent.setSelected(true);
        }

        boolean isPublic = configuration.isPublic();
        this.radioPublic.setSelected(isPublic);
        this.radioNonPublic.setSelected(!isPublic);
        if (MapUtils.isNotEmpty(configuration.getEnvironment())) {
            environmentVariableTable.setEnv(configuration.getEnvironment());
        }
    }

    private void fillComboBoxSilently(JComboBox cbArtifact, Runnable runnable, boolean shouldBeSilent) {
        if (!shouldBeSilent) {
            runnable.run();
            return;
        }
        ItemListener[] listeners = cbArtifact.getItemListeners();
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
    protected void apply(@NotNull SpringCloudDeployConfiguration configuration) {
        configuration.setSubscriptionId(getValueFromComboBox(this.cbSubscription,
                                                             Subscription::subscriptionId,
                                                             Subscription.class));

        if (cbClusters.getSelectedItem() instanceof ServiceResourceInner) {
            configuration.setClusterId(getValueFromComboBox(this.cbClusters, ProxyResource::id,
                                                            ServiceResourceInner.class));
        } else {
            // Set cluster and app value from old configuration in case refreshing when edit existing configuration
            configuration.setClusterId(this.configuration.getClusterId());
        }

        AzureResourceWrapper ar = getValueFromComboBox(this.cbSpringApps, t -> t, AzureResourceWrapper.class);
        if (ar != null && !ar.isFixedOption()) {
            configuration.setAppName(ar.getName());
            configuration.setCreateNewApp(ar.isNewCreate());
        } else {
            configuration.setAppName(this.configuration.getAppName());
            configuration.setCreateNewApp(this.configuration.isCreateNewApp());
        }

        configuration.setCpu(getValueFromComboBox(this.cbCPU, Integer::parseInt, String.class));

        configuration.setMemoryInGB(getValueFromComboBox(this.cbMemory, Integer::parseInt, String.class));
        configuration.setInstanceCount(getValueFromComboBox(this.cbInstanceCount, Integer::parseInt, String.class));
        if (this.java8RadioButton.isSelected()) {
            configuration.saveRuntimeVersion(RuntimeVersion.JAVA_8);
        } else if (this.java11RadioButton.isSelected()) {
            configuration.saveRuntimeVersion(RuntimeVersion.JAVA_11);
        }
        if (StringUtils.isNotEmpty(this.textJvmOptions.getText())) {
            configuration.setJvmOptions(this.textJvmOptions.getText());
        } else {
            configuration.setJvmOptions("");
        }
        configuration.setEnablePersistentStorage(this.radioEnablePersistent.isSelected());
        configuration.setPublic(radioPublic.isSelected());
        configuration.setEnvironment(environmentVariableTable.getEnv());
        AzureArtifact artifact = (AzureArtifact) getCbAzureArtifact().getSelectedItem();
        if (Objects.nonNull(artifact)) {
            configuration.setArtifactIdentifier(
                    AzureArtifactManager.getInstance(project).getArtifactIdentifier(artifact));
        } else {
            configuration.setArtifactIdentifier("");
        }
        syncBeforeRunTasks(artifact, configuration);
    }

    private static <T, Q> T getValueFromComboBox(JComboBox comboBox, Function<Q, T> selectFunc, @NotNull Class<Q> clz) {
        if (comboBox == null || comboBox.getSelectedItem() == null) {
            return null;
        }
        Object s = comboBox.getSelectedItem();
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
