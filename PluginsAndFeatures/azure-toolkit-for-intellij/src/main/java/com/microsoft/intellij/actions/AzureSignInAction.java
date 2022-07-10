/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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

    public static void authActionPerformed(Project project) {
        final JFrame frame = WindowManager.getInstance().getFrame(project);
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (az.isLoggedIn()) {
            final Account account = az.account();
            final AuthType authType = account.getType();
            final String warningMessage = String.format("Signed in as \"%s\" with %s", account.getUsername(), authType.getLabel());
            final String additionalMsg = authType == AuthType.AZURE_CLI ? "(This will not sign you out from Azure CLI)" : "";
            final String msg = String.format("%s\nDo you really want to sign out? %s", warningMessage, additionalMsg);
            final boolean toLogout = DefaultLoader.getUIHelper().showYesNoDialog(frame.getRootPane(), msg,
                "Azure Sign Out", IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
            if (toLogout) {
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
            IDeviceLoginUI deviceLoginUI = null;
            if (auth.getType() == AuthType.DEVICE_CODE) {
                final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("azure-toolkit-auth-%d").build();
                final ExecutorService executorService = Executors.newFixedThreadPool(1, namedThreadFactory);
                auth.setExecutorService(executorService);
                deviceLoginUI = new DeviceLoginUI(executorService::shutdownNow);
                final IDeviceLoginUI finalDeviceLoginUI = deviceLoginUI;
                auth.setDeviceCodeConsumer(info -> manager.runLater(() -> finalDeviceLoginUI.promptDeviceCode(info)));
                auth.setDoAfterLogin(() -> manager.runLater(finalDeviceLoginUI::closePrompt, AzureTask.Modality.ANY));
            }
            final AzureString title = OperationBundle.description("account.sign_in");
            final IDeviceLoginUI finalDeviceLoginUI = deviceLoginUI;
            final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
                final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setIndeterminate(true);
                try {
                    final Account account = Azure.az(AzureAccount.class).login(auth, true);
                    if (account.isLoggedIn()) {
                        SelectSubscriptionsAction.selectSubscriptions(project);
                        manager.runLater(callback);
                    }
                } catch (final Throwable t) {
                    Optional.ofNullable(auth.getExecutorService()).filter(s -> !s.isShutdown()).ifPresent(ExecutorService::shutdown);
                    final Throwable cause = ExceptionUtils.getRootCause(t);
                    Optional.ofNullable(finalDeviceLoginUI).ifPresent(IDeviceLoginUI::closePrompt);
                    if (!(cause instanceof InterruptedException)) {
                        throw t;
                    }
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
