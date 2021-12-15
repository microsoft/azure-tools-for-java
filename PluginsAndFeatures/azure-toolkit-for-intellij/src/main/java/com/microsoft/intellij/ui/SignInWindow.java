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
import com.microsoft.azure.toolkit.intellij.common.help.AzureWebHelpProvider;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;

public class SignInWindow extends AzureDialogWrapper {
    private static final String DESC = "desc_label";
    public static final String AZURE_FREE = "https://azure.microsoft.com/en-us/free/?utm_campaign=javatools";
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
        oauthBtn.addActionListener(e -> updateSelection());
        oauthBtn.setActionCommand(AuthType.OAUTH2.name());
        oauthBtn.putClientProperty(DESC, oauthDesc);
        deviceBtn.addActionListener(e -> updateSelection());
        deviceBtn.setActionCommand(AuthType.DEVICE_CODE.name());
        deviceBtn.putClientProperty(DESC, deviceDesc);
        spBtn.addActionListener(e -> updateSelection());
        spBtn.setActionCommand(AuthType.SERVICE_PRINCIPAL.name());
        spBtn.putClientProperty(DESC, spDesc);
        final Dimension size = this.contentPane.getPreferredSize();
        this.contentPane.setPreferredSize(new Dimension(500, size.height));
        cliBtn.setSelected(true);
        updateSelection();
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
        Azure.az(AzureAccount.class).accounts().stream().filter(a -> a.getAuthType() == AuthType.AZURE_CLI).findAny().ifPresent(az -> {
            this.cliBtn.setText("Azure CLI (checking...)");
            this.cliDesc.setIcon(new AnimatedIcon.Default());
            this.cliBtn.setEnabled(false);
            Mono.just(az).subscribeOn(Schedulers.boundedElastic())
                .flatMap(a -> a.checkAvailable().onErrorResume(e -> Mono.just(false)))
                .doFinally((s) -> {
                    cliBtn.setText("Azure CLI");
                    cliDesc.setIcon(null);
                    oauthBtn.setSelected(cliBtn.isSelected() && !cliBtn.isEnabled());
                    updateSelection();
                }).subscribe(cliBtn::setEnabled);
        });
    }

    protected JPanel createSouthAdditionalPanel() {
        final ActionLink link = new ActionLink("Try Azure for free", e -> {
            AzureActionManager.getInstance().getAction(OPEN_URL).handle(AZURE_FREE);
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

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
