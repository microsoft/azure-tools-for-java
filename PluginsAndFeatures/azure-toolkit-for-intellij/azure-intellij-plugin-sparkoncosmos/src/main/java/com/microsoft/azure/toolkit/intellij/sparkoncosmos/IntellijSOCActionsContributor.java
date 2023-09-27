package com.microsoft.azure.toolkit.intellij.sparkoncosmos;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.cosmos.spark.SparkOnCosmosActionsContributor;
import com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions.*;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijSOCActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {

        final BiPredicate<Object, AnActionEvent> provisionClusterCondition = (r, e) -> r instanceof Object;
        final BiConsumer<Object, AnActionEvent> provisionClusterHandler = (c, e) -> ProvisionClusterAction.provision((SparkOnCosmosADLAccountNode)c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.PROVISION_CLUSTER, provisionClusterCondition, provisionClusterHandler);

        final BiPredicate<Object, AnActionEvent> submitSOCServerlessJobCondition = (r, e) -> r instanceof Object;
        final BiConsumer<Object, AnActionEvent> submitSOCServerlessJobHandler = (c, e) -> SubmitSOCServerlessJob.submit((SparkOnCosmosADLAccountNode)c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.SUBMIT_SOC_SERVERLESS_JOB, submitSOCServerlessJobCondition, submitSOCServerlessJobHandler);

        final BiPredicate<Object, AnActionEvent> viewSOCServerlessJobCondition = (r, e) -> r instanceof Object;
        final BiConsumer<Object, AnActionEvent> viewSOCServerlessJobHandler = (c, e) -> ViewSOCServerlessJobsAction.view((SparkOnCosmosADLAccountNode)c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.VIEW_SOC_SERVERLESS_JOB, viewSOCServerlessJobCondition, viewSOCServerlessJobHandler);

        final BiPredicate<SparkOnCosmosClusterNode, AnActionEvent> deleteClusterCondition = (r, e) -> r instanceof Object;
        final BiConsumer<SparkOnCosmosClusterNode, AnActionEvent> deleteClusterHandler = (c, e) -> DeleteCusterAction.delete(c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.DELETE_CLUSTER, deleteClusterCondition, deleteClusterHandler);

        final BiPredicate<SparkOnCosmosClusterNode, AnActionEvent> viewClusterStatusCondition = (r, e) -> r instanceof Object;
        final BiConsumer<SparkOnCosmosClusterNode, AnActionEvent> viewClusterStatusHandler = (c, e) -> ViewClusterStatusAction.view(c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.VIEW_CLUSTER_STATUS, viewClusterStatusCondition, viewClusterStatusHandler);

        final BiPredicate<SparkOnCosmosClusterNode, AnActionEvent> updateClusterCondition = (r, e) -> r instanceof Object;
        final BiConsumer<SparkOnCosmosClusterNode, AnActionEvent> updateClusterHandler = (c, e) -> UpdateClusterAction.update(c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.UPDATE_CLUSTER, updateClusterCondition, updateClusterHandler);

        final BiPredicate<SparkOnCosmosClusterNode, AnActionEvent> submitClusterJobCondition = (r, e) -> r instanceof Object;
        final BiConsumer<SparkOnCosmosClusterNode, AnActionEvent> submitClusterJobHandler = (c, e) -> SubmitClusterJobAction.submit(c, e);
        am.registerHandler(SparkOnCosmosActionsContributor.SUBMIT_CLUSTER_JOB, submitClusterJobCondition, submitClusterJobHandler);
    }
}
