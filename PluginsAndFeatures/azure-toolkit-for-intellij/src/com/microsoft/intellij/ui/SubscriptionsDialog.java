/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.intellij.actions.SelectSubscriptionsAction;
import com.microsoft.intellij.util.JTableUtils;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.GET_SUBSCRIPTIONS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SELECT_SUBSCRIPTIONS;

public class SubscriptionsDialog extends AzureDialogWrapper {
    private static final int CHECKBOX_COLUMN = 0;
    private static final Logger LOGGER = Logger.getInstance(SubscriptionsDialog.class);
    private final Project project;
    private JPanel contentPane;
    private JPanel panelTable;
    private JBTable table;
    private List<SubscriptionDetail> sdl;

    private SubscriptionsDialog(List<SubscriptionDetail> sdl, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.sdl = sdl;
        this.project = project;
        setModal(true);
        setTitle("Select Subscriptions");
        setOKButtonText("Select");

        setSubscriptions();

        init();

        table.setAutoCreateRowSorter(true);
    }

    /**
     * Open select-subscription dialog.
     */
    public static SubscriptionsDialog go(List<SubscriptionDetail> sdl, Project project) {
        if (CollectionUtils.isEmpty(sdl)) {
            PluginUtil.displayInfoDialog("No subscription", "No subscription in current account, you may get a free "
                + "one from https://azure.microsoft.com/en-us/free/");
            return null;
        }
        SubscriptionsDialog d = new SubscriptionsDialog(sdl, project);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{this.getOKAction(), this.getCancelAction()};
    }

    private void refreshSubscriptions() {
        EventUtil.executeWithLog(ACCOUNT, GET_SUBSCRIPTIONS, (operation) -> {
            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            if (manager == null) {
                return;
            }
            final SubscriptionManager subscriptionManager = manager.getSubscriptionManager();
            subscriptionManager.cleanSubscriptions();
            Azure.az(AzureAccount.class).account().reloadSubscriptions().block();
            SelectSubscriptionsAction.loadSubscriptions(subscriptionManager, project);

            //System.out.println("refreshSubscriptions: calling getSubscriptionDetails()");
            sdl = subscriptionManager.getSubscriptionDetails();
            setSubscriptions();
            // to notify subscribers
            subscriptionManager.setSubscriptionDetails(sdl);
        }, (ex) -> {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), "Refresh Subscriptions Error");
        });
    }

    private void setSubscriptions() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        sdl.sort((sub1, sub2) -> {
            if (sub1.isSelected() != sub2.isSelected()) {
                return sub1.isSelected() ? -1 : 0;
            }
            return StringUtils.compareIgnoreCase(sub1.getSubscriptionName(), sub2.getSubscriptionName());
        });
        for (SubscriptionDetail sd : sdl) {
            model.addRow(new Object[]{sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
        }
        model.fireTableDataChanged();
    }

    private void createUIComponents() {
        DefaultTableModel model = new SubscriptionTableModel();
        model.addColumn("Selected"); // Set the text read by JAWS
        model.addColumn("Subscription name");
        model.addColumn("Subscription ID");

        table = new JBTable();
        table.setModel(model);
        TableColumn column = table.getColumnModel().getColumn(CHECKBOX_COLUMN);
        column.setHeaderValue(""); // Don't show title text
        column.setMinWidth(23);
        column.setMaxWidth(23);
        JTableUtils.enableBatchSelection(table, CHECKBOX_COLUMN);
        table.getTableHeader().setReorderingAllowed(false);
        new TableSpeedSearch(table);

        AnActionButton refreshAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                this.setEnabled(false);
                model.setRowCount(0);
                model.fireTableDataChanged();
                table.getEmptyText().setText("Refreshing");
                AppInsightsClient.createByType(AppInsightsClient.EventType.Subscription, "", "Refresh", null);
                final AzureText title = AzureOperationBundle.title("account|subscription.refresh");
                final AzureTask task = new AzureTask(project, title, true, () -> {
                    try {
                        SubscriptionsDialog.this.refreshSubscriptions();
                    } finally {
                        this.setEnabled(true);
                    }
                }, AzureTask.Modality.ANY);
                AzureTaskManager.getInstance().runInBackground(task);
            }
        };

        ToolbarDecorator tableToolbarDecorator =
            ToolbarDecorator.createDecorator(table)
                .disableUpDownActions()
                .addExtraActions(refreshAction);

        panelTable = tableToolbarDecorator.createPanel();

    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rc = model.getRowCount();
        int unselectedCount = 0;
        for (int ri = 0; ri < rc; ++ri) {
            boolean selected = (boolean) model.getValueAt(ri, CHECKBOX_COLUMN);
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
            boolean selected = (boolean) model.getValueAt(ri, CHECKBOX_COLUMN);
            this.sdl.get(ri).setSelected(selected);
        }

        List<String> selectedIds = this.sdl.stream().filter(SubscriptionDetail::isSelected)
            .map(SubscriptionDetail::getSubscriptionId).collect(Collectors.toList());
        IdentityAzureManager.getInstance().selectSubscriptionByIds(selectedIds);
        IdentityAzureManager.getInstance().getSubscriptionManager().notifySubscriptionListChanged();
        Mono.fromCallable(() -> {
            AzureAccount az = Azure.az(AzureAccount.class);
            selectedIds.stream().limit(5).forEach(sid -> {
                // pr-load regions
                az.listRegions(sid);
            });
            return 1;
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();

        final Map<String, String> properties = new HashMap<>();
        properties.put("subsCount", String.valueOf(rc));
        properties.put("selectedSubsCount", String.valueOf(rc - unselectedCount));
        EventUtil.logEvent(EventType.info, ACCOUNT, SELECT_SUBSCRIPTIONS, null);
        super.doOKAction();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "SubscriptionsDialog";
    }

    private static class SubscriptionTableModel extends DefaultTableModel {
        final Class[] columnClass = new Class[]{
            Boolean.class, String.class, String.class
        };

        @Override
        public boolean isCellEditable(int row, int col) {
            return (col == CHECKBOX_COLUMN);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
        }
    }

}
