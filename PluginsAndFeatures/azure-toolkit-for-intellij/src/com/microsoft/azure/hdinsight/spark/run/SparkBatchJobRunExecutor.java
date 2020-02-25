/*
 * Copyright (c) Microsoft Corporation
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
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.Executor;
import com.intellij.icons.AllIcons;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

public class SparkBatchJobRunExecutor extends Executor {
    @NonNls
    public static final String EXECUTOR_ID = "SparkJobRun";

    @Override
    public String getToolWindowId() {
        return SparkBatchJobRunner.RUNNER_ID;
    }

    @Override
    public Icon getToolWindowIcon() {
        return Optional.ofNullable(StreamUtil.getImageResourceFile(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobRunIcon_13x_Path))
                .map(Icon.class::cast)
                .orElse(AllIcons.Actions.Upload);
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return Optional.ofNullable(StreamUtil.getImageResourceFile(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobRunIcon_16x_Path))
                .map(Icon.class::cast)
                .orElse(AllIcons.Actions.Upload);
    }

    @Override
    public Icon getDisabledIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Submit Spark Job";
    }

    @NotNull
    @Override
    public String getActionName() {
        return "SparkJobRun";
    }

    @NotNull
    public String getId() {
        return EXECUTOR_ID;
    }

    @NotNull
    @Override
    public String getStartActionText() {
        return "Submit Spark Job";
    }

    @Override
    public String getContextActionId() {
        return "SparkJobRun";
    }

    @Override
    public String getHelpId() {
        return null;
    }
}
