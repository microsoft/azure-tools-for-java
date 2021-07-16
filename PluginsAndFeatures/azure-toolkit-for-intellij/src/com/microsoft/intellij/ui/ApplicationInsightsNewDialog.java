/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.ui.TitlePanel;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsNewDialog extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private SubscriptionComboBox comboSub;
    private JComboBox comboGrp;
    private RegionComboBox comboReg;
    private JRadioButton createNewBtn;
    private JRadioButton useExistingBtn;
    private JTextField textGrp;
    private Subscription currentSub;
    static ApplicationInsightsResource resourceToAdd;
    private AzureManager azureManager;
    private Runnable onCreate;

    public ApplicationInsightsNewDialog() {
        super(true);
        setTitle(message("aiErrTtl"));
        try {
            azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                AzurePlugin.log("Not signed in");
            }
        } catch (Exception ex) {
            AzurePlugin.log("Not signed in", ex);
        }
        init();
    }

    protected void init() {
        super.init();
        comboSub.addItemListener(subscriptionListener());
        final ButtonGroup resourceGroup = new ButtonGroup();
        resourceGroup.add(createNewBtn);
        resourceGroup.add(useExistingBtn);
        final ItemListener updateListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final boolean isNewGroup = createNewBtn.isSelected();
                textGrp.setEnabled(isNewGroup);
                comboGrp.setEnabled(!isNewGroup);
            }
        };
        createNewBtn.addItemListener(updateListener);
        useExistingBtn.addItemListener(updateListener);

        createNewBtn.setSelected(true);
    }

    private void createUIComponents() {
        this.comboSub = new SubscriptionComboBox();
        this.comboReg = new RegionComboBox();
    }

    private ItemListener subscriptionListener() {
        return e -> {
            Subscription newSub = (Subscription) comboSub.getSelectedItem();
            String prevResGrpVal = (String) comboGrp.getSelectedItem();
            this.comboReg.setSubscription(newSub);
            if (currentSub.equals(newSub)) {
                populateResourceGroupValues(currentSub.getId(), prevResGrpVal);
            } else {
                populateResourceGroupValues(currentSub.getId(), "");
            }
            currentSub = newSub;
        };
    }

    private void populateResourceGroupValues(String subscriptionId, String valtoSet) {
        try {
            List<String> groupStringList = AzureMvpModel.getInstance().getResourceGroups(subscriptionId).stream()
                    .map(ResourceEx::getResource).map(ResourceGroup::getName).collect(Collectors.toList());
            if (groupStringList.size() > 0) {
                String[] groupArray = groupStringList.toArray(new String[groupStringList.size()]);
                comboGrp.removeAllItems();
                comboGrp.setModel(new DefaultComboBoxModel(groupArray));
                if (valtoSet == null || valtoSet.isEmpty() || !groupStringList.contains(valtoSet)) {
                    comboGrp.setSelectedItem(groupArray[0]);
                } else {
                    comboGrp.setSelectedItem(valtoSet);
                }
            }
        } catch (Exception ex) {
            AzurePlugin.log(message("getValuesErrMsg"), ex);
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected JComponent createTitlePane() {
        return new TitlePanel(message("newKeyTtl"), message("newKeyMsg"));
    }

    @Override
    protected void doOKAction() {
        final boolean grpNotSelected = Objects.isNull(comboGrp.getSelectedItem()) || ((String) comboGrp.getSelectedItem()).isEmpty();
        final boolean regNotSelected = Objects.isNull(comboReg.getSelectedItem()) || ((String) comboReg.getSelectedItem()).isEmpty();
        final boolean subNotSelected = comboSub.getSelectedItem() == null;
        if (txtName.getText().trim().isEmpty()
                || subNotSelected
                || (grpNotSelected && useExistingBtn.isSelected())
                || (textGrp.getText().isEmpty() && createNewBtn.isSelected())
                || regNotSelected) {
            if (subNotSelected || comboSub.getItemCount() <= 0) {
                PluginUtil.displayErrorDialog(message("aiErrTtl"), message("noSubErrMsg"));
            } else if (grpNotSelected || comboGrp.getItemCount() <= 0) {
                PluginUtil.displayErrorDialog(message("aiErrTtl"), message("noResGrpErrMsg"));
            } else {
                PluginUtil.displayErrorDialog(message("aiErrTtl"), message("nameEmptyMsg"));
            }
        } else {
            boolean isNewGroup = createNewBtn.isSelected();
            String resourceGroup = isNewGroup ? textGrp.getText() : (String) comboGrp.getSelectedItem();
            final AzureText title = AzureOperationBundle.title("ai.create.rg", txtName.getText(), resourceGroup);
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, title, false, () -> {
                try {
                    ApplicationInsightsComponent resource = AzureSDKManager.createInsightsResource(
                            currentSub,
                            resourceGroup,
                            isNewGroup,
                            txtName.getText(),
                            (String) comboReg.getSelectedItem());
                    resourceToAdd = new ApplicationInsightsResource(resource, currentSub, true);
                    if (onCreate != null) {
                        onCreate.run();
                    }
                } catch (Exception ex) {
                    PluginUtil.displayErrorDialogInAWTAndLog(message("aiErrTtl"), message("resCreateErrMsg"), ex);
                }
            }));
            super.doOKAction();
        }
    }

    public static ApplicationInsightsResource getResource() {
        return resourceToAdd;
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }
}
