/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.DataKey;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;

public class SparkDataKeys {
    public static final DataKey<IClusterDetail> CLUSTER = DataKey.create("spark.cluster");

    public static final DataKey<RunnerAndConfigurationSettings> RUN_CONFIGURATION_SETTING = DataKey.create("spark.runconfiguration.settings");

    public static final DataKey<String> MAIN_CLASS_NAME = DataKey.create("spark.runconfiguration.mainclassname");
}
