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
package com.microsoft.azure.hdinsight.spark.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionExDialog;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.util.AppInsightsCustomEvent;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.HashSet;
import java.util.List;

public class SubmitAction extends AnAction {
    private List<IClusterDetail> cachedClusterDetails = null;
    private static final HashSet<Project> isActionPerformedSet = new HashSet<>();

    @Override
    public void actionPerformed(final AnActionEvent anActionEvent) {
        synchronized (SubmitAction.class) {
            final Project project = anActionEvent.getProject();
//            RemoteDebug.myDebug(project);
            if(isActionPerformedSet.contains(project)) {
                return;
            }

            isActionPerformedSet.add(project);
            AppInsightsCustomEvent.create(HDInsightBundle.message("SparkSubmissionRightClickProject"), null);
            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    HDInsightUtil.showInfoOnSubmissionMessageWindow(project, "List spark clusters ...", true);

                    cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(true, project);
                    if(!ClusterManagerEx.getInstance().isSelectedSubscriptionExist()) {
                        HDInsightUtil.showWarningMessageOnSubmissionMessageWindow(project, "No selected subscription(s), Please go to HDInsight Explorer to sign in....");
                    }

                    if (ClusterManagerEx.getInstance().isListClusterSuccess()) {
                        HDInsightUtil.showInfoOnSubmissionMessageWindow(project, "List spark clusters successfully");
                    } else {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error : Failed to list spark clusters.");
                    }
                    if (ClusterManagerEx.getInstance().isLIstAdditionalClusterSuccess()) {
                        HDInsightUtil.showInfoOnSubmissionMessageWindow(project, "List additional spark clusters successfully");
                    } else {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error: Failed to list additional cluster");
                    }

                    SparkSubmissionExDialog dialog = new SparkSubmissionExDialog(anActionEvent.getProject(), cachedClusterDetails, new CallBack() {
                        @Override
                        public void run() {
                            isActionPerformedSet.remove(anActionEvent.getProject());
                        }
                    });

                    dialog.setVisible(true);
                }
            });
        }
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        VirtualFile selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());

        Presentation presentation = event.getPresentation();
        if(module == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        String uniqueValue = module.getOptionValue(HDInsightModuleBuilder.UniqueKeyName);
        boolean isVisible = !StringHelper.isNullOrWhiteSpace(uniqueValue) && uniqueValue.equals(HDInsightModuleBuilder.UniqueKeyValue);
        if(isVisible) {
            presentation.setVisible(isVisible);
            JobStatusManager manager = HDInsightUtil.getJobStatusManager(module.getProject());
            presentation.setEnabled(!isActionPerformedSet.contains(module.getProject()) && (manager == null || !manager.isJobRunning()));
        } else {
            presentation.setEnabledAndVisible(false);
        }
    }
}
