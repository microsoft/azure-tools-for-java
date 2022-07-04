/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAccountActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.help.AzureWebHelpProvider;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;

import javax.accessibility.AccessibleContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class SignInWindow extends AzureDialogWrapper {
    private static final String DESC = "desc_label";
    private JPanel contentPane;

    private JRadioButton cliBtn;
    private JLabel cliDesc;
    private JRadioButton oauthBtn;
    private JLabel oauthDesc;
    private JRadioButton deviceBtn;
    private JLabel deviceDesc;
    private JRadioButton spBtn;
    private JLabel spDesc;
    private ButtonGroup authTypeGroup;

    public SignInWindow(Project project) {
        super(project, true, IdeModalityType.PROJECT);
        setModal(true);
        setTitle("Sign In");
        setOKButtonText("Sign in");
        this.setOKActionEnabled(false);
        init();
        checkAccountAvailability();
    }

    @Override
    protected void init() {
        super.init();
        cliBtn.addActionListener(e -> updateSelection());
        cliBtn.setActionCommand(AuthType.AZURE_CLI.name());
        cliBtn.putClientProperty(DESC, cliDesc);
        bindDescriptionLabel(cliBtn, cliDesc);
        oauthBtn.addActionListener(e -> updateSelection());
        oauthBtn.setActionCommand(AuthType.OAUTH2.name());
        oauthBtn.putClientProperty(DESC, oauthDesc);
        bindDescriptionLabel(oauthBtn, oauthDesc);
        deviceBtn.addActionListener(e -> updateSelection());
        deviceBtn.setActionCommand(AuthType.DEVICE_CODE.name());
        deviceBtn.putClientProperty(DESC, deviceDesc);
        bindDescriptionLabel(deviceBtn, deviceDesc);
        spBtn.addActionListener(e -> updateSelection());
        spBtn.setActionCommand(AuthType.SERVICE_PRINCIPAL.name());
        spBtn.putClientProperty(DESC, spDesc);
        bindDescriptionLabel(spBtn, spDesc);

        authTypeGroup = new ButtonGroup();
        authTypeGroup.add(cliBtn);
        authTypeGroup.add(oauthBtn);
        authTypeGroup.add(deviceBtn);
        authTypeGroup.add(spBtn);
        final Dimension size = this.contentPane.getPreferredSize();
        this.contentPane.setPreferredSize(new Dimension(500, size.height));
        cliBtn.setSelected(true);
        updateSelection();
    }

    private void bindDescriptionLabel(@Nonnull final JRadioButton button, @Nonnull final JLabel label) {
        label.setLabelFor(button);
        final AccessibleContext accessibleContext = button.getAccessibleContext();
        accessibleContext.setAccessibleDescription(accessibleContext.getAccessibleDescription() + " " + label.getText());
    }

    private void updateSelection() {
        boolean selectionAvailable = false;
        for (final AbstractButton button : Collections.list(authTypeGroup.getElements())) {
            ((JLabel) button.getClientProperty(DESC)).setEnabled(button.isSelected() && button.isEnabled());
            selectionAvailable = selectionAvailable || (button.isSelected() && button.isEnabled());
        }
        this.setOKActionEnabled(selectionAvailable);
    }

    public AuthType getData() {
        for (final AbstractButton button : Collections.list(authTypeGroup.getElements())) {
            if (button.isSelected() && button.isEnabled()) {
                return AuthType.valueOf(button.getActionCommand());
            }
        }
        throw new AzureToolkitRuntimeException("No auth type is selected");
    }

    private void checkAccountAvailability() {
        // only azure cli need availability check.
        this.oauthBtn.setEnabled(true);
        this.deviceBtn.setEnabled(true);
        this.spBtn.setEnabled(true);
        this.cliBtn.setText("Azure CLI (checking...)");
        this.cliDesc.setIcon(new AnimatedIcon.Default());
        this.cliBtn.setEnabled(false);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final boolean available = AuthType.AZURE_CLI.checkAvailable();
            cliBtn.setEnabled(available);
            cliBtn.setText(available ? "Azure CLI" : "Azure CLI (Not logged in)");
            cliDesc.setIcon(null);
            if (cliBtn.isSelected() && !available) {
                oauthBtn.setSelected(true);
                oauthBtn.requestFocus(); // also need to move focus in case accessibility exception
            }
            updateSelection();
        });
    }

    protected JPanel createSouthAdditionalPanel() {
        final ActionLink link = new ActionLink("Try Azure for free", e -> {
            AzureActionManager.getInstance().getAction(IntellijAccountActionsContributor.TRY_AZURE).handle(null);
        });
        final JPanel panel = new NonOpaquePanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.emptyLeft(10));
        panel.add(link);
        return panel;
    }

    @Override
    @Nullable
    protected String getHelpId() {
        return AzureWebHelpProvider.HELP_SIGN_IN;
    }

    @Override
    protected DialogStyle getStyle() {
        return DialogStyle.COMPACT;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.cliBtn;
    }
}
