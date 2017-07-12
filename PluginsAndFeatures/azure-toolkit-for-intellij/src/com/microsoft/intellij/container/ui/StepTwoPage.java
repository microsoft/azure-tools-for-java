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

/**
 * Created by yanzh on 7/11/2017.
 */
public class StepTwoPage extends AzureWizardStep<PublishWizardModel> implements StepTwoPageView, PublishWizardPageView {
    private static final String TEXT_DESCRIPTION = "Select existing or create new Web App to deploy";
    private static final String TEXT_TITLE = "Deploy to Azure Web App on Linux";
    private static final String REGEX_VALID_RG_NAME = "^[\\w-]*\\w+$";
    private static final String TEXT_TAB_CREATE = "Use Existing";
    private static final String TEXT_TAB_UPDATE = "Refresh List";
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

        presenter.onListWebAppsOnLinux();

        btnRefreshList.addActionListener(event -> onRefreshButtonSelection());
        comboSubscription.addActionListener(event -> onComboSubscriptionSelection());
        btnResourceGroupCreateNew.addActionListener(event -> radioResourceGroupLogic());
        btnResourceGroupUseExisting.addActionListener(event -> radioResourceGroupLogic());

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
    }

    private void setTableWebAppsLoading() {
        DefaultTableModel tableModel = new DefaultTableModel(new String[][]{{"Loading...",""}}, new String[]{"WebApp", "ResourceGroup"}){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableWebApps.setModel(tableModel);
    }

    private void onTextAppNameModify() {
        if (textAppName.getText().matches(REGEX_VALID_APP_NAME)) {
            textAppName.setForeground(new Color(0,0,0));
        } else {
            textAppName.setForeground(new Color(255,0,0));
        }
    }

    private void onTextResourceGroupNameModify() {
        if (textResourceGroupName.getText().matches(REGEX_VALID_RG_NAME)) {
            textResourceGroupName.setForeground(new Color(0,0,0));
        } else {
            textResourceGroupName.setForeground(new Color(255,0,0));
        }
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
        tabFolder.setEnabled(enabledStatus);
        tbtmCreate.setEnabled(enabledStatus);
        tbtmUpdate.setEnabled(enabledStatus);
    }

    private void onComboSubscriptionSelection() {
        int index = comboSubscription.getSelectedIndex();
        presenter.onChangeSubscription(index);
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        return rootPanel;
    }

    @Override
    public void onWizardNextPressed() {
        return;
    }

    @Override
    public void onWizardFinishPressed() {
        int index = tabFolder.getSelectedIndex();
        
        if (tabFolder.getTitleAt(index).equals(TEXT_TAB_CREATE)) {
            deployToNewWebApp();
        } else if (tabFolder.getTitleAt(index).equals(TEXT_TAB_UPDATE)) {
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
        DefaultTableModel tableModel = new DefaultTableModel(rowData, new String[]{"WebApp", "ResourceGroup"});
        tableWebApps.setModel(tableModel);

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

    }

    @Override
    public void onRequestPending(Object payload) {
        setWidgetsEnabledStatus(false);
        setDialogButtonsEnabled(false);
    }
    private void setDialogButtonsEnabled(boolean enabledStatus){
        model.getDialog().getPrevButton().setEnabled(enabledStatus);
        model.getDialog().getFinishButton().setEnabled(enabledStatus);
        model.getDialog().getCancelButton().setEnabled(enabledStatus);
    }

    @Override
    public void onRequestSucceed(Object payload) {
        setWidgetsEnabledStatus(true);
        setDialogButtonsEnabled(true);
    }

    @Override
    public void onRequestFail(Object payload) {
        setWidgetsEnabledStatus(true);
        setDialogButtonsEnabled(true);
    }
}
