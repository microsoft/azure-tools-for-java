/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.properties;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentStatus;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.annotation.Nonnull;
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
    private SpringCloudAppPanel appPanel;
    private ActionLink reset;

    @Nonnull
    private final Project project;
    @Nonnull
    private final SpringCloudApp app;

    public SpringCloudAppPropertiesEditor(@Nonnull Project project, @Nonnull SpringCloudApp app) {
        super();
        this.project = project;
        this.app = app;
        AzureTaskManager.getInstance().runLater(this::init);
    }

    private void init() {
        this.resetToolbar();
        this.reset.setVisible(false);
        this.saveButton.setEnabled(false);
        this.appPanel.setValueChangedListener((changedFromOrigin, data) -> {
            this.reset.setVisible(changedFromOrigin);
            this.saveButton.setEnabled(changedFromOrigin);
        });
        initListeners();
    }

    private void initListeners() {
        this.reset.addActionListener(e -> this.appPanel.reset());
        this.refreshButton.addActionListener(e -> refresh());
        final String deleteTitle = String.format("Deleting app(%s)", this.app.name());
        this.deleteButton.addActionListener(e -> {
            final String message = String.format("Are you sure to delete Spring Cloud App(%s)", this.app.name());
            if (AzureMessager.getMessager().confirm(message, "Delete Spring Cloud App")) {
                AzureTaskManager.getInstance().runInModal(deleteTitle, () -> {
                    this.setEnabled(false);
                    this.app.remove();
                    this.closeEditor();
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
            new DeploySpringCloudAppTask(this.appPanel.getData()).execute();
            this.refresh();
        }));
        AzureEventBus.after("springcloud|app.remove", (SpringCloudApp app) -> {
            if (this.app.name().equals(app.name())) {
                this.closeEditor();
            }
        });
        AzureEventBus.after("springcloud|app.update", (SpringCloudApp app) -> {
            if (this.app.name().equals(app.name())) {
                this.refresh();
            }
        });
    }

    private void refresh() {
        this.reset.setVisible(false);
        this.saveButton.setEnabled(false);
        AzureTaskManager.getInstance().runLater(() -> {
            final String refreshTitle = String.format("Refreshing app(%s)...", Objects.requireNonNull(this.app).name());
            AzureTaskManager.getInstance().runInBackground(refreshTitle, () -> {
                this.appPanel.refresh();
                this.resetToolbar();
            });
        });
    }

    private void setEnabled(boolean enabled) {
        this.saveButton.setEnabled(enabled);
        this.startButton.setEnabled(enabled);
        this.stopButton.setEnabled(enabled);
        this.restartButton.setEnabled(enabled);
        this.deleteButton.setEnabled(enabled);
        this.appPanel.setEnabled(enabled);
    }

    private void resetToolbar() {
        final SpringCloudDeployment deployment = Optional.ofNullable(app.activeDeployment()).stream().findAny()
                .or(() -> app.deployments().stream().findAny())
                .orElse(null);
        if (Objects.isNull(deployment)) {
            AzureMessager.getMessager().alert(String.format("App(%s) has no deployment", this.app.name()));
            this.closeEditor();
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
        DefaultLoader.getUIHelper().closeSpringCloudAppPropertyView(project, this.app.entity().getId());
        PluginUtil.showInfoNotificationProject(project,
                String.format("The editor for app %s is closed.", this.app.name()), "The app " + this.app.name() + " is deleted.");
    }

    private void createUIComponents() {
        this.appPanel = new SpringCloudAppPanel(this.app, this.project);
    }
}
