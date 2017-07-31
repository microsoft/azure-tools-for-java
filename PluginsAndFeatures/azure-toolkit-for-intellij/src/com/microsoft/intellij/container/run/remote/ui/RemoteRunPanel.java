package com.microsoft.intellij.container.run.remote.ui;

/**
 * Created by sechs on 7/24/17.
 */
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.intellij.container.run.remote.ContainerRemoteRunConfiguration;
import com.microsoft.intellij.container.run.remote.ContainerRemoteRunModel;

import javax.swing.*;
import java.awt.*;

public class RemoteRunPanel {
    private Project project;
    private static final String REGEX_VALID_RG_NAME = "^[\\w-]*\\w+$";
    private static final String REGEX_VALID_APP_NAME = "^[\\w-]*\\w+$";
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

    public RemoteRunPanel() {
        AzureMvpModel.getInstance();
        btnRefreshList.addActionListener(event -> onRefreshButtonSelection());
        comboResourceGroup.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if(o != null){
                    if(o instanceof ResourceGroup){
                        setText(((ResourceGroup) o).name());
                    }
                    else{
                        setText(o.toString());
                    }
                }

            }
        });
        comboSubscription.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if(o != null){
                    if(o instanceof Subscription){
                        setText(String.format("%s (%s)", ((Subscription)o).displayName(), ((Subscription)o).subscriptionId()));
                    }
                }
            }
        });
        comboSubscription.addActionListener(event -> onComboSubscriptionSelection());

        comboWebApps.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o != null){
                    if(o instanceof ResourceEx){
                        setText(((ResourceEx<SiteInner>)o).getResource().name());
                    }
                    else{
                        setText(o.toString());
                    }
                }
            }
        });
        comboWebApps.addItemListener((event) -> {
            if(comboWebApps.getSelectedItem() instanceof ResourceEx){
                SiteInner si = (SiteInner) ((ResourceEx) comboWebApps.getSelectedItem()).getResource();
                String sid = ((ResourceEx) comboWebApps.getSelectedItem()).getSubscriptionId();
                Subscription sb = AzureMvpModel.getInstance().getSubscriptionById(sid);
                comboSubscription.setSelectedItem(sb);
                comboResourceGroup.removeAllItems();
                for(ResourceGroup rg : AzureMvpModel.getInstance().getResouceGroupsBySubscriptionId(sid,false)){
                    comboResourceGroup.addItem(rg);
                    if(rg.name().equals(si.resourceGroup())){
                        comboResourceGroup.setSelectedItem(rg);
                    }
                }
                textAppName.setText(si.name());
            }
            else{
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

    public void apply(ContainerRemoteRunConfiguration containerRemoteRunConfiguration) {
        ContainerRemoteRunModel model = containerRemoteRunConfiguration.getContainerRemoteRunModel();
        ContainerRemoteRunModel.AzureContainerRegistryInfo acrInfo = model.getAzureContainerRegistryInfo();
        acrInfo.setServerUrl(textServerUrl.getText());
        acrInfo.setUsername(textUsername.getText());
        acrInfo.setPassword(String.valueOf(passwordField.getPassword()));
        acrInfo.setImageNameWithTag(textImageTag.getText());
        acrInfo.setStartupFile(textStartupFile.getText());

        ContainerRemoteRunModel.WebAppOnLinuxInfo webAppInfo = model.getWebAppOnLinuxInfo();
        // AppName
        webAppInfo.setWebAppName(textAppName.getText());
        Object selectedWebItem = comboWebApps.getSelectedItem();

        // AppId
        if(selectedWebItem instanceof ResourceEx) {
            webAppInfo.setWebAppId(((SiteInner)((ResourceEx) selectedWebItem).getResource()).id());
        }

        // Subs
        Subscription selectedSubscription = (Subscription) comboSubscription.getSelectedItem();
        if(selectedSubscription!=null){
            webAppInfo.setSubscriptionId(selectedSubscription.subscriptionId());
        }

        // RG
        Object selectedResourceGroup = comboResourceGroup.getSelectedItem();
        if(selectedResourceGroup instanceof ResourceGroup){
            webAppInfo.setResourceGroupName(((ResourceGroup) selectedResourceGroup).name());
        }
        else if(selectedResourceGroup != null){
            webAppInfo.setResourceGroupName(selectedResourceGroup.toString());
        }
    }

    public void reset(ContainerRemoteRunConfiguration containerRemoteRunConfiguration) {
        ContainerRemoteRunModel model = containerRemoteRunConfiguration.getContainerRemoteRunModel();
        ContainerRemoteRunModel.AzureContainerRegistryInfo acrInfo = model.getAzureContainerRegistryInfo();
        textServerUrl.setText(acrInfo.getServerUrl());
        textUsername.setText(acrInfo.getUsername());
        passwordField.setText(acrInfo.getPassword());
        textImageTag.setText(acrInfo.getImageNameWithTag());
        textStartupFile.setText(acrInfo.getStartupFile());

        ContainerRemoteRunModel.WebAppOnLinuxInfo webAppInfo = model.getWebAppOnLinuxInfo();
        textAppName.setText(webAppInfo.getWebAppName());

        loadCombosInfo(webAppInfo.getWebAppId());

    }

    private void loadCombosInfo(String activeWebAppId) {
        comboSubscription.removeAllItems();
        comboWebApps.removeAllItems();
        for (Subscription sb : AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            comboSubscription.addItem(sb);
            for (ResourceEx<SiteInner> si : AzureWebAppMvpModel.getInstance().listWebAppsOnLinuxBySubscriptionId(sb.subscriptionId(), false)) {
                comboWebApps.addItem(si);
                if (activeWebAppId != null && si.getResource().id() == activeWebAppId) {
                    comboWebApps.setSelectedItem(si);
                    comboSubscription.setSelectedItem(sb);
                    // update comboBox for RG
                    comboResourceGroup.removeAllItems();
                    for (ResourceGroup rg : AzureMvpModel.getInstance().getResouceGroupsBySubscriptionId(sb.subscriptionId(), false)) {
                        comboResourceGroup.addItem(rg);
                        if(rg.name() == si.getResource().resourceGroup()) {
                            comboResourceGroup.setSelectedItem(rg);
                        }

                    }
                }
            }
        }
        // comboWebApps.addItem("Create New");
    }
}
