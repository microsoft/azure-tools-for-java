package com.microsoft.azure.toolkit.ide.cosmos.spark;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterModule;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class SparkOnCosmosActionsContributor implements IActionsContributor {
    private static final String SPARK_NOTEBOOK_LINK = "https://aka.ms/spkadlnb";
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final String SERVICE_ACTIONS = "actions.sparkoncosmos.service";
    public static final Action.Id<ResourceGroup> GROUP_CREATE_SOC_SERVICE = Action.Id.of("user/sparkoncosmos.create_sparkoncosmos.group");
    public static final String ADLA_NODE_ACTIONS = "actions.sparkoncosmos.adla";
    public static final String CLUSTER_NODE_ACTIONS = "actions.sparkoncosmos.cluster";
    public static final Action.Id<Object> PROVISION_CLUSTER = Action.Id.of("user/sparkoncosmos.provision_cluster.spark");
    public static final Action.Id<Object> SUBMIT_SOC_SERVERLESS_JOB = Action.Id.of("user/sparkoncosmos.submit_serverlessjob.spark");
    public static final Action.Id<Object> VIEW_SOC_SERVERLESS_JOB = Action.Id.of("user/sparkoncosmos.view_serverlessjob.spark");
    public static final Action.Id<SparkOnCosmosClusterNode> DELETE_CLUSTER = Action.Id.of("user/sparkoncosmos.delete_cluster.spark");
    public static final Action.Id<SparkOnCosmosClusterNode> OPEN_SPARK_HISTORY_UI = Action.Id.of("user/sparkoncosmos.open_history_ui.spark");
    public static final Action.Id<SparkOnCosmosClusterNode> OPEN_SPARK_MASTER_UI = Action.Id.of("user/sparkoncosmos.open_master_ui.spark");
    public static final Action.Id<SparkOnCosmosClusterNode> VIEW_CLUSTER_STATUS = Action.Id.of("user/sparkoncosmos.view_cluster_status.spark");
    public static final Action.Id<SparkOnCosmosClusterNode> UPDATE_CLUSTER = Action.Id.of("user/sparkoncosmos.update_cluster.spark");
    public static final Action.Id<SparkOnCosmosClusterNode> SUBMIT_CLUSTER_JOB = Action.Id.of("user/sparkoncosmos.submit_cluster_job.spark");


    public static final Action.Id<Object> OPEN_NOTEBOOK = Action.Id.of("user/sparkoncosmos.open_notebook.spark");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(GROUP_CREATE_SOC_SERVICE)
                .withLabel("Apache Spark on Cosmos")
                .withIdParam(AzResource::getName)
                .visibleWhen(s -> s instanceof ResourceGroup)
                .enableWhen(s -> true)
                .register(am);

        new Action<>(OPEN_NOTEBOOK)
                .withLabel("Open Notebook")
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .withHandler(resource -> {
                    try {
                        Desktop.getDesktop().browse(URI.create(SPARK_NOTEBOOK_LINK));
                    } catch (IOException ignore) {
                    }
                })
                .register(am);

        new Action<>(PROVISION_CLUSTER)
                .withLabel("Provision Spark Cluster")
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .register(am);

        new Action<>(SUBMIT_SOC_SERVERLESS_JOB)
                .withLabel("Submit Apache Spark on Cosmos Serverless Job")
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .register(am);

        new Action<>(VIEW_SOC_SERVERLESS_JOB)
                .withLabel("View Apache Spark on Cosmos Serverless Jobs")
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .register(am);

        new Action<>(DELETE_CLUSTER)
                .withLabel("Delete")
                .enableWhen(s -> s.getRemote().isRunning())
                .withAuthRequired(false)
                .withHandler(r->{})
                .register(am);

        new Action<>(OPEN_SPARK_HISTORY_UI)
                .withLabel("Open Spark History UI")
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .withHandler(r->{
                    try {
                        AzureSparkCosmosCluster remote = r.getRemote();
                        SparkOnCosmosClusterModule module = (SparkOnCosmosClusterModule) r.getModule();
                        SparkOnCosmosADLAccountNode adlAccountNode = module.getAdlAccountNode();
                        AzureSparkServerlessAccount adlAccount = adlAccountNode.getRemote();
                        String suffix = "/?adlaAccountName=" + adlAccount.getName();
                        Desktop.getDesktop().browse(URI.create(String.valueOf(remote.getSparkHistoryUiUri() + suffix)));
                    } catch (IOException ignore) {
                    }
                })
                .register(am);

        new Action<>(OPEN_SPARK_MASTER_UI)
                .withLabel("Open Spark Master UI")
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .withHandler(r->{
                    try {
                        AzureSparkCosmosCluster remote = r.getRemote();
                        SparkOnCosmosClusterModule module = (SparkOnCosmosClusterModule) r.getModule();
                        SparkOnCosmosADLAccountNode adlAccountNode = module.getAdlAccountNode();
                        AzureSparkServerlessAccount adlAccount = adlAccountNode.getRemote();
                        String suffix = "/?adlaAccountName=" + adlAccount.getName();
                        Desktop.getDesktop().browse(URI.create(String.valueOf(remote.getSparkMasterUiUri() + suffix)));
                    } catch (IOException ignore) {
                    }
                })
                .register(am);

        new Action<>(VIEW_CLUSTER_STATUS)
                .withLabel("View Cluster Status")
                .enableWhen(s -> s.getRemote().isRunning())
                .withAuthRequired(false)
                .withHandler(r->{})
                .register(am);

        new Action<>(SUBMIT_CLUSTER_JOB)
                .withLabel("Submit Job")
                .enableWhen(s -> s.getRemote().isStable())
                .withAuthRequired(false)
                .withHandler(r->{})
                .register(am);

        new Action<>(UPDATE_CLUSTER)
                .withLabel("Update")
                .enableWhen(s -> s.getRemote().isStable())
                .withAuthRequired(false)
                .withHandler(r->{})
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                this.OPEN_NOTEBOOK
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup adlaActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                this.PROVISION_CLUSTER,
                this.SUBMIT_SOC_SERVERLESS_JOB,
                this.VIEW_SOC_SERVERLESS_JOB
        );
        am.registerGroup(ADLA_NODE_ACTIONS, adlaActionGroup);

        final ActionGroup clusterActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                this.DELETE_CLUSTER,
                this.OPEN_SPARK_HISTORY_UI,
                this.OPEN_SPARK_MASTER_UI,
                this.SUBMIT_CLUSTER_JOB,
                this.UPDATE_CLUSTER,
                this.VIEW_CLUSTER_STATUS
        );
        am.registerGroup(CLUSTER_NODE_ACTIONS, clusterActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_SOC_SERVICE);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
