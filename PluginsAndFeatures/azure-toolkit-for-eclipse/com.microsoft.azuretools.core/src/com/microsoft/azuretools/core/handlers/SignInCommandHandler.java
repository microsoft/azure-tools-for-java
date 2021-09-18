/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.handlers;

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
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.core.ui.DeviceLoginWindow;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.ui.ServicePrincipalLoginDialog;
import com.microsoft.azuretools.core.ui.SignInDialog;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import lombok.Lombok;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import rx.Single;
import rx.exceptions.Exceptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;


public class SignInCommandHandler extends AzureAbstractHandler {
    private static final String SIGN_IN_ERROR = "Sign In Error";
    private static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    private static final String NO_SUBSCRIPTION = "No subscription in current account, you may get a free one from "
            + "https://azure.microsoft.com/en-us/free/";
    public static final String MUST_SELECT_SUBSCRIPTION =
            "Please select at least one subscription first (Tools -> Azure -> Select Subscriptions)";

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        onAzureSignIn(window.getShell());

        return null;
    }

    public static void doSignIn(Shell shell) {
        requireSignedIn(shell, ()-> { });
    }

    private static boolean showYesNoDialog(Shell shell, String title, String message) {
        MessageBox messageBox = new MessageBox(
                shell,
                SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        messageBox.setMessage(message);
        messageBox.setText(title);
        return SWT.YES == messageBox.open();
    }

    private void onAzureSignIn(Shell shell) {
        final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        boolean isSignIn = authMethodManager.isSignedIn();
        if (isSignIn) {
            boolean res = showYesNoDialog(shell, "Azure Sign Out", SignOutCommandHandler.getSignOutWarningMessage(authMethodManager));
            if (res) {
                EventUtil.executeWithLog(ACCOUNT, SIGNOUT, (operation) -> {
                    authMethodManager.signOut();
                });
            }
        } else {
            signInIfNotSignedIn(shell).subscribe(isLoggedIn -> {
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

    private static AuthConfiguration showSignInWindowAndGetAuthConfiguration(Shell parentShell) throws InterruptedException {
        final SignInDialog dialog = new SignInDialog(parentShell);
        dialog.create();

        if (dialog.open() != Window.OK) {
            throw new InterruptedException("user cancel");
        }

        AuthConfiguration auth = dialog.getData();
        if (auth.getType() == AuthType.SERVICE_PRINCIPAL) {
            ServicePrincipalLoginDialog servicePrincipalLoginDialog = new ServicePrincipalLoginDialog(parentShell);
            if (servicePrincipalLoginDialog.open() == Window.CANCEL) {
                throw new InterruptedException("user cancel");
            }
            auth = servicePrincipalLoginDialog.getModel();
        }
        return auth;
    }


    private static Mono<Boolean> signInIfNotSignedInInternal(Shell projectShell) {
        final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        final IDeviceLoginUI deviceLoginUI = new DeviceLoginWindow(projectShell);
        return Mono.create(sink -> AzureTaskManager.getInstance().runLater(() -> {
            final AuthConfiguration auth;
            try {
                auth = showSignInWindowAndGetAuthConfiguration(projectShell);
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

                    CompletableFuture<AuthMethodDetails> future =
                            account.continueLogin().map(ac -> fromAccountEntity(ac.getEntity())).doFinally(signal -> deviceLoginUI.closePrompt()).toFuture();
                    deviceLoginUI.setFuture(future);

                    try {
                        return future.get();
                    } catch (Throwable ex) {
                        if (!(ex instanceof CancellationException)) {
                            ex.printStackTrace();
                            ErrorWindow.go(projectShell, ex.getMessage(), SIGN_IN_ERROR);
                        }
                        return new AuthMethodDetails();
                    }
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
        final AzureTask<AuthMethodDetails> task = new AzureTask<>(null, title, true,
                () -> doLogin(AzureTaskContext.current().getTask().getMonitor(), auth));
        return AzureTaskManager.getInstance().runInBackgroundAsObservable(task).toSingle();
    }

    private static Single<DeviceCodeAccount> loginDeviceCodeSingle() {
        final AzureString title = AzureOperationBundle.title("account.sign_in");
        final AzureTask<DeviceCodeAccount> deviceCodeTask = new AzureTask<>(null, title, true, () -> {
            final AzureAccount az = Azure.az(AzureAccount.class);
            return (DeviceCodeAccount) checkCanceled(null, az.loginAsync(AuthType.DEVICE_CODE, true), () -> {
                throw Lombok.sneakyThrow(new InterruptedException("user cancel"));
            });
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

    private static AuthMethodDetails doLogin(AzureTask.Monitor monitor, AuthConfiguration auth) {
        AuthMethodDetails authMethodDetailsResult = new AuthMethodDetails();
        switch (auth.getType()) {
            case SERVICE_PRINCIPAL:
                authMethodDetailsResult = call(() -> checkCanceled(monitor, IdentityAzureManager.getInstance().signInServicePrincipal(auth),
                        AuthMethodDetails::new), "sp");
                break;
            case AZURE_CLI:
                authMethodDetailsResult = call(() -> checkCanceled(monitor, IdentityAzureManager.getInstance().signInAzureCli(),
                        AuthMethodDetails::new), "az");
                break;
            case OAUTH2:
                authMethodDetailsResult = call(() -> checkCanceled(monitor, IdentityAzureManager.getInstance().signInOAuth(),
                        AuthMethodDetails::new), "oauth");
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
        try {
            operation.start();
            operation.trackProperties(properties);
            operation.trackProperty(AZURE_ENVIRONMENT, Azure.az(AzureCloud.class).getName());
            return loginCallable.call();
        } catch (Exception e) {
            if (shouldNoticeErrorToUser(e)) {
                EventUtil.logError(operation, ErrorType.userError, e, properties, null);
            }
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        } finally {
            operation.complete();
        }
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


    private static <T> T checkCanceled(AzureTask.Monitor monitor, Mono<? extends T> mono, Supplier<T> supplier) {
        if (monitor == null) {
            return mono.block();
        }
        final Mono<T> cancelMono = Flux.interval(Duration.ofSeconds(1)).map(ignore -> monitor.isCancelled())
                .any(cancel -> cancel).map(ignore -> supplier.get()).subscribeOn(Schedulers.boundedElastic());
        return Mono.firstWithSignal(cancelMono, mono.subscribeOn(Schedulers.boundedElastic())).block();
    }

    public static void requireSignedIn(Shell parentShell, Runnable runnable) {
        signInIfNotSignedIn(parentShell).subscribe((isLoggedIn) -> {
            if (isLoggedIn && isAzureSubsAvailableOrReportError(parentShell, "Error occurs on signing in")) {
                AzureTaskManager.getInstance().runLater(runnable);
            }
        });
    }

    public static boolean isAzureSubsAvailableOrReportError(Shell parentShell, String dialogTitle) {
        try {
            ensureAzureSubsAvailable();
            return true;
        } catch (AzureExecutionException e) {
            PluginUtil.displayErrorDialog(parentShell, dialogTitle, e.getMessage());
            return false;
        }
    }

    private static void ensureAzureSubsAvailable() throws AzureExecutionException {
        if (!AuthMethodManager.getInstance().isSignedIn()) {
            throw new AzureExecutionException(NEED_SIGN_IN);
        }
        IdentityAzureManager azureManager = IdentityAzureManager.getInstance();
        final List<Subscription> subscriptions = azureManager.getSubscriptions();
        if (CollectionUtils.isEmpty(subscriptions)) {
            throw new AzureExecutionException(NO_SUBSCRIPTION);
        }
        final List<Subscription> selectedSubscriptions = azureManager.getSelectedSubscriptions();
        if (CollectionUtils.isEmpty(selectedSubscriptions)) {
            throw new AzureExecutionException(MUST_SELECT_SUBSCRIPTION);
        }

    }
    @AzureOperation(name = "account.sign_in", type = AzureOperation.Type.SERVICE)
    private static Mono<Boolean> signInIfNotSignedIn(Shell shell) {
        if (AuthMethodManager.getInstance().isSignedIn()) {
            return Mono.just(true);
        }
        return signInIfNotSignedInInternal(shell).map(isLoggedIn -> {
            if (isLoggedIn) {
                persistAuthMethodDetails();
                // from rxjava1 single to mono
                AzureTaskManager.getInstance().runLater(() ->
                        SelectSubsriptionsCommandHandler.onSelectSubscriptions(shell));
                return true;
            }
            return false;
        }).doOnError(e -> {
            Throwable cause = Exceptions.getFinalCause(e);
            if (shouldNoticeErrorToUser(cause)) {
                AzureMessager.getMessager().error(e);
            }
        });
    }
}
