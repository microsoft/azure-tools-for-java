/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.mvc;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Scheduler;

public interface IdeSchedulers {
    public Scheduler processBarVisibleAsync(@NotNull String title);

    public Scheduler processBarVisibleSync(@NotNull String title);

    public Scheduler dispatchUIThread();

    public Scheduler dispatchPooledThread();
}
