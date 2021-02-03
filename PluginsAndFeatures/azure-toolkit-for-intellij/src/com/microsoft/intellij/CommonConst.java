/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;

public class CommonConst {
    public static final String SPARK_SUBMISSION_WINDOW_ID = "HDInsight Spark Submission";
    public static final String DEBUG_SPARK_JOB_WINDOW_ID = "Debug Remote Spark Job in Cluster";
    public static final String REMOTE_SPARK_JOB_WINDOW_ID = "Remote Spark Job in Cluster";
    public static final String PLUGIN_ID = "com.microsoft.tooling.msservices.intellij.azure";
    public static final String PLUGIN_NAME = "azure-toolkit-for-intellij";
    public static final String PLUGIN_VERISON = PluginManager.getPlugin(PluginId.getId(PLUGIN_ID)).getVersion();
    public static final String SPARK_APPLICATION_TYPE = "com.microsoft.azure.hdinsight.DefaultSparkApplicationType";

    public static final String LOADING_TEXT = "Loading...";
    public static final String EMPTY_TEXT = "Empty";
    public static final String REFRESH_TEXT = "Refreshing...";
    public static final String RESOURCE_WITH_RESOURCE_GROUP = "%s (Resource Group: %s)";
    public static final String NEW_CREATED_RESOURCE = "%s (New Created)";
}
