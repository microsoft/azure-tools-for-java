/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.task;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.concurrent.Executors;

public class TaskExecutor {
    private static ListeningExecutorService executors = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    public static <T> ListenableFuture<T> submit(@NotNull Task<T> task) {
        final ListenableFuture<T> listenableFuture = executors.submit(task);
        Futures.addCallback(listenableFuture, task.callback, MoreExecutors.directExecutor());
        return listenableFuture;
    }
}
