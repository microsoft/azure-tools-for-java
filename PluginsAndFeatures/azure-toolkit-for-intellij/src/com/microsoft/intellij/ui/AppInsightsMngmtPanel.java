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

package com.microsoft.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsPageTableElement;
import com.microsoft.applicationinsights.preference.ApplicationInsightsPageTableElements;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.ijidea.actions.SelectSubscriptionsAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.util.MethodUtils;
import com.microsoft.intellij.util.PluginUtil;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class AppInsightsMngmtPanel implements AzureAbstractConfigurablePanel {
    private JPanel contentPane;
    private JButton signInOutBtn;
    private JButton selectSubscriptionsBtn;
    private JTable insightsTable;
    private JButton newButton;
    private JButton removeButton;
    private JButton detailsButton;
    private JButton addButton;
    private Project myProject;
    private static final String DISPLAY_NAME = "Application Insights";

    public AppInsightsMngmtPanel(Project project) {
        this.myProject = project;
    }

    @Override
    public void init() {
        insightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        AzureSettings.getSafeInstance(myProject).loadAppInsights();
        loadInfoFirstTime();
        insightsTable.setModel(new InsightsTableModel(getTableContent()));
        for (int i = 0; i < insightsTable.getColumnModel().getColumnCount(); i++) {
            TableColumn each = insightsTable.getColumnModel().getColumn(i);
            each.setPreferredWidth(InsightsTableModel.getColumnWidth(i, 450));
        }
        selectSubscriptionsBtn.addActionListener(manageSubscriptionsListener());
        signInOutBtn.addActionListener(signInOutListener());
        newButton.addActionListener(newButtonListener());
        removeButton.addActionListener(removeButtonListener());
        addButton.addActionListener(addButtonListener());
        detailsButton.addActionListener(detailsButtonListener());

        detailsButton.setEnabled(false);
        removeButton.setEnabled(false);

        insightsTable.getSelectionModel().addListSelectionListener(createAccountsTableListener());
    }

    @Override
    public boolean doOKAction() {
        return true;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getSelectedValue() {
        int selectedIndex = insightsTable.getSelectedRow();
        if (selectedIndex >= 0) {
            return ((InsightsTableModel) insightsTable.getModel()).getKeyAtIndex(selectedIndex);
        }
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void reset() {
    }

    private void loadInfoFirstTime() {
        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                List<SubscriptionDetail> subList = azureManager.getSubscriptionManager().getSubscriptionDetails();
                if (subList.size() > 0) {
//                if (!AzureSettings.getSafeInstance(myProject).isAppInsightsLoaded()) {
                    updateApplicationInsightsResourceRegistry(subList, myProject);
                } else {
                    // just show manually added list from preferences
                    // Neither clear subscription list nor show sign in dialog as user may just want to add key manually.
                    keeepManuallyAddedList(myProject);
                }
//                } else {
                // show list from preferences - getTableContent() does it. So nothing to handle here
//                }
            } else {
                // just show manually added list from preferences
                keeepManuallyAddedList(myProject);
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    private List<ApplicationInsightsPageTableElement> getTableContent() {
        AzureSettings.getSafeInstance(myProject).loadAppInsights();
        List<ApplicationInsightsResource> resourceList = ApplicationInsightsResourceRegistry.getAppInsightsResrcList();
        List<ApplicationInsightsPageTableElement> tableRowElements = new ArrayList<ApplicationInsightsPageTableElement>();
        for (ApplicationInsightsResource resource : resourceList) {
            if (resource != null) {
                ApplicationInsightsPageTableElement ele = new ApplicationInsightsPageTableElement();
                ele.setResourceName(resource.getResourceName());
                ele.setInstrumentationKey(resource.getInstrumentationKey());
                tableRowElements.add(ele);
            }
        }
        ApplicationInsightsPageTableElements elements = new ApplicationInsightsPageTableElements();
        elements.setElements(tableRowElements);
        return elements.getElements();
    }

    public static void updateApplicationInsightsResourceRegistry(List<SubscriptionDetail> subList, Project project) throws Exception {
        // remove all resourecs that were not manually added
        keeepManuallyAddedList(project);
        for (SubscriptionDetail sub : subList) {
            if (sub.isSelected()) {
                try {
                    // fetch resources available for particular subscription
                    List<Resource> resourceList = AzureSDKManager.getApplicationInsightsResources(sub);
                    // Removal logic
                    List<ApplicationInsightsResource> importedList = ApplicationInsightsResourceRegistry.prepareAppResListFromRes(resourceList, sub);
                    // Addition logic
                    ApplicationInsightsResourceRegistry.getAppInsightsResrcList().addAll(importedList);
                } catch (Exception ex) {
                    AzurePlugin.log("Error loading AppInsights information for subscription '" + sub.getSubscriptionName() + "'");
                }
            }
        }
        AzureSettings.getSafeInstance(project).saveAppInsights();
    }

    private static void keeepManuallyAddedList(Project project) {
        List<ApplicationInsightsResource> addedList = ApplicationInsightsResourceRegistry.getAddedResources();
        ApplicationInsightsResourceRegistry.setAppInsightsResrcList(addedList);
        AzureSettings.getSafeInstance(project).saveAppInsights();
    }

    public static void refreshDataForDialog() {
        try {
            Project project = PluginUtil.getSelectedProject();
            if (AuthMethodManager.getInstance().isSignedIn()) {
                List<SubscriptionDetail> subList = AuthMethodManager.getInstance().getAzureManager().getSubscriptionManager().getSubscriptionDetails();
                // authenticated using AD. Proceed for updating application insights registry.
                updateApplicationInsightsResourceRegistry(subList, project);
            } else {
                // Neither clear subscription list nor show sign in dialog as user may just want to add key manually.
                keeepManuallyAddedList(project);
            }
        } catch(Exception ex) {
            AzurePlugin.log(ex.getMessage());
        }
    }

    private ActionListener newButtonListener() {
        return e -> {
            try {
                createNewDilaog();
            } catch(Exception ex) {
                AzurePlugin.log(ex.getMessage(), ex);
            }
        };
    }

    private void createNewDilaog() {
        try {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ApplicationInsightsNewDialog dialog = new ApplicationInsightsNewDialog();
                    dialog.setOnCreate(() -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                        ApplicationInsightsResource resource = ApplicationInsightsNewDialog.getResource();
                        if (resource != null && !ApplicationInsightsResourceRegistry.getAppInsightsResrcList().contains(resource)) {
                            ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resource);
                            AzureSettings.getSafeInstance(myProject).saveAppInsights();
                            ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
                            ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
                        }
                    }));
                    dialog.show();
                }
            }, ModalityState.defaultModalityState());
        } catch(Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    private ActionListener addButtonListener() {
        return e -> {
            ApplicationInsightsAddDialog dialog = new ApplicationInsightsAddDialog(myProject);
            dialog.show();
            if (dialog.isOK()) {
                ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
                ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
            }
        };
    }

    private ActionListener detailsButtonListener() {
        return e -> {
            int index = insightsTable.getSelectedRow();
            ApplicationInsightsResource resource = ApplicationInsightsResourceRegistry.getAppInsightsResrcList().get(index);
            ApplicationInsightsDetailsDialog dialog = new ApplicationInsightsDetailsDialog(resource);
            dialog.show();
        };
    }

    private ActionListener removeButtonListener() {
        return e -> {
            int curSelIndex = insightsTable.getSelectedRow();
            if (curSelIndex > -1) {
                String keyToRemove = ApplicationInsightsResourceRegistry.getKeyAsPerIndex(curSelIndex);
                String moduleName = MethodUtils.getModuleNameAsPerKey(myProject, keyToRemove);
                if (moduleName != null && !moduleName.isEmpty()) {
                    PluginUtil.displayErrorDialog(message("aiErrTtl"), String.format(message("rsrcUseMsg"), moduleName));
                } else {
                    int choice = Messages.showOkCancelDialog(message("rsrcRmvMsg"), message("aiErrTtl"), Messages.getQuestionIcon());
                    if (choice == Messages.OK) {
                        ApplicationInsightsResourceRegistry.getAppInsightsResrcList().remove(curSelIndex);
                        AzureSettings.getSafeInstance(myProject).saveAppInsights();
                        ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
                        ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
                    }
                }
            }
        };
    }

    private ActionListener manageSubscriptionsListener() {
        return e -> {
            SelectSubscriptionsAction.onShowSubscriptions(myProject);
            loadInfoFirstTime();
            ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
            ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
        };
    }

    private ActionListener signInOutListener() {
        return e -> {
            AzureSignInAction.onAzureSignIn(myProject);
            loadInfoFirstTime();
            ((InsightsTableModel) insightsTable.getModel()).setResources(getTableContent());
            ((InsightsTableModel) insightsTable.getModel()).fireTableDataChanged();
        };
    }

    private ListSelectionListener createAccountsTableListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean buttonsEnabled = insightsTable.getSelectedRow() > -1;
                detailsButton.setEnabled(buttonsEnabled);
                removeButton.setEnabled(buttonsEnabled);
            }
        };
    }

    private static class InsightsTableModel extends AbstractTableModel {
        public static final String[] COLUMNS = new String[]{"Resource Name", "Instrumentation Keys"};
        private java.util.List<ApplicationInsightsPageTableElement> resources;

        public InsightsTableModel(List<ApplicationInsightsPageTableElement> accounts) {
            this.resources = accounts;
        }

        public void setResources(List<ApplicationInsightsPageTableElement> accounts) {
            this.resources = accounts;
        }

        public String getKeyAtIndex(int index) {
            return resources.get(index).getInstrumentationKey();
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        public static int getColumnWidth(int column, int totalWidth) {
            switch (column) {
                case 0:
                    return (int) (totalWidth * 0.4);
                default:
                    return (int) (totalWidth * 0.6);
            }
        }

        public int getRowCount() {
            return resources.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            ApplicationInsightsPageTableElement resource = resources.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return resource.getResourceName();
                case 1:
                    return resource.getInstrumentationKey();
            }
            return null;
        }
    }
}
