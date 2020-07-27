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
