package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkProvisionDialog;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;

public class ProvisionClusterAction {
    public static void provision(SparkOnCosmosADLAccountNode target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()->{
            CosmosSparkProvisionDialog dialog = new CosmosSparkProvisionDialog(e.getProject(),target,target.getRemote());
            dialog.show();
        });
    }
}
