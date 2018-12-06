/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.cosmosspark;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.cosmosserverlessspark.spark.ui.livy.batch.CosmosServerlessSparkBatchJobsTableSchema;
import com.microsoft.azure.cosmosserverlessspark.spark.ui.livy.batch.CosmosServerlessSparkBatchJobsViewer;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkADLAccountNode;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterOps;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkClusterDestoryDialog;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkClusterMonitorDialog;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkClusterUpdateDialog;
import com.microsoft.azure.cosmosspark.serverexplore.ui.CosmosSparkProvisionDialog;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJobList;
import com.microsoft.azure.hdinsight.spark.actions.SparkAppSubmitContext;
import com.microsoft.azure.hdinsight.spark.actions.SparkSubmitJobAction;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkConfigurationFactory;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkConfigurationType;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkConfigurationFactory;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkConfigurationType;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableModel;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableViewport;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobViewer;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.UniqueColumnNameTableSchema;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType;
import static com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.CLUSTER;
import static com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.RUN_CONFIGURATION_SETTING;

public class CosmosSparkClusterOpsCtrl implements ILogger {
    @NotNull
    private final CosmosSparkClusterOps sparkServerlessClusterOps;
    private IdeSchedulers ideSchedulers = new IdeaSchedulers(null);

    public CosmosSparkClusterOpsCtrl(@NotNull CosmosSparkClusterOps sparkServerlessClusterOps) {
        this.sparkServerlessClusterOps = sparkServerlessClusterOps;

        this.sparkServerlessClusterOps.getDestroyAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(triplet -> {
                    log().info(String.format("Destroy message received. AdlAccount: %s, cluster: %s, currentNode: %s",
                            triplet.getLeft().getName(),
                            // Type cast is necessary for DestroyableCluster
                            ((AzureSparkServerlessCluster) triplet.getMiddle()).getName(),
                            triplet.getRight().getName()));
                    CosmosSparkClusterDestoryDialog destroyDialog = new CosmosSparkClusterDestoryDialog(
                            triplet.getRight(), triplet.getMiddle());
                    destroyDialog.show();
                }, ex -> log().error(ExceptionUtils.getStackTrace(ex)));

        this.sparkServerlessClusterOps.getProvisionAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(pair -> {
                    log().info(String.format("Provision message received. AdlAccount: %s, node: %s",
                            pair.getLeft().getName(), pair.getRight().getName()));
                    CosmosSparkProvisionDialog provisionDialog = new CosmosSparkProvisionDialog(
                            pair.getRight(), pair.getLeft());
                    provisionDialog.show();
                }, ex -> log().error(ExceptionUtils.getStackTrace(ex)));

        this.sparkServerlessClusterOps.getMonitorAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(pair -> {
                    log().info(String.format("Monitor message received. cluster: %s, node: %s",
                            pair.getLeft().getName(), pair.getRight().getName()));
                    CosmosSparkClusterMonitorDialog monitorDialog = new CosmosSparkClusterMonitorDialog(
                            pair.getRight(), pair.getLeft());
                    monitorDialog.show();
                }, ex -> log().error(ExceptionUtils.getStackTrace(ex)));

        this.sparkServerlessClusterOps.getUpdateAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(pair -> {
                    log().info(String.format("Update message received. cluster: %s, node: %s",
                            pair.getLeft().getName(), pair.getRight().getName()));
                    CosmosSparkClusterUpdateDialog updateDialog = new CosmosSparkClusterUpdateDialog(
                            pair.getRight(), pair.getLeft());
                    updateDialog.show();
                }, ex -> log().error(ExceptionUtils.getStackTrace(ex)));

        this.sparkServerlessClusterOps.getSubmitAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(clusterNodePair -> {
                    log().info(String.format("Submit message received. cluster: %s, node: %s",
                            clusterNodePair.getLeft(), clusterNodePair.getRight()));

                    try {
                        AzureSparkServerlessCluster cluster = clusterNodePair.getLeft();
                        SparkAppSubmitContext context = new SparkAppSubmitContext();
                        Project project = (Project) clusterNodePair.getRight().getProject();

                        final RunManager runManager = RunManager.getInstance(project);
                        final List<RunnerAndConfigurationSettings> batchConfigSettings = runManager
                                .getConfigurationSettingsList(findConfigurationType(CosmosSparkConfigurationType.class));

                        final String runConfigName = "[Azure Data Lake Spark] " + cluster.getName();
                        final RunnerAndConfigurationSettings runConfigurationSetting = batchConfigSettings.stream()
                                .filter(settings -> settings.getConfiguration().getName().startsWith(runConfigName))
                                .findFirst()
                                .orElseGet(() -> runManager.createRunConfiguration(
                                        runConfigName,
                                        new CosmosSparkConfigurationFactory(new CosmosSparkConfigurationType())));

                        context.putData(RUN_CONFIGURATION_SETTING, runConfigurationSetting)
                                .putData(CLUSTER, cluster);

                        Presentation actionPresentation = new Presentation("Submit Job");
                        actionPresentation.setDescription("Submit specified Spark application into the remote cluster");

                        AnActionEvent event = AnActionEvent.createFromDataContext(
                                String.format("Azure Data Lake Spark pool %s:%s context menu",
                                        cluster.getAccount().getName(), cluster.getName()),
                                actionPresentation,
                                context);

                        new SparkSubmitJobAction().actionPerformed(event);
                    } catch (Exception ex) {
                        log().error(ex.getMessage());
                    }
                }, ex -> log().error(ExceptionUtils.getStackTrace(ex)));

        this.sparkServerlessClusterOps.getServerlessSubmitAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(accountNodePair -> {
                    log().info(String.format("Submit message received. account: %s, node: %s",
                            accountNodePair.getLeft().getName(), accountNodePair.getRight().getName()));

                    try {
                        AzureSparkServerlessAccount adlAccount = accountNodePair.getLeft();
                        SparkAppSubmitContext context = new SparkAppSubmitContext();
                        Project project = (Project) accountNodePair.getRight().getProject();

                        final RunManager runManager = RunManager.getInstance(project);
                        final List<RunnerAndConfigurationSettings> batchConfigSettings = runManager
                                .getConfigurationSettingsList(findConfigurationType(CosmosServerlessSparkConfigurationType.class));

                        final String runConfigName = "[Cosmos Serverless Spark] " + adlAccount.getName();
                        final RunnerAndConfigurationSettings runConfigurationSetting = batchConfigSettings.stream()
                                .filter(settings -> settings.getConfiguration().getName().startsWith(runConfigName))
                                .findFirst()
                                .orElseGet(() -> runManager.createRunConfiguration(
                                        runConfigName,
                                        new CosmosServerlessSparkConfigurationFactory(new CosmosServerlessSparkConfigurationType())));

                        context.putData(RUN_CONFIGURATION_SETTING, runConfigurationSetting)
                                .putData(CLUSTER, adlAccount);

                        Presentation actionPresentation = new Presentation("Submit Cosmos Serverless Spark Job");
                        actionPresentation.setDescription("Submit specified Spark application into the remote cluster");

                        AnActionEvent event = AnActionEvent.createFromDataContext(
                                String.format("Cosmos Serverless Cluster %s:%s context menu",
                                        adlAccount.getName(), adlAccount.getName()),
                                actionPresentation,
                                context);

                        new SparkSubmitJobAction().actionPerformed(event);
                    } catch (Exception ex) {
                        log().error(ex.getMessage());
                    }
                }, ex -> log().error(ExceptionUtils.getStackTrace(ex)));

        this.sparkServerlessClusterOps.getViewServerlessJobsAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .flatMap(accountNodePair -> {
                    log().info(String.format("View serverless jobs message received. account: %s, node: %s",
                            accountNodePair.getLeft().getName(), accountNodePair.getRight().getName()));
                    // check if the requested job list tab exists in tool window
                    AzureSparkServerlessAccount account = accountNodePair.getLeft();
                    CosmosSparkADLAccountNode node = accountNodePair.getRight();
                    ToolWindow toolWindow = ToolWindowManager.getInstance((Project) node.getProject()).getToolWindow("Cosmos Serverless Spark Jobs");
                    Content existingContent= toolWindow.getContentManager().findContent(getDisplayName(account.getName()));
                    if (existingContent != null) {
                        // if the requested job list tab already exists in tool window,
                        // show the existing job list tab
                        toolWindow.getContentManager().setSelectedContent(existingContent);
                        toolWindow.activate(null);
                        return null;
                    } else {
                        // create a new tab if the requested job list tab does not exists
                        return account.getSparkBatchJobList()
                                .doOnNext(sparkBatchJobList -> {
                                    // show serverless spark job list
                                    LivyBatchJobViewer.Model model =
                                            new LivyBatchJobViewer.Model(
                                                    new LivyBatchJobTableViewport.Model(
                                                            new LivyBatchJobTableModel(new CosmosServerlessSparkBatchJobsTableSchema()),
                                                            getFirstJobPage(account, sparkBatchJobList)),
                                                    null
                                            );
                                    CosmosServerlessSparkBatchJobsViewer jobView = new CosmosServerlessSparkBatchJobsViewer(account);
                                    jobView.setData(model);

                                    JButton refreshButton = new JButton(PluginUtil.getIcon(CommonConst.REFRESH_DARK_ICON_PATH));
                                    refreshButton.setToolTipText("Refresh Jobs");
                                    refreshButton.addActionListener(actionEvent ->
                                            account.getSparkBatchJobList()
                                                    .doOnNext(sparkBatchJobList1 -> {
                                                        LivyBatchJobViewer.Model refreshedModel =
                                                                new LivyBatchJobViewer.Model(
                                                                        new LivyBatchJobTableViewport.Model(
                                                                                new LivyBatchJobTableModel(new CosmosServerlessSparkBatchJobsTableSchema()),
                                                                                getFirstJobPage(account, sparkBatchJobList)),
                                                                        null
                                                                );
                                                        jobView.setData(refreshedModel);
                                                    })
                                                    .subscribe(jobList -> {}, ex -> log().error(ExceptionUtils.getStackTrace(ex))));

                                    JPanel panel = new JPanel();
                                    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
                                    panel.add(refreshButton);
                                    panel.add(jobView.getComponent());

                                    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                                    Content content = contentFactory.createContent(panel, getDisplayName(account.getName()), false);
                                    content.setDisposer(jobView);
                                    toolWindow.getContentManager().addContent(content);
                                    toolWindow.getContentManager().setSelectedContent(content);
                                    toolWindow.activate(null);
                                });
                    }
                })
                .subscribe(jobList -> {},  ex -> log().warn(ExceptionUtils.getStackTrace(ex)));
    }

    @NotNull
    private String getDisplayName(@NotNull String adlAccountName) {
        return adlAccountName + " Jobs";
    }

    @NotNull
    private LivyBatchJobTableModel.JobPage getFirstJobPage(@NotNull AzureSparkServerlessAccount account,
                                                           @NotNull SparkBatchJobList jobList) {
        return new LivyBatchJobTableModel.JobPage() {
            @Nullable
            @Override
            public List<UniqueColumnNameTableSchema.RowDescriptor> items() {
                CosmosServerlessSparkBatchJobsTableSchema tableSchema = new CosmosServerlessSparkBatchJobsTableSchema();
                return jobList.value().stream()
                        .map(sparkBatchJob -> tableSchema.new CosmosServerlessSparkJobDescriptor(account, sparkBatchJob))
                        .collect(Collectors.toList());
            }

            @Nullable
            @Override
            public String nextPageLink() {
                return jobList.nextLink();
            }
        };
    }
}
