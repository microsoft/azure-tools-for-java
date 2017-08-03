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

package com.microsoft.intellij.runner.container.webapponlinux.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.intellij.runner.container.webapponlinux.WebAppOnLinuxDeployConfiguration;
import com.microsoft.intellij.runner.container.webapponlinux.WebAppOnLinuxDeployModel;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.stream.Collectors;

public class SettingPanel implements WebAppOnLinuxDeployView {
    private static final String REGEX_VALID_RG_NAME = "^[\\w-]*\\w+$";
    private static final String REGEX_VALID_APP_NAME = "^[\\w-]*\\w+$";
    private Project project;
    private JTextField textServerUrl;
    private JTextField textUsername;
    private JPasswordField passwordField;
    private JTextField textAppName;
    private JComboBox comboSubscription;
    private JComboBox comboResourceGroup;
    private JButton btnRefreshList;
    private JTextField textImageTag;
    private JTextField textStartupFile;
    private JComboBox comboWebApps;
    private JPanel panelUpdate;
    private JPanel panelCreate;
    private JPanel rootPanel;
    private JPanel pnlWebAppOnLinuxTable;
    private JBTable webAppTable;
    private List<ResourceEx<SiteInner>> cachedList;
    private ResourceEx<SiteInner> selectedWebApp;

    private final WebAppOnLinuxDeployPresenter<SettingPanel> webAppOnLinuxDeployPresenter;

