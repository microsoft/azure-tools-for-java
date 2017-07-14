/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.container.ui;

import com.intellij.ui.wizard.WizardNavigationState;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.intellij.container.mvp.PublishWizardPageView;
import com.microsoft.intellij.container.mvp.StepTwoPagePresenter;
import com.microsoft.intellij.container.mvp.StepTwoPageView;
import com.microsoft.intellij.ui.components.AzureWizardStep;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class StepTwoPage extends AzureWizardStep<PublishWizardModel> implements StepTwoPageView, PublishWizardPageView {
    private static final String TEXT_DESCRIPTION = "Select existing or create new Web App to deploy";
    private static final String TEXT_TITLE = "Deploy to Azure Web App on Linux";
    private static final String REGEX_VALID_RG_NAME = "^[\\w-]*\\w+$";
    private static final String TEXT_TAB_CREATE = "Create New";
    private static final String TEXT_TAB_UPDATE = "Use Existing";
    private static final String REGEX_VALID_APP_NAME = "^[\\w-]*\\w+$";

    private final StepTwoPagePresenter<StepTwoPage> presenter;
    private JTabbedPane tabFolder;
    private JTable tableWebApps;
    private JButton btnRefreshList;
    private JTextField textAppName;
    private JRadioButton btnResourceGroupCreateNew;
    private JRadioButton btnResourceGroupUseExisting;
    private JComboBox comboSubscription;
    private JTextField textResourceGroupName;
    private JPanel rootPanel;
    private JComboBox comboResourceGroup;
    private JPanel tbtmUpdate;
    private JPanel tbtmCreate;
    private PublishWizardModel model;


    public StepTwoPage(PublishWizardModel publishWizardModel) {
        super(TEXT_TITLE, TEXT_DESCRIPTION);
        model = publishWizardModel;
        presenter = new StepTwoPagePresenter<StepTwoPage>();
        presenter.onAttachView(this);

        tableWebApps.setSelectionMode(SINGLE_SELECTION);
        setTableWebAppsLoading();

        tbtmCreate.setName(TEXT_TAB_CREATE);
        tbtmUpdate.setName(TEXT_TAB_UPDATE);

        btnRefreshList.addActionListener(event -> onRefreshButtonSelection());
        comboSubscription.addActionListener(event -> onComboSubscriptionSelection());
        btnResourceGroupCreateNew.addActionListener(event -> radioResourceGroupLogic());
        btnResourceGroupUseExisting.addActionListener(event -> radioResourceGroupLogic());
        tabFolder.addChangeListener(event -> onTabFolderChange());
        textResourceGroupName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextResourceGroupNameModify();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextResourceGroupNameModify();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onTextResourceGroupNameModify();
            }
        });

        textAppName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextAppNameModify();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextAppNameModify();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onTextAppNameModify();
            }
        });
