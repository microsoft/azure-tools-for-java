/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class SparkFailureTaskDebugSettingsCtrlProvider {
    @NotNull
    private final SettableControl<SparkFailureTaskDebugSettingsModel> view;

    public SparkFailureTaskDebugSettingsCtrlProvider(@NotNull SettableControl<SparkFailureTaskDebugSettingsModel> view) {
        this.view = view;
    }
}
