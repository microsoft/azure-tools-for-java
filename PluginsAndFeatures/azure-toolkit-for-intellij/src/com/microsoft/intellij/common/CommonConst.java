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

package com.microsoft.intellij.common;

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

    public static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    public static final String NEED_SELECT_SUBSCRIPTION = "Please select subscription in azure explorer first.";
    public static final String LOADING_TEXT = "Loading...";
    public static final String EMPTY_TEXT = "Empty";
    public static final String REFRESH_TEXT = "Refreshing...";
    public static final String RESOURCE_WITH_RESOURCE_GROUP = "%s (Resource Group: %s)";
    public static final String NEW_CREATED_RESOURCE = "%s (New Created)";
}
