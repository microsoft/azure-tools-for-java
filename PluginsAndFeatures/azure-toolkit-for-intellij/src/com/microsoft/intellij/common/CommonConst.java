/**
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.common;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.microsoft.intellij.util.PluginUtil;

public class CommonConst {
    public static final String SPARK_SUBMISSION_WINDOW_ID = "HDInsight Spark Submission";
    public static final String DEBUG_SPARK_JOB_WINDOW_ID = "Debug Remote Spark Job in Cluster";
    public static final String REMOTE_SPARK_JOB_WINDOW_ID = "Remote Spark Job in Cluster";
    public static final String PLUGIN_ID = "com.intellij.resharper.azure";
    public static final String PLUGIN_NAME = "azure-toolkit-for-intellij";
    public static final String PLUGIN_VERISON = getPluginVersion();
    public static final String SPARK_APPLICATION_TYPE = "com.microsoft.azure.hdinsight.DefaultSparkApplicationType";

    private static String getPluginVersion() {
        IdeaPluginDescriptor descriptor = PluginUtil.getPluginDescriptor();
        if (descriptor == null)
            return "";

        return descriptor.getVersion();
    }
}
