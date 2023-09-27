package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkClusterUpdateDialog;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;

public class UpdateClusterAction {
    public static void update(SparkOnCosmosClusterNode target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()->{
            CosmosSparkClusterUpdateDialog updateDialog = new CosmosSparkClusterUpdateDialog(
                    target,e.getProject(),target.getRemote()
            );
            updateDialog.show();
        });
    }
}
