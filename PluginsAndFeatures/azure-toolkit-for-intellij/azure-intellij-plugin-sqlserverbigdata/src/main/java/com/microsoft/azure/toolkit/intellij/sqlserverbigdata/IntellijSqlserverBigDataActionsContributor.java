package com.microsoft.azure.toolkit.intellij.sqlserverbigdata;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.sqlserver.spark.SqlserverBigDataActionsContributor;
import com.microsoft.azure.toolkit.ide.sqlserver.spark.SqlserverBigDataNodeProvider;
import com.microsoft.azure.toolkit.intellij.sqlserverbigdata.actions.LinkClusterAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataModule;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijSqlserverBigDataActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> linkClusterCondition = (r, e) -> r instanceof Object;
        final BiConsumer<Object, AnActionEvent> linkClusterHandler = (c, e) -> LinkClusterAction.link(c, e);
        am.registerHandler(SqlserverBigDataActionsContributor.LINK_CLUSTER, linkClusterCondition, linkClusterHandler);
    }
}
