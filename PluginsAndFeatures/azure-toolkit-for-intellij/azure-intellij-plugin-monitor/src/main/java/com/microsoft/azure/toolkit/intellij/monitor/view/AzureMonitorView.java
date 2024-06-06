/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.AnActionLink;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.monitor.AzureMonitorManager;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.MonitorTreePanel;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.WorkspaceSelectionDialog;
import com.microsoft.azure.toolkit.intellij.monitor.view.right.MonitorTabbedPane;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AzureMonitorView extends JPanel implements Disposable {
    private JPanel contentPanel;
    private JPanel leftPanel;
    private ActionLink changeWorkspace;
    @Getter
    private MonitorTreePanel monitorTreePanel;
    private JLabel workspaceName;
    private JPanel workspaceHeader;
    private JPanel rightPanel;
    private MonitorTabbedPane tabbedPanePanel;
    @Getter
    @Nullable
    private LogAnalyticsWorkspace selectedWorkspace;
    private final AzureEventBus.EventListener workspaceChangeListener;
    private final AzureEventBus.EventListener subscriptionChangeListener;
    @Getter
    private final Project project;

    public AzureMonitorView(Project project, @Nullable LogAnalyticsWorkspace logAnalyticsWorkspace, boolean isTableTab, @Nullable String resourceId) {
        super();
        this.selectedWorkspace = logAnalyticsWorkspace;
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init(isTableTab, resourceId);
        this.workspaceChangeListener =  new AzureEventBus.EventListener(e -> {
            this.selectedWorkspace = (LogAnalyticsWorkspace) e.getSource();
            this.updateWorkspaceNameLabel();
            Optional.ofNullable(this.selectedWorkspace).ifPresent(w -> PropertiesComponent.getInstance().setValue(AzureMonitorManager.AZURE_MONITOR_SELECTED_WORKSPACE, w.getId()));
        });
        final IAzureMessager messager = AzureMessager.getMessager();
        this.subscriptionChangeListener = new AzureEventBus.EventListener(e -> {
            LogAnalyticsWorkspace defaultWorkspace = null;
            final Account account = Azure.az(AzureAccount.class).account();
            if (Objects.nonNull(account) && account.getSelectedSubscriptions().size() > 0) {
                final Subscription subscription = account.getSelectedSubscriptions().get(0);
                try {
                    final List<LogAnalyticsWorkspace> workspaceList = Azure.az(AzureLogAnalyticsWorkspace.class)
                            .logAnalyticsWorkspaces(subscription.getId()).list().stream().toList();
                    if (workspaceList.size() > 0) {
                        defaultWorkspace = workspaceList.get(0);
                    }
                } catch (final Throwable t) {
                    messager.error(t);
                }
            }
            this.selectedWorkspace = defaultWorkspace;
            this.updateWorkspaceNameLabel();
        });
        AzureEventBus.on("azure.monitor.change_workspace", workspaceChangeListener);
        AzureEventBus.on("account.subscription_changed.account", subscriptionChangeListener);
        AzureTaskManager.getInstance().runInBackground(AzureString.fromString("Loading logs"), () -> this.monitorTreePanel.refresh());
    }

    public String getQueryString(String queryName) {
        return String.format("%s | take %s", this.getMonitorTreePanel().getQueryString(queryName), Azure.az().config().getMonitorQueryRowNumber());
    }

    public void setSelectedWorkspace(@Nullable LogAnalyticsWorkspace workspace) {
        this.selectedWorkspace = workspace;
        this.updateWorkspaceNameLabel();
    }

    public void setInitResourceId(String resourceId) {
        this.tabbedPanePanel.setInitResourceId(resourceId);
        tabbedPanePanel.selectTab("AppTraces");
    }

    public void dispose() {
        AzureEventBus.off("azure.monitor.change_workspace", workspaceChangeListener);
        AzureEventBus.off("account.subscription_changed.account", subscriptionChangeListener);
    }

    private void updateWorkspaceNameLabel() {
        if (Objects.nonNull(selectedWorkspace)) {
            this.workspaceName.setText(selectedWorkspace.getName());
            this.workspaceName.setToolTipText(selectedWorkspace.getName());
            this.workspaceName.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE_MONITOR));
        } else {
            this.workspaceName.setText("Log Analytics workspace");
            this.workspaceName.setToolTipText("Log Analytics workspace is required");
            this.workspaceName.setIcon(AllIcons.General.Error);
        }
    }

    private void init(boolean isTableTab, @Nullable String resourceId) {
        final GridLayoutManager layout = new GridLayoutManager(1, 1);
        this.setLayout(layout);
        this.add(this.contentPanel, new GridConstraints(0, 0, 1, 1, 0, GridConstraints.ALIGN_FILL, 3, 3, null, null, null, 0));
        this.updateWorkspaceNameLabel();
        this.workspaceName.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        this.monitorTreePanel.setTableTab(isTableTab);
        this.tabbedPanePanel.setTableTab(isTableTab);
        this.tabbedPanePanel.setParentView(this);
        this.tabbedPanePanel.setInitResourceId(resourceId);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        this.changeWorkspace = new AnActionLink("Select", new AnAction() {
            @Override
            @AzureOperation(name = "user/monitor.select_workspace")
            public void actionPerformed(@NotNull AnActionEvent e) {
                AzureTaskManager.getInstance().runLater(() -> {
                    final WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(e.getProject(), selectedWorkspace);
                    if (dialog.showAndGet()) {
                        Optional.ofNullable(dialog.getWorkspace()).ifPresent(w -> AzureEventBus.emit("azure.monitor.change_workspace", w));
                    }
                });
            }

            @Override
            public ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        });
        this.monitorTreePanel = new MonitorTreePanel(project, this);
        this.tabbedPanePanel = new MonitorTabbedPane();
        this.rightPanel = tabbedPanePanel.getContentPanel();
    }

}
