package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkClusterDestoryDialog;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;

public class DeleteCusterAction {
    public static void delete(SparkOnCosmosClusterNode target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()->{
            CosmosSparkClusterDestoryDialog destroyDialog = new CosmosSparkClusterDestoryDialog(
                    target,e.getProject(),target.getRemote());
            destroyDialog.show();
        });
    }
}
