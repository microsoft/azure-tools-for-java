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

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.intellij.ui.SubscriptionsDialog;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.ManageSubscriptionsAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SelectSubscriptionsAction extends AzureAnAction {
    private static final Logger LOGGER = Logger.getInstance(SelectSubscriptionsAction.class);

    public SelectSubscriptionsAction() {
    }

    @Override
    @AzureOperation(name = "select one/more subscriptions to work on", type = AzureOperation.Type.ACTION)
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
        final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
        final AzureManager manager = authMethodManager.getAzureManager();
        if (manager == null) {
            return Single.fromCallable(() -> null);
        }

        final SubscriptionManager subscriptionManager = manager.getSubscriptionManager();

        return loadSubscriptions(subscriptionManager, project)
            .switchMap((subs) -> selectSubscriptions(project, subs))
            .toSingle()
            .doOnSuccess((subs) -> Optional.ofNullable(subs).ifPresent(subscriptionManager::setSubscriptionDetails));
    }

    private static Observable<List<SubscriptionDetail>> selectSubscriptions(final Project project, List<SubscriptionDetail> subs) {
        return AzureTaskManager.getInstance().runLaterAsObservable(new AzureTask<>(() -> {
            final SubscriptionsDialog d = SubscriptionsDialog.go(subs, project);
            return Objects.nonNull(d) ? d.getSubscriptionDetails() : null;
        }));
    }

    @AzureOperation(name = "load all available subscriptions from Azure", type = AzureOperation.Type.SERVICE)
    public static Observable<List<SubscriptionDetail>> loadSubscriptions(final SubscriptionManager subscriptionManager, Project project) {
        return AzureTaskManager.getInstance().runInModalAsObservable(new AzureTask<>(project, "Loading Subscriptions...", false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            return subscriptionManager.getSubscriptionDetails();
        }));
    }
}
