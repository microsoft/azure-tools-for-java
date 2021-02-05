/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.concurrent.Callable;

public abstract class NodeActionListenerAsync extends NodeActionListener {
    private final String progressMessage;

    public NodeActionListenerAsync(@NotNull String progressMessage) {
        this.progressMessage = progressMessage;
    }

    /**
     * Async action.
     *
     * @param actionEvent event object.
     * @return ListenableFuture object.
     */
    public ListenableFuture<Void> actionPerformedAsync(final NodeActionEvent actionEvent) {
        Callable<Boolean> booleanCallable = beforeAsyncActionPerformed();

        boolean shouldRun = true;

        try {
            shouldRun = booleanCallable.call();
        } catch (Exception ignored) {
            // ignore
        }

        final SettableFuture<Void> future = SettableFuture.create();

        if (shouldRun) {
            final Object project = actionEvent.getAction().getNode().getProject();
            AzureTaskManager.getInstance().runInBackground(new AzureTask(project, progressMessage, false, () -> {
                try {
                    actionPerformed(actionEvent);
                    future.set(null);
                } catch (AzureCmdException e) {
                    future.setException(e);
                }
            }));
        } else {
            future.set(null);
        }

        return future;
    }

    @NotNull
    protected abstract Callable<Boolean> beforeAsyncActionPerformed();
}
