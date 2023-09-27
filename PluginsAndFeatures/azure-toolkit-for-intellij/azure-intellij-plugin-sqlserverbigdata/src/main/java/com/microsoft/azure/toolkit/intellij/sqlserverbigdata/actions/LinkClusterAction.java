package com.microsoft.azure.toolkit.intellij.sqlserverbigdata.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataModule;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataService;
import com.microsoft.sqlbigdata.serverexplore.ui.AddNewSqlBigDataClusterForm;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class LinkClusterAction {
    public static void link(Object target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()->{
            int originalSize = ClusterManagerEx.getInstance().getAdditionalClusterDetails().size();
            AddNewSqlBigDataClusterForm form = new AddNewSqlBigDataClusterForm(e.getProject(), null);
            form.show();
            int changedSize = ClusterManagerEx.getInstance().getAdditionalClusterDetails().size();
            if(originalSize!=changedSize){
                SqlserverBigDataService service = az(SqlserverBigDataService.class);
                service.refresh();
            }
        });
    }
}
