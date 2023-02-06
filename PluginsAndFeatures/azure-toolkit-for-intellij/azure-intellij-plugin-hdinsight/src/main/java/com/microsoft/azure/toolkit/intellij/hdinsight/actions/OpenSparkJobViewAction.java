package com.microsoft.azure.toolkit.intellij.hdinsight.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.toolkit.intellij.hdinsight.component.JobViewErrDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.hdinsight.SparkClusterNode;

public class OpenSparkJobViewAction {

    public static void open(SparkClusterNode target, AnActionEvent e) {
        if (JobViewManager.REGISTERED_JOBVIEW_MAP==1 || !Azure.az(AzureAccount.class).isLoggedIn()) {
            // may be a network exception
            if (JobViewManager.getCluster(target.getName())==null) {
                AzureTaskManager.getInstance().runLater(()->{
                    new JobViewErrDialog(e.getProject(), false).show();
                });
            } else {
                HDInsightLoader.getHDInsightHelper().openJobViewEditor(e.getProject(), target.getName());
            }
        } else {
            // openItem
        }
    }

}