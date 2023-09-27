package com.microsoft.azure.toolkit.ide.sqlserver.spark;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Refreshable;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataModule;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataNode;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataService;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SqlserverBigDataActionsContributor implements IActionsContributor {
    public static final String SQLSERVER_CLUSTER_ACTIONS = "actions.sqlserver.spark";
    public static final Action.Id<ResourceGroup> GROUP_CREATE_SQLSERVERBIGDATA_SERVICE = Action.Id.of("user/sqlserverbigdata.create_sqlserverbigdata.group");
    public static final Action.Id<Object> LINK_CLUSTER = Action.Id.of("user/sqlserverbigdata.link_cluster.spark");
    public static final Action.Id<Object> UNLINK_CLUSTER = Action.Id.of("user/sqlserverbigdata.unlink_cluster.spark");
    public static final Action.Id<Refreshable> REFRESH = Action.Id.of("user/sqlserverbigdata.refresh_resource.resource");
    public static final Action.Id<Object> OPEN_SPARK_HISTORY_UI = Action.Id.of("user/sqlserverbigdata.open_history_ui.spark");
    public static final Action.Id<Object> OPEN_YARN_UI = Action.Id.of("user/sqlserverbigdata.open_yarn_ui.spark");
    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(REFRESH)
                .withLabel("Refresh")
                .withIcon(AzureIcons.Action.REFRESH.getIconPath())
                .withIdParam(s -> Optional.ofNullable(s).map(r -> {
                    if (r instanceof AzResource) {
                        return ((AzResource) r).getName();
                    } else if (r instanceof AbstractAzResourceModule) {
                        return ((AbstractAzResourceModule<?, ?, ?>) r).getResourceTypeName();
                    }
                    throw new IllegalArgumentException("Unsupported type: " + r.getClass());
                }).orElse(null))
                .withShortcut(am.getIDEDefaultShortcuts().refresh())
                .withAuthRequired(false)
                .visibleWhen(s -> s instanceof Refreshable)
                .withHandler(Refreshable::refresh)
                .register(am);

        new Action<>(LINK_CLUSTER)
                .withLabel("link cluster")
                .visibleWhen(s -> s instanceof Object)
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .register(am);

        new Action<>(UNLINK_CLUSTER)
                .withLabel("unlink cluster")
                .visibleWhen(s -> s instanceof Object)
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .withHandler(resource -> {
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the SQL Server big data cluster?",
                            "Unlink SQL Server Big Data Cluster", new String[]{"Yes", "No"}, null);
                    if (choice) {
                        SqlserverBigDataNode sqlserverBigDataNode = (SqlserverBigDataNode)resource;
                        IClusterDetail iClusterDetail = sqlserverBigDataNode.getiClusterDetail();
                        SqlBigDataLivyLinkClusterDetail cluster = (SqlBigDataLivyLinkClusterDetail)iClusterDetail;
                        ClusterManagerEx.getInstance().removeAdditionalCluster(cluster);
                        SqlserverBigDataService service = az(SqlserverBigDataService.class);
                        service.refresh();
                    }
                })
                .register(am);

        new Action<>(OPEN_SPARK_HISTORY_UI)
                .withLabel("Open Spark History UI")
                .visibleWhen(s -> s instanceof SqlserverBigDataNode)
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .withHandler(resource -> {
                    try {
                        SqlserverBigDataNode sqlserverBigDataNode = (SqlserverBigDataNode)resource;
                        IClusterDetail iClusterDetail = sqlserverBigDataNode.getiClusterDetail();
                        SqlBigDataLivyLinkClusterDetail detail = (SqlBigDataLivyLinkClusterDetail)iClusterDetail;
                        DefaultLoader.getIdeHelper().openLinkInBrowser(detail.getSparkHistoryUrl());
                    } catch (Exception ignore) {
                    }
                })
                .register(am);

        new Action<>(OPEN_YARN_UI)
                .withLabel("Open Yarn UI")
                .visibleWhen(s -> s instanceof SqlserverBigDataNode)
                .enableWhen(s -> true)
                .withAuthRequired(false)
                .withHandler(resource -> {
                    try {
                        SqlserverBigDataNode sqlserverBigDataNode = (SqlserverBigDataNode)resource;
                        IClusterDetail iClusterDetail = sqlserverBigDataNode.getiClusterDetail();
                        SqlBigDataLivyLinkClusterDetail cluster = (SqlBigDataLivyLinkClusterDetail)iClusterDetail;
                        DefaultLoader.getIdeHelper().openLinkInBrowser(cluster.getYarnUIUrl());
                    } catch (Exception ignore) {
                    }
                })
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup sparkActionGroup = new ActionGroup(
                SqlserverBigDataActionsContributor.REFRESH,
                SqlserverBigDataActionsContributor.OPEN_SPARK_HISTORY_UI,
                SqlserverBigDataActionsContributor.OPEN_YARN_UI,
                SqlserverBigDataActionsContributor.UNLINK_CLUSTER
        );
        am.registerGroup(SQLSERVER_CLUSTER_ACTIONS, sparkActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_SQLSERVERBIGDATA_SERVICE);
    }

}
