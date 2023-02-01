package com.microsoft.azure.toolkit.ide.hdinsight.spark;

import com.azure.resourcemanager.hdinsight.models.StorageAccount;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.OpenHDIAzureStorageExplorerAction;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.*;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.hdinsight.AzureHDInsightService;
import com.microsoft.azure.toolkit.lib.hdinsight.SparkClusterNode;
import com.microsoft.azure.toolkit.lib.hdinsight.StorageAccountNode;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class HDInsightActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.hdinsight.service";
    public static final String SPARK_CLUSTER_ACTIONS = "actions.hdinsight.spark";
    public static final String SPARK_ADDITIONAL_CLUSTER_ACTIONS = "actions.hdinsight_additional.spark";
    public static final String HDINSIGHT_STORAGE_ACTIONS = "actions.hdinsight.storage";

    public static final Action.Id<ResourceGroup> GROUP_CREATE_HDInsight_SERVICE = Action.Id.of("user/hdinsight.create_hdinsight.group");
    public static final Action.Id<Object> LINK_A_CLUSTER = Action.Id.of("user/hdinsight.link_a_cluster.spark");
    public static final Action.Id<Object> UNLINK_A_CLUSTER = Action.Id.of("user/hdinsight.unlink_a_cluster.spark");
    public static final Action.Id<AzResource> OPEN_HDINSIGHT_JOB_VIEW = Action.Id.of("user/hdinsight.open_hdinsight_job_view.spark");
    public static final Action.Id<AzResource> OPEN_AZURE_STORAGE_EXPLORER = Action.Id.of("user/hdinsight.open_azure_storage_explorer.cluster");
    public static final Action.Id<AzResourceModule> OPEN_AZURE_STORAGE_EXPLORER_ON_MODULE = Action.Id.of("user/hdinsight.open_azure_storage_explorer_form.cluster");
    public static final Action.Id<AzResource> OPEN_AZURE_EXPLORER_AMBARI = Action.Id.of("user/hdinsight.open_azure_management_explorer.ambari");
    public static final Action.Id<AzResource> OPEN_AZURE_EXPLORER_JUPYTER = Action.Id.of("user/hdinsight.open_jupyter_explorer.jupyter");
    public static final Action.Id<AzResource> OPEN_SPARK_HISTORY_UI = Action.Id.of("user/hdinsight.open_history_ui.spark");
    public static final Action.Id<AzResource> OPEN_AZURE_STORAGE_MANAGEMENT_EXPLORER = Action.Id.of("user/hdinsight.open_azure_storage_explorer.storage");



    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(LINK_A_CLUSTER)
                .withLabel("Link A Cluster")
                .enableWhen(s -> true)
                .withShortcut(am.getIDEDefaultShortcuts().edit())
                .register(am);

        new Action<>(UNLINK_A_CLUSTER)
                .withLabel("Unlink")
                .enableWhen(s -> s instanceof SparkClusterNode)
                .withHandler(resource -> {
                    SparkClusterNode sparkClusterNode = (SparkClusterNode) resource;
                    boolean choice = DefaultLoader.getUIHelper().showConfirmation("Do you really want to unlink the HDInsight cluster?",
                            "Unlink HDInsight Cluster", new String[]{"Yes", "No"}, null);
                    if (choice) {
                        ClusterManagerEx.getInstance().removeAdditionalCluster(sparkClusterNode.getClusterDetail());
                        AzureHDInsightService service = az(AzureHDInsightService.class);
                        service.refresh();
                    }
                })
                .withShortcut(am.getIDEDefaultShortcuts().edit())
                .register(am);

        new Action<>(OPEN_HDINSIGHT_JOB_VIEW)
                .withLabel("Open HDInsight Spark JobView")
                .enableWhen(s -> s instanceof AzResource)
                .withShortcut(am.getIDEDefaultShortcuts().edit())
                .register(am);

        new Action<>(OPEN_AZURE_STORAGE_EXPLORER)
                .withLabel("Open Azure Storage Explorer")
                .enableWhen(s -> s instanceof SparkClusterNode)
                .withHandler(resource -> {
                    IClusterDetail clusterDetail = ((SparkClusterNode) resource).getClusterDetail();
                    final AzureString title = OperationBundle.description("user/hdinsight.open_azure_storage_explorer.cluster", clusterDetail.getName());
                    AzureTaskManager.getInstance().runInBackground(new AzureTask<>(title, () -> {
                        OpenHDIAzureStorageExplorerAction openHDIAzureStorageExplorerAction = new OpenHDIAzureStorageExplorerAction();
                        openHDIAzureStorageExplorerAction.openResource(clusterDetail);
                    }));
                })
                .withShortcut(am.getIDEDefaultShortcuts().edit())
                .register(am);

        new Action<>(OPEN_AZURE_STORAGE_EXPLORER_ON_MODULE)
                .withLabel("Open Azure Storage Explorer!")
                .enableWhen(s -> true)
                .withShortcut(am.getIDEDefaultShortcuts().edit())
                .register(am);

        new Action<>(OPEN_AZURE_EXPLORER_AMBARI)
                .withLabel("Open Cluster Management Portal(Ambari)")
                .enableWhen(s -> s instanceof SparkClusterNode)
                .withHandler(resource -> {
                    IClusterDetail clusterDetail = ((SparkClusterNode) resource).getClusterDetail();
                    String ambariUrl = clusterDetail.isEmulator() ?
                            ((EmulatorClusterDetail) clusterDetail).getAmbariEndpoint() :
                            ClusterManagerEx.getInstance().getClusterConnectionString(clusterDetail.getName());
                    openUrlLink(clusterDetail,ambariUrl);
                })
                .register(am);

        new Action<>(OPEN_AZURE_EXPLORER_JUPYTER)
                .withLabel("Open Jupyter Notebook")
                .enableWhen(s -> s instanceof SparkClusterNode)
                .withHandler(resource -> {
                    IClusterDetail clusterDetail = ((SparkClusterNode) resource).getClusterDetail();
                    final String jupyterUrl = ClusterManagerEx.getInstance().getClusterConnectionString(clusterDetail.getName()) + "/jupyter/tree";
                    openUrlLink(clusterDetail,jupyterUrl);
                })
                .register(am);

        new Action<>(OPEN_SPARK_HISTORY_UI)
                .withLabel("Open Spark History UI")
                .enableWhen(s -> s instanceof SparkClusterNode)
                .withHandler(resource -> {
                    IClusterDetail clusterDetail = ((SparkClusterNode) resource).getClusterDetail();
                    String sparkHistoryUrl = clusterDetail.isEmulator() ?
                            ((EmulatorClusterDetail)clusterDetail).getSparkHistoryEndpoint() :
                            ClusterManagerEx.getInstance().getClusterConnectionString(clusterDetail.getName()) + "/sparkhistory";
                    openUrlLink(clusterDetail,sparkHistoryUrl);
                })
                .register(am);

        new Action<>(OPEN_AZURE_STORAGE_MANAGEMENT_EXPLORER)
                .withLabel("Open Storage in Azure management Portal")
                .enableWhen(s -> s instanceof StorageAccountNode)
                .withHandler(resource -> {
                    final Account account = Azure.az(AzureAccount.class).account();
                    final String portalUrl = account.getPortalUrl();
                    StorageAccountNode storageAccountNode = (StorageAccountNode)resource;
                    StorageAccount remote = storageAccountNode.getRemote(true);
                    String resourceId = remote.resourceId();
                    String tenantId = storageAccountNode.getSubscription().getTenantId();
                    String url = portalUrl + "/#@" + tenantId + "/resource" + resourceId;
                    DefaultLoader.getIdeHelper().openLinkInBrowser(url);
                })
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                this.LINK_A_CLUSTER
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup sparkActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
//                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                this.OPEN_AZURE_STORAGE_EXPLORER,
                this.OPEN_AZURE_EXPLORER_AMBARI,
                this.OPEN_AZURE_EXPLORER_JUPYTER,
                this.OPEN_SPARK_HISTORY_UI
        );
        am.registerGroup(SPARK_CLUSTER_ACTIONS, sparkActionGroup);

        final ActionGroup sparkAdditionalActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
//                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                this.OPEN_AZURE_STORAGE_EXPLORER,
                this.OPEN_AZURE_EXPLORER_AMBARI,
                this.OPEN_AZURE_EXPLORER_JUPYTER,
                this.OPEN_SPARK_HISTORY_UI,
                "---",
                this.UNLINK_A_CLUSTER
        );
        am.registerGroup(SPARK_ADDITIONAL_CLUSTER_ACTIONS, sparkAdditionalActionGroup);

        final ActionGroup storageActionGroup = new ActionGroup(
                this.OPEN_AZURE_STORAGE_MANAGEMENT_EXPLORER
        );
        am.registerGroup(HDINSIGHT_STORAGE_ACTIONS, storageActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_HDInsight_SERVICE);
    }

    public void openUrlLink(IClusterDetail clusterDetail,String linkUrl){
        if (!StringHelper.isNullOrWhiteSpace(clusterDetail.getName())) {
            try {
                DefaultLoader.getIdeHelper().openLinkInBrowser(linkUrl);
            } catch (Exception exception) {
                DefaultLoader.getUIHelper().showError(exception.getMessage(), "HDInsight Explorer");
            }
        }
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }

}
