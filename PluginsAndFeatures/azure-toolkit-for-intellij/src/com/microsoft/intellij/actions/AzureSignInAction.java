/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.intellij.ui.SignInWindow;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.SignInOutAction;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Single;

import javax.swing.*;

import static com.microsoft.azuretools.authmanage.AuthMethod.AZ;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNOUT;

public class AzureSignInAction extends AzureAnAction {
    private static final Logger LOGGER = Logger.getInstance(AzureSignInAction.class);
    private static final String SIGN_IN = "Azure Sign In...";
    private static final String SIGN_OUT = "Azure Sign Out...";

    public AzureSignInAction() {
        super(AuthMethodManager.getInstance().isSignedIn() ? SIGN_OUT : SIGN_IN);
    }

    public AzureSignInAction(@Nullable String title) {
        super(title, title, UIHelperImpl.loadIcon(SignInOutAction.getIcon()));
    }

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent e, @Nullable Operation operation) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        onAzureSignIn(project);
        return true;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return ACCOUNT;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.SIGNIN;
    }

    @Override
    public void update(AnActionEvent e) {
        try {
            boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
            if (isSignIn) {
                e.getPresentation().setText(SIGN_OUT);
                e.getPresentation().setDescription(SIGN_OUT);
            } else {
                e.getPresentation().setText(SIGN_IN);
                e.getPresentation().setDescription(SIGN_IN);
            }
            e.getPresentation().setIcon(UIHelperImpl.loadIcon(SignInOutAction.getIcon()));
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("update", ex);
        }
    }

    private static String getSignOutWarningMessage(@NotNull AuthMethodManager authMethodManager) {
        final AuthMethodDetails authMethodDetails = authMethodManager.getAuthMethodDetails();
        if (authMethodDetails == null || authMethodDetails.getAuthType() == null) {
            return String.format("Do you really want to sign out?");
        }
        final AuthType authType = authMethodManager.getAuthMethodDetails().getAuthType();
        final String warningMessage;
        switch (authType) {
            case SERVICE_PRINCIPAL:
                warningMessage = String.format("Signed in using file \"%s\"", authMethodDetails.getCredFilePath());
                break;
            case OAUTH2:
            case DEVICE_CODE:
                warningMessage = String.format("Signed in as %s(%s)", authMethodDetails.getAccountEmail(), authType.toString());
                break;
            case AZURE_CLI:
                warningMessage = "Signed in with Azure CLI";
                break;
            default:
                warningMessage = "Signed in by unknown authentication method.";
                break;
        }
        return String.format("%s\nDo you really want to sign out? %s",
                warningMessage, authType== AuthType.AZURE_CLI ? "(This will not sign you out from Azure CLI)" : "");
    }

    public static void onAzureSignIn(Project project) {
        JFrame frame = WindowManager.getInstance().getFrame(project);
        AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        boolean isSignIn = authMethodManager.isSignedIn();
        if (isSignIn) {
            boolean res = DefaultLoader.getUIHelper().showYesNoDialog(frame.getRootPane(),
                                                                      getSignOutWarningMessage(authMethodManager),
                                                                      "Azure Sign Out",
                                                                      new ImageIcon("icons/azure.png"));
            if (res) {
                EventUtil.executeWithLog(ACCOUNT, SIGNOUT, (operation) -> {
                    authMethodManager.signOut();
                });
            }
        } else {
            doSignIn(authMethodManager, project).subscribe();
        }
    }

    @AzureOperation(name = "account.sign_in", type = AzureOperation.Type.SERVICE)
    public static Single<Boolean> doSignIn(AuthMethodManager authMethodManager, Project project) {
        final boolean isSignIn = authMethodManager.isSignedIn();
        if (isSignIn) {
            return Single.fromCallable(() -> true);
        }
        final SignInWindow dialog = new SignInWindow(authMethodManager.getAuthMethodDetails(), project);
        dialog.show();
        if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return dialog
                .login()
                .doOnSuccess(authMethodManager::setAuthMethodDetails)
                .flatMap((a) -> SelectSubscriptionsAction.selectSubscriptions(project))
                .doOnSuccess((subs) -> authMethodManager.notifySignInEventListener())
                .map((unused) -> authMethodManager.isSignedIn());
        } else {
            return Single.fromCallable(authMethodManager::isSignedIn);
        }
    }
}
