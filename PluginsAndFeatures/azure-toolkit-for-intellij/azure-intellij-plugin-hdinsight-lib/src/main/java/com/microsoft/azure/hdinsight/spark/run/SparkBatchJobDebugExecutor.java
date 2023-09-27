/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.icons.AllIcons;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.util.PluginUtil;
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
        return Optional.ofNullable(PluginUtil.getIcon(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobDebugIcon_13x_Path))
                .map(Icon.class::cast)
                .orElse(AllIcons.RunConfigurations.RemoteDebug);
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return Optional.ofNullable(PluginUtil.getIcon(com.microsoft.azure.hdinsight.common.CommonConst.ToolWindowSparkJobDebugIcon_16x_Path))
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
