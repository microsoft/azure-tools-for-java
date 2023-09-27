package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.cosmosserverlessspark.spark.ui.livy.batch.CosmosServerlessSparkBatchJobsTableSchema;
import com.microsoft.azure.cosmosserverlessspark.spark.ui.livy.batch.CosmosServerlessSparkBatchJobsViewer;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkADLAccountNode;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJobList;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableModel;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableViewport;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobViewer;
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.UniqueColumnNameTableSchema;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.*;
import rx.Observable;

import java.util.List;
import java.util.stream.Collectors;

public class ViewSOCServerlessJobsAction {
    public static void view(SparkOnCosmosADLAccountNode target, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(()-> {
            AzureSparkServerlessAccount remote = target.getRemote();
            // check if the requested job list tab exists in tool window
            AzureSparkServerlessAccount account = remote;
            ToolWindow toolWindow = ToolWindowManager.getInstance(e.getProject()).getToolWindow("Cosmos Serverless Spark Jobs");
            toolWindow.setAvailable(true);
            Content existingContent = toolWindow.getContentManager().findContent(getDisplayName(target.getName()));
            if (existingContent != null) {
                // if the requested job list tab already exists in tool window,
                // show the existing job list tab
                toolWindow.getContentManager().setSelectedContent(existingContent);
                toolWindow.activate(null);
            } else {
                // create a new tab if the requested job list tab does not exists
                account.getSparkBatchJobList()
                        .subscribe(sparkBatchJobList -> {
                            // show serverless spark job list
                            CosmosServerlessSparkBatchJobsViewer jobView = new CosmosServerlessSparkBatchJobsViewer(account) {
                                @Override
                                public void refreshActionPerformed(@Nullable AnActionEvent anActionEvent) {
                                    Operation operation = TelemetryManager.createOperation(
                                            TelemetryConstants.SPARK_ON_COSMOS_SERVERLESS, TelemetryConstants.REFRESH_JOB_VIEW_TABLE);
                                    operation.start();
                                    account.getSparkBatchJobList()
                                            .doOnNext(jobList -> {
                                                LivyBatchJobViewer.Model refreshedModel =
                                                        new LivyBatchJobViewer.Model(
                                                                new LivyBatchJobTableViewport.Model(
                                                                        new LivyBatchJobTableModel(new CosmosServerlessSparkBatchJobsTableSchema()),
                                                                        getFirstJobPage(account, jobList)),
                                                                null
                                                        );
                                                this.setData(refreshedModel);
                                            })
                                            .subscribe(
                                                    jobList -> {
                                                    },
                                                    ex -> {
                                                        EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.serviceError, ex,
                                                                ImmutableMap.of("isRefreshJobsTableSucceed", "false"), null);
                                                    },
                                                    () -> EventUtil.logEventWithComplete(EventType.info, operation,
                                                            ImmutableMap.of("isRefreshJobsTableSucceed", "true"), null)
                                            );
                                }
                            };
                            LivyBatchJobViewer.Model model =
                                    new LivyBatchJobViewer.Model(
                                            new LivyBatchJobTableViewport.Model(
                                                    new LivyBatchJobTableModel(new CosmosServerlessSparkBatchJobsTableSchema()),
                                                    getFirstJobPage(account, sparkBatchJobList)),
                                            null
                                    );
                            jobView.setData(model);

                            ContentFactory contentFactory = ContentFactory.getInstance();
                            Content content = contentFactory.createContent(jobView.getComponent(), getDisplayName(account.getName()), false);
                            content.setDisposer(jobView);
                            toolWindow.getContentManager().addContent(content);
                            toolWindow.getContentManager().setSelectedContent(content);
                            toolWindow.activate(null);
                        });
            }
        });
    }

    @NotNull
    private static String getDisplayName(@NotNull String adlAccountName) {
        return adlAccountName + " Jobs";
    }

    @NotNull
    private static LivyBatchJobTableModel.JobPage getFirstJobPage(@NotNull AzureSparkServerlessAccount account,
                                                           @NotNull SparkBatchJobList jobList) {
        return new LivyBatchJobTableModel.JobPage() {
            @Nullable
            @Override
            public List<UniqueColumnNameTableSchema.RowDescriptor> items() {
                CosmosServerlessSparkBatchJobsTableSchema tableSchema = new CosmosServerlessSparkBatchJobsTableSchema();
                return jobList.value().stream()
                        .sorted((job1, job2) -> job1.state().compareTo(job2.state()) != 0
                                // sort by job state in ascending order
                                ? job1.state().compareTo(job2.state())
                                // then sort by submit time in descending order
                                : -job1.submitTime().compareTo(job2.submitTime()))
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
