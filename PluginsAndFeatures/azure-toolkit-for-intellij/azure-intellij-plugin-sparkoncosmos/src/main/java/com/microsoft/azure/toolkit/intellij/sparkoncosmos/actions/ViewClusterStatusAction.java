package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkClusterMonitorDialog;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;

public class ViewClusterStatusAction {
    public static void view(SparkOnCosmosClusterNode target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()->{
            CosmosSparkClusterMonitorDialog monitorDialog = new CosmosSparkClusterMonitorDialog(
                    e.getProject(), target.getRemote());
            monitorDialog.show();
        });
    }
}
