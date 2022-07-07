/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.intellij.serviceexplorer.azure.SignInOutAction;
import com.microsoft.intellij.ui.DeviceLoginUI;
import com.microsoft.intellij.ui.ServicePrincipalLoginDialog;
import com.microsoft.intellij.ui.SignInWindow;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;

public class AzureSignInAction extends AzureAnAction implements DumbAware {
    private static final Logger LOGGER = Logger.getInstance(AzureSignInAction.class);
    private static final String SIGN_IN = "Azure Sign In...";
    private static final String SIGN_OUT = "Azure Sign Out...";
    private static final String SIGN_IN_ERROR = "Sign In Error";

    public AzureSignInAction() {
        super(IdeAzureAccount.getInstance().isLoggedIn() ? SIGN_OUT : SIGN_IN);
    }

    public AzureSignInAction(@Nullable String title) {
        super(title, title, IntelliJAzureIcons.getIcon(SignInOutAction.getIcon()));
    }

    public boolean onActionPerformed(@NotNull AnActionEvent e, @Nullable Operation operation) {
        final Project project = DataKeys.PROJECT.getData(e.getDataContext());
        authActionPerformed(project);
        return true;
    }

    protected String getServiceName(AnActionEvent event) {
        return ACCOUNT;
    }

    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.SIGNIN;
    }

    @Override
    public void update(AnActionEvent e) {
        try {
            final boolean isSignIn = IdeAzureAccount.getInstance().isLoggedIn();
            e.getPresentation().setText(isSignIn ? SIGN_OUT : SIGN_IN);
            e.getPresentation().setDescription(isSignIn ? SIGN_IN : SIGN_OUT);
            e.getPresentation().setIcon(IntelliJAzureIcons.getIcon(SignInOutAction.getIcon()));
        } catch (final Exception ex) {
            ex.printStackTrace();
            LOGGER.error("update", ex);
        }
    }

    private static String getSignOutWarningMessage(@Nonnull Account account) {
        final AuthType authType = account.getType();
        final String warningMessage;
        switch (authType) {
            case SERVICE_PRINCIPAL:
                warningMessage = String.format("Signed in using service principal \"%s\"", account.getClientId());
                break;
            case OAUTH2:
            case DEVICE_CODE:
                warningMessage = String.format("Signed in as %s(%s)", account.getUsername(), authType.toString());
                break;
            case AZURE_CLI:
                warningMessage = "Signed in with Azure CLI";
                break;
            default:
                warningMessage = "Signed in by unknown authentication method.";
                break;
        }
        return String.format("%s\nDo you really want to sign out? %s",
            warningMessage, authType == AuthType.AZURE_CLI ? "(This will not sign you out from Azure CLI)" : "");
    }

    public static void authActionPerformed(Project project) {
        final JFrame frame = WindowManager.getInstance().getFrame(project);
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (az.isLoggedIn()) {
            final String msg = getSignOutWarningMessage(az.account());
            final boolean toLoggout = DefaultLoader.getUIHelper().showYesNoDialog(frame.getRootPane(), msg,
                "Azure Sign Out", IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
            if (toLoggout) {
                az.logout();
            }
        } else {
            login(project, () -> {
            });
        }
    }

    @AzureOperation(name = "account.sign_in", type = AzureOperation.Type.SERVICE)
    private static void login(Project project, Runnable callback) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final AuthConfiguration auth = promptForAuthConfiguration(project);
            if (Objects.isNull(auth)) {
                return;
            }
            if (auth.getType() == AuthType.DEVICE_CODE) {
                final IDeviceLoginUI deviceLoginUI = new DeviceLoginUI();
                auth.setDeviceCodeConsumer(info -> manager.runLater(() -> deviceLoginUI.promptDeviceCode(info)));
                auth.setDoAfterLogin(() -> manager.runLater(deviceLoginUI::closePrompt, AzureTask.Modality.ANY));
            }
            final AzureString title = OperationBundle.description("account.sign_in");
            final AzureTask<Void> task = new AzureTask<>(null, title, true, () -> {
                final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setIndeterminate(true);
                final Account account = Azure.az(AzureAccount.class).login(auth, true);
                if (account.isLoggedIn()) {
                    SelectSubscriptionsAction.selectSubscriptions(project);
                    manager.runLater(callback);
                }
            });
            manager.runInBackground(task);
        });
    }

    @Nullable
    private static AuthConfiguration promptForAuthConfiguration(Project project) {
        final SignInWindow dialog = new SignInWindow(project);
        if (!dialog.showAndGet()) {
            return null;
        }

        AuthConfiguration config = new AuthConfiguration(dialog.getData());
        if (config.getType() == AuthType.SERVICE_PRINCIPAL) {
            final ServicePrincipalLoginDialog spDialog = new ServicePrincipalLoginDialog(project);
            if (!spDialog.showAndGet()) {
                return null;
            }
            config = spDialog.getValue();
        }
        return config;
    }

    public static void requireSignedIn(Project project, Runnable runnable) {
        if (IdeAzureAccount.getInstance().isLoggedIn()) {
            AzureTaskManager.getInstance().runLater(runnable);
        } else {
            login(project, runnable);
        }
    }
}
