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

import com.intellij.openapi.ui.TitlePanel;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsNewDialog extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField txtName;
    private JComboBox comboSub;
    private JComboBox comboGrp;
    private JComboBox comboReg;
    private JRadioButton createNewBtn;
    private JRadioButton useExistingBtn;
    private JTextField textGrp;
    private SubscriptionDetail currentSub;
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
        populateValues();
    }

    private ItemListener subscriptionListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SubscriptionDetail newSub = (SubscriptionDetail) comboSub.getSelectedItem();
                String prevResGrpVal = (String) comboGrp.getSelectedItem();
                if (currentSub.equals(newSub)) {
                    populateResourceGroupValues(currentSub.getSubscriptionId(), prevResGrpVal);
                } else {
                    populateResourceGroupValues(currentSub.getSubscriptionId(), "");
                }
                currentSub = newSub;
            }
        };
    }

    private void populateValues() {
        try {
            if (azureManager == null || azureManager.getSubscriptionManager() == null) {
                // Didn't sign in
                return;
            }
            List<SubscriptionDetail> subList = azureManager.getSubscriptionManager().getSubscriptionDetails()
                    .stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
            // check at least single subscription is associated with the account
            if (subList.size() > 0) {
                comboSub.setModel(new DefaultComboBoxModel(subList.toArray()));
                comboSub.setSelectedIndex(0);
                currentSub = subList.get(0);

                populateResourceGroupValues(currentSub.getSubscriptionId(), "");

                List<String> regionList = AzureSDKManager.getLocationsForInsights(currentSub);
                String[] regionArray = regionList.toArray(new String[regionList.size()]);
                comboReg.setModel(new DefaultComboBoxModel(regionArray));
                comboReg.setSelectedItem(regionArray[0]);
            }
        } catch (Exception ex) {
            AzurePlugin.log(message("getValuesErrMsg"), ex);
        }
    }

    private void populateResourceGroupValues(String subscriptionId, String valtoSet) {
        try {
            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            Azure azure = manager.getAzure(subscriptionId);
            List<com.microsoft.azure.management.resources.ResourceGroup> groups = azure.resourceGroups().list();
            List<String> groupStringList = groups.stream().map(com.microsoft.azure.management.resources.ResourceGroup::name).collect(Collectors.toList());
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
            DefaultLoader.getIdeHelper().runInBackground(null,
                                                         "Creating Application Insights Resource " + txtName.getText(),
                                                         false,
                                                         true,
                                                         "Creating Application Insights Resource " + txtName.getText(),
                                                         new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ApplicationInsightsComponent resource =
                                        AzureSDKManager.createInsightsResource(
                                                currentSub,
                                                resourceGroup,
                                                isNewGroup,
                                                txtName.getText(),
                                                (String) comboReg.getSelectedItem());
                                resourceToAdd = new ApplicationInsightsResource(resource, currentSub , true);
                                if (onCreate != null) {
                                    onCreate.run();
                                }
                            } catch (Exception ex) {
                                PluginUtil.displayErrorDialogInAWTAndLog(message("aiErrTtl"), message("resCreateErrMsg"), ex);
                            }
                        }
                    });
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