    public SettingPanel() {
        webAppOnLinuxDeployPresenter = new WebAppOnLinuxDeployPresenter<>();
        webAppOnLinuxDeployPresenter.onAttachView(this);

        btnRefreshList.addActionListener(event -> onRefreshButtonSelection());
        comboResourceGroup.setRenderer(new ListCellRendererWrapper<ResourceGroup>() {
            @Override
            public void customize(JList jList, ResourceGroup resourceGroup, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (resourceGroup != null) {
                    setText(resourceGroup.name());
                }
            }

        });
        comboSubscription.setRenderer(new ListCellRendererWrapper<Subscription>() {
            @Override
            public void customize(JList jList, Subscription subscription, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (subscription != null) {
                    setText(String.format("%s (%s)", subscription.displayName(), subscription.subscriptionId()));
                }
            }
        });
        comboSubscription.addActionListener(event -> onComboSubscriptionSelection());

        comboWebApps.setRenderer(new ListCellRendererWrapper<ResourceEx<SiteInner>>() {
            @Override
            public void customize(JList jList, ResourceEx<SiteInner> siteInnerResourceEx, int index, boolean
                    isSelected, boolean cellHasFocus) {
                if (siteInnerResourceEx != null) {
                    setText(siteInnerResourceEx.getResource().name());
                }
            }
        });
        comboWebApps.addItemListener((event) -> {
            if (comboWebApps.getSelectedItem() instanceof ResourceEx) {
                SiteInner si = (SiteInner) ((ResourceEx) comboWebApps.getSelectedItem()).getResource();
                String sid = ((ResourceEx) comboWebApps.getSelectedItem()).getSubscriptionId();
                Subscription sb = AzureMvpModel.getInstance().getSubscriptionById(sid);
                comboSubscription.setSelectedItem(sb);
                comboResourceGroup.removeAllItems();
                for (ResourceGroup rg : AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(sid)) {
                    comboResourceGroup.addItem(rg);
                    if (rg.name().equals(si.resourceGroup())) {
                        comboResourceGroup.setSelectedItem(rg);
                    }
                }
                textAppName.setText(si.name());
            } else {
                textAppName.setText("");
            }

        });
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void onTextAppNameModify() {
        if (textAppName.getText().matches(REGEX_VALID_APP_NAME)) {
            textAppName.setForeground(new Color(0, 0, 0));
        } else {
            textAppName.setForeground(new Color(255, 0, 0));
        }
    }


    private void onRefreshButtonSelection() {
        loadCombosInfo(null);
    }

    private void setWidgetsEnabledStatus(boolean enabledStatus) {
        setEnabledRecursively(btnRefreshList, enabledStatus);
        if (enabledStatus == true) {
            //radioResourceGroupLogic();
        }
    }

    private void onComboSubscriptionSelection() {
        // TODO
    }

    private void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    public void apply(WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) {
        WebAppOnLinuxDeployModel model = webAppOnLinuxDeployConfiguration.getWebAppOnLinuxDeployModel();
        PrivateRegistryImageSetting acrInfo = model.getAzureContainerRegistryInfo();
        acrInfo.setServerUrl(textServerUrl.getText());
        acrInfo.setUsername(textUsername.getText());
        acrInfo.setPassword(String.valueOf(passwordField.getPassword()));
        acrInfo.setImageNameWithTag(textImageTag.getText());
        acrInfo.setStartupFile(textStartupFile.getText());

        WebAppOnLinuxDeployModel.WebAppOnLinuxInfo webAppInfo = model.getWebAppOnLinuxInfo();
        // AppName
        webAppInfo.setWebAppName(textAppName.getText());
        Object selectedWebItem = comboWebApps.getSelectedItem();

        // AppId
        if (selectedWebItem instanceof ResourceEx) {
            webAppInfo.setWebAppId(((SiteInner) ((ResourceEx) selectedWebItem).getResource()).id());
        }

        // Subs
        Subscription selectedSubscription = (Subscription) comboSubscription.getSelectedItem();
        if (selectedSubscription != null) {
            webAppInfo.setSubscriptionId(selectedSubscription.subscriptionId());
        }

        // RG
        Object selectedResourceGroup = comboResourceGroup.getSelectedItem();
        if (selectedResourceGroup instanceof ResourceGroup) {
            webAppInfo.setResourceGroupName(((ResourceGroup) selectedResourceGroup).name());
        } else if (selectedResourceGroup != null) {
            webAppInfo.setResourceGroupName(selectedResourceGroup.toString());
        }
    }

    public void reset(WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) {
        WebAppOnLinuxDeployModel model = webAppOnLinuxDeployConfiguration.getWebAppOnLinuxDeployModel();
        PrivateRegistryImageSetting acrInfo = model.getAzureContainerRegistryInfo();
        textServerUrl.setText(acrInfo.getServerUrl());
        textUsername.setText(acrInfo.getUsername());
        passwordField.setText(acrInfo.getPassword());
        textImageTag.setText(acrInfo.getImageNameWithTag());
        textStartupFile.setText(acrInfo.getStartupFile());

        WebAppOnLinuxDeployModel.WebAppOnLinuxInfo webAppInfo = model.getWebAppOnLinuxInfo();
        textAppName.setText(webAppInfo.getWebAppName());

        loadCombosInfo(webAppInfo.getWebAppId());

        webAppOnLinuxDeployPresenter.onRefresh(false);

    }

    private void loadCombosInfo(String activeWebAppId) {
        comboSubscription.removeAllItems();
        comboWebApps.removeAllItems();
        for (Subscription sb : AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            comboSubscription.addItem(sb);
            for (ResourceEx<SiteInner> si : AzureWebAppMvpModel.getInstance().listWebAppsOnLinuxBySubscriptionId(sb
                    .subscriptionId(), false)) {
                comboWebApps.addItem(si);
                if (activeWebAppId != null && si.getResource().id().equals(activeWebAppId)) {
                    comboWebApps.setSelectedItem(si);
                    comboSubscription.setSelectedItem(sb);
                    // update comboBox for RG
                    comboResourceGroup.removeAllItems();
                    for (ResourceGroup rg : AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(sb
                            .subscriptionId())) {
                        comboResourceGroup.addItem(rg);
                        if (rg.name().equals(si.getResource().resourceGroup())) {
                            comboResourceGroup.setSelectedItem(rg);
                        }

                    }
                }
            }
        }
        // comboWebApps.addItem("Create New");
    }

    private void createUIComponents() {
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn("WebAppName");
        tableModel.addColumn("ResourceGroup");

        webAppTable = new JBTable(tableModel);

        webAppTable.setRowSelectionAllowed(true);
        webAppTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        webAppTable.getSelectionModel().addListSelectionListener(event -> {
            System.out.println("table selected");
            if (cachedList != null) {
                selectedWebApp = cachedList.get(webAppTable.getSelectedRow());
            }
        });

        AnActionButton refreshAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                webAppOnLinuxDeployPresenter.onRefresh(true);
            }
        };

        ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(webAppTable)
                .addExtraActions(refreshAction);

        pnlWebAppOnLinuxTable = tableToolbarDecorator.createPanel();
    }

    @Override
    public void renderWebAppOnLinuxList(List<ResourceEx<SiteInner>> webAppOnLinuxList) {
        List<ResourceEx<SiteInner>> sortedList = webAppOnLinuxList.stream()
                .sorted((a, b) -> a.getSubscriptionId().compareToIgnoreCase(b.getSubscriptionId()))
                .collect(Collectors.toList());
        cachedList = sortedList;
        if (cachedList.size() > 0) {
            DefaultTableModel model = (DefaultTableModel) webAppTable.getModel();
            model.getDataVector().clear();
            for (ResourceEx<SiteInner> resource : sortedList) {
                SiteInner app = resource.getResource();
                model.addRow(new String[]{
                        app.name(),
                        app.resourceGroup()
                });
            }
            model.fireTableDataChanged();
        }

    }
}
