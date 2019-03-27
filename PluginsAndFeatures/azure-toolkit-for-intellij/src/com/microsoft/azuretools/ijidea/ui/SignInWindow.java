/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azuretools.adauth.AuthCanceledException;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.DCAuthManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SignInWindow extends AzureDialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(SignInWindow.class);
    private static final String SIGN_IN_ERROR = "Sign In Error";

    private JPanel contentPane;

    private JRadioButton interactiveRadioButton;
    private JRadioButton deviceLoginRadioButton;

    private JRadioButton automatedRadioButton;
    private JLabel authFileLabel;
    private JTextField authFileTextField;
    private JButton browseButton;
    private JButton createNewAuthenticationFileButton;
    private JLabel automatedCommentLabel;
    private JLabel interactiveCommentLabel;
    private JLabel deviceLoginCommentLabel;

    private AuthMethodDetails authMethodDetails;
    private AuthMethodDetails authMethodDetailsResult;

    private String accountEmail;

    private Project project;

    public AuthMethodDetails getAuthMethodDetails() {
        return authMethodDetailsResult;
    }

    public static SignInWindow go(AuthMethodDetails authMethodDetails, Project project) {
        SignInWindow d = new SignInWindow(authMethodDetails, project);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    private SignInWindow(AuthMethodDetails authMethodDetails, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        setModal(true);
        setTitle("Azure Sign In");
        setOKButtonText("Sign in");

        this.authMethodDetails = authMethodDetails;
        authFileTextField.setText(authMethodDetails.getCredFilePath());

        interactiveRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshAuthControlElements();
            }
        });

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

        deviceLoginRadioButton.setSelected(true);
        refreshAuthControlElements();

        init();
    }

    private void refreshAuthControlElements() {
        refreshAutomateLoginElements();
        refreshDeviceLoginElements();
        refreshInteractiveLoginElements();
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

    private void refreshInteractiveLoginElements() {
        interactiveCommentLabel.setEnabled(interactiveRadioButton.isSelected());
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

    private void doDeviceLogin() {
        try {
            final DCAuthManager dcAuthManager = DCAuthManager.getInstance();
            if (dcAuthManager.isSignedIn()) {
                doSignOut();
            }
            deviceLoginAsync(dcAuthManager);
            accountEmail = dcAuthManager.getAccountEmail();
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR);
        }
    }

    private void deviceLoginAsync(@NotNull final DCAuthManager dcAuthManager) {
        ProgressManager.getInstance().run(
            new Task.Modal(project, "Sign In Progress", false) {
                @Override
                public void run(ProgressIndicator indicator) {
                    try {
                        dcAuthManager.deviceLogin(null);
                    } catch (AuthCanceledException ex) {
                        System.out.println(ex.getMessage());
                    } catch (Exception ex) {
                        ApplicationManager.getApplication().invokeLater(
                            () -> ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR));
                    }
                }
            });
    }

    private void doInteractiveLogin() {
        try {
            AdAuthManager adAuthManager = AdAuthManager.getInstance();
            if (adAuthManager.isSignedIn()) {
                doSignOut();
            }
            signInAsync();
            accountEmail = adAuthManager.getAccountEmail();
        } catch (Exception ex) {
            ex.printStackTrace();
            //LOGGER.error("doSignIn", ex);
            ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR);
        }
    }

    private void signInAsync() {
        ProgressManager.getInstance().run(
            new Task.Modal(project, "Sign In Progress", false) {
                @Override
                public void run(ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText("Signing In...");
                    try {
                        AdAuthManager.getInstance().signIn();
                    } catch (AuthCanceledException ex) {
                        System.out.println(ex.getMessage());
                    } catch (Exception ex) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                // To revert after Device Flow is stable.
                                ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR, "Try Device Flow", () -> {
                                    authMethodDetailsResult = new AuthMethodDetails();
                                    doDeviceLogin();
                                    if (!StringUtils.isNullOrEmpty(accountEmail)) {
                                        authMethodDetailsResult.setAuthMethod(AuthMethod.DC);
                                        authMethodDetailsResult.setAccountEmail(accountEmail);
                                        authMethodDetailsResult.setAzureEnv(CommonSettings.getEnvironment().getName());
                                        SignInWindow.super.doOKAction();
                                    }
                                });
                            }
                        });
                    }
                }
            }
        );
    }

    private void doSignOut() {
        try {
            accountEmail = null;
            AdAuthManager.getInstance().signOut();
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), "Sign Out Error");
        }
    }

    private void doCreateServicePrincipal() {
        DCAuthManager dcAuthManager = null;
        try {
            dcAuthManager = DCAuthManager.getInstance();
            if (dcAuthManager.isSignedIn()) {
                dcAuthManager.signOut();
            }

            doDeviceLogin();

            if (!dcAuthManager.isSignedIn()) {
                // canceled by the user
                System.out.println(">> Canceled by the user");
                return;
            }

            AccessTokenAzureManager accessTokenAzureManager = new AccessTokenAzureManager();
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

            SrvPriCreationStatusDialog d1 = SrvPriCreationStatusDialog.go(tidSidsMap, destinationFolder, project);
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

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        authMethodDetailsResult = new AuthMethodDetails();
        if (interactiveRadioButton.isSelected()) {
            doInteractiveLogin();
            if (StringUtils.isNullOrEmpty(accountEmail)) {
                System.out.println("Canceled by the user.");
                return;
            }
            authMethodDetailsResult.setAuthMethod(AuthMethod.AD);
            authMethodDetailsResult.setAccountEmail(accountEmail);
            authMethodDetailsResult.setAzureEnv(CommonSettings.getEnvironment().getName());
        } else if (automatedRadioButton.isSelected()) { // automated
            String authPath = authFileTextField.getText();
            if (StringUtils.isNullOrWhiteSpace(authPath)) {
                JOptionPane.showMessageDialog(
                    contentPane,
                    "Select authentication file",
                    "Sign in dialog info",
                    JOptionPane.INFORMATION_MESSAGE);
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
        }

        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        authMethodDetailsResult = authMethodDetails;
        super.doCancelAction();
    }

    @Override
    public void doHelpAction() {
        JXHyperlink helpLink = new JXHyperlink();
        helpLink.setURI(URI.create("https://docs.microsoft.com/en-us/azure/azure-toolkit-for-intellij-sign-in" +
            "-instructions"));
        helpLink.doClick();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "SignInWindow";
    }
}
