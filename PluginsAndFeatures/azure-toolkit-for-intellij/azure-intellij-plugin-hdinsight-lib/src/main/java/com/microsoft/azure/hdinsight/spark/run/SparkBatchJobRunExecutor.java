/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.Executor;
import com.intellij.icons.AllIcons;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.intellij.util.PluginUtil;
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
        return Optional.ofNullable(PluginUtil.getIcon(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobRunIcon_13x_Path))
                .map(Icon.class::cast)
                .orElse(AllIcons.Actions.Upload);
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return Optional.ofNullable(PluginUtil.getIcon(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobRunIcon_16x_Path))
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
