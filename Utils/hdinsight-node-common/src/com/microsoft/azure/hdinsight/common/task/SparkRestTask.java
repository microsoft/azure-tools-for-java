/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.task;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azure.hdinsight.spark.jobs.framework.RequestDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class SparkRestTask extends Task<String> {

    public SparkRestTask(@NotNull RequestDetail requestDetail, @Nullable FutureCallback<String> callback) {
        super(callback);
    }
    @Override
    public String call() throws Exception {
        return null;
    }
}
