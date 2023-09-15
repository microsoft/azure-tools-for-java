/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.auth;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.subscription.SelectSubscriptionsAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
public class SignInAction extends AnAction implements DumbAware {
    private static final String SIGN_IN = "Sign In...";
    private static final String SIGN_OUT = "Sign Out";
    private static final String SIGN_IN_ERROR = "Sign In Error";

    public SignInAction() {
        super(Azure.az(AzureAccount.class).isLoggedIn() ? SIGN_OUT : SIGN_IN);
    }

    public SignInAction(@Nullable String title) {
        super(title, title, IntelliJAzureIcons.getIcon(Azure.az(AzureAccount.class).isLoggedIn()
            ? com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_OUT
            : com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_IN));
    }

    @AzureOperation(name = "user/account.authenticate")
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (Objects.nonNull(project)) {
            authActionPerformed(project);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        try {
            final boolean isSignIn = Azure.az(AzureAccount.class).isLoggedIn();
            e.getPresentation().setText(isSignIn ? SIGN_OUT : SIGN_IN);
            e.getPresentation().setDescription(isSignIn ? SIGN_IN : SIGN_OUT);
            e.getPresentation().setIcon(IntelliJAzureIcons.getIcon(isSignIn
                ? com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_OUT
                : com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_IN));
        } catch (final Exception ex) {
            ex.printStackTrace();
            log.error("update", ex);
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
            final boolean toLogout = Messages.showYesNoDialog(null, msg, "Azure Sign Out", "Yes", "No",
                IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)) == 0;
            if (toLogout) {
                az.logout();
            }
        } else {
            login(project, (account) -> {
            });
        }
    }

    @AzureOperation(name = "internal/account.sign_in")
    private static void login(Project project, Consumer<IAccount> callback) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final AuthConfiguration auth = promptForAuthConfiguration(project);
            if (Objects.isNull(auth)) {
                manager.runOnPooledThread(() -> callback.accept(null));
                return;
            }
            final DeviceLoginWindow[] dcWindow = new DeviceLoginWindow[1];
            if (auth.getType() == AuthType.DEVICE_CODE) {
                dcWindow[0] = setupDeviceCodeAuth(project, auth);
            }
            final AzureString title = OperationBundle.description("internal/account.sign_in");
            final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
                final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setIndeterminate(true);
                try {
                    final Account account = Azure.az(AzureAccount.class).login(auth, Azure.az().config().isAuthPersistenceEnabled());
                    if (account.isLoggedIn()) {
                        SelectSubscriptionsAction.selectSubscriptions(project);
                        manager.runOnPooledThread(() -> callback.accept(account));
                    } else {
                        manager.runOnPooledThread(() -> callback.accept(null));
                    }
                } catch (final Throwable t) {
                    manager.runOnPooledThread(() -> callback.accept(null));
                    final Throwable cause = ExceptionUtils.getRootCause(t);
                    Optional.ofNullable(dcWindow[0]).ifPresent(w -> manager.runLater((Runnable) w::doCancelAction));
                    if (!(cause instanceof InterruptedException)) {
                        throw t;
                    }
                }
            });
            manager.runInBackground(task);
        });
    }

    private static DeviceLoginWindow setupDeviceCodeAuth(Project project, AuthConfiguration auth) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        auth.setExecutorService(Executors.newFixedThreadPool(1));
        final DeviceLoginWindow dcWindow = new DeviceLoginWindow(project);
        dcWindow.setDoOnCancel(() -> {
            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                auth.getExecutorService().shutdownNow();
            }
        });
        auth.setDeviceCodeConsumer(info -> manager.runLater(() -> dcWindow.show(info)));
        auth.setDoAfterLogin(() -> manager.runLater((Runnable) dcWindow::close, AzureTask.Modality.ANY));
        return dcWindow;
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

    public static void requireSignedIn(Project project, Consumer<IAccount> consumer) {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            final Account account = Azure.az(AzureAccount.class).account();
            AzureTaskManager.getInstance().runOnPooledThread(() -> consumer.accept(account));
        } else {
            login(project, consumer);
        }
    }
}
