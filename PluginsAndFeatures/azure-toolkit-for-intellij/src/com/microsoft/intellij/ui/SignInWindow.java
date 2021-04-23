/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.azure.core.management.AzureEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.*;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.azuretools.telemetrywrapper.*;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import rx.Single;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class SignInWindow extends AzureDialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(SignInWindow.class);
    private static final String SIGN_IN_ERROR = "Sign In Error";
    private static final String USER_CANCEL = "user cancel";

    private JPanel contentPane;

    private JRadioButton deviceLoginRadioButton;

    private JRadioButton spRadioButton;
    private JLabel authFileLabel;
    private JTextField authFileTextField;
    private JButton browseButton;
    private JButton createNewAuthenticationFileButton;
    private JLabel servicePrincipalCommentLabel;
    private JLabel deviceLoginCommentLabel;
    private JRadioButton azureCliRadioButton;
    private JPanel azureCliPanel;
    private JLabel azureCliCommentLabel;
    private JRadioButton oauthLoginRadioButton;
    private JLabel labelOAuthLogin;

    private AuthMethodDetails authMethodDetails;
    private AuthMethodDetails authMethodDetailsResult;

    private String accountEmail;

    private Project project;
    private Map<AbstractButton, List<JComponent>> radioButtonComponentsMap = new HashMap<>(3);

    public SignInWindow(AuthMethodDetails authMethodDetails, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        setModal(true);
        setTitle("Azure Sign In");
        setOKButtonText("Sign in");

        this.authMethodDetails = authMethodDetails;
        authFileTextField.setText(authMethodDetails == null ? null : authMethodDetails.getCredFilePath());

        oauthLoginRadioButton.addItemListener(e -> refreshAuthControlElements());

        spRadioButton.addActionListener(e -> refreshAuthControlElements());

        deviceLoginRadioButton.addActionListener(e -> refreshAuthControlElements());

        azureCliRadioButton.addActionListener(e -> refreshAuthControlElements());

        browseButton.addActionListener(e -> {
            refreshAuthControlElements();
            doSelectCredFilepath();
        });

        createNewAuthenticationFileButton.addActionListener(e -> {
            refreshAuthControlElements();
            doCreateServicePrincipal();
        });

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(oauthLoginRadioButton);
        buttonGroup.add(deviceLoginRadioButton);
        buttonGroup.add(spRadioButton);
        buttonGroup.add(azureCliRadioButton);
        azureCliRadioButton.setSelected(true);

        radioButtonComponentsMap.put(spRadioButton, Arrays.asList(servicePrincipalCommentLabel,
                                                                  authFileLabel, authFileTextField, browseButton, createNewAuthenticationFileButton));
        radioButtonComponentsMap.put(deviceLoginRadioButton, Arrays.asList(deviceLoginCommentLabel));
        init();
        checkAccountAvailability();
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

    public Single<AuthMethodDetails> login() {
        final IAzureOperationTitle title = AzureOperationBundle.title("account.sign_in");
        final AzureTask<AuthMethodDetails> task = new AzureTask<>(null, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            return this.doLogin();
        });
        return AzureTaskManager.getInstance().runInModalAsObservable(task).toSingle();
    }

    private @Nullable AuthMethodDetails doLogin() {
        authMethodDetailsResult = new AuthMethodDetails();
        if (spRadioButton.isSelected()) { // automated
            final Map<String, String> properties = new HashMap<>();
            properties.put(AZURE_ENVIRONMENT, CommonSettings.getEnvironment().getName());
            properties.putAll(signInSPProp);
            EventUtil.logEvent(EventType.info, ACCOUNT, SIGNIN, properties, null);
            final String authPath = authFileTextField.getText();
            if (StringUtils.isNullOrWhiteSpace(authPath)) {
                final String title = "Sign in dialog info";
                final String message = "Select authentication file";
                DefaultLoader.getUIHelper().showMessageDialog(contentPane, message, title, Messages.getInformationIcon());
                return null;
            }

            authMethodDetailsResult = doServicePrincipalLogin(authPath);
        } else if (deviceLoginRadioButton.isSelected()) {
            authMethodDetailsResult = doDeviceLogin();
        } else if (azureCliRadioButton.isSelected()) {
            authMethodDetailsResult = call(() -> IdentityAzureManager.getInstance().signInAzureCli().block(), signInAZProp);
        } else if (oauthLoginRadioButton.isSelected()) {
            authMethodDetailsResult = call(() -> IdentityAzureManager.getInstance().signInOAuth().block(), signInAZProp);
        }
        return authMethodDetailsResult;
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
        radioButtonComponentsMap.keySet().forEach(radio -> radioButtonComponentsMap.get(radio).forEach(comp -> comp.setEnabled(radio.isSelected())));
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
            });
    }

    private void enableAzureCliLogin() {
        azureCliCommentLabel.setIcon(null);
        azureCliRadioButton.setEnabled(true);
        azureCliRadioButton.setText("Azure CLI");
        // the default selection is oauth2, if user doesn't change it, will set the default login to azure cli
        if (oauthLoginRadioButton.isSelected()) {
            azureCliRadioButton.setSelected(true);
        }
    }

    private void disableAzureCliLogin() {
        azureCliCommentLabel.setIcon(null);
        azureCliCommentLabel.setEnabled(false);
        azureCliRadioButton.setEnabled(false);
        azureCliRadioButton.setText("Azure CLI (Not logged in)");
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

    private AuthMethodDetails doServicePrincipalLogin(final String authPath) {
        try {
            IdentityAzureManager authManager = IdentityAzureManager.getInstance();
            if (AuthMethodManager.getInstance().isSignedIn()) {
                doSignOut();
            }
            return authManager.signInServicePrincipal(AuthFile.fromFile(authPath)).block();

        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR);
        }
        return new AuthMethodDetails();
    }

    @Nullable
    private synchronized AuthMethodDetails doDeviceLogin() {
        CompletableFuture<AuthMethodDetails> deviceCodeLoginFuture = new CompletableFuture<>();
        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
                doSignOut();
            }
            final IDeviceLoginUI deviceLoginUI = CommonSettings.getUiFactory().getDeviceLoginUI();
            final AzureAccount az = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class);
            AzureEnvironment env = AzureEnvironmentUtils.stringToAzureEnvironment(CommonSettings.getEnvironment().getName());
            com.microsoft.azure.toolkit.lib.Azure.az(AzureCloud.class).set(env);
            final Account account = az.loginAsync(AuthType.DEVICE_CODE, true).block();
            Disposable subscribe = account.continueLogin().doOnCancel(() -> {
                deviceCodeLoginFuture.completeExceptionally(new IllegalStateException("user cancel"));
            }).doOnSuccess(ac -> {
                deviceCodeLoginFuture.complete(fromAccountEntity(ac.getEntity()));
            }).doOnError(deviceCodeLoginFuture::completeExceptionally).doFinally(signal -> {
                deviceLoginUI.closePrompt();
            }).subscribe();
            deviceLoginUI.setDisposable(subscribe);
            if (ApplicationManager.getApplication().isDispatchThread()) {
                deviceLoginUI.promptDeviceCode(((DeviceCodeAccount) account).getDeviceCode());
            } else {
                AzureTaskManager.getInstance().runAndWait(() ->
                                                              deviceLoginUI.promptDeviceCode(((DeviceCodeAccount) account).getDeviceCode()));
            }
            return Mono.fromFuture(deviceCodeLoginFuture).block();

        } catch (Exception ex) {
            if (ex instanceof IllegalStateException && USER_CANCEL.equals(ex.getMessage())) {
            } else {
                ex.printStackTrace();
                ErrorWindow.show(project, ex.getMessage(), SIGN_IN_ERROR);
            }
        }
        return null;
    }

    private static AuthMethodDetails fromAccountEntity(AccountEntity entity) {
        AuthMethodDetails authMethodDetails = new AuthMethodDetails();
        authMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
        authMethodDetails.setAuthType(entity.getType());
        authMethodDetails.setClientId(entity.getClientId());
        authMethodDetails.setTenantId(entity.getTenantIds().get(0));
        authMethodDetails.setAzureEnv(AzureEnvironmentUtils.getCloudNameForAzureCli(entity.getEnvironment()));
        authMethodDetails.setAccountEmail(entity.getEmail());
        return authMethodDetails;
    }

    private <T> T call(Callable<T> loginCallable, Map<String, String> properties) {
        Operation operation = TelemetryManager.createOperation(ACCOUNT, SIGNIN);
        Optional.ofNullable(ProgressManager.getInstance().getProgressIndicator()).ifPresent(indicator -> indicator.setText2("Signing in..."));

        try {
            operation.start();
            operation.trackProperties(properties);
            operation.trackProperty(AZURE_ENVIRONMENT, CommonSettings.getEnvironment().getName());
            return loginCallable.call();
        } catch (Exception e) {
            EventUtil.logError(operation, ErrorType.userError, e, properties, null);
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        } finally {
            operation.complete();
        }
    }

    private void doSignOut() {
        try {
            accountEmail = null;
            // AuthMethod.AD is deprecated.
            AuthMethodManager.getInstance().signOut();
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), "Sign Out Error");
        }
    }

    private void doCreateServicePrincipal() {
        final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        final IdentityAzureManager dcAuthManager = IdentityAzureManager.getInstance();
        try {
            if (authMethodManager.isSignedIn()) {
                authMethodManager.signOut();
            }
            doDeviceLogin();
            if (!dcAuthManager.isSignedIn()) {
                System.out.println(">> Canceled by the user");
                return;
            }

            final SubscriptionManager subscriptionManager = dcAuthManager.getSubscriptionManager();

            Optional.ofNullable(ProgressManager.getInstance().getProgressIndicator()).ifPresent(indicator -> indicator.setText2("Loading subscriptions..."));
            subscriptionManager.getSubscriptionDetails();

            final SrvPriSettingsDialog d = SrvPriSettingsDialog.go(subscriptionManager.getSubscriptionDetails(), project);
            final List<SubscriptionDetail> subscriptionDetailsUpdated;
            final String destinationFolder;
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
                .go(dcAuthManager, tidSidsMap, destinationFolder, project);
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
                    AuthMethodManager.getInstance().signOut();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
