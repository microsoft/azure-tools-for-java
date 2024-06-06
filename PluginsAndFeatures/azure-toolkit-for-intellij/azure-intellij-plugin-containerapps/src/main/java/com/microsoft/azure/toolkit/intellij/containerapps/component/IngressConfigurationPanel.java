/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.ui.JBIntSpinner;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class IngressConfigurationPanel implements AzureFormPanel<IngressConfig> {
    @Getter
    private JPanel pnlRoot;
    private JLabel lblIngress;
    private JCheckBox chkIngress;
    private JLabel lblTargetPort;
    private JBIntSpinner txtTargetPort;
    private JCheckBox chkExternalTraffic;
    private JLabel lblExternalTraffic;
    @Getter
    @Setter
    private ContainerApp containerApp;

    public IngressConfigurationPanel() {
        $$$setupUI$$$();
        this.chkIngress.addItemListener(this::onSelectIngress);
    }

    private void onSelectIngress(ItemEvent itemEvent) {
        this.lblExternalTraffic.setVisible(chkIngress.isSelected());
        this.chkExternalTraffic.setVisible(chkIngress.isSelected());
        this.lblTargetPort.setVisible(chkIngress.isSelected());
        this.txtTargetPort.setVisible(chkIngress.isSelected());
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.txtTargetPort = new JBIntSpinner(80, 1, 65535);
    }

    @Override
    public void setValue(final IngressConfig config) {
        if (Objects.isNull(config)) {
            return;
        }
        chkIngress.setSelected(config.isEnableIngress());
        chkExternalTraffic.setSelected(config.isExternal());
        txtTargetPort.setValue(config.getTargetPort());
    }

    public void setValueFromContainerApp(final ContainerApp containerApp) {
        if (Objects.equals(containerApp, this.containerApp)) {
            return;
        }
        this.containerApp = containerApp;
        Optional.ofNullable(containerApp.getIngressConfig()).ifPresent(this::setValue);
    }

    @Override
    public IngressConfig getValue() {
        return IngressConfig.builder().enableIngress(chkIngress.isSelected())
            .external(chkExternalTraffic.isSelected())
            .targetPort(txtTargetPort.getNumber()).build();
    }

    public void setEnabled(boolean enable) {
        Stream.of(lblTargetPort, lblIngress, lblExternalTraffic, chkExternalTraffic, chkIngress, txtTargetPort)
            .forEach(component -> component.setEnabled(enable));
    }

    private void onSelectIngress(boolean enableIngress) {
        this.lblExternalTraffic.setVisible(enableIngress);
        this.chkExternalTraffic.setVisible(enableIngress);
        this.lblTargetPort.setVisible(enableIngress);
        this.txtTargetPort.setVisible(enableIngress);
    }

}
