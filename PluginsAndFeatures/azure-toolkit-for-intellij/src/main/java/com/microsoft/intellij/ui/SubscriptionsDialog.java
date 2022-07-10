/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.JTableUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SELECT_SUBSCRIPTIONS;

public class SubscriptionsDialog extends AzureDialogWrapper {
    private static final int CHECKBOX_COLUMN = 0;
    private static final Logger LOGGER = Logger.getInstance(SubscriptionsDialog.class);
    private final Project project;
    private JPanel contentPane;
    private JPanel panelTable;
    private JBTable table;

    private List<String> selected;
    private List<Subscription> candidates;

    public SubscriptionsDialog(@Nonnull Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        setModal(true);
        setTitle("Select Subscriptions");
        setOKButtonText("Select");
        init();
        table.setAutoCreateRowSorter(true);
    }

    /**
     * Open select-subscription dialog.
     */
    public void select(@Nonnull Consumer<List<String>> selectedSubscriptionsConsumer) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runOnPooledThread(() -> {
            final List<Subscription> candidates = Azure.az(AzureAccount.class).account().getSubscriptions();
            manager.runLater(() -> {
                if (CollectionUtils.isNotEmpty(candidates)) {
                    this.setCandidates(candidates);
                    if (this.showAndGet()) {
                        selectedSubscriptionsConsumer.accept(this.selected);
                    }
                } else {
                    final int result = Messages.showOkCancelDialog(
                        "No subscription in current account", "No Subscription", "Try Azure for Free",
                        Messages.getCancelButton(), Messages.getWarningIcon());
                    if (result == Messages.OK) {
                        BrowserUtil.browse("https://azure.microsoft.com/en-us/free/");
                    }
                }
            });
        });
    }

    private void loadSubscriptions() {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (!az.isLoggedIn()) {
            return;
        }
        manager.runOnPooledThread(() -> {
            final Account account = az.account();
            final List<Subscription> candidates = account.getSubscriptions();
            manager.runLater(() -> setCandidates(candidates));
        });
    }

    @AzureOperation(name = "account.refresh_subscriptions")
    private void reloadSubscriptions() {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (!az.isLoggedIn()) {
            return;
        }
        manager.runOnPooledThread(() -> {
            final Account account = az.account();
            final List<Subscription> candidates = account.reloadSubscriptions();
            manager.runLater(() -> setCandidates(candidates), AzureTask.Modality.ANY);
        });
    }

    private void setCandidates(@Nonnull List<Subscription> candidates) {
        this.candidates = candidates;
        final DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        candidates.sort((sub1, sub2) -> StringUtils.compareIgnoreCase(sub1.getName(), sub2.getName()));
        candidates.sort(Comparator.comparing(Subscription::isSelected).reversed());
        for (final Subscription sd : candidates) {
            model.addRow(new Object[]{sd.isSelected(), sd.getName(), sd.getId()});
        }
        model.fireTableDataChanged();
    }

    private void createUIComponents() {
        contentPane = new JPanel();
        contentPane.setPreferredSize(new Dimension(350, 200));

        final DefaultTableModel model = new SubscriptionTableModel();
        model.addColumn("Selected"); // Set the text read by JAWS
        model.addColumn("Subscription name");
        model.addColumn("Subscription ID");

        table = new JBTable(model);
        final TableColumn column = table.getColumnModel().getColumn(CHECKBOX_COLUMN);
        column.setHeaderValue(""); // Don't show title text
        column.setMinWidth(23);
        column.setMaxWidth(23);
        JTableUtils.enableBatchSelection(table, CHECKBOX_COLUMN);
        table.getTableHeader().setReorderingAllowed(false);
        // new TableSpeedSearch(table);
        final AnActionButton refreshAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                this.setEnabled(false);
                model.setRowCount(0);
                model.fireTableDataChanged();
                table.getEmptyText().setText("Refreshing");
                AppInsightsClient.createByType(AppInsightsClient.EventType.Subscription, "", "Refresh", null);
                final AzureString title = OperationBundle.description("account.refresh_subscriptions");
                final AzureTask<Void> task = new AzureTask<>(project, title, true, () -> {
                    try {
                        SubscriptionsDialog.this.reloadSubscriptions();
                    } finally {
                        this.setEnabled(true);
                    }
                }, AzureTask.Modality.ANY);
                AzureTaskManager.getInstance().runInBackground(task);
            }
        };
        refreshAction.registerCustomShortcutSet(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK, contentPane);
        final ToolbarDecorator tableToolbarDecorator =
            ToolbarDecorator.createDecorator(table)
                .disableUpDownActions()
                .addExtraActions(refreshAction);

        panelTable = tableToolbarDecorator.createPanel();

    }

    @Override
    protected void doOKAction() {
        final DefaultTableModel model = (DefaultTableModel) table.getModel();
        final int rc = model.getRowCount();
        int unselectedCount = 0;
        for (int ri = 0; ri < rc; ++ri) {
            final boolean selected = (boolean) model.getValueAt(ri, CHECKBOX_COLUMN);
            if (!selected) {
                unselectedCount++;
            }
        }

        if (rc != 0 && unselectedCount == rc) {
            DefaultLoader.getUIHelper().showMessageDialog(
                contentPane, "Please select at least one subscription",
                "Subscription dialog info", Messages.getInformationIcon());
            return;
        }

        for (int ri = 0; ri < rc; ++ri) {
            final boolean selected = (boolean) model.getValueAt(ri, CHECKBOX_COLUMN);
            this.candidates.get(ri).setSelected(selected);
        }

        final AzureAccount az = Azure.az(AzureAccount.class);
        this.selected = this.candidates.stream().filter(Subscription::isSelected)
            .map(Subscription::getId).collect(Collectors.toList());
        final Map<String, String> properties = new HashMap<>();
        properties.put("subsCount", String.valueOf(rc));
        properties.put("selectedSubsCount", String.valueOf(rc - unselectedCount));
        EventUtil.logEvent(EventType.info, ACCOUNT, SELECT_SUBSCRIPTIONS, null);
        super.doOKAction();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{this.getOKAction(), this.getCancelAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "SubscriptionsDialog";
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.table;
    }

    private static class SubscriptionTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int col) {
            return col == CHECKBOX_COLUMN;
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == CHECKBOX_COLUMN ? Boolean.class : super.getColumnClass(col);
        }
    }

}
