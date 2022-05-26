/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppConfigPanel;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstancesPanel;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;

public class SpringCloudAppPropertiesEditor extends AzResourcePropertiesEditor<SpringCloudApp> {
    private JButton refreshButton;
    private JButton startButton;
    private JButton stopButton;
    private JButton restartButton;
    private JButton deleteButton;
    private JPanel contentPanel;
    private JButton saveButton;
    private ActionLink resetButton;
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
    private final SpringCloudAppDraft draft;

    public SpringCloudAppPropertiesEditor(@Nonnull Project project, @Nonnull SpringCloudApp app, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile, app, project);
        this.project = project;
        this.app = app;
        this.draft = (SpringCloudAppDraft) this.app.update();
        this.rerender();
        this.initListeners();
    }

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> {
            this.lblSubscription.setText(this.draft.getSubscription().getName());
            this.lblCluster.setText(this.draft.getParent().getName());
            this.lblApp.setText(this.draft.getName());
            AzureTaskManager.getInstance().runLater(() -> this.formConfig.updateForm(this.draft));
            AzureTaskManager.getInstance().runOnPooledThread((() -> {
                final SpringCloudAppConfig config = SpringCloudAppConfig.fromApp(this.draft);
                this.refreshToolbar();
                AzureTaskManager.getInstance().runLater(() -> this.formConfig.setValue(config));
            }));
            this.panelInstances.setApp(this.draft);
        });
    }

    private void initListeners() {
        this.resetButton.addActionListener(e -> this.reset());
        this.refreshButton.addActionListener(e -> refresh());
        final String deleteTitle = String.format("Deleting app(%s)", this.draft.getName());
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        this.deleteButton.addActionListener(e -> {
            final String message = String.format("Are you sure to delete Spring app(%s)", this.draft.name());
            if (AzureMessager.getMessager().confirm(message, "Delete Azure Spring App")) {
                tm.runInModal(deleteTitle, () -> {
                    IntellijShowPropertiesViewAction.closePropertiesView(this.draft, this.project);
                    this.draft.delete();
                });
            }
        });
        final String startTitle = String.format("Starting app(%s)", this.draft.name());
        this.startButton.addActionListener(e -> tm.runInBackground(startTitle, this.draft::start));
        final String stopTitle = String.format("Stopping app(%s)", this.draft.name());
        this.stopButton.addActionListener(e -> tm.runInBackground(stopTitle, this.draft::stop));
        final String restartTitle = String.format("Restarting app(%s)", this.draft.name());
        this.restartButton.addActionListener(e -> tm.runInBackground(restartTitle, this.draft::restart));
        final String saveTitle = String.format("Saving updates of app(%s)", this.draft.name());
        this.saveButton.addActionListener(e -> tm.runInBackground(saveTitle, this::save));
        this.formConfig.setDataChangedListener((data) -> this.refreshToolbar());
    }

    @Nonnull
    private SpringCloudAppConfig getConfig() {
        final SpringCloudAppConfig config = this.formConfig.getValue();
        config.setSubscriptionId(this.draft.getSubscriptionId());
        config.setClusterName(this.draft.getParent().getName());
        config.setAppName(this.draft.getName());
        return config;
    }

    private void save() {
        this.setEnabled(false);
        final SpringCloudAppConfig config = getConfig();
        this.draft.setConfig(config);
        final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(config);
        AzureTaskManager.getInstance().runInBackground("Saving updates", task::execute);
    }

    private void reset() {
        this.draft.reset();
        this.rerender();
    }

    @Override
    public boolean isModified() {
        final SpringCloudAppConfig draftConfig = SpringCloudAppConfig.fromApp(this.draft);
        final SpringCloudAppConfig config = this.getConfig();
        return !Objects.equals(config, draftConfig);
    }

    protected void refresh() {
        final String refreshTitle = String.format("Refreshing app(%s)...", this.draft.getName());
        AzureTaskManager.getInstance().runInBackground(refreshTitle, () -> {
            this.draft.reset();
            this.draft.refresh();
            this.rerender();
        });
    }

    private void setEnabled(boolean enabled) {
        this.resetButton.setVisible(enabled);
        this.saveButton.setEnabled(enabled);
        this.startButton.setEnabled(enabled);
        this.stopButton.setEnabled(enabled);
        this.restartButton.setEnabled(enabled);
        this.deleteButton.setEnabled(enabled);
        this.formConfig.setEnabled(enabled);
        this.panelInstances.setEnabled(enabled);
    }

    private void refreshToolbar() {
        AzureTaskManager.getInstance().runOnPooledThread((() -> {
            // get status from app instead of draft since status of draft is not correct
            final String status = this.app.getStatus();
            final boolean modified = this.isModified();
            if (StringUtils.equalsIgnoreCase(status, AzResource.Status.INACTIVE)) {
                AzureMessager.getMessager().warning(String.format("App(%s) has no active deployment", this.app.getName()), null);
            }
            AzureTaskManager.getInstance().runLater(() -> {
                final AzResourceBase.FormalStatus formalStatus = this.app.getFormalStatus();
                final boolean normal = formalStatus.isRunning() || formalStatus.isStopped();
                this.setEnabled(normal);
                this.resetButton.setVisible(modified && normal);
                this.saveButton.setEnabled(modified && normal);
                this.startButton.setEnabled(formalStatus.isStopped());
                this.stopButton.setEnabled(formalStatus.isRunning());
                this.restartButton.setEnabled(formalStatus.isRunning());
                this.deleteButton.setEnabled(!formalStatus.isWriting());
            });
        }));
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return contentPanel;
    }

    private void createUIComponents() {
    }
}
