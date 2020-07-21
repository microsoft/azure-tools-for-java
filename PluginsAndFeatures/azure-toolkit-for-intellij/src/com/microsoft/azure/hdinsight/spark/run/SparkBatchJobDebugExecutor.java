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

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.icons.AllIcons;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;


public class SparkBatchJobDebugExecutor extends Executor {
    @NonNls
    public static final String EXECUTOR_ID = "SparkJobDebug";

    @Override
    public String getToolWindowId() {
        return SparkBatchJobDebuggerRunner.RUNNER_ID;
    }

    @Override
    public Icon getToolWindowIcon() {
        return Optional.ofNullable(StreamUtil.getImageResourceFile(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobDebugIcon_13x_Path))
                .map(Icon.class::cast)
                .orElse(AllIcons.RunConfigurations.RemoteDebug);
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return Optional.ofNullable(StreamUtil.getImageResourceFile(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobDebugIcon_16x_Path))
                .map(Icon.class::cast)
                .orElse(AllIcons.RunConfigurations.RemoteDebug);
    }

    @Override
    public Icon getDisabledIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Debug Spark Job in cluster";
    }

    @NotNull
    @Override
    public String getActionName() {
        return "SparkJobDebug";
    }

    @NotNull
    public String getId() {
        return EXECUTOR_ID;
    }

    @NotNull
    @Override
    public String getStartActionText() {
        return "Remotely debug Spark Job";
    }

    @Override
    public String getContextActionId() {
        return "SparkJobDebug";
    }

    @Override
    public String getHelpId() {
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Executor)) {
            return false;
        }

        final Executor other = (Executor) obj;

        // Intellij requires the executor equaling DefaultDebugExecutor to enable the support for multiple debug tabs
        // And all executors are Singletons, only compare the ID
        return other.getId().equals(DefaultDebugExecutor.EXECUTOR_ID) || other.getId().equals(this.getId());
    }
}
