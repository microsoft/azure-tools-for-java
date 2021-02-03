/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class SparkBatchJobFinishedEvent implements SparkBatchJobSubmissionEvent {
    private final boolean isJobSucceed;
    @NotNull
    private final String state;
    @NotNull
    private final String diagnostics;

    public SparkBatchJobFinishedEvent(boolean isJobSucceed, @NotNull String state, @Nullable String diagnostics) {
        this.isJobSucceed = isJobSucceed;
        this.state = state;
        this.diagnostics = diagnostics;
    }

    public boolean getIsJobSucceed() {
        return isJobSucceed;
    }

    @NotNull
    public String getState() {
        return state;
    }

    @Nullable
    public String getDiagnostics() {
        return diagnostics;
    }
}
