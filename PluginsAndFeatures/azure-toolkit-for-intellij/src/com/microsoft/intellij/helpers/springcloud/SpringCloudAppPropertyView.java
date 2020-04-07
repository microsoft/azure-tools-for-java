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

package com.microsoft.intellij.helpers.springcloud;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.*;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.intellij.runner.springcloud.ui.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.DefaultAzureResourceTracker;
import com.microsoft.tooling.msservices.serviceexplorer.IDataRefreshableComponent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNodePresenter;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpringCloudAppPropertyView extends BaseEditor implements IDataRefreshableComponent<AppResourceInner, DeploymentResourceInner> {
    private static final String DELETE_APP_PROMPT_MESSAGE = "This operation will delete the Spring Cloud App: '%s'.\n" +
            "Are you sure you want to continue?";
    private static final String DELETE_APP_DIRTY_PROMPT_MESSAGE = "This operation will discard your changes and delete the Spring Cloud App: '%s'.\n" +
            "Are you sure you want to continue?";
    private static final String OPERATE_APP_PROMPT_MESSAGE = "This operation will discard your changes.\nAre you sure you want to continue?";
    private static final String NOT_AVAILABLE = " - ";

    private JButton triggerPublicButton;
    private JComboBox javaVersionCombo;
    private JComboBox cpuCombo;
    private JTextField jvmOpsTextField;
    private JButton refreshButton;
    private JButton startButton;
    private JButton stopButton;
    private JButton restartButton;
    private JButton deleteButton;
    private JLabel lblAppName;
    private JLabel lblPublic;
    private JPanel instanceDetailHolder;
    private JPanel instanceDetailPanel;
    private JPanel statusPanel;
    private JButton triggerPersistentButton;
    private JComboBox memCombo;
    private JPanel mainPanel;
    private JPanel publicPanel;
    private JButton saveButton;
    private JBTable instanceTable;
    private HyperlinkLabel testUrlLink;
    private HyperlinkLabel publicUrlHyperLink;
    private JLabel subsLabel;
    private JLabel resourceGroupLabel;
    private JLabel clusterLabel;
    private JLabel appNameLabel;
    private JLabel persistentLabel;
    private EnvironmentVariablesTextFieldWithBrowseButton envTable;
    private HideableDecorator instancePanelDecorator;

    private SpringAppViewModel model;
    private Project project;
    private AppResourceInner appResourceInner;
    private DeploymentResourceInner deploymentResourceInner;
    private String appId;
    private String appName;
    private DefaultTableModel instancesTableModel;

    public SpringCloudAppPropertyView(Project project, String appId) {
        this.project = project;
        this.appId = appId;
        this.appName = SpringCloudIdHelper.getAppName(appId);
        instancesTableModel = new DefaultTableModel() {
            public boolean isCellEditable(int var1, int var2) {
                return false;
            }
        };
        instancesTableModel.addColumn("App Instances Name");
        instancesTableModel.addColumn("Status");
        instancesTableModel.addColumn("Discover Status");
        instanceTable.setModel(instancesTableModel);
        instanceTable.setRowSelectionAllowed(true);
        instanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instanceTable.getEmptyText().setText("Loading instances status");
        instanceTable.setPreferredSize(new Dimension(-1, 200));
        this.saveButton.addActionListener(e -> {
            wrapperOperations(TelemetryConstants.SAVE_SPRING_CLOUD_APP, project, (changes) -> {
                if (changes.isEmpty()) {
                    PluginUtil.showInfoNotificationProject(project, "No actions performed", "You have no changes to apply.");
                    return;
                }
                save(changes);
            });

        });

        this.refreshButton.addActionListener(e -> {
            wrapperOperations(TelemetryConstants.REFRESH_SPRING_CLOUD_APP, project, (changes) -> {
                // DO nothing
            });
        });

        this.triggerPublicButton.addActionListener(e -> {
            this.triggerPublicUrl();
            syncSaveStatus("enablePublicUrl");
        });
        this.triggerPersistentButton.addActionListener(e -> {
            this.triggerPersistentStorage();
            saveButton.setEnabled(true);
            syncSaveStatus("enablePersistentStorage");
        });
        this.deleteButton.addActionListener(e -> {
            wrapperOperations(TelemetryConstants.DELETE_SPRING_CLOUD_APP, project, (changes) -> {
                try {
                    AzureSpringCloudMvpModel.deleteApp(appId).await();
                    monitorStatus(appId, deploymentResourceInner);
                } catch (IOException | InterruptedException ex) {
                    PluginUtil.showErrorNotificationProject(project,
                            String.format("Cannot delete app '%s' due to error.", this.appName), ex.getMessage());
                }
            });

        });
        this.startButton.addActionListener(e -> {
            wrapperOperations(TelemetryConstants.START_SPRING_CLOUD_APP, project, (changes) -> {
                try {
                    AzureSpringCloudMvpModel.startApp(appId, appResourceInner.properties().activeDeploymentName()).await();
                    monitorStatus(appId, deploymentResourceInner);
                } catch (IOException | InterruptedException ex) {
                    PluginUtil.showErrorNotificationProject(project, String.format("Cannot start app '%s' due to error.", this.appName), ex.getMessage());
                }
            });
        });

        this.stopButton.addActionListener(e -> {
            wrapperOperations(TelemetryConstants.STOP_SPRING_CLOUD_APP, project, (changes) -> {
                try {
                    AzureSpringCloudMvpModel.stopApp(appId, appResourceInner.properties().activeDeploymentName()).await();
                    monitorStatus(appId, deploymentResourceInner);
                } catch (IOException | InterruptedException ex) {
                    PluginUtil.showErrorNotificationProject(project, String.format("Cannot stop app '%s' due to error.", this.appName), ex.getMessage());
                }
            });
        });
        this.restartButton.addActionListener(e -> {
            wrapperOperations(TelemetryConstants.RESTART_SPRING_CLOUD_APP, project, (changes) -> {
                try {
                    AzureSpringCloudMvpModel.restartApp(appId, appResourceInner.properties().activeDeploymentName()).await();
                    monitorStatus(appId, deploymentResourceInner);
                } catch (IOException | InterruptedException ex) {
                    PluginUtil.showErrorNotificationProject(project, String.format("Cannot restart app '%s' due to error.", this.appName), ex.getMessage());
                }
            });
        });
        refreshData();

        jvmOpsTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent documentEvent) {
                syncSaveStatus("jvmOptions");
            }
        });
        cpuCombo.addActionListener(e -> {
            syncSaveStatus("cpu");
        });
        memCombo.addActionListener(e -> {
            syncSaveStatus("memoryInGB");
        });
        javaVersionCombo.addActionListener(e -> {
            syncSaveStatus("javaVersion");
        });
        instancePanelDecorator = new HideableDecorator(instanceDetailHolder, "Instances", true);
        instancePanelDecorator.setContentComponent(instanceDetailPanel);
        instancePanelDecorator.setOn(true);

        IntStream.range(1, 5).forEach(cpuCombo::addItem);
        IntStream.range(1, 9).forEach(memCombo::addItem);

        Arrays.asList("Java_8", "Java_11").forEach(javaVersionCombo::addItem);
        disableAllInput();
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @NotNull
    @Override
    public String getName() {
        return this.model == null ? "Untitled" : this.model.getAppName();
    }

    @Override
    public void notifyDataRefresh(AppResourceInner appInner, DeploymentResourceInner deploymentResourceInner) {
        this.prepareViewModel(appInner, deploymentResourceInner, this.model == null ? null : this.model.getTestUrl());
    }

    @Override
    public void dispose() {
        DefaultAzureResourceTracker.getInstance().unregisterNode(appId, SpringCloudAppPropertyView.this);
    }

    private static void monitorStatus(String appId, DeploymentResourceInner deploymentResourceInner) throws IOException, InterruptedException {
        SpringCloudAppNodePresenter.awaitAndMonitoringStatus(appId,
                deploymentResourceInner == null ? null : deploymentResourceInner.properties().status());
    }

    private void wrapperOperations(String actionName, Project project, Consumer<Map<String, Object>> action) {
        Map<String, Object> changes;
        try {
            changes = getModifiedDataMap();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            PluginUtil.showErrorNotificationProject(project, "Cannot get model state", ex.getMessage());
            return;
        }
        String promptMessage;
        if (actionName.startsWith("delete")) {
            promptMessage = String.format(changes.isEmpty() ? DELETE_APP_PROMPT_MESSAGE : DELETE_APP_DIRTY_PROMPT_MESSAGE, this.appName);
        } else {
            promptMessage = changes.isEmpty() ? "" : String.format(OPERATE_APP_PROMPT_MESSAGE, actionName, this.appName);
        }
        if (promptMessage.isEmpty() || actionName.startsWith("save") || DefaultLoader.getUIHelper()
                .showConfirmation(this.mainPanel,
                        promptMessage,
                        "Azure Explorer",
                        new String[]{"Yes", "No"},
                        null)) {
            disableAllInput();
            DefaultLoader.getIdeHelper().runInBackground(null, actionName, false,
                true, String.format("%s...", actionName), () -> {
                    action.accept(changes);
                    refreshData();
                });

        }
    }

    private void disableAllInput() {
        this.triggerPersistentButton.setEnabled(false);
        this.triggerPublicButton.setEnabled(false);
        this.javaVersionCombo.setEnabled(false);
        this.cpuCombo.setEnabled(false);
        this.memCombo.setEnabled(false);
        this.jvmOpsTextField.setEnabled(false);
        this.envTable.setEditable(false);
        this.saveButton.setEnabled(false);
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(false);
        this.restartButton.setEnabled(false);
        this.deleteButton.setEnabled(false);
        this.refreshButton.setEnabled(false);
    }

    private void restoreAllInput() {
        this.triggerPersistentButton.setEnabled(true);
        this.triggerPublicButton.setEnabled(true);
        this.cpuCombo.setEnabled(true);
        this.memCombo.setEnabled(true);
        this.javaVersionCombo.setEnabled(true);
        this.jvmOpsTextField.setEnabled(true);
        this.envTable.setEditable(true);
    }

    private void syncSaveStatus(String propertyName) {
        boolean changed = false;
        switch (propertyName) {
            case "jvmOptions":
                changed = !StringUtils.equals(jvmOpsTextField.getText(), model.getJvmOptions());
                break;
            case "cpu":
                changed = Integer.parseInt(Objects.toString(this.cpuCombo.getSelectedItem(), null)) != model.getCpu();
                break;
            case "memoryInGB":
                changed = Integer.parseInt(Objects.toString(this.memCombo.getSelectedItem(), null)) != model.getMemoryInGB();
                break;
            case "enablePublicUrl":
                changed = (model.isEnablePublicUrl() == StringUtils.equalsIgnoreCase(this.triggerPublicButton.getText(), "Enable"));
                break;
            case "enablePersistentStorage":
                changed = model.isEnablePersistentStorage() == StringUtils.equalsIgnoreCase(this.triggerPersistentButton.getText(), "Enable");
                break;
            default:
                break;
        }
        saveButton.setEnabled(changed);
    }

    private void triggerPersistentStorage() {
        final String text = this.triggerPersistentButton.getText();

        boolean enablePersist = StringUtils.equalsIgnoreCase(text, "Enable");
        if (enablePersist) {
            Font font = publicUrlHyperLink.getFont();
            if (model.isEnablePersistentStorage()) {
                renderPersistent();
            } else {
                this.persistentLabel.setText("Persistent storage is not available before you save the settings.");
                persistentLabel.setFont(new Font(font.getName(), Font.ITALIC, font.getSize()));
            }
        } else {
            this.persistentLabel.setText(" - ");
        }

        this.triggerPersistentButton.setText(enablePersist ? "Disable" : "Enable");
    }

    private void triggerPublicUrl() {
        final String text = this.triggerPublicButton.getText();
        boolean updatePublicTrue = StringUtils.equalsIgnoreCase(text, "Enable");
        setPublic(updatePublicTrue, StringUtils.isNotEmpty(this.model.getPublicUrl()) ?
                this.model.getPublicUrl() : "URL is not available before you save the settings.");
    }

    private void refreshData() {
        Observable.fromCallable(() -> {
                AppResourceInner app = AzureSpringCloudMvpModel.getAppById(appId);
                if (app == null) {
                    return Triple.of(app, (DeploymentResourceInner) null, (String) null);
                }
                DeploymentResourceInner deploy = StringUtils.isNotEmpty(app.properties().activeDeploymentName())
                        ? AzureSpringCloudMvpModel.getAppDeployment(appId, app.properties().activeDeploymentName()) : null;
                String testUrl = AzureSpringCloudMvpModel.getTestEndpoint(appId);
                return Triple.of(app, deploy, testUrl);
            }
        ).subscribeOn(Schedulers.io()).subscribe(tuple -> {
                ApplicationManager.getApplication().invokeLater(() -> this.prepareViewModel(tuple.getLeft(), tuple.getMiddle(), tuple.getRight()));
            }
        );
    }

    private Map<String, Object> getModifiedDataMap() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (model == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        compareModel(model, "jvmOptions", this.jvmOpsTextField, map);
        compareModelComboBinding(model, "cpu", this.cpuCombo, map);
        compareModelComboBinding(model, "memoryInGB", this.memCombo, map);
        compareModelTextComboBinding(model, "javaVersion", this.javaVersionCombo, map);

        // is public

        final String text = this.triggerPublicButton.getText();
        boolean currentEnableFlag = !StringUtils.equalsIgnoreCase(text, "Enable");
        if (model.isEnablePublicUrl() != currentEnableFlag) {
            map.put("enablePublicUrl", currentEnableFlag);
        }

        boolean currentEnablePersist = !StringUtils.equalsIgnoreCase(this.triggerPersistentButton.getText(), "Enable");
        if (model.isEnablePersistentStorage() != currentEnablePersist) {
            map.put("enablePersistentStorage", currentEnablePersist);
        }
        return map;
    }

    private void save(Map<String, Object> map) {
        try {
            DeploymentResourceProperties deploymentResourceProperties = deploymentResourceInner.properties();
            final DeploymentSettings deploymentSettings = deploymentResourceProperties.deploymentSettings();
            deploymentResourceProperties = deploymentResourceProperties.withDeploymentSettings(deploymentSettings);
            if (map.containsKey("cpu")) {
                deploymentSettings.withCpu((Integer) map.get("cpu"));
            }
            if (map.containsKey("memoryInGB")) {
                deploymentSettings.withMemoryInGB((Integer) map.get("memoryInGB"));
            }

            if (map.containsKey("jvmOptions")) {
                deploymentSettings.withJvmOptions((String) map.get("jvmOptions"));
            }

            if (map.containsKey("javaVersion")) {
                deploymentSettings.withRuntimeVersion(RuntimeVersion.fromString((String) map.get("runtimeVersion")));
            }
            AppResourceProperties appUpdate = null;
            if (map.containsKey("enablePublicUrl")) {
                if (appUpdate == null) {
                    appUpdate = new AppResourceProperties();
                }
                appUpdate.withPublicProperty((Boolean) map.get("enablePublicUrl"));
            }

            if (map.containsKey("enablePersistentStorage")) {
                if (appUpdate == null) {
                    appUpdate = new AppResourceProperties();
                }
                boolean isEnablePersist = (Boolean) map.get("enablePersistentStorage");
                PersistentDisk pd = new PersistentDisk();
                pd.withMountPath("/persistent");
                pd.withSizeInGB(isEnablePersist ? 50 : 0);
                appUpdate.withPersistentDisk(pd);
            }
            if (appUpdate != null) {
                AzureSpringCloudMvpModel.updateAppProperties(appId, appUpdate);
            }
            deploymentResourceInner = AzureSpringCloudMvpModel
                    .updateProperties(appId, appResourceInner.properties().activeDeploymentName(), deploymentResourceProperties);

            ApplicationManager.getApplication().invokeLater(() ->
                    PluginUtil.displayInfoDialog("Update successfully", "Update app configuration successfully"));
            refreshData();

        } catch (Exception e) {
            ApplicationManager.getApplication().invokeLater(() ->
                    PluginUtil.displayErrorDialog("Failed to update app configuration", e.getMessage()));
        }
    }

    private void compareModelTextComboBinding(Object model, String propertyName, JComboBox comboBox, Map<String, Object> deltaMap)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String value = (String) PropertyUtils.getSimpleProperty(model, propertyName);
        String userInput = (String) comboBox.getModel().getSelectedItem();
        if (!Objects.equals(value, userInput)) {
            deltaMap.put(propertyName, userInput);
        }
    }

    private void compareModelComboBinding(Object model, String propertyName, JComboBox comboBox, Map<String, Object> deltaMap)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Integer value = (Integer) PropertyUtils.getSimpleProperty(model, propertyName);
        // userInput may be integer or string
        String userInput = Objects.toString(comboBox.getModel().getSelectedItem(), null);
        if (StringUtils.isNotEmpty(userInput) && !Objects.equals(value, Integer.parseInt(userInput))) {
            deltaMap.put(propertyName, Integer.parseInt(userInput));
        }
    }

    private void compareModel(Object model, String propertyName, JTextField textField, Map<String, Object> deltaMap)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String text = Objects.toString(PropertyUtils.getSimpleProperty(model, propertyName), null);
        if (StringUtils.equals(text, textField.getText())) {
            return;
        }
        deltaMap.put(propertyName, textField.getText());
    }

    private void setPublic(boolean isPublic, String publicUrl) {
        this.triggerPublicButton.setText(isPublic ? "Disable" : "Enable");
        if (isPublic) {

            if (publicUrl.startsWith("http")) {
                publicUrlHyperLink.setHyperlinkText(publicUrl);
                publicUrlHyperLink.setHyperlinkTarget(publicUrl);
                Font font = publicUrlHyperLink.getFont();
                publicUrlHyperLink.setFont(new Font(font.getName(), Font.PLAIN, font.getSize()));
            } else {
                publicUrlHyperLink.setText(publicUrl);
                Font font = publicUrlHyperLink.getFont();
                publicUrlHyperLink.setFont(new Font(font.getName(), Font.ITALIC, font.getSize()));
            }
        } else {
            publicUrlHyperLink.setText(" - ");
        }
    }

    private void renderPersistent() {
        Font font = persistentLabel.getFont();
        persistentLabel.setFont(new Font(font.getName(), Font.PLAIN, font.getSize()));
        this.persistentLabel.setText(String.format("%s (%dG of %dG used)",
                model.getPersistentMountPath(), model.getUsedStorageInGB(), model.getTotalStorageInGB()));
    }

    private void handleTextComboBinding(Object model, String propertyName, JComboBox comboBox)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String text = Objects.toString(PropertyUtils.getSimpleProperty(model, propertyName), null);
        comboBox.getModel().setSelectedItem(text);
    }

    private void handleNumberComboBinding(Object model, String propertyName, JComboBox comboBox)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Integer value = (Integer) PropertyUtils.getSimpleProperty(model, propertyName);
        comboBox.getModel().setSelectedItem(value);
    }

    private void handleTextDataBinding(Object model, String propertyName, JTextField textField)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String text = Objects.toString(PropertyUtils.getSimpleProperty(model, propertyName), null);
        textField.setText(text);
    }

    private void prepareViewModel(AppResourceInner app, DeploymentResourceInner deploy, String testUrl) {
        try {

            if (app == null) {
                setData(null);
                return;
            }
            this.appResourceInner = app;
            this.deploymentResourceInner = deploy;
            this.model = new SpringAppViewModel();
            this.model.setTestUrl(testUrl);

            // persistent storage
            if (app.properties().persistentDisk() != null && app.properties().persistentDisk().sizeInGB().intValue() > 0) {
                this.model.setEnablePersistentStorage(true);
                this.model.setUsedStorageInGB(app.properties().persistentDisk().usedInGB());
                this.model.setTotalStorageInGB(app.properties().persistentDisk().sizeInGB());
                this.model.setPersistentMountPath(app.properties().persistentDisk().mountPath());
            } else {
                this.model.setEnablePersistentStorage(false);
            }

            Subscription subs = AzureMvpModel.getInstance().getSubscriptionById(SpringCloudIdHelper.getSubscriptionId(this.appId));
            this.model.setSubscriptionName(subs == null ? null : subs.displayName());
            this.model.setResourceGroup(SpringCloudIdHelper.getResourceGroup(this.appId));
            if (deploy != null) {
                DeploymentSettings settings = deploy.properties().deploymentSettings();
                this.model.setJavaVersion(settings.runtimeVersion().toString());
                this.model.setJvmOptions(settings.jvmOptions());
                this.model.setCpu(settings.cpu());
                this.model.setMemoryInGB(settings.memoryInGB());
                if (deploy.properties().instances() != null) {
                    this.model.setDownInstanceCount((int) deploy.properties().instances().stream().filter(
                        t -> StringUtils.equalsIgnoreCase(t.discoveryStatus(), "DOWN")).count());
                    this.model.setUpInstanceCount(deploy.properties().instances().size() - this.model.getDownInstanceCount());
                    this.model.setInstance(deploymentResourceInner.properties().instances().stream().map(t -> {
                        SpringAppInstanceViewModel instanceViewModel = new SpringAppInstanceViewModel();
                        instanceViewModel.setName(t.name());
                        instanceViewModel.setStatus(t.status());
                        instanceViewModel.setDiscoveryStatus(t.discoveryStatus());
                        return instanceViewModel;
                    }).collect(Collectors.toList()));
                } else {
                    this.model.setUpInstanceCount(0);
                    this.model.setDownInstanceCount(0);
                }
                // env variable
                this.model.setEnvironment(settings.environmentVariables());
            } else {
                this.model.setUpInstanceCount(0);
                this.model.setDownInstanceCount(0);
            }
            // public url
            this.model.setEnablePublicUrl(app.properties().publicProperty());
            if (this.model.isEnablePublicUrl()) {
                this.model.setPublicUrl(app.properties().url());
            }

            this.model.setClusterName(SpringCloudIdHelper.getClusterName(this.appId));
            this.model.setAppName(app.name());

            // button enable
            DeploymentResourceStatus status = deploy == null ? DeploymentResourceStatus.UNKNOWN : deploy.properties().status();
            boolean stopped = DeploymentResourceStatus.STOPPED.equals(status);
            boolean unknown = DeploymentResourceStatus.UNKNOWN.equals(status);
            this.model.setCanStop(!stopped && !unknown);
            this.model.setCanStart(stopped);
            this.model.setCanReStart(!stopped && !unknown);
            // status
            this.model.setStatus(status.toString());
            this.setData(model);
        } catch (AzureExecutionException e) {
            ApplicationManager.getApplication().invokeLater(() -> {
                PluginUtil.showErrorNotificationProject(project, "Cannot binding data to Spring Cloud property view.", e.getMessage());
            });
        }
    }

    private void setData(SpringAppViewModel model) throws AzureExecutionException {
        if (model == null) {
            DefaultLoader.getUIHelper().closeSpringCloudAppPropertyView(project, appId);
            PluginUtil.showInfoNotificationProject(project,
                String.format("The editor for app %s is closed.", this.appName), "The app " + this.appName + " is deleted.");
            return;
        }
        try {
            this.subsLabel.setText(model.getSubscriptionName());
            this.resourceGroupLabel.setText(model.getResourceGroup());
            this.clusterLabel.setText(model.getClusterName());
            this.appNameLabel.setText(model.getAppName());
            handleTextDataBinding(model, "jvmOptions", this.jvmOpsTextField);
            setPublic(model.isEnablePublicUrl(), model.getPublicUrl());
            handleNumberComboBinding(model, "cpu", this.cpuCombo);
            handleNumberComboBinding(model, "memoryInGB", this.memCombo);
            handleTextComboBinding(model, "javaVersion", this.javaVersionCombo);
            this.testUrlLink.setHyperlinkText(model.getTestUrl());
            this.testUrlLink.setHyperlinkTarget(model.getTestUrl());
            this.triggerPersistentButton.setText(model.isEnablePersistentStorage() ? "Disable" : "Enable");
            if (model.isEnablePersistentStorage()) {
                renderPersistent();
            } else {
                this.persistentLabel.setText(NOT_AVAILABLE);
            }
            String statusLineText = model.getStatus();
            if (model.getUpInstanceCount().intValue() + model.getDownInstanceCount().intValue() > 0) {
                statusLineText = String.format("%s - Discovery Status(UP %d, DOWN %d)",
                    model.getStatus(), model.getUpInstanceCount(), model.getDownInstanceCount());
            }
            Border statusLine = BorderFactory.createTitledBorder(statusLineText);
            this.statusPanel.setBorder(statusLine);
            this.startButton.setEnabled(model.isCanStart());
            this.stopButton.setEnabled(model.isCanStop());
            this.restartButton.setEnabled(model.isCanReStart());

            this.refreshButton.setEnabled(true);
            this.deleteButton.setEnabled(true);
            this.saveButton.setEnabled(false);
            instancesTableModel.getDataVector().removeAllElements();

            instanceTable.getEmptyText().setText("Empty");

            if (model.getInstance() != null) {
                for (final SpringAppInstanceViewModel deploymentInstance : model.getInstance()) {
                    instancesTableModel.addRow(new String[]{
                        deploymentInstance.getName(), deploymentInstance.getStatus(), deploymentInstance.getDiscoveryStatus()});
                }
            }
            instanceTable.setModel(instancesTableModel);
            instanceTable.updateUI();
            envTable.setEnvironmentVariables(model.getEnvironment() == null ? new HashMap<>() : new HashMap<>(model.getEnvironment()));
            restoreAllInput();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new AzureExecutionException("Cannot get property through reflection", e);
        } catch (Exception ex) {
            throw new AzureExecutionException("Cannot get property through reflection", ex);
        }
    }
}
