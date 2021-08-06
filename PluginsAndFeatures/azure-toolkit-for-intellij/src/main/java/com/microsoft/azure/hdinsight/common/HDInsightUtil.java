/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.ToolWindowKey;
import com.microsoft.intellij.CommonConst;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;
import org.jetbrains.annotations.NotNull;
import rx.subjects.ReplaySubject;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Error;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;

public class HDInsightUtil {
    private static final Object LOCK = new Object();
    private static final int TELEMETRY_MESSAGE_MAX_LEN = 50;

    // The replay subject for the message showed in HDInsight tool window
    // The replay subject will replay all notifications before the initialization is done
    // The replay buffer size is 1MB.
    private static ReplaySubject<SimpleImmutableEntry<MessageInfoType, String>> toolWindowMessageSubject = ReplaySubject.create(1024 * 1024);

    public static ReplaySubject<SimpleImmutableEntry<MessageInfoType, String>> getToolWindowMessageSubject() {
        return toolWindowMessageSubject;
    }

    public static void setHDInsightRootModule(@NotNull AzureModule azureModule) {
        // Enable HDInsight new SDK for IntelliJ
        DefaultLoader.getIdeHelper().setApplicationProperty(
                com.microsoft.azure.hdinsight.common.CommonConst.ENABLE_HDINSIGHT_NEW_SDK, "true");
        HDInsightRootModuleImpl hdInsightRootModule = new HDInsightRootModuleImpl(azureModule);

        azureModule.setHdInsightModule(hdInsightRootModule);
    }

    public static String normalizeTelemetryMessage(@NotNull String message) {
        if (message.length() > TELEMETRY_MESSAGE_MAX_LEN) {
            return message.substring(0, TELEMETRY_MESSAGE_MAX_LEN);
        } else {
            return message;
        }
    }

    public static void setJobRunningStatus(@NotNull Project project, boolean isRun) {
        HDInsightUtil.getJobStatusManager(project).ifPresent(jobStatusManager -> jobStatusManager.setJobRunningState(isRun));
    }

    public static Optional<JobStatusManager> getJobStatusManager(@NotNull Project project) {
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        return Optional.ofNullable(PluginUtil.getToolWindowManager(key))
                       .map(SparkSubmissionToolWindowProcessor.class::cast)
                       .map(SparkSubmissionToolWindowProcessor::getJobStatusManager);
    }

    protected static void initializeToolWindowProcessorWithSubscribe(
            SparkSubmissionToolWindowProcessor processor,
            ReplaySubject<SimpleImmutableEntry<MessageInfoType, String>> messageSubject
            ) {
        processor.initialize();
        messageSubject.subscribe(
                entry -> {
                    switch (entry.getKey()) {
                        case Error:
                            processor.setError(entry.getValue());
                            break;
                        case Info:
                            processor.setInfo(entry.getValue());
                            break;
                        case Warning:
                            processor.setWarning(entry.getValue());
                            break;
                    }
                },
                System.err::print
        );
    }

    public static SparkSubmissionToolWindowProcessor getSparkSubmissionToolWindowManager(Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        if(!PluginUtil.isContainsToolWindowKey(key)) {
            SparkSubmissionToolWindowProcessor sparkSubmissionToolWindowProcessor = new SparkSubmissionToolWindowProcessor(toolWindow);
            PluginUtil.registerToolWindowManager(key, sparkSubmissionToolWindowProcessor);

            // make sure tool window process initialize on swing dispatch
            if(ApplicationManager.getApplication().isDispatchThread()) {
                initializeToolWindowProcessorWithSubscribe(
                        sparkSubmissionToolWindowProcessor, toolWindowMessageSubject);
            } else {
                AzureTaskManager.getInstance().runLater(()-> initializeToolWindowProcessorWithSubscribe(
                        sparkSubmissionToolWindowProcessor, toolWindowMessageSubject));
            }
        }

        return (SparkSubmissionToolWindowProcessor)PluginUtil.getToolWindowManager(key);
    }

    public static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message, boolean isNeedClear) {
        showInfoOnSubmissionMessageWindow(project, Info, message, isNeedClear);
    }

    public static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, Info, message, false);
    }

    public static void showErrorMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, Error, message, false);
    }

    public static void showWarningMessageOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final String message) {
        showInfoOnSubmissionMessageWindow(project, Warning, message, false);
    }

    private static void showInfoOnSubmissionMessageWindow(@NotNull final Project project, @NotNull final MessageInfoType type, @NotNull final String message, @NotNull final boolean isNeedClear) {

        synchronized (LOCK) {
            try {
                final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);
                showSubmissionMessage(toolWindow, project, message, type, isNeedClear);
            } catch (Exception ignore) { /* IDE disposed */ }
        }
    }

    private static void showSubmissionMessage(@NotNull final ToolWindow toolWindow, @NotNull Project project, @NotNull String message, @NotNull MessageInfoType type, @NotNull final boolean isNeedClear) {
        if(!toolWindow.isVisible()) {
            if (ApplicationManager.getApplication().isDispatchThread()) {
                toolWindow.show(null);
            } else {
                AzureTaskManager.getInstance().runLater(() -> toolWindow.show(null));
            }
        }

        final SparkSubmissionToolWindowProcessor processor = getSparkSubmissionToolWindowManager(project);
        if (isNeedClear) {
            processor.clearAll();
        }

        toolWindowMessageSubject.onNext(new SimpleImmutableEntry<>(type, message));
    }
}
