/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SignInWindow extends AzureDialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(SignInWindow.class);
    private static final String SIGN_IN_ERROR = "Sign In Error";
    private JPanel contentPane;

    private JRadioButton deviceLoginRadioButton;
    private JRadioButton spRadioButton;
    private JLabel servicePrincipalCommentLabel;
    private JLabel deviceLoginCommentLabel;
    private JRadioButton azureCliRadioButton;
    private JLabel azureCliCommentLabel;
    private JRadioButton oauthLoginRadioButton;
    private JLabel labelOAuthLogin;

    private AuthMethodDetails authMethodDetails;

    private String accountEmail;

    private Project project;
    private Map<AbstractButton, JComponent> radioButtonComponentsMap = new HashMap<>(3);

    public SignInWindow(AuthMethodDetails authMethodDetails, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        setModal(true);
        setTitle("Azure Sign In");
        setOKButtonText("Sign in");

        this.authMethodDetails = authMethodDetails;

        oauthLoginRadioButton.addItemListener(e -> refreshAuthControlElements());

        spRadioButton.addActionListener(e -> refreshAuthControlElements());

        deviceLoginRadioButton.addActionListener(e -> refreshAuthControlElements());

        azureCliRadioButton.addActionListener(e -> refreshAuthControlElements());


        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(oauthLoginRadioButton);
        buttonGroup.add(deviceLoginRadioButton);
        buttonGroup.add(spRadioButton);
        buttonGroup.add(azureCliRadioButton);
        azureCliRadioButton.setSelected(true);

        radioButtonComponentsMap.put(spRadioButton, servicePrincipalCommentLabel);
        radioButtonComponentsMap.put(deviceLoginRadioButton, deviceLoginCommentLabel);
        radioButtonComponentsMap.put(oauthLoginRadioButton, labelOAuthLogin);
        radioButtonComponentsMap.put(azureCliRadioButton, azureCliCommentLabel);
        init();
        this.setOKActionEnabled(false);
        checkAccountAvailability();
    }

    @Nullable
    public static SignInWindow go(AuthMethodDetails authMethodDetails, Project project) {
        SignInWindow signInWindow = new SignInWindow(authMethodDetails, project);
        signInWindow.show();
        if (signInWindow.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return signInWindow;
        }

        return null;
    }

    public AuthType getData() {
        if (spRadioButton.isSelected()) {
            return AuthType.SERVICE_PRINCIPAL;
        }
        if (deviceLoginRadioButton.isSelected()) {
            return AuthType.DEVICE_CODE;
        }

        if (oauthLoginRadioButton.isSelected()) {
            return AuthType.OAUTH2;
        }
        if (azureCliRadioButton.isSelected()) {
            return AuthType.AZURE_CLI;
        }
        throw new AzureToolkitRuntimeException("No auth type is selected");
    }

    @Override
    public void doHelpAction() {
        final JXHyperlink helpLink = new JXHyperlink();
        helpLink.setURI(URI.create("https://docs.microsoft.com/en-us/azure/azure-toolkit-for-intellij-sign-in-instructions"));
        helpLink.doClick();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "SignInWindow";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void init() {
        super.init();
        azureCliRadioButton.setText("Azure CLI (checking...)");
        azureCliRadioButton.setEnabled(false);
        azureCliCommentLabel.setIcon(new AnimatedIcon.Default());
        azureCliCommentLabel.setEnabled(true);
        refreshAuthControlElements();
    }

    private void refreshAuthControlElements() {
        radioButtonComponentsMap.keySet().forEach(radio -> radioButtonComponentsMap.get(radio).setEnabled(radio.isSelected()));
        this.setOKActionEnabled(true);
    }

    private void checkAccountAvailability() {
        Flux.fromIterable(Azure.az(AzureAccount.class).accounts()).subscribeOn(Schedulers.boundedElastic())
            .flatMap(ac -> Mono.zip(Mono.just(ac), ac.checkAvailable().onErrorResume(e -> Mono.just(false))))
            .filter(Tuple2::getT2).map(Tuple2::getT1).collectList().subscribe(accounts -> {
                if (accounts.stream().anyMatch(ac -> ac.getAuthType() == AuthType.AZURE_CLI)) {
                    enableAzureCliLogin();
                } else {
                    disableAzureCliLogin();
                }
                if (accounts.stream().anyMatch(ac -> ac.getAuthType() == AuthType.OAUTH2)) {
                    oauthLoginRadioButton.setEnabled(true);
                    labelOAuthLogin.setEnabled(true);
                } else {
                    oauthLoginRadioButton.setEnabled(false);
                    labelOAuthLogin.setEnabled(false);
                }
                if (accounts.stream().anyMatch(ac -> ac.getAuthType() == AuthType.DEVICE_CODE)) {
                    deviceLoginRadioButton.setEnabled(true);
                    deviceLoginCommentLabel.setEnabled(true);
                } else {
                    deviceLoginRadioButton.setEnabled(false);
                    deviceLoginCommentLabel.setEnabled(false);
                }

                // if the selected radio button is disabled, select the first enabled button
                final JRadioButton firstSelected = Stream.of(azureCliRadioButton, oauthLoginRadioButton, deviceLoginRadioButton, spRadioButton).filter(
                    AbstractButton::isSelected).findFirst().orElse(null);
                if (firstSelected != null && !firstSelected.isEnabled()) {
                    Stream.of(azureCliRadioButton, oauthLoginRadioButton, deviceLoginRadioButton, spRadioButton)
                          .filter(Component::isEnabled).findFirst().ifPresent(button -> button.setSelected(true));
                }
                refreshAuthControlElements();
                this.setOKActionEnabled(true);
            });
    }

    private void enableAzureCliLogin() {
        azureCliCommentLabel.setIcon(null);
        azureCliRadioButton.setEnabled(true);
        azureCliRadioButton.setText("Azure CLI");
    }

    private void disableAzureCliLogin() {
        azureCliCommentLabel.setIcon(null);
        azureCliCommentLabel.setEnabled(false);
        azureCliRadioButton.setEnabled(false);
        azureCliRadioButton.setText("Azure CLI (Not logged in)");
    }
}
