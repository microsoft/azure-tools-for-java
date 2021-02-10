/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.task;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

public abstract class Task<V> implements Callable<V> {

    protected static Logger logger = Logger.getLogger(Task.class.getName());

    protected FutureCallback<V> callback;

    public Task(@Nullable FutureCallback<V> callback) {
            this.callback = callback;
    }

    public static final FutureCallback<Object> EMPTY_CALLBACK = new FutureCallback<Object>() {
        @Override
        public void onSuccess(Object o) {
            logger.info("task success");
        }

        @Override
        public void onFailure(Throwable throwable) {
            logger.info("task failed");
        }
    };
}
