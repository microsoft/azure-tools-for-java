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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.hdinsight.common.JobStatusManager;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionExDialog;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;

import java.util.HashSet;
import java.util.Optional;

public class SubmitAction extends AnAction {
    private static final HashSet<Project> isActionPerformedSet = new HashSet<>();

    @Override
    public void actionPerformed(final AnActionEvent anActionEvent) {
        synchronized (SubmitAction.class) {
            final Project project = anActionEvent.getProject();
            if(isActionPerformedSet.contains(project)) {
                return;
            }

            isActionPerformedSet.add(project);
            AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionRightClickProject"), null);

            SparkSubmissionExDialog dialog = new SparkSubmissionExDialog(anActionEvent.getProject(), new CallBack() {
                @Override
                public void run() {
                    isActionPerformedSet.remove(anActionEvent.getProject());
                }
            });

            dialog.setVisible(true);
        }
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);

        Presentation presentation = event.getPresentation();
        if(module == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        if(this.checkActionVisible(event.getProject())) {
            presentation.setVisible(true);
            Optional<JobStatusManager> manager = HDInsightUtil.getJobStatusManager(module.getProject());
            presentation.setEnabled(!isActionPerformedSet.contains(module.getProject()) &&
                    !manager.map(JobStatusManager::isJobRunning).orElse(false));
        } else {
            presentation.setEnabledAndVisible(false);
        }
    }

    private boolean checkActionVisible(Project project) {
        Module[] moduels = ModuleManager.getInstance(project).getModules();
        for(Module module : moduels) {
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();

            for(OrderEntry orderEntry : orderEntries) {
                if (orderEntry.getPresentableName().contains("spark-core")) {
                    return true;
                }
            }
        }

        return false;
    }
}
