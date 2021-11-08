/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.compute.security.model.SecurityRule;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class InboundPortRulesForm extends JPanel implements AzureForm<List<SecurityRule>> {
    private JLabel lblSelectInboundPorts;
    private JPanel pnlInboundPorts;
    private JCheckBox chkHTTP;
    private JCheckBox chkHTTPS;
    private JCheckBox chkSSH;
    private JCheckBox chkRDP;
    private JPanel pnlRoot;

    public InboundPortRulesForm() {
        super();
    }

    @Override
    public List<SecurityRule> getValue() {
        final List<SecurityRule> policies = new ArrayList<>();
        Optional.ofNullable(chkHTTP.isSelected() ? SecurityRule.HTTP_RULE : null).ifPresent(policies::add);
        Optional.ofNullable(chkHTTPS.isSelected() ? SecurityRule.HTTPS_RULE : null).ifPresent(policies::add);
        Optional.ofNullable(chkSSH.isSelected() ? SecurityRule.SSH_RULE : null).ifPresent(policies::add);
        Optional.ofNullable(chkRDP.isSelected() ? SecurityRule.RDP_RULE : null).ifPresent(policies::add);
        return policies;
    }

    @Override
    public void setValue(List<SecurityRule> data) {
        if (CollectionUtils.isEmpty(data)) {
            Stream.of(chkRDP, chkHTTP, chkHTTPS, chkSSH).forEach(component -> component.setSelected(false));
            return;
        }
        chkHTTP.setSelected(data.contains(SecurityRule.HTTP_RULE));
        chkHTTPS.setSelected(data.contains(SecurityRule.HTTPS_RULE));
        chkSSH.setSelected(data.contains(SecurityRule.SSH_RULE));
        chkRDP.setSelected(data.contains(SecurityRule.RDP_RULE));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.emptyList();
    }

    public List<JLabel> getLabels() {
        return Arrays.asList(lblSelectInboundPorts);
    }

    public void addActionListenerToComponents(final ActionListener actionListener) {
        Stream.of(chkRDP, chkHTTP, chkHTTPS, chkSSH).forEach(component -> component.addActionListener(actionListener));
    }

    @Override
    public void setVisible(boolean flag) {
        super.setVisible(flag);
        this.pnlRoot.setVisible(flag);
    }

    public void toggleInboundPortsPolicy(boolean allowInboundPorts) {
        Stream.of(chkRDP, chkHTTP, chkHTTPS, chkSSH).forEach(chk -> {
            chk.setEnabled(allowInboundPorts);
            if (!allowInboundPorts) {
                chk.setSelected(false);
            }
        });
    }
}
