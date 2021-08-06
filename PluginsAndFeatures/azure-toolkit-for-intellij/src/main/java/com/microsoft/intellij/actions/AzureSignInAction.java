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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.aad.msal4j.MsalClientException;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.SignInOutAction;
import com.microsoft.intellij.ui.DeviceLoginUI;
import com.microsoft.intellij.ui.ServicePrincipalLoginDialog;
import com.microsoft.intellij.ui.SignInWindow;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import rx.Single;
import rx.exceptions.Exceptions;

import javax.swing.JFrame;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.AZURE_ENVIRONMENT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNIN;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNIN_METHOD;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNOUT;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

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

    public boolean onActionPerformed(@NotNull AnActionEvent e, @Nullable Operation operation) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        onAzureSignIn(project);
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
            return "Do you really want to sign out?";
        }
        final AuthType authType = authMethodDetails.getAuthType();
        final String warningMessage;
        switch (authType) {
            case SERVICE_PRINCIPAL:
                warningMessage = String.format("Signed in using service principal \"%s\"", authMethodDetails.getClientId());
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
            warningMessage, authType == AuthType.AZURE_CLI ? "(This will not sign you out from Azure CLI)" : "");
    }

    public static void onAzureSignIn(Project project) {
        JFrame frame = WindowManager.getInstance().getFrame(project);
        AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        boolean isSignIn = authMethodManager.isSignedIn();
        if (isSignIn) {
            boolean res = DefaultLoader.getUIHelper().showYesNoDialog(frame.getRootPane(), getSignOutWarningMessage(authMethodManager),
                "Azure Sign Out", AzureIconLoader.loadIcon(AzureIconSymbol.Common.AZURE));
            if (res) {
                EventUtil.executeWithLog(ACCOUNT, SIGNOUT, (operation) -> {
                    authMethodManager.signOut();
                });
            }
        } else {
            signInIfNotSignedIn(project).subscribe(isLoggedIn -> {
                if (isLoggedIn) {
                    AzureAccount az = Azure.az(AzureAccount.class);
                    AzureTaskManager.getInstance().runOnPooledThread(() ->
                        authMethodManager.getAzureManager().getSelectedSubscriptions().stream().limit(5).forEach(s -> {
                            // pre-load regions;
                            az.listRegions(s.getId());
                        }));
                }

            });
        }
    }

    @AzureOperation(name = "account.sign_in", type = AzureOperation.Type.SERVICE)
    private static Mono<Boolean> signInIfNotSignedIn(Project project) {
        if (AuthMethodManager.getInstance().isSignedIn()) {
            return Mono.just(true);
        }
        return signInIfNotSignedInInternal(project).flatMap(isLoggedIn -> {
            if (isLoggedIn) {
                persistAuthMethodDetails();
                // from rxjava1 single to mono
                return Mono.create(sink -> SelectSubscriptionsAction.selectSubscriptions(project)
                    .subscribeOn(rx.schedulers.Schedulers.io())
                    .doAfterTerminate(() -> sink.success(isLoggedIn)).doOnUnsubscribe(() -> sink.success(isLoggedIn)).subscribe());
            }
            return Mono.just(false);
        }).doOnError(e -> {
            Throwable cause = Exceptions.getFinalCause(e);
            if (shouldNoticeErrorToUser(cause)) {
                AzureMessager.getMessager().error(e);
            }
        });
    }

    private static boolean shouldNoticeErrorToUser(Throwable cause) {
        if (cause instanceof InterruptedException) {
            return false;
        }
        if (cause instanceof MsalClientException && StringUtils.equals(cause.getMessage(), "No Authorization code was returned from the server")) {
            return false;
        }
        return true;
    }

    private static void persistAuthMethodDetails() {
        AuthMethodManager.getInstance().persistAuthMethodDetails();
        AuthMethodManager.getInstance().notifySignInEventListener();
    }

    private static AuthConfiguration showSignInWindowAndGetAuthConfiguration(Project project) throws InterruptedException {
        final SignInWindow dialog = new SignInWindow(new AuthMethodDetails(), project);
        if (!dialog.showAndGet()) {
            throw new InterruptedException("user cancel");
        }

        AuthConfiguration auth = new AuthConfiguration();
        AuthType type = dialog.getData();
        auth.setType(type);
        if (type == AuthType.SERVICE_PRINCIPAL) {
            final ServicePrincipalLoginDialog spDialog = new ServicePrincipalLoginDialog(project);
            if (!spDialog.showAndGet()) {
                throw new InterruptedException("user cancel");
            }
            auth = spDialog.getData();
        }
        return auth;
    }

    private static Mono<Boolean> signInIfNotSignedInInternal(Project project) {
        final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        final IDeviceLoginUI deviceLoginUI = new DeviceLoginUI();
        return Mono.create(sink -> AzureTaskManager.getInstance().runLater(() -> {
            final AuthConfiguration auth;
            try {
                auth = showSignInWindowAndGetAuthConfiguration(project);
            } catch (InterruptedException e) {
                sink.error(e);
                return;
            }
            Single<AuthMethodDetails> single;
            if (auth.getType() != AuthType.DEVICE_CODE) {
                single = loginNonDeviceCodeSingle(auth);
            } else {

                single = loginDeviceCodeSingle().map(account -> {
                    AzureTaskManager.getInstance().runLater(() -> deviceLoginUI.promptDeviceCode(account.getDeviceCode()));
                    return account.continueLogin().map(ac -> fromAccountEntity(ac.getEntity())).doFinally(signal ->
                        deviceLoginUI.closePrompt()).subscribeOn(Schedulers.boundedElastic()).block();
                });
            }

            single.subscribeOn(rx.schedulers.Schedulers.io()).subscribe(authMethodDetails -> {
                if (authMethodManager.isSignedIn()) {
                    authMethodManager.setAuthMethodDetails(authMethodDetails);
                }
                sink.success(authMethodManager.isSignedIn());
            }, sink::error);
        }));

    }

    private static Single<AuthMethodDetails> loginNonDeviceCodeSingle(AuthConfiguration auth) {
        final AzureString title = AzureOperationBundle.title("account.sign_in");
        final AzureTask<AuthMethodDetails> task = new AzureTask<>(null, title, true, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            return doLogin(indicator, auth);
        });
        return AzureTaskManager.getInstance().runInBackgroundAsObservable(task).toSingle();
    }

    private static Single<DeviceCodeAccount> loginDeviceCodeSingle() {
        final AzureString title = AzureOperationBundle.title("account.sign_in");
        final AzureTask<DeviceCodeAccount> deviceCodeTask = new AzureTask<>(null, title, true, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            final AzureAccount az = Azure.az(AzureAccount.class);
            return (DeviceCodeAccount) az.loginAsync(AuthType.DEVICE_CODE, true).block();
        });
        return AzureTaskManager.getInstance().runInBackgroundAsObservable(deviceCodeTask).toSingle();
    }

    private static AuthMethodDetails fromAccountEntity(AccountEntity entity) {
        final AuthMethodDetails authMethodDetails = new AuthMethodDetails();
        authMethodDetails.setAuthMethod(AuthMethod.IDENTITY);
        authMethodDetails.setAuthType(entity.getType());
        authMethodDetails.setClientId(entity.getClientId());
        authMethodDetails.setTenantId(CollectionUtils.isEmpty(entity.getTenantIds()) ? "" : entity.getTenantIds().get(0));
        authMethodDetails.setAzureEnv(AzureEnvironmentUtils.getCloudNameForAzureCli(entity.getEnvironment()));
        authMethodDetails.setAccountEmail(entity.getEmail());
        return authMethodDetails;
    }

    private static AuthMethodDetails doLogin(ProgressIndicator indicator, AuthConfiguration auth) {
        AuthMethodDetails authMethodDetailsResult = new AuthMethodDetails();
        switch (auth.getType()) {
            case SERVICE_PRINCIPAL:
                authMethodDetailsResult = call(() -> checkCanceled(indicator, IdentityAzureManager.getInstance().signInServicePrincipal(auth)), "sp");
                break;
            case AZURE_CLI:
                authMethodDetailsResult = call(() -> checkCanceled(indicator, IdentityAzureManager.getInstance().signInAzureCli()), "az");
                break;
            case OAUTH2:
                authMethodDetailsResult = call(() -> checkCanceled(indicator, IdentityAzureManager.getInstance().signInOAuth()), "oauth");
                break;
            default:
                break;
        }
        return authMethodDetailsResult;
    }

    private static <T> T call(Callable<T> loginCallable, String authMethod) {
        final Operation operation = TelemetryManager.createOperation(ACCOUNT, SIGNIN);
        final Map<String, String> properties = new HashMap<>();
        properties.put(SIGNIN_METHOD, authMethod);
        Optional.ofNullable(ProgressManager.getInstance().getProgressIndicator()).ifPresent(indicator -> indicator.setText2("Signing in..."));

        try {
            operation.start();
            operation.trackProperties(properties);
            operation.trackProperty(AZURE_ENVIRONMENT, Azure.az(AzureCloud.class).getName());
            return loginCallable.call();
        } catch (Exception e) {
            EventUtil.logError(operation, ErrorType.userError, e, properties, null);
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        } finally {
            operation.complete();
        }
    }

    private static AuthMethodDetails checkCanceled(ProgressIndicator indicator, Mono<? extends AuthMethodDetails> mono) {
        final Mono<AuthMethodDetails> cancelMono = Flux.interval(Duration.ofSeconds(1)).map(ignore -> indicator.isCanceled())
            .any(cancel -> cancel).map(ignore -> new AuthMethodDetails()).subscribeOn(Schedulers.boundedElastic());
        return Mono.firstWithSignal(cancelMono, mono.subscribeOn(Schedulers.boundedElastic())).block();
    }

    public static void requireSignedIn(Project project, Runnable runnable) {
        signInIfNotSignedIn(project).subscribe((isLoggedIn) -> {
            if (isLoggedIn && AzureLoginHelper.isAzureSubsAvailableOrReportError(message("common.error.signIn"))) {
                AzureTaskManager.getInstance().runLater(runnable);
            }
        });
    }
}
