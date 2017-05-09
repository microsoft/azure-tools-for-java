/**
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
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.common;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.intellij.ToolWindowKey;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.util.AppInsightsCustomEvent;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;
import com.microsoft.intellij.common.CommonConst;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;

public class HDInsightUtil {
    private static final Object LOCK = new Object();

    public static void setHDInsightRootModule(@NotNull AzureModule azureModule) {
        HDInsightRootModuleImpl hdInsightRootModule =  new HDInsightRootModuleImpl(azureModule);

        // add telemetry for HDInsight Node
        hdInsightRootModule.addClickActionListener(new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                AppInsightsCustomEvent.create(HDInsightBundle.message("HDInsightExplorerHDInsightNodeExpand"), null);
            }
        });

        azureModule.setHdInsightModule(hdInsightRootModule);
    }

    @Nullable
    public static JobStatusManager getJobStatusManager(@NotNull Project project) {
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);
        if(PluginUtil.isContainsToolWindowKey(key)){
            return ((SparkSubmissionToolWindowProcessor)PluginUtil.getToolWindowManager(key)).getJobStatusManager();
        } else {
            return null;
        }
    }

    public static SparkSubmissionToolWindowProcessor getSparkSubmissionToolWindowManager(Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);
        ToolWindowKey key = new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        if(!PluginUtil.isContainsToolWindowKey(key)) {
            SparkSubmissionToolWindowProcessor sparkSubmissionToolWindowProcessor = new SparkSubmissionToolWindowProcessor(toolWindow);
            PluginUtil.registerToolWindowManager(key, sparkSubmissionToolWindowProcessor);
            sparkSubmissionToolWindowProcessor.initialize();
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
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CommonConst.SPARK_SUBMISSION_WINDOW_ID);

        if (!toolWindow.isVisible()) {
            synchronized (LOCK) {
                if (!toolWindow.isVisible()) {
                    if (ApplicationManager.getApplication().isDispatchThread()) {
                        toolWindow.show(null);
                    } else {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    toolWindow.show(null);
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        showSubmissionMessage(getSparkSubmissionToolWindowManager(project), message, type, isNeedClear);
    }

    private static void showSubmissionMessage(SparkSubmissionToolWindowProcessor processor, @NotNull String message, @NotNull MessageInfoType type, @NotNull final boolean isNeedClear) {
        if (isNeedClear) {
            processor.clearAll();
        }

        switch (type) {
            case Error:
                processor.setError(message);
                break;
            case Info:
                processor.setInfo(message);
                break;
            case Warning:
                processor.setWarning(message);
                break;
        }
    }
}
