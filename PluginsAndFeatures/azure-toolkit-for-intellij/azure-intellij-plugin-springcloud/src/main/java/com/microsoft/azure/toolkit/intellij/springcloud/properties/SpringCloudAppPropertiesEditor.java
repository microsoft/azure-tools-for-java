/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppConfigPanel;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstancesPanel;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentStatus;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudAppPropertiesEditor extends BaseEditor {
    private JButton refreshButton;
    private JButton startButton;
    private JButton stopButton;
    private JButton restartButton;
    private JButton deleteButton;
    private JPanel contentPanel;
    private JButton saveButton;
    private ActionLink reset;
    private JBLabel lblSubscription;
    private JBLabel lblCluster;
    private JBLabel lblApp;
    private SpringCloudAppConfigPanel formConfig;
    private SpringCloudAppInstancesPanel panelInstances;

    @Nonnull
    private final Project project;
    @Nonnull
    private final SpringCloudApp app;
    @Nonnull
    private SpringCloudAppConfig originalConfig;

    public SpringCloudAppPropertiesEditor(@Nonnull Project project, @Nonnull SpringCloudApp app, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile);
        this.project = project;
        this.app = app;
        this.rerender();
        this.initListeners();
    }

    private void rerender() {
        AzureTaskManager.getInstance().runLater(() -> {
            this.reset.setVisible(false);
            this.saveButton.setEnabled(false);
            this.lblSubscription.setText(this.app.subscription().getName());
            this.lblCluster.setText(this.app.getCluster().name());
            this.lblApp.setText(this.app.name());
            AzureTaskManager.getInstance().runLater(() -> this.formConfig.updateForm(this.app));
            AzureTaskManager.getInstance().runOnPooledThread((() -> {
                final SpringCloudDeployment deployment = Optional.ofNullable(this.app.activeDeployment()).orElse(null);
                AzureTaskManager.getInstance().runLater(() -> this.resetToolbar(deployment));
            }));
            AzureTaskManager.getInstance().runOnPooledThread((() -> {
                this.originalConfig = SpringCloudAppConfig.fromApp(this.app);
                AzureTaskManager.getInstance().runLater(() -> this.formConfig.setData(this.originalConfig));
            }));
            this.panelInstances.setApp(this.app);
        });
    }

    private void initListeners() {
        this.reset.addActionListener(e -> this.formConfig.reset());
        this.refreshButton.addActionListener(e -> refresh());
        final String deleteTitle = String.format("Deleting app(%s)", this.app.name());
        this.deleteButton.addActionListener(e -> {
            final String message = String.format("Are you sure to delete Spring Cloud App(%s)", this.app.name());
            if (AzureMessager.getMessager().confirm(message, "Delete Spring Cloud App")) {
                AzureTaskManager.getInstance().runInModal(deleteTitle, () -> {
                    this.setEnabled(false);
                    this.closeEditor();
                    this.app.remove();
                });
            }
        });
        final String startTitle = String.format("Starting app(%s)", this.app.name());
        this.startButton.addActionListener(e -> AzureTaskManager.getInstance().runInBackground(startTitle, () -> {
            this.setEnabled(false);
            this.app.start();
            this.refresh();
        }));
        final String stopTitle = String.format("Stopping app(%s)", this.app.name());
        this.stopButton.addActionListener(e -> AzureTaskManager.getInstance().runInBackground(stopTitle, () -> {
            this.setEnabled(false);
            this.app.stop();
            this.refresh();
        }));
        final String restartTitle = String.format("Restarting app(%s)", this.app.name());
        this.restartButton.addActionListener(e -> AzureTaskManager.getInstance().runInBackground(restartTitle, () -> {
            this.setEnabled(false);
            this.app.restart();
            this.refresh();
        }));
        final String saveTitle = String.format("Saving updates of app(%s)", this.app.name());
        this.saveButton.addActionListener(e -> AzureTaskManager.getInstance().runInBackground(saveTitle, () -> {
            this.setEnabled(false);
            this.reset.setVisible(false);
            new DeploySpringCloudAppTask(getConfig()).execute();
            this.refresh();
        }));
        this.formConfig.setDataChangedListener((data) -> {
            final boolean changedFromOrigin = !Objects.equals(this.getConfig(), this.originalConfig);
            this.reset.setVisible(changedFromOrigin);
            this.saveButton.setEnabled(changedFromOrigin);
        });
        AzureEventBus.after("springcloud|app.remove", (SpringCloudApp app) -> {
            if (this.app.name().equals(app.name())) {
                AzureMessager.getMessager().info(String.format("Spring Cloud App(%s) is deleted", this.app.name()), "");
                this.closeEditor();
            }
        });
        AzureEventBus.after("springcloud|app.update", (SpringCloudApp app) -> {
            if (this.app.name().equals(app.name())) {
                this.refresh();
            }
        });
    }

    @Nonnull
    private SpringCloudAppConfig getConfig() {
        final SpringCloudAppConfig config = this.formConfig.getData();
        config.setSubscriptionId(this.app.subscriptionId());
        config.setClusterName(this.app.getCluster().name());
        config.setAppName(this.app.name());
        return config;
    }

    private void refresh() {
        this.reset.setVisible(false);
        this.saveButton.setEnabled(false);
        AzureTaskManager.getInstance().runLater(() -> {
            final String refreshTitle = String.format("Refreshing app(%s)...", Objects.requireNonNull(this.app).name());
            AzureTaskManager.getInstance().runInBackground(refreshTitle, () -> {
                this.app.refresh();
                final SpringCloudDeployment deployment = Optional.ofNullable(this.app.activeDeployment()).map(d -> d.refresh()).orElse(null);
                AzureTaskManager.getInstance().runLater(this::rerender);
            });
        });
    }

    private void setEnabled(boolean enabled) {
        this.saveButton.setEnabled(enabled);
        this.startButton.setEnabled(enabled);
        this.stopButton.setEnabled(enabled);
        this.restartButton.setEnabled(enabled);
        this.deleteButton.setEnabled(enabled);
        this.formConfig.setEnabled(enabled);
        this.panelInstances.setEnabled(enabled);
    }

    private void resetToolbar(@Nullable SpringCloudDeployment deployment) {
        if (Objects.isNull(deployment)) {
            AzureMessager.getMessager().warning(String.format("App(%s) has no active deployment", this.app.name()), null);
            this.setEnabled(false);
            return;
        }
        final SpringCloudDeploymentStatus status = deployment.entity().getStatus();
        switch (status) {
            case STOPPED:
                this.setEnabled(true);
                this.stopButton.setEnabled(false);
                this.restartButton.setEnabled(false);
                break;
            case RUNNING:
                this.setEnabled(true);
                this.startButton.setEnabled(false);
                break;
            case FAILED:
                this.setEnabled(false);
                this.deleteButton.setEnabled(true);
                break;
            case ALLOCATING:
            case UPGRADING:
            case COMPILING:
            case UNKNOWN:
                this.setEnabled(false);
                break;
            default:
        }
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return contentPanel;
    }

    @Nonnull
    @Override
    public String getName() {
        return this.app.name();
    }

    @Override
    public void dispose() {
    }

    private void closeEditor() {
//        DefaultLoader.getUIHelper().closeSpringCloudAppPropertyView(project, this.app.entity().getId());
    }

    private void createUIComponents() {
    }
}
