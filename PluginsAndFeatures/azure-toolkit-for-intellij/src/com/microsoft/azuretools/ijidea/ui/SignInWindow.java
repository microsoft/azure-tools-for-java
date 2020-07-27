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

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azuretools.adauth.AuthCanceledException;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.*;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.sdkmanage.AzureCliAzureManager;
import com.microsoft.azuretools.telemetrywrapper.*;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class SignInWindow extends AzureDialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(SignInWindow.class);
    private static final String SIGN_IN_ERROR = "Sign In Error";

    private JPanel contentPane;

    private JRadioButton deviceLoginRadioButton;

    private JRadioButton automatedRadioButton;
    private JLabel authFileLabel;
    private JTextField authFileTextField;
    private JButton browseButton;
    private JButton createNewAuthenticationFileButton;
    private JLabel automatedCommentLabel;
    private JLabel deviceLoginCommentLabel;
    private JRadioButton azureCliRadioButton;
    private JPanel azureCliPanel;
    private JLabel azureCliCommentLabel;

    private AuthMethodDetails authMethodDetails;
    private AuthMethodDetails authMethodDetailsResult;

    private String accountEmail;

    private Project project;

    private SignInWindow(AuthMethodDetails authMethodDetails, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        setModal(true);
        setTitle("Azure Sign In");
        setOKButtonText("Sign in");

        this.authMethodDetails = authMethodDetails;
        authFileTextField.setText(authMethodDetails.getCredFilePath());

        automatedRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshAuthControlElements();
            }
        });

        deviceLoginRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshAuthControlElements();
            }
        });

        azureCliRadioButton.addActionListener(e -> refreshAuthControlElements());

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSelectCredFilepath();
            }
        });

        createNewAuthenticationFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCreateServicePrincipal();
            }
        });

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(deviceLoginRadioButton);
        buttonGroup.add(automatedRadioButton);
        buttonGroup.add(azureCliRadioButton);
        deviceLoginRadioButton.setSelected(true);

        init();
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return authMethodDetailsResult;
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

    @Override
    public void doCancelAction() {
        authMethodDetailsResult = authMethodDetails;
        super.doCancelAction();
    }

    @Override
    public void doHelpAction() {
        JXHyperlink helpLink = new JXHyperlink();
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
    protected void doOKAction() {
        authMethodDetailsResult = new AuthMethodDetails();
        if (automatedRadioButton.isSelected()) { // automated
            EventUtil.logEvent(EventType.info, ACCOUNT, SIGNIN, signInSPProp, null);
            String authPath = authFileTextField.getText();
            if (StringUtils.isNullOrWhiteSpace(authPath)) {
                DefaultLoader.getUIHelper().showMessageDialog(contentPane,
                                                              "Select authentication file",
                                                              "Sign in dialog info",
                                                              Messages.getInformationIcon());
                return;
            }

            authMethodDetailsResult.setAuthMethod(AuthMethod.SP);
            // TODO: check field is empty, check file is valid
            authMethodDetailsResult.setCredFilePath(authPath);
        } else if (deviceLoginRadioButton.isSelected()) {
            doDeviceLogin();
            if (StringUtils.isNullOrEmpty(accountEmail)) {
                System.out.println("Canceled by the user.");
                return;
            }
            authMethodDetailsResult.setAuthMethod(AuthMethod.DC);
            authMethodDetailsResult.setAccountEmail(accountEmail);
            authMethodDetailsResult.setAzureEnv(CommonSettings.getEnvironment().getName());
        } else if (azureCliRadioButton.isSelected()) {
            doLogin(() -> AzureCliAzureManager.getInstance().signIn(), signInAZProp);
            if (AzureCliAzureManager.getInstance().isSignedIn()) {
                authMethodDetailsResult.setAuthMethod(AuthMethod.AZ);
            } else {
                return;
            }
        }

        super.doOKAction();
    }

    @Override
    protected void init() {
        super.init();
        AzureTokenWrapper tokenWrapper = null;
        try {
            tokenWrapper = AzureAuthHelper.getAzureCLICredential();
        } catch (IOException e) {
            // swallow exception while getting azure cli credential
        }
        if (tokenWrapper != null) {
            azureCliPanel.setEnabled(true);
            azureCliRadioButton.setText("Azure CLI");
            azureCliRadioButton.setEnabled(true);
            azureCliRadioButton.setSelected(true);
        } else {
            azureCliPanel.setEnabled(false);
            azureCliRadioButton.setText("Azure CLI (Not logged in)");
            azureCliRadioButton.setEnabled(false);
        }
        refreshAuthControlElements();
    }

    private AuthMethodManager getAuthMethodManager() {
        return AuthMethodManager.getInstance();
    }

    private void refreshAuthControlElements() {
        refreshAutomateLoginElements();
        refreshDeviceLoginElements();
        refreshAzureCliElements();
    }

    private void refreshAutomateLoginElements() {
        automatedCommentLabel.setEnabled(automatedRadioButton.isSelected());
        authFileLabel.setEnabled(automatedRadioButton.isSelected());
        authFileTextField.setEnabled(automatedRadioButton.isSelected());
        browseButton.setEnabled(automatedRadioButton.isSelected());
        createNewAuthenticationFileButton.setEnabled(automatedRadioButton.isSelected());
    }

    private void refreshDeviceLoginElements() {
        deviceLoginCommentLabel.setEnabled(deviceLoginRadioButton.isSelected());
    }

    private void refreshAzureCliElements() {
        azureCliCommentLabel.setEnabled(azureCliRadioButton.isSelected());
    }

    private void doSelectCredFilepath() {
        FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("azureauth");
        fileDescriptor.setTitle("Select Authentication File");
        final VirtualFile file = FileChooser.chooseFile(
            fileDescriptor,
            this.project,
            LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home"))
        );
        if (file != null) {
            authFileTextField.setText(file.getPath());
        }
    }

    @Nullable
    private synchronized BaseADAuthManager doDeviceLogin() {
        try {
            BaseADAuthManager dcAuthManager = AuthMethod.DC.getAdAuthManager();
            if (dcAuthManager.isSignedIn()) {
                doSignOut();
            }
            doLogin(() -> dcAuthManager.signIn(null), signInDCProp);
            accountEmail = dcAuthManager.getAccountEmail();

            return dcAuthManager;
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR);
        }

        return null;
    }

    private void doLogin(Callable loginCallable, Map<String, String> properties) {
        Operation operation = TelemetryManager.createOperation(ACCOUNT, SIGNIN);
        ProgressManager.getInstance().run(
                new Task.Modal(project, "Sign In Progress", false) {
                    @Override
                    public void run(ProgressIndicator indicator) {
                        try {
                            EventUtil.logEvent(EventType.info, operation, properties);
                            operation.start();
                            loginCallable.call();
                        } catch (AuthCanceledException ex) {
                            EventUtil.logError(operation, ErrorType.userError, ex, properties, null);
                            System.out.println(ex.getMessage());
                        } catch (Exception ex) {
                            EventUtil.logError(operation, ErrorType.userError, ex, properties, null);
                            ApplicationManager.getApplication().invokeLater(
                                () -> ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR));
                        } finally {
                            operation.complete();
                        }
                    }
                });
    }

    private void doSignOut() {
        try {
            accountEmail = null;
            // AuthMethod.AD is deprecated.
            AuthMethod.DC.getAdAuthManager().signOut();
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), "Sign Out Error");
        }
    }

    private void doCreateServicePrincipal() {
        BaseADAuthManager dcAuthManager = null;
        try {
            if (getAuthMethodManager().isSignedIn()) {
                getAuthMethodManager().signOut();
            }

            dcAuthManager = doDeviceLogin();
            if (dcAuthManager == null || !dcAuthManager.isSignedIn()) {
                // canceled by the user
                System.out.println(">> Canceled by the user");
                return;
            }

            AccessTokenAzureManager accessTokenAzureManager = new AccessTokenAzureManager(dcAuthManager);
            SubscriptionManager subscriptionManager = accessTokenAzureManager.getSubscriptionManager();

            ProgressManager.getInstance().run(new Task.Modal(project, "Load Subscriptions Progress", true) {
                @Override
                public void run(ProgressIndicator progressIndicator) {
                    progressIndicator.setText("Loading subscriptions...");
                    try {
                        progressIndicator.setIndeterminate(true);
                        subscriptionManager.getSubscriptionDetails();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        //LOGGER.error("doCreateServicePrincipal::Task.Modal", ex);
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                ErrorWindow.show(project, ex.getMessage(), "Load Subscription Error");
                            }
                        });

                    }
                }
            });

            SrvPriSettingsDialog d = SrvPriSettingsDialog.go(subscriptionManager.getSubscriptionDetails(), project);
            List<SubscriptionDetail> subscriptionDetailsUpdated;
            String destinationFolder;
            if (d != null) {
                subscriptionDetailsUpdated = d.getSubscriptionDetails();
                destinationFolder = d.getDestinationFolder();
            } else {
                System.out.println(">> Canceled by the user");
                return;
            }

            Map<String, List<String>> tidSidsMap = new HashMap<>();
            for (SubscriptionDetail sd : subscriptionDetailsUpdated) {
                if (sd.isSelected()) {
                    System.out.format(">> %s\n", sd.getSubscriptionName());
                    String tid = sd.getTenantId();
                    List<String> sidList;
                    if (!tidSidsMap.containsKey(tid)) {
                        sidList = new LinkedList<>();
                    } else {
                        sidList = tidSidsMap.get(tid);
                    }
                    sidList.add(sd.getSubscriptionId());
                    tidSidsMap.put(tid, sidList);
                }
            }

            SrvPriCreationStatusDialog d1 = SrvPriCreationStatusDialog
                    .go(accessTokenAzureManager, tidSidsMap, destinationFolder, project);
            if (d1 == null) {
                System.out.println(">> Canceled by the user");
                return;
            }

            String path = d1.getSelectedAuthFilePath();
            if (path == null) {
                System.out.println(">> No file was created");
                return;
            }

            authFileTextField.setText(path);
            PluginUtil.displayInfoDialog("Authentication File Created", String.format(
                    "Your credentials have been exported to %s, please keep the authentication file safe", path));
        } catch (Exception ex) {
            ex.printStackTrace();
            //LOGGER.error("doCreateServicePrincipal", ex);
            ErrorWindow.show(project, ex.getMessage(), "Get Subscription Error");

        } finally {
            if (dcAuthManager != null) {
                try {
                    System.out.println(">> Signing out...");
                    dcAuthManager.signOut();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
