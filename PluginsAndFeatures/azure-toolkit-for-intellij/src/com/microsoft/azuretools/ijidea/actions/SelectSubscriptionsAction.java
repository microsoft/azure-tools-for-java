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

package com.microsoft.azuretools.ijidea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.ui.ErrorWindow;
import com.microsoft.azuretools.ijidea.ui.SubscriptionsDialog;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.ManageSubscriptionsAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Single;

import java.util.List;

public class SelectSubscriptionsAction extends AzureAnAction {
    private static final Logger LOGGER = Logger.getInstance(SelectSubscriptionsAction.class);

    public SelectSubscriptionsAction() {
    }

    @Override
    @AzureOperation(value = "select one/more subscriptions to work on", type = AzureOperation.Type.ACTION)
    public boolean onActionPerformed(@NotNull AnActionEvent e, @Nullable Operation operation) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        selectSubscriptions(project).subscribe();
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        try {
            boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
            e.getPresentation().setEnabled(isSignIn);
            e.getPresentation().setIcon(UIHelperImpl.loadIcon(ManageSubscriptionsAction.getIcon()));
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("update", ex);
        }
    }

    public static Single<List<SubscriptionDetail>> selectSubscriptions(Project project) {
        //Project project = ProjectManager.getInstance().getDefaultProject();();

        final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        final AzureManager manager = authMethodManager.getAzureManager();
        if (manager == null) {
            return Single.fromCallable(() -> null);
        }

        final SubscriptionManager subscriptionManager = manager.getSubscriptionManager();

        return loadSubscriptions(subscriptionManager, project)
            .switchMap((list) -> selectSubscriptions(project, subscriptionManager))
            .toSingle()
            .doOnSuccess(subscriptionManager::setSubscriptionDetails);
    }

    private static Observable<List<SubscriptionDetail>> selectSubscriptions(final Project project, final SubscriptionManager subscriptionManager) {
        return AzureTaskManager.getInstance().runLater(new AzureTask<>(() -> {
            //System.out.println("onShowSubscriptions: calling getSubscriptionDetails()");
            SubscriptionsDialog d = SubscriptionsDialog.go(subscriptionManager.getSubscriptionDetails(), project);
            List<SubscriptionDetail> subscriptionDetailsUpdated;
            return d != null ? d.getSubscriptionDetails() : null;
        }));
    }

    @AzureOperation(value = "load all available subscriptions from Azure", type = AzureOperation.Type.SERVICE)
    public static Observable<List<SubscriptionDetail>> loadSubscriptions(final SubscriptionManager subscriptionManager, Project project) {
        return AzureTaskManager.getInstance().runInModal(new AzureTask<>(project, "Loading Subscriptions...", true, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            //System.out.println("updateSubscriptionWithProgressDialog: calling getSubscriptionDetails()");
            EventUtil.executeWithLog(TelemetryConstants.ACCOUNT, TelemetryConstants.GET_SUBSCRIPTIONS, (operation) -> {
                return subscriptionManager.getSubscriptionDetails();
            }, AzureExceptionHandler::onUncaughtException);
        }));
    }
}
