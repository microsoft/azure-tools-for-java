/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.mvc;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class DummyIdeSchedulers implements IdeSchedulers {
    @Override
    public Scheduler processBarVisibleAsync(@NotNull String title) {
        return Schedulers.io();
    }

    @Override
    public Scheduler processBarVisibleSync(@NotNull String title) {
        return Schedulers.immediate();
    }

    @Override
    public Scheduler dispatchUIThread() {
        return Schedulers.computation();
    }

    @Override
    public Scheduler dispatchPooledThread() {
        return Schedulers.io();
    }
}