//        presenter.onRefreshWebAppsOnLinux();
        presenter.onListWebAppsOnLinux();
    }

    private void onTabFolderChange() {
        setControlButtonEnabledStatus(true);
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        model.getDialog().setOKActionEnabled(false);
        setControlButtonEnabledStatus(true);
        return rootPanel;
    }

    private void setTableWebAppsLoading() {
        DefaultTableModel tableModel = new DefaultTableModel(new String[][]{{"Loading...", ""}}, new String[]{"WebApp", "ResourceGroup"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableWebApps.setModel(tableModel);
    }

    private boolean isPageCompleted() {
        int index = tabFolder.getSelectedIndex();
        if (tabFolder.getTitleAt(index).equals(tbtmCreate.getName())) {
            return isTabCreateComplete();
        } else if (tabFolder.getTitleAt(index).equals(tbtmUpdate.getName())) {
            return isTabUpdateComplete();
        } else {
            return false;
        }
    }

    private boolean isTabUpdateComplete() {
        return this.tableWebApps.getSelectedRow() >= 0;
    }

    private boolean isTabCreateComplete() {
        boolean a = this.comboSubscription.getSelectedIndex() >= 0;
        boolean d = textAppName.getText().matches(REGEX_VALID_APP_NAME);
        boolean b = btnResourceGroupCreateNew.isSelected()
                && textResourceGroupName.getText().matches(REGEX_VALID_RG_NAME);
        boolean c = this.btnResourceGroupUseExisting.isSelected() && this.comboResourceGroup.getSelectedIndex() >= 0;
        return a && d && (b || c);
    }

    private void onTextAppNameModify() {
        if (textAppName.getText().matches(REGEX_VALID_APP_NAME)) {
            textAppName.setForeground(new Color(0, 0, 0));
        } else {
            textAppName.setForeground(new Color(255, 0, 0));
        }
        setControlButtonEnabledStatus(true);
    }

    private void onTextResourceGroupNameModify() {
        if (textResourceGroupName.getText().matches(REGEX_VALID_RG_NAME)) {
            textResourceGroupName.setForeground(new Color(0, 0, 0));
        } else {
            textResourceGroupName.setForeground(new Color(255, 0, 0));
        }
        setControlButtonEnabledStatus(true);
    }

    private void radioResourceGroupLogic() {
        boolean enabled = btnResourceGroupCreateNew.isSelected();
        textResourceGroupName.setEnabled(enabled);
        comboResourceGroup.setEnabled(!enabled);
    }

    private void onRefreshButtonSelection() {
        setTableWebAppsLoading();
        presenter.onRefreshWebAppsOnLinux();
    }

    private void setWidgetsEnabledStatus(boolean enabledStatus) {
        setEnabledRecursively(tabFolder, enabledStatus);
        if (enabledStatus == true) {
            radioResourceGroupLogic();
        }
    }

    private void setControlButtonEnabledStatus(boolean enabled) {
        if (model.getCurrentStep() != this) {
            return;
        }
        if (enabled) {
            model.resetDefaultButtonEnabledStatus();
            model.setAndCacheFinishEnabled(isPageCompleted());
        } else {
            model.getCurrentNavigationState().setEnabledToAll(false);
        }
    }

    private void onComboSubscriptionSelection() {
        int index = comboSubscription.getSelectedIndex();
        if (index < 0) {
            return;
        }
        presenter.onChangeSubscription(index);
    }

    void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    @Override
    public boolean onFinish() {
        if (model.getDialog().isOKActionEnabled()) {
            return super.onFinish();
        } else {
            onWizardFinishPressed();
            return true; //avoid going to doCancel()
        }
    }

    @Override
    public void onWizardNextPressed() {
        return;
    }

    @Override
    public void onWizardFinishPressed() {
        int index = tabFolder.getSelectedIndex();

        if (tabFolder.getTitleAt(index).equals(tbtmCreate.getName())) {
            deployToNewWebApp();
        } else if (tabFolder.getTitleAt(index).equals(tbtmUpdate.getName())) {
            deployToExisitingWebApp();
        }
    }

    private void deployToExisitingWebApp() {
        presenter.onDeployToExisitingWebApp(tableWebApps.getSelectedRow());
    }

    private void deployToNewWebApp() {
        if (btnResourceGroupCreateNew.isSelected()) {
            try {
                presenter.onDeployToNewWebApp(textAppName.getText(), comboSubscription.getSelectedIndex(),
                        textResourceGroupName.getText(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (btnResourceGroupUseExisting.isSelected()) {
            try {
                presenter.onDeployToNewWebApp(textAppName.getText(), comboSubscription.getSelectedIndex(),
                        comboResourceGroup.getSelectedItem().toString(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void fillWebApps(List<SiteInner> wal) {
        List<String[]> data = new ArrayList<>();

        for (SiteInner si : wal) {
            data.add(new String[]{si.name(), si.resourceGroup()});
        }
        String[][] rowData = new String[data.size()][2];
        data.toArray(rowData);
        DefaultTableModel tableModel = new DefaultTableModel(rowData, new String[]{"WebApp", "ResourceGroup"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableWebApps.setModel(tableModel);
        tableWebApps.getSelectionModel().addListSelectionListener(e -> onTableWebAppsSelection());

    }

    private void onTableWebAppsSelection() {
        setControlButtonEnabledStatus(true);
    }

    @Override
    public void fillSubscriptions(List<SubscriptionDetail> sdl) {
        if (sdl == null || sdl.size() <= 0) {
            return;
        }
        comboSubscription.removeAllItems();
        for (SubscriptionDetail sd : sdl) {
            comboSubscription.addItem(sd.getSubscriptionName());
        }
        if (comboSubscription.getItemCount() > 0) {
            comboSubscription.setSelectedIndex(0);
        }
    }

    @Override
    public void fillResourceGroups(List<ResourceGroup> rgl) {
        if (rgl == null || rgl.size() <= 0) {
            return;
        }
        comboResourceGroup.removeAllItems();

        for (ResourceGroup rg : rgl) {
            comboResourceGroup.addItem(rg.name());
        }

        if (comboResourceGroup.getItemCount() > 0) {
            comboResourceGroup.setSelectedIndex(0);
        }
    }

    @Override
    public void finishDeploy() {
        this.model.getDialog().setOKActionEnabled(true);
        model.finish();
    }

    @Override
    public void onRequestPending(Object payload) {
        setWidgetsEnabledStatus(false);
        setControlButtonEnabledStatus(false);
    }

    @Override
    public void onRequestSucceed(Object payload) {
        setWidgetsEnabledStatus(true);
        setControlButtonEnabledStatus(true);
    }

    @Override
    public void onRequestFail(Object payload) {
        setWidgetsEnabledStatus(true);
        setControlButtonEnabledStatus(true);
    }


    // TODO: dispose detach presenter
}
